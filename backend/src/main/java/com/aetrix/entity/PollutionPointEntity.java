package com.aetrix.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pollution_points", indexes = {
        @Index(name = "idx_poll_category", columnList = "risk_category"),
        @Index(name = "idx_poll_outlier", columnList = "is_extreme_outlier"),
        @Index(name = "idx_poll_city", columnList = "city")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollutionPointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city", length = 50, nullable = false)
    @Builder.Default
    private String city = "Ahmedabad";

    @Column(name = "point_id", length = 20)
    private String pointId;

    @Column(name = "lat")
    private Double lat;

    @Column(name = "lng")
    private Double lng;

    @Column(name = "risk_score")
    private Double riskScore;

    @Column(name = "risk_category", length = 20)
    private String riskCategory;

    @Column(name = "color_hex", length = 10)
    private String colorHex;

    @Column(name = "is_extreme_outlier")
    private Boolean isExtremeOutlier;

    @Column(name = "lst_celsius")
    private Double lstCelsius;

    @Column(name = "ndvi_sentinel")
    private Double ndviSentinel;

    @Column(name = "sm_surface")
    private Double smSurface;

    @Column(name = "ingested_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime ingestedAt;
}
