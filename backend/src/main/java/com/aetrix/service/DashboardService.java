package com.aetrix.service;

import com.aetrix.dto.*;
import com.aetrix.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

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

    private static final String CITY = "Ahmedabad";
    private static final String STATE = "Gujarat";
    private static final String COUNTRY = "India";

    @Cacheable("dashboard-overview")
    public DashboardOverview getOverview() {
        // UHI Data
        Double cityMeanLst = uhiHeatmapRepository.findCityMeanLst();
        Double cityMaxLst = uhiHeatmapRepository.findCityMaxLst();
        int uhiAnomalyCount = (int) uhiHeatmapRepository.countByIsAnomalyTrue();
        int uhiTotalPoints = (int) uhiHeatmapRepository.count();

        // Vegetation Data
        Double cityMeanNdvi = vegetationPointRepository.findCityMeanNdvi();
        int healthyCount = (int) vegetationPointRepository.countByHealthLabel("Healthy");
        int stressedCount = (int) vegetationPointRepository.countByHealthLabel("Stressed");
        int alertCount = (int) vegetationAlertRepository.count();

        // Pollution Data
        Double cityMeanRisk = pollutionPointRepository.findCityMeanRisk();
        int criticalCount = (int) pollutionPointRepository.countByRiskCategory("Critical");
        int highCount = (int) pollutionPointRepository.countByRiskCategory("High");
        int outlierCount = (int) pollutionPointRepository.countByIsExtremeOutlierTrue();

        // Forecast Data
        ForecastTrendDto trend = forecastService.getTrend();
        ForecastBreachDto breach = forecastService.getBreach();

        // Action Data
        int totalActions = (int) actionItemRepository.count();
        int pendingActions = (int) actionItemRepository.countByStatus("pending");
        int highPriorityActions = (int) actionItemRepository.countByPriority("high");

        // Total data points
        int totalDataPoints = uhiTotalPoints +
                (int) vegetationPointRepository.count() +
                (int) pollutionPointRepository.count() +
                (int) forecastStepRepository.count();

        // Determine overall trend
        String overallTrend = calculateOverallTrend(criticalCount, highCount, uhiAnomalyCount, stressedCount);
        String trendDescription = getTrendDescription(overallTrend);

        return DashboardOverview.builder()
                .city(CITY)
                .state(STATE)
                .country(COUNTRY)
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
}
