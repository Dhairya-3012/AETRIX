"""
Environmental Analysis ML Models
================================

A comprehensive ML package for satellite-based environmental analysis.

Features:
---------
- Feature 2: Urban Heat Island (UHI) Detection
- Feature 3: Vegetation Stress & NDVI Monitoring
- Feature 4: Air Quality & Pollution Hotspot Finder
- Feature 5: Trend Prediction & Forecasting
- Feature 6: Environment Action Plan Generator

Usage:
------
    from ml_models import (
        UHIDetectionModel,
        VegetationStressModel,
        PollutionHotspotModel,
        TrendForecastModel,
        ActionPlanGenerator,
        get_model_accuracy
    )

    # Train individual models
    uhi = UHIDetectionModel("Ahmedabad_TimeSeries_Final.csv")
    uhi.train_all_models()

    # Get accuracy metrics
    metrics = get_model_accuracy("Ahmedabad_TimeSeries_Final.csv")
"""

__version__ = "1.0.0"
__author__ = "Environmental Analysis Team"

# Core Models
from .uhi_detection import UHIDetectionModel
from .vegetation_stress import VegetationStressModel
from .pollution_hotspot import PollutionHotspotModel
from .trend_forecast import TrendForecastModel
from .action_plan import ActionPlanGenerator

# Accuracy Metrics
from .accuracy_metrics import ModelAccuracyReporter, get_model_accuracy

__all__ = [
    # Models
    'UHIDetectionModel',
    'VegetationStressModel',
    'PollutionHotspotModel',
    'TrendForecastModel',
    'ActionPlanGenerator',
    # Accuracy
    'ModelAccuracyReporter',
    'get_model_accuracy',
]

# Optional API imports
try:
    from .api_endpoints import router as api_router, create_app
    __all__.extend(['api_router', 'create_app'])
except ImportError:
    pass
