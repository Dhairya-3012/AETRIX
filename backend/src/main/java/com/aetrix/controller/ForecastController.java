package com.aetrix.controller;

import com.aetrix.dto.*;
import com.aetrix.service.ForecastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/forecast")
@RequiredArgsConstructor
@Tag(name = "Forecast", description = "Temperature Forecast APIs")
public class ForecastController {

    private final ForecastService forecastService;

    @GetMapping("/trend")
    @Operation(summary = "Get forecast trend with historical and predicted data")
    public ResponseEntity<ApiResponse<ForecastTrendDto>> getTrend() {
        ForecastTrendDto trend = forecastService.getTrend();
        return ResponseEntity.ok(ApiResponse.success(trend));
    }

    @GetMapping("/breach")
    @Operation(summary = "Get threshold breach analysis")
    public ResponseEntity<ApiResponse<ForecastBreachDto>> getBreach() {
        ForecastBreachDto breach = forecastService.getBreach();
        return ResponseEntity.ok(ApiResponse.success(breach));
    }

    @GetMapping("/historical")
    @Operation(summary = "Get historical temperature data")
    public ResponseEntity<ApiResponse<List<ForecastStep>>> getHistorical() {
        List<ForecastStep> historical = forecastService.getHistorical();
        return ResponseEntity.ok(ApiResponse.success(historical));
    }

    @GetMapping("/predicted")
    @Operation(summary = "Get predicted temperature data")
    public ResponseEntity<ApiResponse<List<ForecastStep>>> getPredicted() {
        List<ForecastStep> predicted = forecastService.getPredicted();
        return ResponseEntity.ok(ApiResponse.success(predicted));
    }

    @GetMapping("/ai-summary")
    @Operation(summary = "Get AI-generated forecast summary")
    public ResponseEntity<ApiResponse<GrokSummaryResponse>> getAiSummary() {
        GrokSummaryResponse summary = forecastService.getAiSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @PostMapping("/ai-summary/regenerate")
    @Operation(summary = "Regenerate AI forecast summary")
    public ResponseEntity<ApiResponse<GrokSummaryResponse>> regenerateAiSummary() {
        GrokSummaryResponse summary = forecastService.regenerateAiSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
