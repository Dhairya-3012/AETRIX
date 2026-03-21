package com.aetrix.controller;

import com.aetrix.dto.*;
import com.aetrix.service.UhiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/uhi")
@RequiredArgsConstructor
@Tag(name = "UHI", description = "Urban Heat Island Analysis APIs")
public class UhiController {

    private final UhiService uhiService;

    @GetMapping("/summary")
    @Operation(summary = "Get UHI summary statistics")
    public ResponseEntity<ApiResponse<UhiHeatmapSummary>> getSummary() {
        UhiHeatmapSummary summary = uhiService.getSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/heatmap")
    @Operation(summary = "Get all UHI heatmap points")
    public ResponseEntity<ApiResponse<List<UhiHeatmapPoint>>> getHeatmap() {
        List<UhiHeatmapPoint> heatmap = uhiService.getHeatmap();
        return ResponseEntity.ok(ApiResponse.success(heatmap));
    }

    @GetMapping("/hotspots")
    @Operation(summary = "Get all UHI hotspots")
    public ResponseEntity<ApiResponse<List<UhiHotspot>>> getHotspots() {
        List<UhiHotspot> hotspots = uhiService.getHotspots();
        return ResponseEntity.ok(ApiResponse.success(hotspots));
    }

    @GetMapping("/hotspots/{id}")
    @Operation(summary = "Get UHI hotspot by ID")
    public ResponseEntity<ApiResponse<UhiHotspot>> getHotspotById(@PathVariable Long id) {
        UhiHotspot hotspot = uhiService.getHotspotById(id);
        if (hotspot == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(hotspot));
    }

    @GetMapping("/anomalies")
    @Operation(summary = "Get all anomalous UHI points")
    public ResponseEntity<ApiResponse<List<UhiHeatmapPoint>>> getAnomalies() {
        List<UhiHeatmapPoint> anomalies = uhiService.getAnomalies();
        return ResponseEntity.ok(ApiResponse.success(anomalies));
    }

    @GetMapping("/ai-summary")
    @Operation(summary = "Get AI-generated UHI summary")
    public ResponseEntity<ApiResponse<GrokSummaryResponse>> getAiSummary() {
        GrokSummaryResponse summary = uhiService.getAiSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @PostMapping("/ai-summary/regenerate")
    @Operation(summary = "Regenerate AI UHI summary")
    public ResponseEntity<ApiResponse<GrokSummaryResponse>> regenerateAiSummary() {
        GrokSummaryResponse summary = uhiService.regenerateAiSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
