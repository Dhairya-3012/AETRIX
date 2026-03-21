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
public class ActionPlanDto {
    private List<ActionItemDto> actions;
    private Integer totalActions;
    private Integer highPriorityCount;
    private Integer mediumPriorityCount;
    private Integer lowPriorityCount;
    private Integer pendingCount;
    private Integer inProgressCount;
    private Integer completedCount;
    private String earliestDeadline;
    private List<String> departments;
    private List<String> satelliteSources;
    private String city;
}
