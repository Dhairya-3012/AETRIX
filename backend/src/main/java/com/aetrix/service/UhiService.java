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

    private static final String DEFAULT_CITY = "Ahmedabad";
    private static final String DATA_SOURCE = "MODIS LST";

    // Legacy method for backward compatibility
    public UhiHeatmapSummary getSummary() {
        return getSummary(DEFAULT_CITY);
    }

    @Cacheable(value = "uhi-summary", key = "#city")
    public UhiHeatmapSummary getSummary(String city) {
        String targetCity = city != null ? city : DEFAULT_CITY;

        long total = uhiHeatmapRepository.countByCity(targetCity);
        long anomalyCount = uhiHeatmapRepository.countByCityAndIsAnomalyTrue(targetCity);
        Double meanLst = uhiHeatmapRepository.findCityMeanLst(targetCity);
        Double maxLst = uhiHeatmapRepository.findCityMaxLst(targetCity);
        Double minLst = uhiHeatmapRepository.findCityMinLst(targetCity);

        long criticalCount = uhiHeatmapRepository.countByCityAndSeverity(targetCity, "critical");
        long highCount = uhiHeatmapRepository.countByCityAndSeverity(targetCity, "high");
        long moderateCount = uhiHeatmapRepository.countByCityAndSeverity(targetCity, "moderate");

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
                .city(targetCity)
                .build();
    }

    // Legacy method
    public List<UhiHeatmapPoint> getHeatmap() {
        return getHeatmap(DEFAULT_CITY);
    }

    @Cacheable(value = "uhi-heatmap", key = "#city")
    public List<UhiHeatmapPoint> getHeatmap(String city) {
        String targetCity = city != null ? city : DEFAULT_CITY;
        return uhiHeatmapRepository.findByCity(targetCity).stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    // Legacy method
    public List<UhiHotspot> getHotspots() {
        return getHotspots(DEFAULT_CITY);
    }

    @Cacheable(value = "uhi-hotspots", key = "#city")
    public List<UhiHotspot> getHotspots(String city) {
        String targetCity = city != null ? city : DEFAULT_CITY;
        return uhiHotspotRepository.findByCityOrderByAvgLstCelsiusDesc(targetCity).stream()
                .map(this::hotspotEntityToDto)
                .collect(Collectors.toList());
    }

    public UhiHotspot getHotspotById(Long id) {
        return uhiHotspotRepository.findById(id)
                .map(this::hotspotEntityToDto)
                .orElse(null);
    }

    // Legacy method
    public List<UhiHeatmapPoint> getAnomalies() {
        return getAnomalies(DEFAULT_CITY);
    }

    public List<UhiHeatmapPoint> getAnomalies(String city) {
        String targetCity = city != null ? city : DEFAULT_CITY;
        return uhiHeatmapRepository.findByCityAndIsAnomalyTrue(targetCity).stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    // Legacy method
    public GrokSummaryResponse getAiSummary() {
        return getAiSummary(DEFAULT_CITY);
    }

    public GrokSummaryResponse getAiSummary(String city) {
        String targetCity = city != null ? city : DEFAULT_CITY;
        UhiHeatmapSummary summary = getSummary(targetCity);
        List<UhiHotspot> hotspots = getHotspots(targetCity);
        String prompt = grokLLMService.buildUhiPrompt(targetCity, summary, hotspots);
        return grokLLMService.getSummary("uhi", targetCity, prompt);
    }

    // Legacy method
    public GrokSummaryResponse regenerateAiSummary() {
        return regenerateAiSummary(DEFAULT_CITY);
    }

    public GrokSummaryResponse regenerateAiSummary(String city) {
        String targetCity = city != null ? city : DEFAULT_CITY;
        UhiHeatmapSummary summary = getSummary(targetCity);
        List<UhiHotspot> hotspots = getHotspots(targetCity);
        String prompt = grokLLMService.buildUhiPrompt(targetCity, summary, hotspots);
        return grokLLMService.regenerateSummary("uhi", targetCity, prompt);
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
