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

    private static final String CITY = "Ahmedabad";
    private static final String DATA_SOURCE = "Multi-satellite Composite Analysis";

    @Cacheable("pollution-summary")
    public PollutionRiskSummary getSummary() {
        long total = pollutionPointRepository.count();
        long criticalCount = pollutionPointRepository.countByRiskCategory("Critical");
        long highCount = pollutionPointRepository.countByRiskCategory("High");
        long mediumCount = pollutionPointRepository.countByRiskCategory("Medium");
        long lowCount = pollutionPointRepository.countByRiskCategory("Low");
        long outlierCount = pollutionPointRepository.countByIsExtremeOutlierTrue();
        Double meanRisk = pollutionPointRepository.findCityMeanRisk();

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
                .city(CITY)
                .build();
    }

    @Cacheable("pollution-map")
    public List<PollutionPoint> getMap() {
        return pollutionPointRepository.findAll().stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    @Cacheable("pollution-hotspots")
    public List<PollutionHotspot> getHotspots() {
        return pollutionHotspotRepository.findAllByOrderByAvgRiskScoreDesc().stream()
                .map(this::hotspotEntityToDto)
                .collect(Collectors.toList());
    }

    public List<PollutionPoint> getOutliers() {
        return pollutionPointRepository.findByIsExtremeOutlierTrue().stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    public List<PollutionPoint> getCompliance() {
        // Return points that need compliance attention (critical and high risk)
        return pollutionPointRepository.findAllOrderByRiskScoreDesc().stream()
                .filter(p -> "Critical".equals(p.getRiskCategory()) || "High".equals(p.getRiskCategory()))
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    public GrokSummaryResponse getAiSummary() {
        PollutionRiskSummary summary = getSummary();
        List<PollutionHotspot> hotspots = getHotspots();
        String prompt = grokLLMService.buildPollutionPrompt(CITY, summary, hotspots);
        return grokLLMService.getSummary("pollution", prompt);
    }

    public GrokSummaryResponse regenerateAiSummary() {
        PollutionRiskSummary summary = getSummary();
        List<PollutionHotspot> hotspots = getHotspots();
        String prompt = grokLLMService.buildPollutionPrompt(CITY, summary, hotspots);
        return grokLLMService.regenerateSummary("pollution", prompt);
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
