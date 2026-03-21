package com.aetrix.controller;

import com.aetrix.dto.*;
import com.aetrix.service.DashboardService;
import com.aetrix.service.FileIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard Overview APIs")
public class DashboardController {

    private final DashboardService dashboardService;
    private final FileIngestionService fileIngestionService;

    @GetMapping("/overview")
    @Operation(summary = "Get dashboard overview with all key metrics")
    public ResponseEntity<ApiResponse<DashboardOverview>> getOverview() {
        DashboardOverview overview = dashboardService.getOverview();
        return ResponseEntity.ok(ApiResponse.success(overview));
    }

    @PostMapping("/admin/reingest")
    @Operation(summary = "Force re-ingest all data from JSON files (Admin only)")
    public ResponseEntity<ApiResponse<Map<String, String>>> forceReIngest() {
        fileIngestionService.forceReIngestAll();
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("message", "All data re-ingested successfully")
        ));
    }
}
