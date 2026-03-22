package com.aetrix.service;

import com.aetrix.entity.*;
import com.aetrix.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileIngestionService {

    private static final List<String> SUPPORTED_CITIES = Arrays.asList(
            "Ahmedabad", "Bangalore", "Delhi", "Mumbai"
    );

    private final ObjectMapper objectMapper;
    private final UhiHeatmapRepository uhiHeatmapRepository;
    private final UhiHotspotRepository uhiHotspotRepository;
    private final VegetationPointRepository vegetationPointRepository;
    private final VegetationAlertRepository vegetationAlertRepository;
    private final PollutionPointRepository pollutionPointRepository;
    private final PollutionHotspotRepository pollutionHotspotRepository;
    private final ForecastStepRepository forecastStepRepository;
    private final ActionItemRepository actionItemRepository;

    public List<String> getSupportedCities() {
        return SUPPORTED_CITIES;
    }

    @PostConstruct
    @Transactional
    public void ingestAll() {
        log.info("Starting AETRIX multi-city data ingestion from JSON files...");

        for (String city : SUPPORTED_CITIES) {
            ingestCityData(city);
        }

        log.info("AETRIX multi-city data ingestion completed successfully.");
    }

    @Transactional
    public void ingestCityData(String city) {
        String cityLower = city.toLowerCase();
        String basePath = "data/" + cityLower + "/";

        log.info("Ingesting data for city: {}", city);

        // Check if data for this city already exists
        if (uhiHeatmapRepository.countByCity(city) > 0) {
            log.info("Data for {} already exists, skipping ingestion.", city);
            return;
        }

        ingestUhiHeatmap(city, basePath);
        ingestUhiHotspots(city, basePath);
        ingestVegetationMap(city, basePath);
        ingestVegetationAlerts(city, basePath);
        ingestPollutionRiskMap(city, basePath);
        ingestPollutionHotspots(city, basePath);
        ingestForecastTrend(city, basePath);
        ingestActionPlan(city, basePath);

        log.info("Completed ingestion for city: {}", city);
    }

    @Transactional
    public void forceReIngestCity(String city) {
        log.info("Force re-ingesting data for city: {}", city);

        // Clear existing data for this city
        vegetationAlertRepository.deleteByCity(city);
        vegetationPointRepository.deleteByCity(city);
        uhiHotspotRepository.deleteByCity(city);
        uhiHeatmapRepository.deleteByCity(city);
        pollutionHotspotRepository.deleteByCity(city);
        pollutionPointRepository.deleteByCity(city);
        forecastStepRepository.deleteByCity(city);
        actionItemRepository.deleteByCity(city);

        // Re-ingest
        String cityLower = city.toLowerCase();
        String basePath = "data/" + cityLower + "/";

        ingestUhiHeatmapForce(city, basePath);
        ingestUhiHotspotsForce(city, basePath);
        ingestVegetationMapForce(city, basePath);
        ingestVegetationAlertsForce(city, basePath);
        ingestPollutionRiskMapForce(city, basePath);
        ingestPollutionHotspotsForce(city, basePath);
        ingestForecastTrendForce(city, basePath);
        ingestActionPlanForce(city, basePath);

        log.info("Force re-ingestion completed for city: {}", city);
    }

    @Transactional
    public void forceReIngestAll() {
        log.info("Force re-ingesting all cities...");

        for (String city : SUPPORTED_CITIES) {
            forceReIngestCity(city);
        }

        log.info("Force re-ingestion completed for all cities.");
    }

    private void ingestUhiHeatmap(String city, String basePath) {
        try {
            JsonNode root = loadJsonFile(basePath + "uhi_heatmap.json");
            if (root == null) return;

            List<UhiHeatmapPointEntity> entities = new ArrayList<>();
            JsonNode points = root.has("points") ? root.get("points") : root;

            if (points.isArray()) {
                for (JsonNode node : points) {
                    UhiHeatmapPointEntity entity = UhiHeatmapPointEntity.builder()
                            .city(city)
                            .pointId(getTextValue(node, "id"))
                            .lat(getDoubleValue(node, "lat"))
                            .lng(getDoubleValue(node, "lng"))
                            .lstCelsius(getDoubleValue(node, "lst_celsius"))
                            .isAnomaly(getBooleanValue(node, "is_anomaly"))
                            .severity(getTextValue(node, "severity"))
                            .anomalyScore(getDoubleValue(node, "anomaly_score"))
                            .ndvi(getDoubleValue(node, "ndvi"))
                            .soilMoisture(getDoubleValue(node, "soil_moisture"))
                            .build();
                    entities.add(entity);
                }
            }

            uhiHeatmapRepository.saveAll(entities);
            log.info("Ingested {} UHI heatmap points for {}", entities.size(), city);
        } catch (Exception e) {
            log.warn("Failed to ingest UHI heatmap for {}: {}", city, e.getMessage());
        }
    }

    private void ingestUhiHeatmapForce(String city, String basePath) {
        ingestUhiHeatmap(city, basePath);
    }

    private void ingestUhiHotspots(String city, String basePath) {
        try {
            JsonNode root = loadJsonFile(basePath + "uhi_hotspots.json");
            if (root == null) return;

            List<UhiHotspotEntity> entities = new ArrayList<>();
            JsonNode hotspots = root.has("hotspots") ? root.get("hotspots") : root;

            if (hotspots.isArray()) {
                for (JsonNode node : hotspots) {
                    UhiHotspotEntity entity = UhiHotspotEntity.builder()
                            .city(city)
                            .zoneName(getTextValue(node, "zone_name"))
                            .avgLstCelsius(getDoubleValue(node, "avg_lst_celsius"))
                            .maxLstCelsius(getDoubleValue(node, "max_lst_celsius"))
                            .minLstCelsius(getDoubleValue(node, "min_lst_celsius"))
                            .pointCount(getIntValue(node, "point_count"))
                            .severity(getTextValue(node, "severity"))
                            .centerLat(getDoubleValue(node, "center_lat"))
                            .centerLng(getDoubleValue(node, "center_lng"))
                            .boundaryPoints(getArrayAsString(node, "boundary_points"))
                            .pointIds(getArrayAsString(node, "point_ids"))
                            .build();
                    entities.add(entity);
                }
            }

            uhiHotspotRepository.saveAll(entities);
            log.info("Ingested {} UHI hotspots for {}", entities.size(), city);
        } catch (Exception e) {
            log.warn("Failed to ingest UHI hotspots for {}: {}", city, e.getMessage());
        }
    }

    private void ingestUhiHotspotsForce(String city, String basePath) {
        ingestUhiHotspots(city, basePath);
    }

    private void ingestVegetationMap(String city, String basePath) {
        try {
            JsonNode root = loadJsonFile(basePath + "vegetation_map.json");
            if (root == null) return;

            List<VegetationPointEntity> entities = new ArrayList<>();
            JsonNode points = root.has("points") ? root.get("points") : root;

            if (points.isArray()) {
                for (JsonNode node : points) {
                    VegetationPointEntity entity = VegetationPointEntity.builder()
                            .city(city)
                            .pointId(getTextValue(node, "id"))
                            .lat(getDoubleValue(node, "lat"))
                            .lng(getDoubleValue(node, "lng"))
                            .ndviSentinel(getDoubleValue(node, "NDVI_Sentinel"))
                            .ndviLandsat(getDoubleValue(node, "NDVI_Landsat"))
                            .healthLabel(getTextValue(node, "health_label"))
                            .healthScore(getDoubleValue(node, "health_score"))
                            .colorHex(getTextValue(node, "color_hex"))
                            .lstCelsius(getDoubleValue(node, "lst_celsius"))
                            .smSurface(getDoubleValue(node, "sm_surface"))
                            .build();
                    entities.add(entity);
                }
            }

            vegetationPointRepository.saveAll(entities);
            log.info("Ingested {} vegetation points for {}", entities.size(), city);
        } catch (Exception e) {
            log.warn("Failed to ingest vegetation map for {}: {}", city, e.getMessage());
        }
    }

    private void ingestVegetationMapForce(String city, String basePath) {
        ingestVegetationMap(city, basePath);
    }

    private void ingestVegetationAlerts(String city, String basePath) {
        try {
            JsonNode root = loadJsonFile(basePath + "vegetation_alerts.json");
            if (root == null) return;

            List<VegetationAlertEntity> entities = new ArrayList<>();
            JsonNode alerts = root.has("alerts") ? root.get("alerts") : root;

            if (alerts.isArray()) {
                for (JsonNode node : alerts) {
                    VegetationAlertEntity entity = VegetationAlertEntity.builder()
                            .city(city)
                            .pointId(getTextValue(node, "id"))
                            .lat(getDoubleValue(node, "lat"))
                            .lng(getDoubleValue(node, "lng"))
                            .ndviSentinel(getDoubleValue(node, "NDVI_Sentinel"))
                            .zScore(getDoubleValue(node, "z_score"))
                            .severity(getTextValue(node, "severity"))
                            .message(getTextValue(node, "message"))
                            .build();
                    entities.add(entity);
                }
            }

            vegetationAlertRepository.saveAll(entities);
            log.info("Ingested {} vegetation alerts for {}", entities.size(), city);
        } catch (Exception e) {
            log.warn("Failed to ingest vegetation alerts for {}: {}", city, e.getMessage());
        }
    }

    private void ingestVegetationAlertsForce(String city, String basePath) {
        ingestVegetationAlerts(city, basePath);
    }

    private void ingestPollutionRiskMap(String city, String basePath) {
        try {
            JsonNode root = loadJsonFile(basePath + "pollution_risk_map.json");
            if (root == null) return;

            List<PollutionPointEntity> entities = new ArrayList<>();
            JsonNode points = root.has("points") ? root.get("points") : root;

            if (points.isArray()) {
                for (JsonNode node : points) {
                    PollutionPointEntity entity = PollutionPointEntity.builder()
                            .city(city)
                            .pointId(getTextValue(node, "id"))
                            .lat(getDoubleValue(node, "lat"))
                            .lng(getDoubleValue(node, "lng"))
                            .riskScore(getDoubleValue(node, "risk_score"))
                            .riskCategory(getTextValue(node, "risk_category"))
                            .colorHex(getTextValue(node, "color_hex"))
                            .isExtremeOutlier(getBooleanValue(node, "is_extreme_outlier"))
                            .lstCelsius(getDoubleValue(node, "lst_celsius"))
                            .ndviSentinel(getDoubleValue(node, "NDVI_Sentinel"))
                            .smSurface(getDoubleValue(node, "sm_surface"))
                            .build();
                    entities.add(entity);
                }
            }

            pollutionPointRepository.saveAll(entities);
            log.info("Ingested {} pollution risk points for {}", entities.size(), city);
        } catch (Exception e) {
            log.warn("Failed to ingest pollution risk map for {}: {}", city, e.getMessage());
        }
    }

    private void ingestPollutionRiskMapForce(String city, String basePath) {
        ingestPollutionRiskMap(city, basePath);
    }

    private void ingestPollutionHotspots(String city, String basePath) {
        try {
            JsonNode root = loadJsonFile(basePath + "pollution_hotspots.json");
            if (root == null) return;

            List<PollutionHotspotEntity> entities = new ArrayList<>();
            JsonNode hotspots = root.has("hotspots") ? root.get("hotspots") : root;

            if (hotspots.isArray()) {
                for (JsonNode node : hotspots) {
                    PollutionHotspotEntity entity = PollutionHotspotEntity.builder()
                            .city(city)
                            .zoneName(getTextValue(node, "zone_name"))
                            .avgRiskScore(getDoubleValue(node, "avg_risk_score"))
                            .maxRiskScore(getDoubleValue(node, "max_risk_score"))
                            .pointCount(getIntValue(node, "point_count"))
                            .severity(getTextValue(node, "severity"))
                            .recommendedAction(getTextValue(node, "recommended_action"))
                            .responsibleDept(getTextValue(node, "responsible_dept"))
                            .centerLat(getDoubleValue(node, "center_lat"))
                            .centerLng(getDoubleValue(node, "center_lng"))
                            .boundaryPoints(getArrayAsString(node, "boundary_points"))
                            .build();
                    entities.add(entity);
                }
            }

            pollutionHotspotRepository.saveAll(entities);
            log.info("Ingested {} pollution hotspots for {}", entities.size(), city);
        } catch (Exception e) {
            log.warn("Failed to ingest pollution hotspots for {}: {}", city, e.getMessage());
        }
    }

    private void ingestPollutionHotspotsForce(String city, String basePath) {
        ingestPollutionHotspots(city, basePath);
    }

    private void ingestForecastTrend(String city, String basePath) {
        try {
            JsonNode root = loadJsonFile(basePath + "forecast_trend.json");
            if (root == null) return;

            List<ForecastStepEntity> entities = new ArrayList<>();

            JsonNode historical = root.get("historical");
            if (historical != null && historical.isArray()) {
                for (JsonNode node : historical) {
                    ForecastStepEntity entity = ForecastStepEntity.builder()
                            .city(city)
                            .step(getIntValue(node, "step"))
                            .valueCelsius(getDoubleValue(node, "value_celsius"))
                            .stepType("historical")
                            .build();
                    entities.add(entity);
                }
            }

            JsonNode forecast = root.get("forecast");
            if (forecast != null && forecast.isArray()) {
                for (JsonNode node : forecast) {
                    ForecastStepEntity entity = ForecastStepEntity.builder()
                            .city(city)
                            .step(getIntValue(node, "step"))
                            .predictedCelsius(getDoubleValue(node, "predicted_celsius"))
                            .lowerBound(getDoubleValue(node, "lower_bound_celsius"))
                            .upperBound(getDoubleValue(node, "upper_bound_celsius"))
                            .stepType("predicted")
                            .build();
                    entities.add(entity);
                }
            }

            forecastStepRepository.saveAll(entities);
            log.info("Ingested {} forecast steps for {}", entities.size(), city);
        } catch (Exception e) {
            log.warn("Failed to ingest forecast trend for {}: {}", city, e.getMessage());
        }
    }

    private void ingestForecastTrendForce(String city, String basePath) {
        ingestForecastTrend(city, basePath);
    }

    private void ingestActionPlan(String city, String basePath) {
        try {
            JsonNode root = loadJsonFile(basePath + "action_plan.json");
            if (root == null) return;

            List<ActionItemEntity> entities = new ArrayList<>();
            JsonNode actions = root.has("actions") ? root.get("actions") : root;

            if (actions.isArray()) {
                for (JsonNode node : actions) {
                    ActionItemEntity entity = ActionItemEntity.builder()
                            .city(city)
                            .actionId(getTextValue(node, "action_id"))
                            .title(getTextValue(node, "title"))
                            .description(getTextValue(node, "description"))
                            .priority(getTextValue(node, "priority"))
                            .priorityScore(getIntValue(node, "priority_score"))
                            .zoneName(getTextValue(node, "zone_name"))
                            .centerLat(getDoubleValue(node, "center_lat"))
                            .centerLng(getDoubleValue(node, "center_lng"))
                            .affectedCount(getIntValue(node, "affected_count"))
                            .modelTriggeredBy(getTextValue(node, "model_triggered_by"))
                            .keyFinding(getTextValue(node, "key_finding"))
                            .satelliteSources(getTextValue(node, "satellite_sources"))
                            .responsibleDept(getTextValue(node, "responsible_dept"))
                            .deadline(getTextValue(node, "deadline"))
                            .expectedImpact(getTextValue(node, "expected_impact"))
                            .status(getTextValue(node, "status", "pending"))
                            .generatedAt(LocalDateTime.now())
                            .build();
                    entities.add(entity);
                }
            }

            actionItemRepository.saveAll(entities);
            log.info("Ingested {} action items for {}", entities.size(), city);
        } catch (Exception e) {
            log.warn("Failed to ingest action plan for {}: {}", city, e.getMessage());
        }
    }

    private void ingestActionPlanForce(String city, String basePath) {
        ingestActionPlan(city, basePath);
    }

    private JsonNode loadJsonFile(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                log.warn("JSON file not found: {}", path);
                return null;
            }
            InputStream inputStream = resource.getInputStream();
            return objectMapper.readTree(inputStream);
        } catch (Exception e) {
            log.warn("Failed to load JSON file {}: {}", path, e.getMessage());
            return null;
        }
    }

    private String getTextValue(JsonNode node, String field) {
        return getTextValue(node, field, null);
    }

    private String getTextValue(JsonNode node, String field, String defaultValue) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode != null && !fieldNode.isNull()) {
            return fieldNode.asText();
        }
        return defaultValue;
    }

    private Double getDoubleValue(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode != null && !fieldNode.isNull() && fieldNode.isNumber()) {
            return fieldNode.asDouble();
        }
        return null;
    }

    private Integer getIntValue(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode != null && !fieldNode.isNull() && fieldNode.isNumber()) {
            return fieldNode.asInt();
        }
        return null;
    }

    private Boolean getBooleanValue(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode != null && !fieldNode.isNull()) {
            return fieldNode.asBoolean();
        }
        return null;
    }

    private String getArrayAsString(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode != null && !fieldNode.isNull()) {
            try {
                return objectMapper.writeValueAsString(fieldNode);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
