package com.aetrix.service;

import com.aetrix.dto.*;
import com.aetrix.entity.ActionItemEntity;
import com.aetrix.repository.ActionItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActionPlanService {

    private final ActionItemRepository actionItemRepository;
    private final GrokLLMService grokLLMService;

    private static final String CITY = "Ahmedabad";

    @Cacheable("action-plan")
    public ActionPlanDto getPlan() {
        List<ActionItemDto> actions = actionItemRepository.findAllByOrderByPriorityScoreDesc()
                .stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());

        long highCount = actionItemRepository.countByPriority("high");
        long mediumCount = actionItemRepository.countByPriority("medium");
        long lowCount = actionItemRepository.countByPriority("low");
        long pendingCount = actionItemRepository.countByStatus("pending");
        long inProgressCount = actionItemRepository.countByStatus("in_progress");
        long completedCount = actionItemRepository.countByStatus("completed");

        // Extract unique departments
        Set<String> departments = actions.stream()
                .map(ActionItemDto::getResponsibleDept)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Extract unique satellite sources
        Set<String> sources = actions.stream()
                .map(ActionItemDto::getSatelliteSources)
                .filter(Objects::nonNull)
                .flatMap(s -> Arrays.stream(s.split(",\\s*")))
                .collect(Collectors.toSet());

        // Find earliest deadline
        String earliestDeadline = actions.stream()
                .map(ActionItemDto::getDeadline)
                .filter(Objects::nonNull)
                .min(String::compareTo)
                .orElse(null);

        return ActionPlanDto.builder()
                .actions(actions)
                .totalActions(actions.size())
                .highPriorityCount((int) highCount)
                .mediumPriorityCount((int) mediumCount)
                .lowPriorityCount((int) lowCount)
                .pendingCount((int) pendingCount)
                .inProgressCount((int) inProgressCount)
                .completedCount((int) completedCount)
                .earliestDeadline(earliestDeadline)
                .departments(new ArrayList<>(departments))
                .satelliteSources(new ArrayList<>(sources))
                .city(CITY)
                .build();
    }

    @Cacheable("action-summary")
    public ActionPlanDto getSummary() {
        return getPlan();
    }

    public List<ActionItemDto> getHighPriority() {
        return actionItemRepository.findByPriorityOrderByPriorityScoreDesc("high")
                .stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    public ActionItemDto getById(Long id) {
        return actionItemRepository.findById(id)
                .map(this::entityToDto)
                .orElse(null);
    }

    @Transactional
    @CacheEvict(value = {"action-plan", "action-summary"}, allEntries = true)
    public ActionItemDto updateStatus(Long id, String status) {
        Optional<ActionItemEntity> entityOpt = actionItemRepository.findById(id);
        if (entityOpt.isPresent()) {
            ActionItemEntity entity = entityOpt.get();
            entity.setStatus(status);
            actionItemRepository.save(entity);
            return entityToDto(entity);
        }
        return null;
    }

    public GrokSummaryResponse getAiSummary() {
        ActionPlanDto plan = getPlan();
        String prompt = grokLLMService.buildActionPlanPrompt(CITY, plan);
        return grokLLMService.getSummary("action-plan", prompt);
    }

    @CacheEvict(value = {"action-plan", "action-summary"}, allEntries = true)
    public GrokSummaryResponse regenerateAiSummary() {
        ActionPlanDto plan = getPlan();
        String prompt = grokLLMService.buildActionPlanPrompt(CITY, plan);
        return grokLLMService.regenerateSummary("action-plan", prompt);
    }

    private ActionItemDto entityToDto(ActionItemEntity entity) {
        return ActionItemDto.builder()
                .id(entity.getId())
                .actionId(entity.getActionId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .priority(entity.getPriority())
                .priorityScore(entity.getPriorityScore())
                .zoneName(entity.getZoneName())
                .centerLat(entity.getCenterLat())
                .centerLng(entity.getCenterLng())
                .affectedCount(entity.getAffectedCount())
                .modelTriggeredBy(entity.getModelTriggeredBy())
                .keyFinding(entity.getKeyFinding())
                .satelliteSources(entity.getSatelliteSources())
                .responsibleDept(entity.getResponsibleDept())
                .deadline(entity.getDeadline())
                .expectedImpact(entity.getExpectedImpact())
                .status(entity.getStatus())
                .generatedAt(entity.getGeneratedAt())
                .build();
    }
}
