package com.aetrix.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "vegetation_points", indexes = {
        @Index(name = "idx_veg_health", columnList = "health_label")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VegetationPointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "point_id", length = 20)
    private String pointId;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lng")
    private Double lng;

    @Column(name = "ndvi_sentinel")
    private Double ndviSentinel;

    @Column(name = "ndvi_landsat")
    private Double ndviLandsat;

    @Column(name = "health_label", length = 20)
    private String healthLabel;

    @Column(name = "health_score")
    private Double healthScore;

    @Column(name = "color_hex", length = 10)
    private String colorHex;

    @Column(name = "lst_celsius")
    private Double lstCelsius;

    @Column(name = "sm_surface")
    private Double smSurface;

    @Column(name = "ingested_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime ingestedAt;
}
