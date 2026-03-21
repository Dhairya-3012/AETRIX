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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileIngestionService {

    private final ObjectMapper objectMapper;
    private final UhiHeatmapRepository uhiHeatmapRepository;
    private final UhiHotspotRepository uhiHotspotRepository;
    private final VegetationPointRepository vegetationPointRepository;
    private final VegetationAlertRepository vegetationAlertRepository;
    private final PollutionPointRepository pollutionPointRepository;
    private final PollutionHotspotRepository pollutionHotspotRepository;
    private final ForecastStepRepository forecastStepRepository;
    private final ActionItemRepository actionItemRepository;

    @PostConstruct
    @Transactional
    public void ingestAll() {
        log.info("Starting AETRIX data ingestion from JSON files...");

        ingestUhiHeatmap();
        ingestUhiHotspots();
        ingestVegetationMap();
        ingestVegetationAlerts();
        ingestPollutionRiskMap();
        ingestPollutionHotspots();
        ingestForecastTrend();
        ingestActionPlan();

        log.info("AETRIX data ingestion completed successfully.");
    }

    /**
     * Force re-ingest all data by clearing existing data first.
     * Use this to fix corrupted/incomplete data.
     */
    @Transactional
    public void forceReIngestAll() {
        log.info("Force re-ingesting all data (clearing existing data first)...");

        // Clear all existing data
        vegetationAlertRepository.deleteAll();
        vegetationPointRepository.deleteAll();
        uhiHotspotRepository.deleteAll();
        uhiHeatmapRepository.deleteAll();
        pollutionHotspotRepository.deleteAll();
        pollutionPointRepository.deleteAll();
        forecastStepRepository.deleteAll();
        actionItemRepository.deleteAll();

        log.info("Cleared all existing data. Starting fresh ingestion...");

        // Re-ingest all
        ingestUhiHeatmapForce();
        ingestUhiHotspotsForce();
        ingestVegetationMapForce();
        ingestVegetationAlertsForce();
        ingestPollutionRiskMapForce();
        ingestPollutionHotspotsForce();
        ingestForecastTrendForce();
        ingestActionPlanForce();

        log.info("Force re-ingestion completed successfully.");
    }

    private void ingestUhiHeatmapForce() {
        try {
            JsonNode root = loadJsonFile("data/uhi_heatmap.json");
            if (root == null) return;

            List<UhiHeatmapPointEntity> entities = new ArrayList<>();
            JsonNode points = root.has("points") ? root.get("points") : root;

            if (points.isArray()) {
                for (JsonNode node : points) {
                    UhiHeatmapPointEntity entity = UhiHeatmapPointEntity.builder()
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
            log.info("Force ingested {} UHI heatmap points", entities.size());
        } catch (Exception e) {
            log.error("Failed to force ingest UHI heatmap: {}", e.getMessage());
        }
    }

    private void ingestUhiHotspotsForce() {
        try {
            JsonNode root = loadJsonFile("data/uhi_hotspots.json");
            if (root == null) return;

            List<UhiHotspotEntity> entities = new ArrayList<>();
            JsonNode hotspots = root.has("hotspots") ? root.get("hotspots") : root;

            if (hotspots.isArray()) {
                for (JsonNode node : hotspots) {
                    UhiHotspotEntity entity = UhiHotspotEntity.builder()
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
            log.info("Force ingested {} UHI hotspots", entities.size());
        } catch (Exception e) {
            log.error("Failed to force ingest UHI hotspots: {}", e.getMessage());
        }
    }

    private void ingestVegetationMapForce() {
        try {
            JsonNode root = loadJsonFile("data/vegetation_map.json");
            if (root == null) return;

            List<VegetationPointEntity> entities = new ArrayList<>();
            JsonNode points = root.has("points") ? root.get("points") : root;

            if (points.isArray()) {
                for (JsonNode node : points) {
                    VegetationPointEntity entity = VegetationPointEntity.builder()
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
            log.info("Force ingested {} vegetation points", entities.size());
        } catch (Exception e) {
            log.error("Failed to force ingest vegetation map: {}", e.getMessage());
        }
    }

    private void ingestVegetationAlertsForce() {
        try {
            JsonNode root = loadJsonFile("data/vegetation_alerts.json");
            if (root == null) return;

            List<VegetationAlertEntity> entities = new ArrayList<>();
            JsonNode alerts = root.has("alerts") ? root.get("alerts") : root;

            if (alerts.isArray()) {
                for (JsonNode node : alerts) {
                    VegetationAlertEntity entity = VegetationAlertEntity.builder()
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
            log.info("Force ingested {} vegetation alerts", entities.size());
        } catch (Exception e) {
            log.error("Failed to force ingest vegetation alerts: {}", e.getMessage());
        }
    }

    private void ingestPollutionRiskMapForce() {
        try {
            JsonNode root = loadJsonFile("data/pollution_risk_map.json");
            if (root == null) return;

            List<PollutionPointEntity> entities = new ArrayList<>();
            JsonNode points = root.has("points") ? root.get("points") : root;

            if (points.isArray()) {
                for (JsonNode node : points) {
                    PollutionPointEntity entity = PollutionPointEntity.builder()
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
            log.info("Force ingested {} pollution risk points", entities.size());
        } catch (Exception e) {
            log.error("Failed to force ingest pollution risk map: {}", e.getMessage());
        }
    }

    private void ingestPollutionHotspotsForce() {
        try {
            JsonNode root = loadJsonFile("data/pollution_hotspots.json");
            if (root == null) return;

            List<PollutionHotspotEntity> entities = new ArrayList<>();
            JsonNode hotspots = root.has("hotspots") ? root.get("hotspots") : root;

            if (hotspots.isArray()) {
                for (JsonNode node : hotspots) {
                    PollutionHotspotEntity entity = PollutionHotspotEntity.builder()
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
            log.info("Force ingested {} pollution hotspots", entities.size());
        } catch (Exception e) {
            log.error("Failed to force ingest pollution hotspots: {}", e.getMessage());
        }
    }

    private void ingestForecastTrendForce() {
        try {
            JsonNode root = loadJsonFile("data/forecast_trend.json");
            if (root == null) return;

            List<ForecastStepEntity> entities = new ArrayList<>();

            JsonNode historical = root.get("historical");
            if (historical != null && historical.isArray()) {
                for (JsonNode node : historical) {
                    ForecastStepEntity entity = ForecastStepEntity.builder()
                            .step(getIntValue(node, "step"))
                            .valueCelsius(getDoubleValue(node, "value_celsius"))
                            .stepType("historical")
                            .build();
                    entities.add(entity);
                }
            }

            JsonNode predicted = root.get("predicted");
            if (predicted != null && predicted.isArray()) {
                for (JsonNode node : predicted) {
                    ForecastStepEntity entity = ForecastStepEntity.builder()
                            .step(getIntValue(node, "step"))
                            .predictedCelsius(getDoubleValue(node, "predicted_celsius"))
                            .lowerBound(getDoubleValue(node, "lower_bound"))
                            .upperBound(getDoubleValue(node, "upper_bound"))
                            .stepType("predicted")
                            .build();
                    entities.add(entity);
                }
            }

            forecastStepRepository.saveAll(entities);
            log.info("Force ingested {} forecast steps", entities.size());
        } catch (Exception e) {
            log.error("Failed to force ingest forecast trend: {}", e.getMessage());
        }
    }

    private void ingestActionPlanForce() {
        try {
            JsonNode root = loadJsonFile("data/action_plan.json");
            if (root == null) return;

            List<ActionItemEntity> entities = new ArrayList<>();
            JsonNode actions = root.has("actions") ? root.get("actions") : root;

            if (actions.isArray()) {
                for (JsonNode node : actions) {
                    ActionItemEntity entity = ActionItemEntity.builder()
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
            log.info("Force ingested {} action items", entities.size());
        } catch (Exception e) {
            log.error("Failed to force ingest action plan: {}", e.getMessage());
        }
    }

    private void ingestUhiHeatmap() {
        if (uhiHeatmapRepository.count() > 0) {
            log.info("UHI heatmap data already exists, skipping ingestion.");
            return;
        }

        try {
            JsonNode root = loadJsonFile("data/uhi_heatmap.json");
            if (root == null) return;

            List<UhiHeatmapPointEntity> entities = new ArrayList<>();
            JsonNode points = root.has("points") ? root.get("points") : root;

            if (points.isArray()) {
                for (JsonNode node : points) {
                    UhiHeatmapPointEntity entity = UhiHeatmapPointEntity.builder()
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
            log.info("Ingested {} UHI heatmap points", entities.size());
        } catch (Exception e) {
            log.warn("Failed to ingest UHI heatmap: {}", e.getMessage());
        }
    }

    private void ingestUhiHotspots() {
        if (uhiHotspotRepository.count() > 0) {
            log.info("UHI hotspots data already exists, skipping ingestion.");
            return;
        }

        try {
            JsonNode root = loadJsonFile("data/uhi_hotspots.json");
            if (root == null) return;

            List<UhiHotspotEntity> entities = new ArrayList<>();
            JsonNode hotspots = root.has("hotspots") ? root.get("hotspots") : root;

            if (hotspots.isArray()) {
                for (JsonNode node : hotspots) {
                    UhiHotspotEntity entity = UhiHotspotEntity.builder()
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
            log.info("Ingested {} UHI hotspots", entities.size());
        } catch (Exception e) {
            log.warn("Failed to ingest UHI hotspots: {}", e.getMessage());
        }
    }

    private void ingestVegetationMap() {
        if (vegetationPointRepository.count() > 0) {
            log.info("Vegetation map data already exists, skipping ingestion.");
            return;
        }

        try {
            JsonNode root = loadJsonFile("data/vegetation_map.json");
            if (root == null) return;

            List<VegetationPointEntity> entities = new ArrayList<>();
            JsonNode points = root.has("points") ? root.get("points") : root;

            if (points.isArray()) {
                for (JsonNode node : points) {
                    VegetationPointEntity entity = VegetationPointEntity.builder()
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
            log.info("Ingested {} vegetation points", entities.size());
        } catch (Exception e) {
            log.warn("Failed to ingest vegetation map: {}", e.getMessage());
        }
    }

    private void ingestVegetationAlerts() {
        if (vegetationAlertRepository.count() > 0) {
            log.info("Vegetation alerts data already exists, skipping ingestion.");
            return;
        }

        try {
            JsonNode root = loadJsonFile("data/vegetation_alerts.json");
            if (root == null) return;

            List<VegetationAlertEntity> entities = new ArrayList<>();
            JsonNode alerts = root.has("alerts") ? root.get("alerts") : root;

            if (alerts.isArray()) {
                for (JsonNode node : alerts) {
                    VegetationAlertEntity entity = VegetationAlertEntity.builder()
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
            log.info("Ingested {} vegetation alerts", entities.size());
        } catch (Exception e) {
            log.warn("Failed to ingest vegetation alerts: {}", e.getMessage());
        }
    }

    private void ingestPollutionRiskMap() {
        if (pollutionPointRepository.count() > 0) {
            log.info("Pollution risk map data already exists, skipping ingestion.");
            return;
        }

        try {
            JsonNode root = loadJsonFile("data/pollution_risk_map.json");
            if (root == null) return;

            List<PollutionPointEntity> entities = new ArrayList<>();
            JsonNode points = root.has("points") ? root.get("points") : root;

            if (points.isArray()) {
                for (JsonNode node : points) {
                    PollutionPointEntity entity = PollutionPointEntity.builder()
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
            log.info("Ingested {} pollution risk points", entities.size());
        } catch (Exception e) {
            log.warn("Failed to ingest pollution risk map: {}", e.getMessage());
        }
    }

    private void ingestPollutionHotspots() {
        if (pollutionHotspotRepository.count() > 0) {
            log.info("Pollution hotspots data already exists, skipping ingestion.");
            return;
        }

        try {
            JsonNode root = loadJsonFile("data/pollution_hotspots.json");
            if (root == null) return;

            List<PollutionHotspotEntity> entities = new ArrayList<>();
            JsonNode hotspots = root.has("hotspots") ? root.get("hotspots") : root;

            if (hotspots.isArray()) {
                for (JsonNode node : hotspots) {
                    PollutionHotspotEntity entity = PollutionHotspotEntity.builder()
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
            log.info("Ingested {} pollution hotspots", entities.size());
        } catch (Exception e) {
            log.warn("Failed to ingest pollution hotspots: {}", e.getMessage());
        }
    }

    private void ingestForecastTrend() {
        if (forecastStepRepository.count() > 0) {
            log.info("Forecast trend data already exists, skipping ingestion.");
            return;
        }

        try {
            JsonNode root = loadJsonFile("data/forecast_trend.json");
            if (root == null) return;

            List<ForecastStepEntity> entities = new ArrayList<>();

            // Historical data
            JsonNode historical = root.get("historical");
            if (historical != null && historical.isArray()) {
                for (JsonNode node : historical) {
                    ForecastStepEntity entity = ForecastStepEntity.builder()
                            .step(getIntValue(node, "step"))
                            .valueCelsius(getDoubleValue(node, "value_celsius"))
                            .stepType("historical")
                            .build();
                    entities.add(entity);
                }
            }

            // Predicted data
            JsonNode predicted = root.get("predicted");
            if (predicted != null && predicted.isArray()) {
                for (JsonNode node : predicted) {
                    ForecastStepEntity entity = ForecastStepEntity.builder()
                            .step(getIntValue(node, "step"))
                            .predictedCelsius(getDoubleValue(node, "predicted_celsius"))
                            .lowerBound(getDoubleValue(node, "lower_bound"))
                            .upperBound(getDoubleValue(node, "upper_bound"))
                            .stepType("predicted")
                            .build();
                    entities.add(entity);
                }
            }

            forecastStepRepository.saveAll(entities);
            log.info("Ingested {} forecast steps", entities.size());
        } catch (Exception e) {
            log.warn("Failed to ingest forecast trend: {}", e.getMessage());
        }
    }

    private void ingestActionPlan() {
        if (actionItemRepository.count() > 0) {
            log.info("Action plan data already exists, skipping ingestion.");
            return;
        }

        try {
            JsonNode root = loadJsonFile("data/action_plan.json");
            if (root == null) return;

            List<ActionItemEntity> entities = new ArrayList<>();
            JsonNode actions = root.has("actions") ? root.get("actions") : root;

            if (actions.isArray()) {
                for (JsonNode node : actions) {
                    ActionItemEntity entity = ActionItemEntity.builder()
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
            log.info("Ingested {} action items", entities.size());
        } catch (Exception e) {
            log.warn("Failed to ingest action plan: {}", e.getMessage());
        }
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
