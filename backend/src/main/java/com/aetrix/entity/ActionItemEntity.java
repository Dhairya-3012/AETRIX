package com.aetrix.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "action_items", indexes = {
        @Index(name = "idx_action_priority", columnList = "priority"),
        @Index(name = "idx_action_status", columnList = "status"),
        @Index(name = "idx_action_city", columnList = "city")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city", length = 50, nullable = false)
    @Builder.Default
    private String city = "Ahmedabad";

    @Column(name = "action_id", length = 50)
    private String actionId;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "priority", length = 20)
    private String priority;

    @Column(name = "priority_score")
    private Integer priorityScore;

    @Column(name = "zone_name", length = 100)
    private String zoneName;

    @Column(name = "center_lat")
    private Double centerLat;

    @Column(name = "center_lng")
    private Double centerLng;

    @Column(name = "affected_count")
    private Integer affectedCount;

    @Column(name = "model_triggered_by", length = 100)
    private String modelTriggeredBy;

    @Column(name = "key_finding", columnDefinition = "TEXT")
    private String keyFinding;

    @Column(name = "satellite_sources", columnDefinition = "TEXT")
    private String satelliteSources;

    @Column(name = "responsible_dept", length = 100)
    private String responsibleDept;

    @Column(name = "deadline", length = 50)
    private String deadline;

    @Column(name = "expected_impact", columnDefinition = "TEXT")
    private String expectedImpact;

    @Column(name = "status", length = 30)
    private String status;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "ingested_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime ingestedAt;
}
