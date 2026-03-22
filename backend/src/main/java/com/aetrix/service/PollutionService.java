package com.aetrix.service;

import com.aetrix.dto.*;
import com.aetrix.entity.PollutionHotspotEntity;
import com.aetrix.entity.PollutionPointEntity;
import com.aetrix.repository.PollutionHotspotRepository;
import com.aetrix.repository.PollutionPointRepository;
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
public class PollutionService {

    private final PollutionPointRepository pollutionPointRepository;
    private final PollutionHotspotRepository pollutionHotspotRepository;
    private final GrokLLMService grokLLMService;
    private final ObjectMapper objectMapper;

    private static final String DEFAULT_CITY = "Ahmedabad";
    private static final String DATA_SOURCE = "Multi-satellite Composite Analysis";

    // Legacy method for backward compatibility
    public PollutionRiskSummary getSummary() {
        return getSummary(DEFAULT_CITY);
    }

    @Cacheable(value = "pollution-summary", key = "#city")
    public PollutionRiskSummary getSummary(String city) {
        String targetCity = city != null ? city : DEFAULT_CITY;

        long total = pollutionPointRepository.countByCity(targetCity);
        long criticalCount = pollutionPointRepository.countByCityAndRiskCategory(targetCity, "Critical");
        long highCount = pollutionPointRepository.countByCityAndRiskCategory(targetCity, "High");
        long mediumCount = pollutionPointRepository.countByCityAndRiskCategory(targetCity, "Medium");
        long lowCount = pollutionPointRepository.countByCityAndRiskCategory(targetCity, "Low");
        long outlierCount = pollutionPointRepository.countByCityAndIsExtremeOutlierTrue(targetCity);
        Double meanRisk = pollutionPointRepository.findCityMeanRisk(targetCity);

        return PollutionRiskSummary.builder()
                .totalPoints((int) total)
                .cityMeanRisk(meanRisk)
                .criticalCount((int) criticalCount)
                .criticalPercentage(total > 0 ? (criticalCount * 100.0 / total) : 0.0)
                .highCount((int) highCount)
                .highPercentage(total > 0 ? (highCount * 100.0 / total) : 0.0)
                .mediumCount((int) mediumCount)
                .mediumPercentage(total > 0 ? (mediumCount * 100.0 / total) : 0.0)
                .lowCount((int) lowCount)
                .lowPercentage(total > 0 ? (lowCount * 100.0 / total) : 0.0)
                .extremeOutlierCount((int) outlierCount)
                .dataSource(DATA_SOURCE)
                .city(targetCity)
                .build();
    }

    // Legacy method
    public List<PollutionPoint> getMap() {
        return getMap(DEFAULT_CITY);
    }

    @Cacheable(value = "pollution-map", key = "#city")
    public List<PollutionPoint> getMap(String city) {
        String targetCity = city != null ? city : DEFAULT_CITY;
        return pollutionPointRepository.findByCity(targetCity).stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    // Legacy method
    public List<PollutionHotspot> getHotspots() {
        return getHotspots(DEFAULT_CITY);
    }

    @Cacheable(value = "pollution-hotspots", key = "#city")
    public List<PollutionHotspot> getHotspots(String city) {
        String targetCity = city != null ? city : DEFAULT_CITY;
        return pollutionHotspotRepository.findByCityOrderByAvgRiskScoreDesc(targetCity).stream()
                .map(this::hotspotEntityToDto)
                .collect(Collectors.toList());
    }

    // Legacy method
    public List<PollutionPoint> getOutliers() {
        return getOutliers(DEFAULT_CITY);
    }

    public List<PollutionPoint> getOutliers(String city) {
        String targetCity = city != null ? city : DEFAULT_CITY;
        return pollutionPointRepository.findByCityAndIsExtremeOutlierTrue(targetCity).stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    // Legacy method
    public List<PollutionPoint> getCompliance() {
        return getCompliance(DEFAULT_CITY);
    }

    public List<PollutionPoint> getCompliance(String city) {
        String targetCity = city != null ? city : DEFAULT_CITY;
        return pollutionPointRepository.findByCityOrderByRiskScoreDesc(targetCity).stream()
                .filter(p -> "Critical".equals(p.getRiskCategory()) || "High".equals(p.getRiskCategory()))
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    // Legacy method
    public GrokSummaryResponse getAiSummary() {
        return getAiSummary(DEFAULT_CITY);
    }

    public GrokSummaryResponse getAiSummary(String city) {
        String targetCity = city != null ? city : DEFAULT_CITY;
        PollutionRiskSummary summary = getSummary(targetCity);
        List<PollutionHotspot> hotspots = getHotspots(targetCity);
        String prompt = grokLLMService.buildPollutionPrompt(targetCity, summary, hotspots);
        return grokLLMService.getSummary("pollution", targetCity, prompt);
    }

    // Legacy method
    public GrokSummaryResponse regenerateAiSummary() {
        return regenerateAiSummary(DEFAULT_CITY);
    }

    public GrokSummaryResponse regenerateAiSummary(String city) {
        String targetCity = city != null ? city : DEFAULT_CITY;
        PollutionRiskSummary summary = getSummary(targetCity);
        List<PollutionHotspot> hotspots = getHotspots(targetCity);
        String prompt = grokLLMService.buildPollutionPrompt(targetCity, summary, hotspots);
        return grokLLMService.regenerateSummary("pollution", targetCity, prompt);
    }

    private PollutionPoint entityToDto(PollutionPointEntity entity) {
        return PollutionPoint.builder()
                .pointId(entity.getPointId())
                .lat(entity.getLat())
                .lng(entity.getLng())
                .riskScore(entity.getRiskScore())
                .riskCategory(entity.getRiskCategory())
                .colorHex(entity.getColorHex())
                .isExtremeOutlier(entity.getIsExtremeOutlier())
                .lstCelsius(entity.getLstCelsius())
                .ndviSentinel(entity.getNdviSentinel())
                .smSurface(entity.getSmSurface())
                .build();
    }

    private PollutionHotspot hotspotEntityToDto(PollutionHotspotEntity entity) {
        List<List<Double>> boundaryPoints = new ArrayList<>();

        try {
            if (entity.getBoundaryPoints() != null) {
                boundaryPoints = objectMapper.readValue(entity.getBoundaryPoints(),
                        new TypeReference<List<List<Double>>>() {});
            }
        } catch (Exception e) {
            log.warn("Failed to parse boundary points for hotspot: {}", entity.getZoneName());
        }

        return PollutionHotspot.builder()
                .id(entity.getId())
                .zoneName(entity.getZoneName())
                .avgRiskScore(entity.getAvgRiskScore())
                .maxRiskScore(entity.getMaxRiskScore())
                .pointCount(entity.getPointCount())
                .severity(entity.getSeverity())
                .recommendedAction(entity.getRecommendedAction())
                .responsibleDept(entity.getResponsibleDept())
                .centerLat(entity.getCenterLat())
                .centerLng(entity.getCenterLng())
                .boundaryPoints(boundaryPoints)
                .build();
    }
}
