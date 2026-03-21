package com.aetrix.service;

import com.aetrix.dto.*;
import com.aetrix.entity.LlmSummaryEntity;
import com.aetrix.repository.LlmSummaryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GrokLLMService {

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    private final LlmSummaryRepository llmSummaryRepository;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.model}")
    private String groqModel;

    @Value("${groq.api.max-tokens}")
    private int maxTokens;

    @Value("${groq.api.temperature}")
    private double temperature;

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @Cacheable(value = "grok-summaries", key = "#featureKey")
    public GrokSummaryResponse getSummary(String featureKey, String prompt) {
        // Check if we have a recent cached summary in DB
        Optional<LlmSummaryEntity> cachedSummary = llmSummaryRepository.findByFeatureKey(featureKey);
        if (cachedSummary.isPresent()) {
            LlmSummaryEntity entity = cachedSummary.get();
            LocalDateTime generatedAt = entity.getLastRegenerated() != null
                    ? entity.getLastRegenerated()
                    : entity.getGeneratedAt();

            // Return cached if within 60 minutes
            if (generatedAt != null && ChronoUnit.MINUTES.between(generatedAt, LocalDateTime.now()) < 60) {
                log.info("Returning cached summary for feature: {}", featureKey);
                return GrokSummaryResponse.builder()
                        .featureKey(featureKey)
                        .summaryText(entity.getSummaryText())
                        .modelUsed(entity.getModelUsed())
                        .generatedAt(generatedAt)
                        .fromCache(true)
                        .build();
            }
        }

        // Call Groq API
        String summaryText = callGroqApi(prompt);
        if (summaryText != null) {
            saveSummaryToDb(featureKey, summaryText, prompt);
            return GrokSummaryResponse.builder()
                    .featureKey(featureKey)
                    .summaryText(summaryText)
                    .modelUsed(groqModel)
                    .generatedAt(LocalDateTime.now())
                    .fromCache(false)
                    .build();
        }

        // Return fallback if API fails
        return GrokSummaryResponse.builder()
                .featureKey(featureKey)
                .summaryText("Unable to generate summary at this time. Please try again later.")
                .modelUsed("fallback")
                .generatedAt(LocalDateTime.now())
                .fromCache(false)
                .build();
    }

    @CacheEvict(value = "grok-summaries", key = "#featureKey")
    @Transactional
    public GrokSummaryResponse regenerateSummary(String featureKey, String prompt) {
        // Delete existing summary
        llmSummaryRepository.findByFeatureKey(featureKey).ifPresent(entity -> {
            llmSummaryRepository.delete(entity);
        });

        // Generate new summary
        String summaryText = callGroqApi(prompt);
        if (summaryText != null) {
            saveSummaryToDb(featureKey, summaryText, prompt);
            return GrokSummaryResponse.builder()
                    .featureKey(featureKey)
                    .summaryText(summaryText)
                    .modelUsed(groqModel)
                    .generatedAt(LocalDateTime.now())
                    .fromCache(false)
                    .build();
        }

        return GrokSummaryResponse.builder()
                .featureKey(featureKey)
                .summaryText("Unable to regenerate summary at this time. Please try again later.")
                .modelUsed("fallback")
                .generatedAt(LocalDateTime.now())
                .fromCache(false)
                .build();
    }

    private String callGroqApi(String prompt) {
        try {
            String requestBody = objectMapper.writeValueAsString(new GrokRequest(
                    groqModel,
                    List.of(new GrokMessage("user", prompt)),
                    maxTokens,
                    temperature
            ));

            log.debug("Calling Groq API with model: {}", groqModel);

            Request request = new Request.Builder()
                    .url(groqApiUrl)
                    .header("Authorization", "Bearer " + groqApiKey)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(requestBody, JSON))
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (response.isSuccessful()) {
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    JsonNode choices = jsonNode.get("choices");
                    if (choices != null && choices.isArray() && choices.size() > 0) {
                        JsonNode message = choices.get(0).get("message");
                        if (message != null && message.has("content")) {
                            return message.get("content").asText();
                        }
                    }
                    log.warn("Groq API returned unexpected response structure: {}", responseBody);
                } else {
                    log.error("Groq API call failed with status {}: {}", response.code(), responseBody);
                }
            }
        } catch (Exception e) {
            log.error("Error calling Groq API: {}", e.getMessage(), e);
        }
        return null;
    }

    @Transactional
    private void saveSummaryToDb(String featureKey, String summaryText, String prompt) {
        Optional<LlmSummaryEntity> existing = llmSummaryRepository.findByFeatureKey(featureKey);
        if (existing.isPresent()) {
            LlmSummaryEntity entity = existing.get();
            entity.setSummaryText(summaryText);
            entity.setPromptUsed(prompt);
            entity.setModelUsed(groqModel);
            entity.setLastRegenerated(LocalDateTime.now());
            llmSummaryRepository.save(entity);
        } else {
            LlmSummaryEntity entity = LlmSummaryEntity.builder()
                    .featureKey(featureKey)
                    .summaryText(summaryText)
                    .promptUsed(prompt)
                    .modelUsed(groqModel)
                    .build();
            llmSummaryRepository.save(entity);
        }
    }

    // Prompt builders
    public String buildUhiPrompt(String city, UhiHeatmapSummary summary, List<UhiHotspot> hotspots) {
        String topHotspot = hotspots.isEmpty() ? "N/A" : hotspots.get(0).getZoneName();
        String topTemp = hotspots.isEmpty() ? "N/A" : String.format("%.1f", hotspots.get(0).getAvgLstCelsius());
        String topSeverity = hotspots.isEmpty() ? "N/A" : hotspots.get(0).getSeverity();

        return String.format(
                "You are an environmental analyst on the AETRIX pan-India platform. " +
                "Summarize this Urban Heat Island analysis for %s in 3 bullet points " +
                "for a municipal officer. City mean LST: %.1f°C. Anomalous locations: " +
                "%d out of %d. Top hotspot: %s at %s°C (%s). " +
                "Be concise. Use plain language. Flag the most urgent area.",
                city,
                summary.getCityMeanLst() != null ? summary.getCityMeanLst() : 0.0,
                summary.getAnomalyCount() != null ? summary.getAnomalyCount() : 0,
                summary.getTotalPoints() != null ? summary.getTotalPoints() : 0,
                topHotspot, topTemp, topSeverity
        );
    }

    public String buildVegetationPrompt(String city, VegetationSummary summary, List<VegetationAlert> alerts) {
        return String.format(
                "You are on the AETRIX all-India environmental platform. Summarize the " +
                "NDVI vegetation health analysis for %s in 3 bullet points for an " +
                "environmental researcher. Healthy: %.1f%% (%d zones), Stressed: %.1f%% (%d), " +
                "Barren: %.1f%% (%d). Mean NDVI: %.3f. Critical stress alerts: %d locations. " +
                "Recommend the single most urgent intervention.",
                city,
                summary.getHealthyPercentage() != null ? summary.getHealthyPercentage() : 0.0,
                summary.getHealthyCount() != null ? summary.getHealthyCount() : 0,
                summary.getStressedPercentage() != null ? summary.getStressedPercentage() : 0.0,
                summary.getStressedCount() != null ? summary.getStressedCount() : 0,
                summary.getBarrenPercentage() != null ? summary.getBarrenPercentage() : 0.0,
                summary.getBarrenCount() != null ? summary.getBarrenCount() : 0,
                summary.getCityMeanNdvi() != null ? summary.getCityMeanNdvi() : 0.0,
                alerts.size()
        );
    }

    public String buildPollutionPrompt(String city, PollutionRiskSummary summary, List<PollutionHotspot> hotspots) {
        String topZone = hotspots.isEmpty() ? "N/A" : hotspots.get(0).getZoneName();
        String topScore = hotspots.isEmpty() ? "N/A" : String.format("%.1f", hotspots.get(0).getAvgRiskScore());
        int topPoints = hotspots.isEmpty() ? 0 : hotspots.get(0).getPointCount();

        return String.format(
                "You are on the AETRIX all-India satellite intelligence platform. Summarize " +
                "the pollution risk map for %s in 3 bullet points for a Pollution Control " +
                "Board officer. Critical: %d, High: %d, Medium: %d, Low: %d. Mean risk score: %.1f/100. " +
                "Top cluster: %s (score %s/100, %d locations). State which department must act " +
                "and within what timeframe.",
                city,
                summary.getCriticalCount() != null ? summary.getCriticalCount() : 0,
                summary.getHighCount() != null ? summary.getHighCount() : 0,
                summary.getMediumCount() != null ? summary.getMediumCount() : 0,
                summary.getLowCount() != null ? summary.getLowCount() : 0,
                summary.getCityMeanRisk() != null ? summary.getCityMeanRisk() : 0.0,
                topZone, topScore, topPoints
        );
    }

    public String buildForecastPrompt(String city, ForecastTrendDto trend, ForecastBreachDto breach) {
        return String.format(
                "You are on the AETRIX India environmental intelligence platform. Summarize " +
                "the temperature forecast for %s in 2 sentences for city planners. " +
                "Model: ARIMA(2,1,2). Trend: %s at %.2f. Predicted mean: %.1f°C " +
                "(range %.1f°C to %.1f°C over 30 steps). Currently %.1f%% of %s " +
                "(%d locations) above %.1f°C danger threshold. Write as a risk advisory.",
                city,
                trend.getTrendDirection() != null ? trend.getTrendDirection() : "unknown",
                trend.getTrendRate() != null ? trend.getTrendRate() : 0.0,
                trend.getPredictedMean() != null ? trend.getPredictedMean() : 0.0,
                trend.getPredictedMin() != null ? trend.getPredictedMin() : 0.0,
                trend.getPredictedMax() != null ? trend.getPredictedMax() : 0.0,
                breach.getBreachPercentage() != null ? breach.getBreachPercentage() : 0.0,
                city,
                breach.getBreachCount() != null ? breach.getBreachCount() : 0,
                breach.getDangerThreshold() != null ? breach.getDangerThreshold() : 35.0
        );
    }

    public String buildActionPlanPrompt(String city, ActionPlanDto plan) {
        String action1Title = plan.getActions().isEmpty() ? "N/A" : plan.getActions().get(0).getTitle();
        int action1Count = plan.getActions().isEmpty() ? 0 :
                (plan.getActions().get(0).getAffectedCount() != null ? plan.getActions().get(0).getAffectedCount() : 0);
        String depts = plan.getDepartments().stream().limit(3).collect(Collectors.joining(", "));
        String sources = plan.getSatelliteSources().stream().collect(Collectors.joining(", "));

        return String.format(
                "You are on the AETRIX all-India environmental governance platform. Write a " +
                "one-paragraph executive briefing for a senior government officer summarising " +
                "the environment action plan for %s. Total actions: %d (%d high priority, %d medium " +
                "priority). Most urgent: %s — affecting %d locations. Departments: %s. " +
                "Earliest deadline: %s. Satellite sources used: %s. Frame this in the context of " +
                "India's national environmental monitoring goals.",
                city,
                plan.getTotalActions(),
                plan.getHighPriorityCount(),
                plan.getMediumPriorityCount(),
                action1Title,
                action1Count,
                depts,
                plan.getEarliestDeadline() != null ? plan.getEarliestDeadline() : "N/A",
                sources
        );
    }

    public boolean isGrokConnected() {
        return groqApiKey != null && !groqApiKey.equals("demo-key-replace-me");
    }

    public List<String> getCachedFeatures() {
        return llmSummaryRepository.findAll().stream()
                .map(LlmSummaryEntity::getFeatureKey)
                .collect(Collectors.toList());
    }

    // Inner classes for Grok API request
    private record GrokRequest(String model, List<GrokMessage> messages, int max_tokens, double temperature) {}
    private record GrokMessage(String role, String content) {}
}
