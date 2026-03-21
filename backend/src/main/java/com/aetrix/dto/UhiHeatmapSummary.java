package com.aetrix.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UhiHeatmapSummary {
    private Integer totalPoints;
    private Integer anomalyCount;
    private Double anomalyPercentage;
    private Double cityMeanLst;
    private Double cityMaxLst;
    private Double cityMinLst;
    private Integer criticalCount;
    private Integer highCount;
    private Integer moderateCount;
    private String dataSource;
    private String city;
}
