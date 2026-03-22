package com.aetrix.service;

import com.aetrix.dto.*;
import com.aetrix.entity.ForecastStepEntity;
import com.aetrix.repository.ForecastStepRepository;
import com.aetrix.repository.UhiHeatmapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForecastService {

    private final ForecastStepRepository forecastStepRepository;
    private final UhiHeatmapRepository uhiHeatmapRepository;
    private final GrokLLMService grokLLMService;

    private static final String DEFAULT_CITY = "Ahmedabad";
    private static final double DANGER_THRESHOLD = 35.0;

    @Cacheable(value = "forecast-trend", key = "#city")
    public ForecastTrendDto getTrend(String city) {
        if (city == null || city.isBlank()) city = DEFAULT_CITY;

        List<ForecastStep> historical = forecastStepRepository.findByCityAndStepTypeOrderByStepAsc(city, "historical")
                .stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());

        List<ForecastStep> predicted = forecastStepRepository.findByCityAndStepTypeOrderByStepAsc(city, "predicted")
                .stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());

        Double predictedMean = forecastStepRepository.findPredictedMean(city);
        Double predictedMin = forecastStepRepository.findPredictedMin(city);
        Double predictedMax = forecastStepRepository.findPredictedMax(city);

        // Calculate trend direction and rate
        String trendDirection = "stable";
        double trendRate = 0.0;
        if (!historical.isEmpty() && !predicted.isEmpty()) {
            double lastHistorical = historical.get(historical.size() - 1).getValueCelsius() != null
                    ? historical.get(historical.size() - 1).getValueCelsius() : 0;
            double firstPredicted = predicted.get(0).getPredictedCelsius() != null
                    ? predicted.get(0).getPredictedCelsius() : 0;
            double lastPredicted = predicted.get(predicted.size() - 1).getPredictedCelsius() != null
                    ? predicted.get(predicted.size() - 1).getPredictedCelsius() : 0;

            trendRate = (lastPredicted - lastHistorical) / predicted.size();
            if (trendRate > 0.1) trendDirection = "increasing";
            else if (trendRate < -0.1) trendDirection = "decreasing";
        }

        return ForecastTrendDto.builder()
                .historical(historical)
                .predicted(predicted)
                .trendDirection(trendDirection)
                .trendRate(trendRate)
                .predictedMean(predictedMean)
                .predictedMin(predictedMin)
                .predictedMax(predictedMax)
                .modelType("ARIMA(2,1,2)")
                .city(city)
                .build();
    }

    // Legacy method
    public ForecastTrendDto getTrend() {
        return getTrend(DEFAULT_CITY);
    }

    @Cacheable(value = "forecast-breach", key = "#city")
    public ForecastBreachDto getBreach(String city) {
        if (city == null || city.isBlank()) city = DEFAULT_CITY;

        long totalPoints = uhiHeatmapRepository.countByCity(city);
        final String finalCity = city;
        long breachCount = uhiHeatmapRepository.findByCity(city).stream()
                .filter(p -> p.getLstCelsius() != null && p.getLstCelsius() > DANGER_THRESHOLD)
                .count();

        double breachPercentage = totalPoints > 0 ? (breachCount * 100.0 / totalPoints) : 0.0;

        String riskLevel = "low";
        if (breachPercentage > 50) riskLevel = "critical";
        else if (breachPercentage > 30) riskLevel = "high";
        else if (breachPercentage > 15) riskLevel = "moderate";

        String whatIfWarmer = String.format("If temperatures rise by 2°C, breach count could increase to %.0f locations (%.1f%%).",
                breachCount * 1.3, breachPercentage * 1.3);
        String whatIfCooler = String.format("If temperatures drop by 2°C, breach count could decrease to %.0f locations (%.1f%%).",
                breachCount * 0.7, breachPercentage * 0.7);

        return ForecastBreachDto.builder()
                .dangerThreshold(DANGER_THRESHOLD)
                .breachCount((int) breachCount)
                .breachPercentage(breachPercentage)
                .totalPoints((int) totalPoints)
                .riskLevel(riskLevel)
                .whatIfWarmer(whatIfWarmer)
                .whatIfCooler(whatIfCooler)
                .city(city)
                .build();
    }

    // Legacy method
    public ForecastBreachDto getBreach() {
        return getBreach(DEFAULT_CITY);
    }

    // Legacy method
    public List<ForecastStep> getHistorical() {
        return getHistorical(DEFAULT_CITY);
    }

    public List<ForecastStep> getHistorical(String city) {
        if (city == null || city.isBlank()) city = DEFAULT_CITY;
        return forecastStepRepository.findByCityAndStepTypeOrderByStepAsc(city, "historical")
                .stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    // Legacy method
    public List<ForecastStep> getPredicted() {
        return getPredicted(DEFAULT_CITY);
    }

    public List<ForecastStep> getPredicted(String city) {
        if (city == null || city.isBlank()) city = DEFAULT_CITY;
        return forecastStepRepository.findByCityAndStepTypeOrderByStepAsc(city, "predicted")
                .stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    // Legacy method
    public GrokSummaryResponse getAiSummary() {
        return getAiSummary(DEFAULT_CITY);
    }

    public GrokSummaryResponse getAiSummary(String city) {
        if (city == null || city.isBlank()) city = DEFAULT_CITY;
        ForecastTrendDto trend = getTrend(city);
        ForecastBreachDto breach = getBreach(city);
        String prompt = grokLLMService.buildForecastPrompt(city, trend, breach);
        return grokLLMService.getSummary("forecast", city, prompt);
    }

    // Legacy method
    public GrokSummaryResponse regenerateAiSummary() {
        return regenerateAiSummary(DEFAULT_CITY);
    }

    public GrokSummaryResponse regenerateAiSummary(String city) {
        if (city == null || city.isBlank()) city = DEFAULT_CITY;
        ForecastTrendDto trend = getTrend(city);
        ForecastBreachDto breach = getBreach(city);
        String prompt = grokLLMService.buildForecastPrompt(city, trend, breach);
        return grokLLMService.regenerateSummary("forecast", city, prompt);
    }

    private ForecastStep entityToDto(ForecastStepEntity entity) {
        return ForecastStep.builder()
                .step(entity.getStep())
                .valueCelsius(entity.getValueCelsius())
                .stepType(entity.getStepType())
                .predictedCelsius(entity.getPredictedCelsius())
                .lowerBound(entity.getLowerBound())
                .upperBound(entity.getUpperBound())
                .build();
    }
}
