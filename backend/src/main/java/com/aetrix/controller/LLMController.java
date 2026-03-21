package com.aetrix.controller;

import com.aetrix.dto.*;
import com.aetrix.service.GrokLLMService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/llm")
@RequiredArgsConstructor
@Tag(name = "LLM", description = "Grok LLM Integration APIs")
public class LLMController {

    private final GrokLLMService grokLLMService;

    @PostMapping("/summarize")
    @Operation(summary = "Generate AI summary for a feature")
    public ResponseEntity<ApiResponse<GrokSummaryResponse>> summarize(
            @RequestBody GrokSummaryRequest request) {
        // This endpoint is handled by individual feature services
        // Return a placeholder response
        return ResponseEntity.ok(ApiResponse.success(
                GrokSummaryResponse.builder()
                        .featureKey(request.getFeature())
                        .summaryText("Please use the specific feature endpoint for AI summaries (e.g., /api/v1/uhi/ai-summary)")
                        .build()
        ));
    }

    @PostMapping("/regenerate")
    @Operation(summary = "Regenerate AI summary for a feature")
    public ResponseEntity<ApiResponse<GrokSummaryResponse>> regenerate(
            @RequestBody GrokSummaryRequest request) {
        // This endpoint is handled by individual feature services
        return ResponseEntity.ok(ApiResponse.success(
                GrokSummaryResponse.builder()
                        .featureKey(request.getFeature())
                        .summaryText("Please use the specific feature endpoint for regenerating AI summaries")
                        .build()
        ));
    }

    @GetMapping("/status")
    @Operation(summary = "Get Grok LLM connection status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("grokConnected", grokLLMService.isGrokConnected());
        status.put("cachedFeatures", grokLLMService.getCachedFeatures());
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
