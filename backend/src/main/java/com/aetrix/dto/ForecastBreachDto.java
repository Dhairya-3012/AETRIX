package com.aetrix.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastBreachDto {
    private Double dangerThreshold;
    private Integer breachCount;
    private Double breachPercentage;
    private Integer totalPoints;
    private String riskLevel;
    private String whatIfWarmer;
    private String whatIfCooler;
    private String city;
}
