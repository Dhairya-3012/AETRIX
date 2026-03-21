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
public class PollutionHotspot {
    private Long id;
    private String zoneName;
    private Double avgRiskScore;
    private Double maxRiskScore;
    private Integer pointCount;
    private String severity;
    private String recommendedAction;
    private String responsibleDept;
    private Double centerLat;
    private Double centerLng;
    private List<List<Double>> boundaryPoints;
}
