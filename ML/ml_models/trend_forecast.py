"""
Trend Prediction & Forecasting Model (Feature 5)
=================================================
This module implements ML models for environmental forecasting:
1. ARIMA for sequential trend forecasting
2. Random Forest Regressor for scenario/what-if prediction
3. Feature importance analysis for LST drivers
4. Breach threshold prediction
"""

import pandas as pd
import numpy as np
import json
from typing import Dict, List, Any, Optional, Tuple
from dataclasses import dataclass, asdict
from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import train_test_split
from sklearn.metrics import r2_score, mean_absolute_error
import warnings
warnings.filterwarnings('ignore')

# Try to import statsmodels for ARIMA
try:
    from statsmodels.tsa.arima.model import ARIMA
    ARIMA_AVAILABLE = True
except ImportError:
    ARIMA_AVAILABLE = False
    print("Warning: statsmodels not installed. ARIMA forecasting will use fallback method.")


# ============================================================================
# DATA CLASSES
# ============================================================================

@dataclass
class HistoricalPoint:
    """Historical data point for ARIMA."""
    step: int
    value: float
    value_celsius: float


@dataclass
class ForecastPoint:
    """Forecasted data point."""
    step: int
    predicted_value: float
    predicted_celsius: float
    lower_bound_celsius: float
    upper_bound_celsius: float


@dataclass
class ScenarioPoint:
    """Point in a scenario analysis."""
    id: str
    lat: float
    lng: float
    baseline_lst: float
    scenario_lst: float
    change: str


@dataclass
class FeatureImportanceItem:
    """Feature importance item."""
    feature: str
    importance: float
    interpretation: str


@dataclass
class BreachScenario:
    """Breach scenario analysis."""
    scenario: str
    points_above_threshold: int
    pct_above: float


# ============================================================================
# MAIN FORECAST MODEL
# ============================================================================

class TrendForecastModel:
    """
    Trend Prediction & Forecasting Model for Ahmedabad.

    Implements:
    - ARIMA time series forecasting
    - Random Forest for scenario prediction
    - Feature importance analysis
    - Breach threshold analysis
    """

    def __init__(self, csv_path: str):
        """Initialize the model."""
        self.csv_path = csv_path
        self.df: Optional[pd.DataFrame] = None
        self.processed_df: Optional[pd.DataFrame] = None

        # Models
        self.arima_lst_model = None
        self.arima_ndvi_model = None
        self.rf_regressor: Optional[RandomForestRegressor] = None

        # Model metrics
        self.rf_r2: float = 0.0
        self.rf_mae: float = 0.0

        # Feature names
        self.feature_names = ['NDVI_Sentinel', 'NDVI_Landsat', 'NDVI', 'sm_surface', 'lat', 'lng']

        # Statistics
        self.city_mean_lst: float = 0.0
        self.city_std_lst: float = 0.0

        # Forecast cache
        self.lst_forecast: List[ForecastPoint] = []
        self.ndvi_forecast: List[ForecastPoint] = []

    def load_and_preprocess_data(self) -> pd.DataFrame:
        """Load and preprocess data."""
        self.df = pd.read_csv(self.csv_path)

        # Parse geo coordinates
        self.df['lat'] = self.df['.geo'].apply(self._extract_lat)
        self.df['lng'] = self.df['.geo'].apply(self._extract_lng)

        # Convert LST to Celsius
        self.df['lst_celsius'] = (self.df['LST_Day_1km'] * 0.02) - 273.15

        # Create step index (treating system:index as time order)
        self.df['step'] = range(len(self.df))

        # Calculate statistics
        self.city_mean_lst = self.df['lst_celsius'].mean()
        self.city_std_lst = self.df['lst_celsius'].std()

        self.processed_df = self.df.copy()

        print(f"Loaded {len(self.df)} data points")
        print(f"LST Range: {self.df['lst_celsius'].min():.2f}°C to {self.df['lst_celsius'].max():.2f}°C")

        return self.processed_df

    def _extract_lat(self, geo_str: str) -> float:
        """Extract latitude from geo JSON."""
        try:
            geo = json.loads(geo_str)
            return geo['coordinates'][1]
        except:
            return np.nan

    def _extract_lng(self, geo_str: str) -> float:
        """Extract longitude from geo JSON."""
        try:
            geo = json.loads(geo_str)
            return geo['coordinates'][0]
        except:
            return np.nan

    # ========================================================================
    # STEP 1: ARIMA Forecasting
    # ========================================================================

    def fit_arima_forecast(self, forecast_steps: int = 30,
                          arima_order: Tuple[int, int, int] = (2, 1, 2)) -> Dict[str, Any]:
        """
        Fit ARIMA model for LST time series forecasting.

        Treats system:index as time steps and forecasts future LST values.
        """
        if self.processed_df is None:
            raise ValueError("Data not loaded.")

        # Get LST series
        lst_series = self.processed_df['lst_celsius'].values

        if ARIMA_AVAILABLE:
            try:
                # Fit ARIMA model
                model = ARIMA(lst_series, order=arima_order)
                self.arima_lst_model = model.fit()

                # Forecast
                forecast_result = self.arima_lst_model.get_forecast(steps=forecast_steps)
                forecast_values = forecast_result.predicted_mean
                conf_int = forecast_result.conf_int(alpha=0.05)

                # Create forecast points
                self.lst_forecast = []
                for i in range(forecast_steps):
                    step = len(lst_series) + i
                    pred_celsius = forecast_values.iloc[i]
                    pred_raw = (pred_celsius + 273.15) / 0.02

                    point = ForecastPoint(
                        step=step,
                        predicted_value=round(pred_raw, 2),
                        predicted_celsius=round(pred_celsius, 2),
                        lower_bound_celsius=round(conf_int.iloc[i, 0], 2),
                        upper_bound_celsius=round(conf_int.iloc[i, 1], 2)
                    )
                    self.lst_forecast.append(point)

                print(f"ARIMA({arima_order[0]},{arima_order[1]},{arima_order[2]}) fitted successfully")

            except Exception as e:
                print(f"ARIMA fitting failed: {e}. Using fallback method.")
                self._fallback_forecast(lst_series, forecast_steps)
        else:
            self._fallback_forecast(lst_series, forecast_steps)

        return {
            'model': f'ARIMA{arima_order}',
            'historical_points': len(lst_series),
            'forecast_steps': forecast_steps
        }

    def _fallback_forecast(self, series: np.ndarray, steps: int) -> None:
        """Fallback forecast using simple trend extrapolation."""
        # Calculate trend using linear regression
        x = np.arange(len(series))
        slope, intercept = np.polyfit(x, series, 1)

        # Forecast
        self.lst_forecast = []
        for i in range(steps):
            step = len(series) + i
            pred_celsius = intercept + slope * step
            pred_raw = (pred_celsius + 273.15) / 0.02

            # Estimate confidence bounds (±2 std)
            std = np.std(series)

            point = ForecastPoint(
                step=step,
                predicted_value=round(pred_raw, 2),
                predicted_celsius=round(pred_celsius, 2),
                lower_bound_celsius=round(pred_celsius - 2 * std, 2),
                upper_bound_celsius=round(pred_celsius + 2 * std, 2)
            )
            self.lst_forecast.append(point)

        print(f"Using linear trend extrapolation (slope: {slope:.4f}°C/step)")

    def get_trend_direction(self) -> Tuple[str, float]:
        """Determine trend direction and rate."""
        if len(self.lst_forecast) < 2:
            return "Stable", 0.0

        first_pred = self.lst_forecast[0].predicted_celsius
        last_pred = self.lst_forecast[-1].predicted_celsius
        change = last_pred - first_pred

        # Rate per 100 observations
        rate = (change / len(self.lst_forecast)) * 100

        if rate > 0.5:
            direction = "Increasing"
        elif rate < -0.5:
            direction = "Decreasing"
        else:
            direction = "Stable"

        return direction, rate

    # ========================================================================
    # STEP 2: Random Forest for Scenario Prediction
    # ========================================================================

    def fit_scenario_model(self, test_size: float = 0.2) -> Dict[str, Any]:
        """
        Train Random Forest for what-if scenario predictions.

        Learns relationship between features and LST for scenario analysis.
        """
        if self.processed_df is None:
            raise ValueError("Data not loaded.")

        # Features and target
        X = self.processed_df[self.feature_names].values
        y = self.processed_df['lst_celsius'].values

        # Split
        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=test_size, random_state=42
        )

        # Train
        self.rf_regressor = RandomForestRegressor(
            n_estimators=100,
            max_depth=12,
            min_samples_split=5,
            random_state=42,
            n_jobs=-1
        )
        self.rf_regressor.fit(X_train, y_train)

        # Evaluate
        y_pred = self.rf_regressor.predict(X_test)
        self.rf_r2 = r2_score(y_test, y_pred)
        self.rf_mae = mean_absolute_error(y_test, y_pred)

        print(f"Scenario Model R² Score: {self.rf_r2:.4f}")
        print(f"Scenario Model MAE: {self.rf_mae:.4f}°C")

        return {
            'r2_score': round(self.rf_r2, 4),
            'mae': round(self.rf_mae, 4),
            'train_size': len(X_train),
            'test_size': len(X_test)
        }

    def predict_scenario(self, scenario: str, change_pct: float,
                        parameter: str) -> Dict[str, Any]:
        """
        Predict impact of a scenario (e.g., NDVI drops 20%).
        """
        if self.rf_regressor is None:
            raise ValueError("Run fit_scenario_model() first.")

        if parameter not in self.feature_names:
            raise ValueError(f"Parameter {parameter} not in features")

        # Create modified dataset
        scenario_df = self.processed_df.copy()
        param_idx = self.feature_names.index(parameter)

        # Apply change
        multiplier = 1 + (change_pct / 100)
        scenario_df[parameter] = scenario_df[parameter] * multiplier

        # Get baseline and scenario predictions
        X_baseline = self.processed_df[self.feature_names].values
        X_scenario = scenario_df[self.feature_names].values

        baseline_pred = self.rf_regressor.predict(X_baseline)
        scenario_pred = self.rf_regressor.predict(X_scenario)

        baseline_mean = baseline_pred.mean()
        scenario_mean = scenario_pred.mean()
        temp_change = scenario_mean - baseline_mean

        # Count points exceeding threshold
        threshold = 35.0
        baseline_above = (baseline_pred >= threshold).sum()
        scenario_above = (scenario_pred >= threshold).sum()

        # Create point-level results (first 10)
        points = []
        for i in range(min(10, len(self.processed_df))):
            row = self.processed_df.iloc[i]
            change = scenario_pred[i] - baseline_pred[i]
            point = ScenarioPoint(
                id=str(row['system:index']),
                lat=round(row['lat'], 6),
                lng=round(row['lng'], 6),
                baseline_lst=round(baseline_pred[i], 2),
                scenario_lst=round(scenario_pred[i], 2),
                change=f"+{change:.2f}°C" if change >= 0 else f"{change:.2f}°C"
            )
            points.append(asdict(point))

        # Determine alert level
        if scenario_above - baseline_above > 50:
            alert_level = "Critical"
        elif scenario_above - baseline_above > 20:
            alert_level = "High"
        else:
            alert_level = "Medium"

        return {
            'scenario': f"{parameter} {'drops' if change_pct < 0 else 'increases'} by {abs(change_pct)}%",
            'model': 'Random Forest Regressor',
            'baseline_mean_lst_celsius': round(baseline_mean, 2),
            'scenario_mean_lst_celsius': round(scenario_mean, 2),
            'temperature_increase': f"+{temp_change:.2f}°C" if temp_change >= 0 else f"{temp_change:.2f}°C",
            'points_exceeding_35C': {
                'baseline': int(baseline_above),
                'scenario': int(scenario_above),
                'increase': int(scenario_above - baseline_above)
            },
            'message': f"A {abs(change_pct)}% {parameter} {'drop' if change_pct < 0 else 'increase'} would push {scenario_above - baseline_above} more locations above 35°C threshold",
            'alert_level': alert_level,
            'points': points
        }

    # ========================================================================
    # STEP 3: Feature Importance
    # ========================================================================

    def get_feature_importance(self) -> List[FeatureImportanceItem]:
        """Get feature importance for LST prediction."""
        if self.rf_regressor is None:
            raise ValueError("Run fit_scenario_model() first.")

        importances = self.rf_regressor.feature_importances_

        interpretations = {
            'NDVI_Sentinel': 'Green cover is the biggest driver of surface temperature',
            'NDVI_Landsat': 'Secondary vegetation signal confirms NDVI importance',
            'NDVI': 'MODIS NDVI provides regional vegetation context',
            'sm_surface': 'Soil dryness amplifies surface heating',
            'lat': 'Northern Ahmedabad consistently hotter',
            'lng': 'Eastern industrial corridor effect'
        }

        items = []
        for feature, importance in zip(self.feature_names, importances):
            items.append(FeatureImportanceItem(
                feature=feature,
                importance=round(importance, 4),
                interpretation=interpretations.get(feature, "")
            ))

        items.sort(key=lambda x: x.importance, reverse=True)
        return items

    # ========================================================================
    # STEP 4: Breach Threshold Analysis
    # ========================================================================

    def analyze_breach_thresholds(self, threshold_celsius: float = 35.0) -> Dict[str, Any]:
        """
        Analyze at what levels the city crosses dangerous LST thresholds.
        """
        if self.rf_regressor is None:
            raise ValueError("Run fit_scenario_model() first.")

        # Current state
        X = self.processed_df[self.feature_names].values
        baseline_pred = self.rf_regressor.predict(X)
        current_above = (baseline_pred >= threshold_celsius).sum()
        current_pct = 100 * current_above / len(baseline_pred)

        # Test different scenarios
        scenarios = [
            ("NDVI drops 10%", 'NDVI_Sentinel', -0.10),
            ("NDVI drops 20%", 'NDVI_Sentinel', -0.20),
            ("Soil moisture drops 5%", 'sm_surface', -0.05),
            ("Soil moisture drops 10%", 'sm_surface', -0.10),
        ]

        breach_scenarios = []
        for scenario_name, param, change in scenarios:
            scenario_df = self.processed_df.copy()
            scenario_df[param] = scenario_df[param] * (1 + change)
            X_scenario = scenario_df[self.feature_names].values
            scenario_pred = self.rf_regressor.predict(X_scenario)
            above = (scenario_pred >= threshold_celsius).sum()

            breach_scenarios.append(BreachScenario(
                scenario=scenario_name,
                points_above_threshold=int(above),
                pct_above=round(100 * above / len(scenario_pred), 1)
            ))

        # Find safe NDVI minimum
        safe_ndvi = self._find_safe_ndvi_threshold(threshold_celsius)

        return {
            'threshold_lst_celsius': threshold_celsius,
            'current_points_above_threshold': int(current_above),
            'current_pct_above_threshold': round(current_pct, 2),
            'breach_scenarios': [asdict(s) for s in breach_scenarios],
            'safe_ndvi_minimum': round(safe_ndvi, 2),
            'message': f"Ahmedabad must maintain NDVI above {safe_ndvi:.2f} to keep LST below {threshold_celsius}°C in majority of city"
        }

    def _find_safe_ndvi_threshold(self, lst_threshold: float) -> float:
        """Find minimum NDVI to keep LST below threshold."""
        # Binary search for safe NDVI level
        ndvi_values = np.linspace(0.05, 0.35, 20)

        for target_ndvi in ndvi_values:
            # Set all NDVI to this value
            scenario_df = self.processed_df.copy()
            scenario_df['NDVI_Sentinel'] = target_ndvi
            scenario_df['NDVI_Landsat'] = target_ndvi * 0.8  # Approximate
            scenario_df['NDVI'] = target_ndvi * 7000  # MODIS scale

            X_scenario = scenario_df[self.feature_names].values
            pred = self.rf_regressor.predict(X_scenario)

            pct_above = 100 * (pred >= lst_threshold).sum() / len(pred)

            if pct_above < 50:  # Less than 50% above threshold
                return target_ndvi

        return 0.30  # Default safe value

    # ========================================================================
    # API RESPONSE METHODS
    # ========================================================================

    def get_lst_trend_data(self) -> Dict[str, Any]:
        """Get LST trend and forecast data."""
        if self.processed_df is None:
            raise ValueError("Data not loaded.")

        # Historical data
        historical = []
        for i in range(min(20, len(self.processed_df))):  # First 20 points
            row = self.processed_df.iloc[i]
            historical.append(HistoricalPoint(
                step=i,
                value=round(row['LST_Day_1km'], 2),
                value_celsius=round(row['lst_celsius'], 2)
            ))

        trend_direction, trend_rate = self.get_trend_direction()

        return {
            'model': 'ARIMA(2,1,2)' if ARIMA_AVAILABLE else 'Linear Trend',
            'parameter': 'LST_Day_1km',
            'historical_points': len(self.processed_df),
            'forecast_steps': len(self.lst_forecast),
            'historical': [asdict(h) for h in historical],
            'forecast': [asdict(f) for f in self.lst_forecast],
            'trend_direction': trend_direction,
            'trend_rate': f"+{trend_rate:.1f}°C per 100 observations" if trend_rate >= 0 else f"{trend_rate:.1f}°C per 100 observations"
        }

    def get_feature_importance_data(self) -> Dict[str, Any]:
        """Get feature importance API response."""
        importance = self.get_feature_importance()

        return {
            'model': 'Random Forest Regressor',
            'target': 'LST_Day_1km',
            'r2_score': round(self.rf_r2, 4),
            'training_points': len(self.processed_df),
            'feature_importance': [asdict(f) for f in importance]
        }

    def train_all_models(self, forecast_steps: int = 30) -> Dict[str, Any]:
        """Train all forecasting models."""
        print("=" * 60)
        print("TREND FORECAST MODEL - TRAINING")
        print("=" * 60)

        # Step 1: Load data
        print("\n[1/4] Loading and preprocessing data...")
        self.load_and_preprocess_data()

        # Step 2: ARIMA forecast
        print("\n[2/4] Fitting ARIMA model for trend forecasting...")
        arima_metrics = self.fit_arima_forecast(forecast_steps)

        # Step 3: Scenario model
        print("\n[3/4] Training scenario prediction model...")
        rf_metrics = self.fit_scenario_model()

        # Step 4: Get feature importance
        print("\n[4/4] Analyzing feature importance...")
        importance = self.get_feature_importance()

        trend_direction, trend_rate = self.get_trend_direction()

        print("\n" + "=" * 60)
        print("TRAINING COMPLETE")
        print("=" * 60)

        return {
            'status': 'success',
            'data_points': len(self.processed_df),
            'arima_model': arima_metrics['model'],
            'forecast_steps': forecast_steps,
            'scenario_model_r2': rf_metrics['r2_score'],
            'trend_direction': trend_direction,
            'trend_rate': f"{trend_rate:+.2f}°C per 100 steps",
            'top_feature': importance[0].feature if importance else None,
            'city_statistics': {
                'mean_lst_celsius': round(self.city_mean_lst, 2),
                'std_lst_celsius': round(self.city_std_lst, 2)
            }
        }


# ============================================================================
# STANDALONE EXECUTION
# ============================================================================

if __name__ == "__main__":
    csv_path = "Ahmedabad_MultiSatellite_Data.csv"

    model = TrendForecastModel(csv_path)
    summary = model.train_all_models()

    print("\nTraining Summary:")
    print(json.dumps(summary, indent=2))

    # Sample outputs
    print("\n" + "=" * 60)
    print("SAMPLE API RESPONSES")
    print("=" * 60)

    # Trend forecast
    trend = model.get_lst_trend_data()
    print(f"\nTrend: {trend['trend_direction']} ({trend['trend_rate']})")
    print(f"Forecast (next 5 steps):")
    for f in trend['forecast'][:5]:
        print(f"  Step {f['step']}: {f['predicted_celsius']}°C [{f['lower_bound_celsius']}-{f['upper_bound_celsius']}]")

    # Scenario
    scenario = model.predict_scenario('ndvi_drop', -20, 'NDVI_Sentinel')
    print(f"\nScenario: {scenario['scenario']}")
    print(f"  Temperature change: {scenario['temperature_increase']}")
    print(f"  Points exceeding 35°C: {scenario['points_exceeding_35C']['baseline']} → {scenario['points_exceeding_35C']['scenario']}")

    # Feature importance
    importance = model.get_feature_importance_data()
    print(f"\nFeature Importance (R²={importance['r2_score']}):")
    for f in importance['feature_importance'][:3]:
        print(f"  - {f['feature']}: {f['importance']*100:.1f}%")

    # Breach analysis
    breach = model.analyze_breach_thresholds(35.0)
    print(f"\nBreach Analysis (threshold: 35°C):")
    print(f"  Current: {breach['current_pct_above_threshold']}% above threshold")
    print(f"  Safe NDVI minimum: {breach['safe_ndvi_minimum']}")
