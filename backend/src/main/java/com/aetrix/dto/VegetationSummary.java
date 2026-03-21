package com.aetrix.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VegetationSummary {
    private Integer totalPoints;
    private Double cityMeanNdvi;
    private Integer healthyCount;
    private Double healthyPercentage;
    private Integer stressedCount;
    private Double stressedPercentage;
    private Integer barrenCount;
    private Double barrenPercentage;
    private Integer alertCount;
    private String dataSource;
    private String city;
}
