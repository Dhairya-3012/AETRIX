package com.aetrix.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pollution_hotspots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PollutionHotspotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zone_name", length = 100)
    private String zoneName;

    @Column(name = "avg_risk_score")
    private Double avgRiskScore;

    @Column(name = "max_risk_score")
    private Double maxRiskScore;

    @Column(name = "point_count")
    private Integer pointCount;

    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "recommended_action", columnDefinition = "TEXT")
    private String recommendedAction;

    @Column(name = "responsible_dept", length = 100)
    private String responsibleDept;

    @Column(name = "center_lat")
    private Double centerLat;

    @Column(name = "center_lng")
    private Double centerLng;

    @Column(name = "boundary_points", columnDefinition = "TEXT")
    private String boundaryPoints;

    @Column(name = "ingested_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime ingestedAt;
}
