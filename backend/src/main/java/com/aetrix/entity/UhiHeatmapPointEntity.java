package com.aetrix.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "uhi_heatmap_points", indexes = {
        @Index(name = "idx_uhi_anomaly", columnList = "is_anomaly"),
        @Index(name = "idx_uhi_severity", columnList = "severity")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UhiHeatmapPointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "point_id", length = 20)
    private String pointId;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lng")
    private Double lng;

    @Column(name = "lst_celsius")
    private Double lstCelsius;

    @Column(name = "is_anomaly")
    private Boolean isAnomaly;

    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "anomaly_score")
    private Double anomalyScore;

    @Column(name = "ndvi")
    private Double ndvi;

    @Column(name = "soil_moisture")
    private Double soilMoisture;

    @Column(name = "ingested_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime ingestedAt;
}
