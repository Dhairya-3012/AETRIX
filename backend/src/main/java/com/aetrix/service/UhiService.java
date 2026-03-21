package com.aetrix.service;

import com.aetrix.dto.*;
import com.aetrix.entity.UhiHeatmapPointEntity;
import com.aetrix.entity.UhiHotspotEntity;
import com.aetrix.repository.UhiHeatmapRepository;
import com.aetrix.repository.UhiHotspotRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UhiService {

    private final UhiHeatmapRepository uhiHeatmapRepository;
    private final UhiHotspotRepository uhiHotspotRepository;
    private final GrokLLMService grokLLMService;
    private final ObjectMapper objectMapper;

    private static final String CITY = "Ahmedabad";
    private static final String DATA_SOURCE = "MODIS LST";

    @Cacheable("uhi-summary")
    public UhiHeatmapSummary getSummary() {
        long total = uhiHeatmapRepository.count();
        long anomalyCount = uhiHeatmapRepository.countByIsAnomalyTrue();
        Double meanLst = uhiHeatmapRepository.findCityMeanLst();
        Double maxLst = uhiHeatmapRepository.findCityMaxLst();
        Double minLst = uhiHeatmapRepository.findCityMinLst();

        long criticalCount = uhiHeatmapRepository.countBySeverity("critical");
        long highCount = uhiHeatmapRepository.countBySeverity("high");
        long moderateCount = uhiHeatmapRepository.countBySeverity("moderate");

        return UhiHeatmapSummary.builder()
                .totalPoints((int) total)
                .anomalyCount((int) anomalyCount)
                .anomalyPercentage(total > 0 ? (anomalyCount * 100.0 / total) : 0.0)
                .cityMeanLst(meanLst)
                .cityMaxLst(maxLst)
                .cityMinLst(minLst)
                .criticalCount((int) criticalCount)
                .highCount((int) highCount)
                .moderateCount((int) moderateCount)
                .dataSource(DATA_SOURCE)
                .city(CITY)
                .build();
    }

    @Cacheable("uhi-heatmap")
    public List<UhiHeatmapPoint> getHeatmap() {
        return uhiHeatmapRepository.findAll().stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    @Cacheable("uhi-hotspots")
    public List<UhiHotspot> getHotspots() {
        return uhiHotspotRepository.findAllByOrderByAvgLstCelsiusDesc().stream()
                .map(this::hotspotEntityToDto)
                .collect(Collectors.toList());
    }

    public UhiHotspot getHotspotById(Long id) {
        return uhiHotspotRepository.findById(id)
                .map(this::hotspotEntityToDto)
                .orElse(null);
    }

    public List<UhiHeatmapPoint> getAnomalies() {
        return uhiHeatmapRepository.findByIsAnomalyTrue().stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    public GrokSummaryResponse getAiSummary() {
        UhiHeatmapSummary summary = getSummary();
        List<UhiHotspot> hotspots = getHotspots();
        String prompt = grokLLMService.buildUhiPrompt(CITY, summary, hotspots);
        return grokLLMService.getSummary("uhi", prompt);
    }

    public GrokSummaryResponse regenerateAiSummary() {
        UhiHeatmapSummary summary = getSummary();
        List<UhiHotspot> hotspots = getHotspots();
        String prompt = grokLLMService.buildUhiPrompt(CITY, summary, hotspots);
        return grokLLMService.regenerateSummary("uhi", prompt);
    }

    private UhiHeatmapPoint entityToDto(UhiHeatmapPointEntity entity) {
        return UhiHeatmapPoint.builder()
                .pointId(entity.getPointId())
                .lat(entity.getLat())
                .lng(entity.getLng())
                .lstCelsius(entity.getLstCelsius())
                .isAnomaly(entity.getIsAnomaly())
                .severity(entity.getSeverity())
                .anomalyScore(entity.getAnomalyScore())
                .ndvi(entity.getNdvi())
                .soilMoisture(entity.getSoilMoisture())
                .build();
    }

    private UhiHotspot hotspotEntityToDto(UhiHotspotEntity entity) {
        List<List<Double>> boundaryPoints = new ArrayList<>();
        List<String> pointIds = new ArrayList<>();

        try {
            if (entity.getBoundaryPoints() != null) {
                boundaryPoints = objectMapper.readValue(entity.getBoundaryPoints(),
                        new TypeReference<List<List<Double>>>() {});
            }
            if (entity.getPointIds() != null) {
                pointIds = objectMapper.readValue(entity.getPointIds(),
                        new TypeReference<List<String>>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to parse JSON fields for hotspot: {}", entity.getZoneName());
        }

        return UhiHotspot.builder()
                .id(entity.getId())
                .zoneName(entity.getZoneName())
                .avgLstCelsius(entity.getAvgLstCelsius())
                .maxLstCelsius(entity.getMaxLstCelsius())
                .minLstCelsius(entity.getMinLstCelsius())
                .pointCount(entity.getPointCount())
                .severity(entity.getSeverity())
                .centerLat(entity.getCenterLat())
                .centerLng(entity.getCenterLng())
                .boundaryPoints(boundaryPoints)
                .pointIds(pointIds)
                .build();
    }
}
