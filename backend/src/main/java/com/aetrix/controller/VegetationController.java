package com.aetrix.controller;

import com.aetrix.dto.*;
import com.aetrix.service.VegetationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vegetation")
@RequiredArgsConstructor
@Tag(name = "Vegetation", description = "Vegetation Health Analysis APIs")
public class VegetationController {

    private final VegetationService vegetationService;

    @GetMapping("/summary")
    @Operation(summary = "Get vegetation summary statistics")
    public ResponseEntity<ApiResponse<VegetationSummary>> getSummary() {
        VegetationSummary summary = vegetationService.getSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/map")
    @Operation(summary = "Get all vegetation map points")
    public ResponseEntity<ApiResponse<List<VegetationPoint>>> getMap() {
        List<VegetationPoint> map = vegetationService.getMap();
        return ResponseEntity.ok(ApiResponse.success(map));
    }

    @GetMapping("/alerts")
    @Operation(summary = "Get vegetation stress alerts")
    public ResponseEntity<ApiResponse<List<VegetationAlert>>> getAlerts() {
        List<VegetationAlert> alerts = vegetationService.getAlerts();
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/plantation")
    @Operation(summary = "Get plantation recommendations")
    public ResponseEntity<ApiResponse<List<VegetationPoint>>> getPlantation() {
        List<VegetationPoint> recommendations = vegetationService.getPlantationRecommendations();
        return ResponseEntity.ok(ApiResponse.success(recommendations));
    }

    @GetMapping("/ai-summary")
    @Operation(summary = "Get AI-generated vegetation summary")
    public ResponseEntity<ApiResponse<GrokSummaryResponse>> getAiSummary() {
        GrokSummaryResponse summary = vegetationService.getAiSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @PostMapping("/ai-summary/regenerate")
    @Operation(summary = "Regenerate AI vegetation summary")
    public ResponseEntity<ApiResponse<GrokSummaryResponse>> regenerateAiSummary() {
        GrokSummaryResponse summary = vegetationService.regenerateAiSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
