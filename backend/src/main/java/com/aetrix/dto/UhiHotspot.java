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
public class UhiHotspot {
    private Long id;
    private String zoneName;
    private Double avgLstCelsius;
    private Double maxLstCelsius;
    private Double minLstCelsius;
    private Integer pointCount;
    private String severity;
    private Double centerLat;
    private Double centerLng;
    private List<List<Double>> boundaryPoints;
    private List<String> pointIds;
}
