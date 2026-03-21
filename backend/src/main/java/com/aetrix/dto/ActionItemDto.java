package com.aetrix.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionItemDto {
    private Long id;
    private String actionId;
    private String title;
    private String description;
    private String priority;
    private Integer priorityScore;
    private String zoneName;
    private Double centerLat;
    private Double centerLng;
    private Integer affectedCount;
    private String modelTriggeredBy;
    private String keyFinding;
    private String satelliteSources;
    private String responsibleDept;
    private String deadline;
    private String expectedImpact;
    private String status;
    private LocalDateTime generatedAt;
}
