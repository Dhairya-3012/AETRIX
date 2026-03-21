package com.aetrix.service;

import com.aetrix.dto.*;
import com.aetrix.entity.VegetationAlertEntity;
import com.aetrix.entity.VegetationPointEntity;
import com.aetrix.repository.VegetationAlertRepository;
import com.aetrix.repository.VegetationPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VegetationService {

    private final VegetationPointRepository vegetationPointRepository;
    private final VegetationAlertRepository vegetationAlertRepository;
    private final GrokLLMService grokLLMService;

    private static final String CITY = "Ahmedabad";
    private static final String DATA_SOURCE = "Sentinel-2 NDVI, Landsat NDVI";

    @Cacheable("vegetation-summary")
    public VegetationSummary getSummary() {
        long total = vegetationPointRepository.count();
        long healthyCount = vegetationPointRepository.countByHealthLabel("Healthy");
        long stressedCount = vegetationPointRepository.countByHealthLabel("Stressed");
        long barrenCount = vegetationPointRepository.countByHealthLabel("Barren");
        long alertCount = vegetationAlertRepository.count();
        Double meanNdvi = vegetationPointRepository.findCityMeanNdvi();

        return VegetationSummary.builder()
                .totalPoints((int) total)
                .cityMeanNdvi(meanNdvi)
                .healthyCount((int) healthyCount)
                .healthyPercentage(total > 0 ? (healthyCount * 100.0 / total) : 0.0)
                .stressedCount((int) stressedCount)
                .stressedPercentage(total > 0 ? (stressedCount * 100.0 / total) : 0.0)
                .barrenCount((int) barrenCount)
                .barrenPercentage(total > 0 ? (barrenCount * 100.0 / total) : 0.0)
                .alertCount((int) alertCount)
                .dataSource(DATA_SOURCE)
                .city(CITY)
                .build();
    }

    @Cacheable("vegetation-map")
    public List<VegetationPoint> getMap() {
        return vegetationPointRepository.findAll().stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    @Cacheable("vegetation-alerts")
    public List<VegetationAlert> getAlerts() {
        return vegetationAlertRepository.findAllOrderByZScoreDesc().stream()
                .map(this::alertEntityToDto)
                .collect(Collectors.toList());
    }

    public List<VegetationPoint> getPlantationRecommendations() {
        // Return stressed and barren areas as plantation recommendations
        return vegetationPointRepository.findAllOrderByNdviAsc().stream()
                .filter(p -> "Stressed".equals(p.getHealthLabel()) || "Barren".equals(p.getHealthLabel()))
                .limit(20)
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    public GrokSummaryResponse getAiSummary() {
        VegetationSummary summary = getSummary();
        List<VegetationAlert> alerts = getAlerts();
        String prompt = grokLLMService.buildVegetationPrompt(CITY, summary, alerts);
        return grokLLMService.getSummary("vegetation", prompt);
    }

    public GrokSummaryResponse regenerateAiSummary() {
        VegetationSummary summary = getSummary();
        List<VegetationAlert> alerts = getAlerts();
        String prompt = grokLLMService.buildVegetationPrompt(CITY, summary, alerts);
        return grokLLMService.regenerateSummary("vegetation", prompt);
    }

    private VegetationPoint entityToDto(VegetationPointEntity entity) {
        return VegetationPoint.builder()
                .pointId(entity.getPointId())
                .lat(entity.getLat())
                .lng(entity.getLng())
                .ndviSentinel(entity.getNdviSentinel())
                .ndviLandsat(entity.getNdviLandsat())
                .healthLabel(entity.getHealthLabel())
                .healthScore(entity.getHealthScore())
                .colorHex(entity.getColorHex())
                .lstCelsius(entity.getLstCelsius())
                .smSurface(entity.getSmSurface())
                .build();
    }

    private VegetationAlert alertEntityToDto(VegetationAlertEntity entity) {
        return VegetationAlert.builder()
                .id(entity.getId())
                .pointId(entity.getPointId())
                .lat(entity.getLat())
                .lng(entity.getLng())
                .ndviSentinel(entity.getNdviSentinel())
                .zScore(entity.getZScore())
                .severity(entity.getSeverity())
                .message(entity.getMessage())
                .build();
    }
}
