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

    private static final String DEFAULT_CITY = "Ahmedabad";
    private static final String DATA_SOURCE = "Sentinel-2 NDVI, Landsat NDVI";

    // Legacy method for backward compatibility
    public VegetationSummary getSummary() {
        return getSummary(DEFAULT_CITY);
    }

    @Cacheable(value = "vegetation-summary", key = "#city")
    public VegetationSummary getSummary(String city) {
        String targetCity = city != null ? city : DEFAULT_CITY;

        long total = vegetationPointRepository.countByCity(targetCity);
        long healthyCount = vegetationPointRepository.countByCityAndHealthLabel(targetCity, "Healthy");
        long stressedCount = vegetationPointRepository.countByCityAndHealthLabel(targetCity, "Stressed");
        long barrenCount = vegetationPointRepository.countByCityAndHealthLabel(targetCity, "Barren");
        long alertCount = vegetationAlertRepository.countByCity(targetCity);
        Double meanNdvi = vegetationPointRepository.findCityMeanNdvi(targetCity);

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
                .city(targetCity)
                .build();
    }

    // Legacy method
    public List<VegetationPoint> getMap() {
        return getMap(DEFAULT_CITY);
    }

    @Cacheable(value = "vegetation-map", key = "#city")
    public List<VegetationPoint> getMap(String city) {
        String targetCity = city != null ? city : DEFAULT_CITY;
        return vegetationPointRepository.findByCity(targetCity).stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    // Legacy method
    public List<VegetationAlert> getAlerts() {
        return getAlerts(DEFAULT_CITY);
    }

    @Cacheable(value = "vegetation-alerts", key = "#city")
    public List<VegetationAlert> getAlerts(String city) {
        String targetCity = city != null ? city : DEFAULT_CITY;
        return vegetationAlertRepository.findByCityOrderByZScoreDesc(targetCity).stream()
                .map(this::alertEntityToDto)
                .collect(Collectors.toList());
    }

    // Legacy method
    public List<VegetationPoint> getPlantationRecommendations() {
        return getPlantationRecommendations(DEFAULT_CITY);
    }

    public List<VegetationPoint> getPlantationRecommendations(String city) {
        String targetCity = city != null ? city : DEFAULT_CITY;
        return vegetationPointRepository.findByCityOrderByNdviAsc(targetCity).stream()
                .filter(p -> "Stressed".equals(p.getHealthLabel()) || "Barren".equals(p.getHealthLabel()))
                .limit(20)
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    // Legacy method
    public GrokSummaryResponse getAiSummary() {
        return getAiSummary(DEFAULT_CITY);
    }

    public GrokSummaryResponse getAiSummary(String city) {
        String targetCity = city != null ? city : DEFAULT_CITY;
        VegetationSummary summary = getSummary(targetCity);
        List<VegetationAlert> alerts = getAlerts(targetCity);
        String prompt = grokLLMService.buildVegetationPrompt(targetCity, summary, alerts);
        return grokLLMService.getSummary("vegetation", targetCity, prompt);
    }

    // Legacy method
    public GrokSummaryResponse regenerateAiSummary() {
        return regenerateAiSummary(DEFAULT_CITY);
    }

    public GrokSummaryResponse regenerateAiSummary(String city) {
        String targetCity = city != null ? city : DEFAULT_CITY;
        VegetationSummary summary = getSummary(targetCity);
        List<VegetationAlert> alerts = getAlerts(targetCity);
        String prompt = grokLLMService.buildVegetationPrompt(targetCity, summary, alerts);
        return grokLLMService.regenerateSummary("vegetation", targetCity, prompt);
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
