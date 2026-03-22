package com.aetrix.controller;

import com.aetrix.dto.*;
import com.aetrix.service.PollutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pollution")
@RequiredArgsConstructor
@Tag(name = "Pollution", description = "Pollution Risk Analysis APIs")
public class PollutionController {

    private final PollutionService pollutionService;

    @GetMapping("/summary")
    @Operation(summary = "Get pollution risk summary statistics")
    public ResponseEntity<ApiResponse<PollutionRiskSummary>> getSummary(
            @RequestParam(required = false) String city) {
        PollutionRiskSummary summary = pollutionService.getSummary(city);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/map")
    @Operation(summary = "Get all pollution risk map points")
    public ResponseEntity<ApiResponse<List<PollutionPoint>>> getMap(
            @RequestParam(required = false) String city) {
        List<PollutionPoint> map = pollutionService.getMap(city);
        return ResponseEntity.ok(ApiResponse.success(map));
    }

    @GetMapping("/hotspots")
    @Operation(summary = "Get pollution hotspots")
    public ResponseEntity<ApiResponse<List<PollutionHotspot>>> getHotspots(
            @RequestParam(required = false) String city) {
        List<PollutionHotspot> hotspots = pollutionService.getHotspots(city);
        return ResponseEntity.ok(ApiResponse.success(hotspots));
    }

    @GetMapping("/compliance")
    @Operation(summary = "Get compliance report data")
    public ResponseEntity<ApiResponse<List<PollutionPoint>>> getCompliance(
            @RequestParam(required = false) String city) {
        List<PollutionPoint> compliance = pollutionService.getCompliance(city);
        return ResponseEntity.ok(ApiResponse.success(compliance));
    }

    @GetMapping("/outliers")
    @Operation(summary = "Get extreme outlier points")
    public ResponseEntity<ApiResponse<List<PollutionPoint>>> getOutliers(
            @RequestParam(required = false) String city) {
        List<PollutionPoint> outliers = pollutionService.getOutliers(city);
        return ResponseEntity.ok(ApiResponse.success(outliers));
    }

    @GetMapping("/ai-summary")
    @Operation(summary = "Get AI-generated pollution summary")
    public ResponseEntity<ApiResponse<GrokSummaryResponse>> getAiSummary(
            @RequestParam(required = false) String city) {
        GrokSummaryResponse summary = pollutionService.getAiSummary(city);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @PostMapping("/ai-summary/regenerate")
    @Operation(summary = "Regenerate AI pollution summary")
    public ResponseEntity<ApiResponse<GrokSummaryResponse>> regenerateAiSummary(
            @RequestParam(required = false) String city) {
        GrokSummaryResponse summary = pollutionService.regenerateAiSummary(city);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
