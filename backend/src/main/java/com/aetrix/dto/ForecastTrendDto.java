package com.aetrix.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForecastTrendDto {
    private List<ForecastStep> historical;
    private List<ForecastStep> predicted;
    private String trendDirection;
    private Double trendRate;
    private Double predictedMean;
    private Double predictedMin;
    private Double predictedMax;
    private String modelType;
    private String city;
}
