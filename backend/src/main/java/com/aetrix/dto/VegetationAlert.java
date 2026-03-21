package com.aetrix.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VegetationAlert {
    private Long id;
    private String pointId;
    private Double lat;
    private Double lng;
    private Double ndviSentinel;
    private Double zScore;
    private String severity;
    private String message;
}
