package com.aetrix.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "uhi_hotspots", indexes = {
        @Index(name = "idx_uhi_hotspot_city", columnList = "city")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UhiHotspotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city", length = 50, nullable = false)
    @Builder.Default
    private String city = "Ahmedabad";

    @Column(name = "zone_name", length = 100)
    private String zoneName;

    @Column(name = "avg_lst_celsius")
    private Double avgLstCelsius;

    @Column(name = "max_lst_celsius")
    private Double maxLstCelsius;

    @Column(name = "min_lst_celsius")
    private Double minLstCelsius;

    @Column(name = "point_count")
    private Integer pointCount;

    @Column(name = "severity", length = 20)
    private String severity;

    @Column(name = "center_lat")
    private Double centerLat;

    @Column(name = "center_lng")
    private Double centerLng;

    @Column(name = "boundary_points", columnDefinition = "TEXT")
    private String boundaryPoints;

    @Column(name = "point_ids", columnDefinition = "TEXT")
    private String pointIds;

    @Column(name = "ingested_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime ingestedAt;
}
