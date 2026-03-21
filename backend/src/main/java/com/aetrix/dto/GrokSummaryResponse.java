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
public class GrokSummaryResponse {
    private String featureKey;
    private String summaryText;
    private String modelUsed;
    private LocalDateTime generatedAt;
    private Boolean fromCache;
}
