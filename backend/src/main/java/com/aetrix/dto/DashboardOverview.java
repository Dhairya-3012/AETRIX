package com.aetrix.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverview {
    private String city;
    private String state;
    private String country;

    // UHI Summary
    private Double cityMeanLst;
    private Double cityMaxLst;
    private Integer uhiAnomalyCount;
    private Integer uhiTotalPoints;

    // Vegetation Summary
    private Double cityMeanNdvi;
    private Integer vegetationHealthyCount;
    private Integer vegetationStressedCount;
    private Integer vegetationAlertCount;

    // Pollution Summary
    private Double cityMeanRisk;
    private Integer pollutionCriticalCount;
    private Integer pollutionHighCount;
    private Integer pollutionOutlierCount;

    // Forecast Summary
    private String forecastTrend;
    private Double predictedMeanLst;
    private Integer breachCount;

    // Action Summary
    private Integer totalActions;
    private Integer pendingActions;
    private Integer highPriorityActions;

    // Data Info
    private Integer totalDataPoints;
    private List<String> dataSources;
    private LocalDateTime lastUpdated;

    // Trend Indicator
    private String overallTrend;
    private String trendDescription;
}
