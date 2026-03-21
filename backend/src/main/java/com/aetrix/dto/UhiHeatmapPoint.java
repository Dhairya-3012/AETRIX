package com.aetrix.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UhiHeatmapPoint {
    private String pointId;
    private Double lat;
    private Double lng;
    private Double lstCelsius;
    private Boolean isAnomaly;
    private String severity;
    private Double anomalyScore;
    private Double ndvi;
    private Double soilMoisture;
}
