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

    private static final String CITY = "Ahmedabad";
    private static final double DANGER_THRESHOLD = 35.0;

    @Cacheable("forecast-trend")
    public ForecastTrendDto getTrend() {
        List<ForecastStep> historical = forecastStepRepository.findByStepTypeOrderByStepAsc("historical")
                .stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());

        List<ForecastStep> predicted = forecastStepRepository.findByStepTypeOrderByStepAsc("predicted")
                .stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());

        Double predictedMean = forecastStepRepository.findPredictedMean();
        Double predictedMin = forecastStepRepository.findPredictedMin();
        Double predictedMax = forecastStepRepository.findPredictedMax();

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
                .city(CITY)
                .build();
    }

    @Cacheable("forecast-breach")
    public ForecastBreachDto getBreach() {
        long totalPoints = uhiHeatmapRepository.count();
        long breachCount = uhiHeatmapRepository.findAll().stream()
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
                .city(CITY)
                .build();
    }

    public List<ForecastStep> getHistorical() {
        return forecastStepRepository.findByStepTypeOrderByStepAsc("historical")
                .stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    public List<ForecastStep> getPredicted() {
        return forecastStepRepository.findByStepTypeOrderByStepAsc("predicted")
                .stream()
                .map(this::entityToDto)
                .collect(Collectors.toList());
    }

    public GrokSummaryResponse getAiSummary() {
        ForecastTrendDto trend = getTrend();
        ForecastBreachDto breach = getBreach();
        String prompt = grokLLMService.buildForecastPrompt(CITY, trend, breach);
        return grokLLMService.getSummary("forecast", prompt);
    }

    public GrokSummaryResponse regenerateAiSummary() {
        ForecastTrendDto trend = getTrend();
        ForecastBreachDto breach = getBreach();
        String prompt = grokLLMService.buildForecastPrompt(CITY, trend, breach);
        return grokLLMService.regenerateSummary("forecast", prompt);
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
