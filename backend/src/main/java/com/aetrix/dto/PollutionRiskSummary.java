package com.aetrix.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollutionRiskSummary {
    private Integer totalPoints;
    private Double cityMeanRisk;
    private Integer criticalCount;
    private Double criticalPercentage;
    private Integer highCount;
    private Double highPercentage;
    private Integer mediumCount;
    private Double mediumPercentage;
    private Integer lowCount;
    private Double lowPercentage;
    private Integer extremeOutlierCount;
    private String dataSource;
    private String city;
}
