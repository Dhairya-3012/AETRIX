package com.aetrix.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VegetationPoint {
    private String pointId;
    private Double lat;
    private Double lng;
    private Double ndviSentinel;
    private Double ndviLandsat;
    private String healthLabel;
    private Double healthScore;
    private String colorHex;
    private Double lstCelsius;
    private Double smSurface;
}
