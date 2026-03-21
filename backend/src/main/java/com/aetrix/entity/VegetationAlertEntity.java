package com.aetrix.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "vegetation_alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VegetationAlertEntity {

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

    @Column(name = "z_score")  
    private Double zScore; 

    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "ingested_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime ingestedAt;
}
