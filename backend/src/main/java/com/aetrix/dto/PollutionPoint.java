package com.aetrix.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollutionPoint {
    private String pointId;
    private Double lat;
    private Double lng;
    private Double riskScore;
    private String riskCategory;
    private String colorHex;
    private Boolean isExtremeOutlier;
    private Double lstCelsius;
    private Double ndviSentinel;
    private Double smSurface;
}
