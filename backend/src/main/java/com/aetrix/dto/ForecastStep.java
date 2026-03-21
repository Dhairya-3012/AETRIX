package com.aetrix.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastStep {
    private Integer step;
    private Double valueCelsius;
    private String stepType;
    private Double predictedCelsius;
    private Double lowerBound;
    private Double upperBound;
}
