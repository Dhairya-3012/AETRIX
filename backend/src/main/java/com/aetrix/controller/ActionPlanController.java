package com.aetrix.controller;

import com.aetrix.dto.*;
import com.aetrix.service.ActionPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/actions")
@RequiredArgsConstructor
@Tag(name = "Actions", description = "Action Plan Management APIs")
public class ActionPlanController {

    private final ActionPlanService actionPlanService;

    @GetMapping
    @Operation(summary = "Get all action items")
    public ResponseEntity<ApiResponse<ActionPlanDto>> getAll() {
        ActionPlanDto plan = actionPlanService.getPlan();
        return ResponseEntity.ok(ApiResponse.success(plan));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get action plan summary")
    public ResponseEntity<ApiResponse<ActionPlanDto>> getSummary() {
        ActionPlanDto summary = actionPlanService.getSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/high-priority")
    @Operation(summary = "Get high priority actions")
    public ResponseEntity<ApiResponse<List<ActionItemDto>>> getHighPriority() {
        List<ActionItemDto> actions = actionPlanService.getHighPriority();
        return ResponseEntity.ok(ApiResponse.success(actions));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get action item by ID")
    public ResponseEntity<ApiResponse<ActionItemDto>> getById(@PathVariable Long id) {
        ActionItemDto action = actionPlanService.getById(id);
        if (action == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(action));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update action item status")
    public ResponseEntity<ApiResponse<ActionItemDto>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Status is required"));
        }
        ActionItemDto action = actionPlanService.updateStatus(id, status);
        if (action == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(action, "Status updated successfully"));
    }

    @GetMapping("/ai-summary")
    @Operation(summary = "Get AI-generated action plan summary")
    public ResponseEntity<ApiResponse<GrokSummaryResponse>> getAiSummary() {
        GrokSummaryResponse summary = actionPlanService.getAiSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @PostMapping("/regenerate")
    @Operation(summary = "Regenerate AI action plan summary")
    public ResponseEntity<ApiResponse<GrokSummaryResponse>> regenerate() {
        GrokSummaryResponse summary = actionPlanService.regenerateAiSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
