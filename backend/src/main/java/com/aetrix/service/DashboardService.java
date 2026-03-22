package com.aetrix.service;

import com.aetrix.dto.*;
import com.aetrix.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final UhiHeatmapRepository uhiHeatmapRepository;
    private final UhiHotspotRepository uhiHotspotRepository;
    private final VegetationPointRepository vegetationPointRepository;
    private final VegetationAlertRepository vegetationAlertRepository;
    private final PollutionPointRepository pollutionPointRepository;
    private final ForecastStepRepository forecastStepRepository;
    private final ActionItemRepository actionItemRepository;
    private final ForecastService forecastService;

    private static final String DEFAULT_CITY = "Ahmedabad";
    private static final String COUNTRY = "India";

    private static final Map<String, CityInfo> CITY_INFO = new HashMap<>();

    static {
        CITY_INFO.put("Ahmedabad", new CityInfo("Gujarat", 23.022, 72.571));
        CITY_INFO.put("Bangalore", new CityInfo("Karnataka", 12.972, 77.594));
        CITY_INFO.put("Delhi", new CityInfo("Delhi", 28.644, 77.216));
        CITY_INFO.put("Mumbai", new CityInfo("Maharashtra", 19.076, 72.877));
    }

    public List<String> getSupportedCities() {
        return List.copyOf(CITY_INFO.keySet());
    }

    public CityInfo getCityInfo(String city) {
        return CITY_INFO.getOrDefault(city, CITY_INFO.get(DEFAULT_CITY));
    }

    @Cacheable(value = "dashboard-overview", key = "#city")
    public DashboardOverview getOverview(String city) {
        if (city == null || city.isBlank()) {
            city = DEFAULT_CITY;
        }

        CityInfo cityInfo = getCityInfo(city);

        // UHI Data
        Double cityMeanLst = uhiHeatmapRepository.findCityMeanLst(city);
        Double cityMaxLst = uhiHeatmapRepository.findCityMaxLst(city);
        int uhiAnomalyCount = (int) uhiHeatmapRepository.countByCityAndIsAnomalyTrue(city);
        int uhiTotalPoints = (int) uhiHeatmapRepository.countByCity(city);

        // Vegetation Data
        Double cityMeanNdvi = vegetationPointRepository.findCityMeanNdvi(city);
        int healthyCount = (int) vegetationPointRepository.countByCityAndHealthLabel(city, "Healthy");
        int stressedCount = (int) vegetationPointRepository.countByCityAndHealthLabel(city, "Stressed");
        int alertCount = (int) vegetationAlertRepository.countByCity(city);

        // Pollution Data
        Double cityMeanRisk = pollutionPointRepository.findCityMeanRisk(city);
        int criticalCount = (int) pollutionPointRepository.countByCityAndRiskCategory(city, "Critical");
        int highCount = (int) pollutionPointRepository.countByCityAndRiskCategory(city, "High");
        int outlierCount = (int) pollutionPointRepository.countByCityAndIsExtremeOutlierTrue(city);

        // Forecast Data
        ForecastTrendDto trend = forecastService.getTrend(city);
        ForecastBreachDto breach = forecastService.getBreach(city);

        // Action Data
        int totalActions = (int) actionItemRepository.countByCity(city);
        int pendingActions = (int) actionItemRepository.countByCityAndStatus(city, "pending");
        int highPriorityActions = (int) actionItemRepository.countByCityAndPriority(city, "high");

        // Total data points
        int totalDataPoints = uhiTotalPoints +
                (int) vegetationPointRepository.countByCity(city) +
                (int) pollutionPointRepository.countByCity(city) +
                (int) forecastStepRepository.countByCity(city);

        // Determine overall trend
        String overallTrend = calculateOverallTrend(criticalCount, highCount, uhiAnomalyCount, stressedCount);
        String trendDescription = getTrendDescription(overallTrend);

        return DashboardOverview.builder()
                .city(city)
                .state(cityInfo.state())
                .country(COUNTRY)
                .lat(cityInfo.lat())
                .lng(cityInfo.lng())
                .cityMeanLst(cityMeanLst)
                .cityMaxLst(cityMaxLst)
                .uhiAnomalyCount(uhiAnomalyCount)
                .uhiTotalPoints(uhiTotalPoints)
                .cityMeanNdvi(cityMeanNdvi)
                .vegetationHealthyCount(healthyCount)
                .vegetationStressedCount(stressedCount)
                .vegetationAlertCount(alertCount)
                .cityMeanRisk(cityMeanRisk)
                .pollutionCriticalCount(criticalCount)
                .pollutionHighCount(highCount)
                .pollutionOutlierCount(outlierCount)
                .forecastTrend(trend.getTrendDirection())
                .predictedMeanLst(trend.getPredictedMean())
                .breachCount(breach.getBreachCount())
                .totalActions(totalActions)
                .pendingActions(pendingActions)
                .highPriorityActions(highPriorityActions)
                .totalDataPoints(totalDataPoints)
                .dataSources(Arrays.asList("MODIS LST", "Sentinel-2 NDVI", "Landsat NDVI", "SMAP Soil Moisture"))
                .lastUpdated(LocalDateTime.now())
                .overallTrend(overallTrend)
                .trendDescription(trendDescription)
                .build();
    }

    // Legacy method for backward compatibility
    public DashboardOverview getOverview() {
        return getOverview(DEFAULT_CITY);
    }

    private String calculateOverallTrend(int criticalCount, int highCount, int anomalyCount, int stressedCount) {
        int criticalScore = criticalCount * 4 + highCount * 2 + anomalyCount + stressedCount;

        if (criticalScore > 100) return "critical";
        if (criticalScore > 50) return "degrading";
        if (criticalScore > 20) return "stable";
        return "improving";
    }

    private String getTrendDescription(String trend) {
        return switch (trend) {
            case "critical" -> "Multiple environmental indicators require immediate attention";
            case "degrading" -> "Environmental conditions show signs of decline";
            case "stable" -> "Environmental indicators within acceptable ranges";
            case "improving" -> "Positive trends observed across environmental metrics";
            default -> "Analysis in progress";
        };
    }

    public record CityInfo(String state, double lat, double lng) {}
}
