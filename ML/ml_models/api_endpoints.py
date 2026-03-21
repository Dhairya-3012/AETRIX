"""
Combined FastAPI Endpoints for All Environmental Analysis Features
====================================================================
Features:
- Feature 2: UHI Detection
- Feature 3: Vegetation Stress
- Feature 4: Pollution Hotspot
- Feature 5: Trend Forecast
- Feature 6: Action Plan Generator
"""

from fastapi import APIRouter, HTTPException, Query, Response
from pydantic import BaseModel, Field
from typing import Dict, List, Any, Optional
import os
from datetime import datetime

# Import models
from .uhi_detection import UHIDetectionModel
from .vegetation_stress import VegetationStressModel
from .pollution_hotspot import PollutionHotspotModel
from .trend_forecast import TrendForecastModel
from .action_plan import ActionPlanGenerator


# ============================================================================
# PYDANTIC MODELS
# ============================================================================

class ScenarioRequest(BaseModel):
    """Request for scenario prediction."""
    scenario: str = Field(default="ndvi_drop")
    change_pct: float = Field(..., ge=-50, le=50)
    parameter: str = Field(default="NDVI_Sentinel")


class ActionPlanRequest(BaseModel):
    """Request for action plan generation."""
    city: str = Field(default="Ahmedabad")
    include_features: List[str] = Field(default=["uhi", "vegetation", "pollution", "forecast"])


class StatusUpdateRequest(BaseModel):
    """Request for updating action status."""
    status: str
    notes: str = ""
    updated_by: str = ""


# ============================================================================
# GLOBAL MODEL INSTANCES
# ============================================================================

_models: Dict[str, Any] = {
    'uhi': None,
    'vegetation': None,
    'pollution': None,
    'forecast': None,
    'action_plan': None
}

_initialized = False


def get_model(model_name: str):
    """Get a model instance."""
    if not _initialized:
        raise HTTPException(
            status_code=503,
            detail="Models not initialized. Call /api/initialize first."
        )
    return _models.get(model_name)


# ============================================================================
# ROUTER SETUP
# ============================================================================

router = APIRouter(prefix="/api", tags=["Environmental Analysis"])


# ============================================================================
# INITIALIZATION ENDPOINT
# ============================================================================

@router.post("/initialize")
async def initialize_all_models(
    csv_path: str = Query(
        default="Ahmedabad_MultiSatellite_Data.csv",
        description="Path to satellite data CSV"
    )
):
    """
    Initialize all ML models.

    This must be called before using any other endpoints.
    """
    global _models, _initialized

    if not os.path.exists(csv_path):
        raise HTTPException(status_code=404, detail=f"CSV file not found: {csv_path}")

    try:
        # Initialize UHI model
        _models['uhi'] = UHIDetectionModel(csv_path)
        _models['uhi'].train_all_models()

        # Initialize Vegetation model
        _models['vegetation'] = VegetationStressModel(csv_path)
        _models['vegetation'].train_all_models()

        # Initialize Pollution model
        _models['pollution'] = PollutionHotspotModel(csv_path)
        _models['pollution'].train_all_models()

        # Initialize Forecast model
        _models['forecast'] = TrendForecastModel(csv_path)
        _models['forecast'].train_all_models()

        # Initialize Action Plan Generator (references other models)
        _models['action_plan'] = ActionPlanGenerator(csv_path)
        _models['action_plan'].uhi_model = _models['uhi']
        _models['action_plan'].vegetation_model = _models['vegetation']
        _models['action_plan'].pollution_model = _models['pollution']
        _models['action_plan'].forecast_model = _models['forecast']

        _initialized = True

        return {
            'status': 'success',
            'message': 'All models initialized successfully',
            'models_loaded': list(_models.keys()),
            'data_points': len(_models['uhi'].processed_df)
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/status")
async def get_system_status():
    """Get system status and model availability."""
    return {
        'initialized': _initialized,
        'models': {
            'uhi': _models['uhi'] is not None,
            'vegetation': _models['vegetation'] is not None,
            'pollution': _models['pollution'] is not None,
            'forecast': _models['forecast'] is not None,
            'action_plan': _models['action_plan'] is not None
        }
    }


# ============================================================================
# FEATURE 2: UHI DETECTION ENDPOINTS
# ============================================================================

@router.get("/uhi/heatmap")
async def get_uhi_heatmap():
    """Get UHI heatmap data with all points and anomaly labels."""
    model = get_model('uhi')
    return model.get_heatmap_data()


@router.get("/uhi/hotspots")
async def get_uhi_hotspots():
    """Get DBSCAN-identified UHI hotspot zones."""
    model = get_model('uhi')
    return model.get_hotspots_data()


@router.get("/uhi/top-hottest")
async def get_uhi_top_hottest(limit: int = Query(default=10, ge=1, le=50)):
    """Get top hottest locations ranked by LST."""
    model = get_model('uhi')
    return model.get_top_hottest(top_n=limit)


@router.get("/uhi/feature-importance")
async def get_uhi_feature_importance():
    """Get feature importance from UHI Random Forest model."""
    model = get_model('uhi')
    return model.get_feature_importance_response()


@router.post("/uhi/predict")
async def predict_uhi_lst(lat: float, lng: float, NDVI_Sentinel: float, sm_surface: float):
    """Predict LST at a specific location."""
    model = get_model('uhi')
    result = model.predict_lst(lat, lng, NDVI_Sentinel, sm_surface)
    return {
        'predicted_lst_celsius': result.predicted_lst_celsius,
        'predicted_lst_raw': result.predicted_lst_raw,
        'heat_risk': result.heat_risk,
        'confidence': result.confidence,
        'message': result.message
    }


# ============================================================================
# FEATURE 3: VEGETATION STRESS ENDPOINTS
# ============================================================================

@router.get("/vegetation/map")
async def get_vegetation_map():
    """Get vegetation map with health classification for all points."""
    model = get_model('vegetation')
    return model.get_vegetation_map_data()


@router.get("/vegetation/alerts")
async def get_vegetation_alerts():
    """Get critical vegetation stress alerts (Z-score anomalies)."""
    model = get_model('vegetation')
    return model.get_alerts_data()


@router.get("/vegetation/plantation-zones")
async def get_plantation_zones(limit: int = Query(default=10, ge=1, le=50)):
    """Get top recommended plantation zones."""
    model = get_model('vegetation')
    return model.get_plantation_zones_data(top_n=limit)


@router.get("/vegetation/feature-importance")
async def get_vegetation_feature_importance():
    """Get feature importance for NDVI drivers."""
    model = get_model('vegetation')
    return model.get_feature_importance_data()


# ============================================================================
# FEATURE 4: POLLUTION HOTSPOT ENDPOINTS
# ============================================================================

@router.get("/pollution/risk-map")
async def get_pollution_risk_map():
    """Get pollution risk map with scores and categories."""
    model = get_model('pollution')
    return model.get_risk_map_data()


@router.get("/pollution/hotspots")
async def get_pollution_hotspots():
    """Get DBSCAN-identified pollution cluster zones."""
    model = get_model('pollution')
    return model.get_hotspots_data()


@router.get("/pollution/extreme-outliers")
async def get_pollution_outliers():
    """Get Isolation Forest flagged extreme risk locations."""
    model = get_model('pollution')
    return model.get_extreme_outliers_data()


@router.get("/pollution/compliance-report")
async def get_compliance_report():
    """Get compliance status report for regulators."""
    model = get_model('pollution')
    return model.get_compliance_report()


# ============================================================================
# FEATURE 5: TREND FORECAST ENDPOINTS
# ============================================================================

@router.get("/forecast/lst-trend")
async def get_lst_trend():
    """Get ARIMA forecast for LST sequence."""
    model = get_model('forecast')
    return model.get_lst_trend_data()


@router.post("/forecast/scenario")
async def predict_scenario(request: ScenarioRequest):
    """What-if scenario prediction."""
    model = get_model('forecast')
    return model.predict_scenario(
        scenario=request.scenario,
        change_pct=request.change_pct,
        parameter=request.parameter
    )


@router.get("/forecast/feature-importance")
async def get_forecast_feature_importance():
    """Get feature importance for LST prediction."""
    model = get_model('forecast')
    return model.get_feature_importance_data()


@router.get("/forecast/breach-analysis")
async def get_breach_analysis(threshold: float = Query(default=35.0)):
    """Analyze threshold breach scenarios."""
    model = get_model('forecast')
    return model.analyze_breach_thresholds(threshold_celsius=threshold)


# ============================================================================
# FEATURE 6: ACTION PLAN ENDPOINTS
# ============================================================================

@router.post("/action-plan/generate")
async def generate_action_plan(request: ActionPlanRequest):
    """Generate comprehensive action plan from all ML outputs."""
    generator = get_model('action_plan')
    return generator.generate_plan(include_features=request.include_features)


@router.get("/action-plan/summary")
async def get_action_plan_summary():
    """Get quick summary of action plan for dashboard."""
    generator = get_model('action_plan')
    return generator.get_summary()


@router.patch("/action-plan/update-status/{action_id}")
async def update_action_status(action_id: int, request: StatusUpdateRequest):
    """Update status of an action item."""
    generator = get_model('action_plan')
    return generator.update_action_status(
        action_id=action_id,
        status=request.status,
        notes=request.notes,
        updated_by=request.updated_by
    )


@router.get("/action-plan/export-pdf")
async def export_action_plan_pdf():
    """
    Get action plan data for PDF generation.

    Note: Actual PDF generation requires reportlab.
    This returns the structured data for PDF creation.
    """
    generator = get_model('action_plan')
    pdf_data = generator.export_pdf_data()

    # Return as JSON (actual PDF generation would require reportlab)
    return {
        'content_type': 'application/json',
        'filename': f"Ahmedabad_ActionPlan_{datetime.now().strftime('%Y-%m-%d')}.json",
        'data': pdf_data,
        'note': 'Install reportlab for actual PDF generation'
    }


# ============================================================================
# COMBINED DASHBOARD ENDPOINT
# ============================================================================

@router.get("/dashboard/overview")
async def get_dashboard_overview():
    """Get combined overview for dashboard display."""
    if not _initialized:
        raise HTTPException(status_code=503, detail="Models not initialized")

    uhi = _models['uhi']
    veg = _models['vegetation']
    pol = _models['pollution']
    forecast = _models['forecast']

    return {
        'city': 'Ahmedabad',
        'last_updated': datetime.now().isoformat(),
        'data_points': len(uhi.processed_df),
        'uhi_summary': {
            'mean_lst_celsius': round(uhi.city_mean_lst, 2),
            'max_lst_celsius': round(uhi.city_max_lst, 2),
            'heat_anomalies': int(uhi.processed_df['is_heat_anomaly'].sum()),
            'hotspot_zones': len(uhi.hotspot_zones)
        },
        'vegetation_summary': {
            'mean_ndvi': round(veg.city_mean_ndvi, 4),
            'healthy_pct': round(100 * (veg.processed_df['health_label'] == 'Healthy').sum() / len(veg.processed_df), 1),
            'stressed_pct': round(100 * (veg.processed_df['health_label'] == 'Stressed').sum() / len(veg.processed_df), 1),
            'barren_pct': round(100 * (veg.processed_df['health_label'] == 'Barren').sum() / len(veg.processed_df), 1)
        },
        'pollution_summary': {
            'mean_risk_score': round(pol.city_mean_risk, 1),
            'critical_zones': sum(1 for z in pol.hotspot_zones if z.severity == 'Critical'),
            'extreme_outliers': int(pol.processed_df['is_extreme_outlier'].sum())
        },
        'forecast_summary': {
            'trend_direction': forecast.get_trend_direction()[0],
            'trend_rate': f"{forecast.get_trend_direction()[1]:+.2f}°C/100 steps",
            'top_driver': forecast.get_feature_importance()[0].feature if forecast.get_feature_importance() else None
        }
    }


# ============================================================================
# MAIN APP RUNNER
# ============================================================================

def create_app():
    """Create FastAPI app with all routers."""
    from fastapi import FastAPI

    app = FastAPI(
        title="Ahmedabad Environmental Analysis API",
        description="ML-powered environmental analysis for Urban Heat Islands, Vegetation Stress, Pollution Hotspots, and Trend Forecasting",
        version="1.0.0"
    )

    app.include_router(router)

    return app


if __name__ == "__main__":
    import uvicorn

    app = create_app()
    uvicorn.run(app, host="0.0.0.0", port=8000)
