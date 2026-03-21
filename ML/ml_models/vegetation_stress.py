"""
Vegetation Stress & NDVI Monitoring Model (Feature 3)
======================================================
This module implements ML models for vegetation health analysis:
1. Random Forest Classifier for health classification (Healthy/Stressed/Barren)
2. Z-score based critical stress alerts
3. Random Forest Regressor for NDVI drivers/feature importance
4. Plantation zone recommender (hottest + lowest NDVI)
"""

import pandas as pd
import numpy as np
import json
from typing import Dict, List, Any, Optional
from dataclasses import dataclass, asdict
from sklearn.ensemble import RandomForestClassifier, RandomForestRegressor
from sklearn.preprocessing import StandardScaler, LabelEncoder
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, r2_score, classification_report
import warnings
warnings.filterwarnings('ignore')


# ============================================================================
# DATA CLASSES
# ============================================================================

@dataclass
class VegetationPoint:
    """Represents a vegetation data point."""
    id: str
    lat: float
    lng: float
    NDVI_Sentinel: float
    NDVI_Landsat: float
    NDVI_MODIS: float
    lst_celsius: float
    sm_surface: float
    health_label: str
    health_score: float
    color_hex: str


@dataclass
class StressAlert:
    """Represents a critical stress alert."""
    id: str
    lat: float
    lng: float
    NDVI_Sentinel: float
    z_score: float
    severity: str
    message: str


@dataclass
class PlantationZone:
    """Represents a recommended plantation zone."""
    rank: int
    id: str
    lat: float
    lng: float
    NDVI_Sentinel: float
    lst_celsius: float
    priority: str
    action: str
    estimated_trees_needed: int
    nearest_landmark: str


@dataclass
class FeatureImportanceItem:
    """Feature importance from Random Forest."""
    feature: str
    importance: float
    interpretation: str


# ============================================================================
# MAIN VEGETATION STRESS MODEL
# ============================================================================

class VegetationStressModel:
    """
    Vegetation Stress & NDVI Monitoring Model for Ahmedabad.

    Implements:
    - Health classification using Random Forest Classifier
    - Z-score based critical stress detection
    - Feature importance analysis for NDVI drivers
    - Plantation zone recommendations
    """

    def __init__(self, csv_path: str):
        """Initialize the model with data path."""
        self.csv_path = csv_path
        self.df: Optional[pd.DataFrame] = None
        self.processed_df: Optional[pd.DataFrame] = None

        # Models
        self.rf_classifier: Optional[RandomForestClassifier] = None
        self.rf_regressor: Optional[RandomForestRegressor] = None
        self.label_encoder: Optional[LabelEncoder] = None
        self.feature_scaler: Optional[StandardScaler] = None

        # Model metrics
        self.classifier_accuracy: float = 0.0
        self.regressor_r2: float = 0.0

        # City statistics
        self.city_mean_ndvi: float = 0.0
        self.city_std_ndvi: float = 0.0
        self.z_score_threshold: float = -2.0  # 2 std below mean

        # Health thresholds
        self.HEALTHY_THRESHOLD = 0.30
        self.STRESSED_THRESHOLD = 0.15

        # Color mapping
        self.health_colors = {
            'Healthy': '#228B22',    # Forest Green
            'Stressed': '#DAA520',   # Goldenrod
            'Barren': '#808080'      # Gray
        }

        # Feature names for regression
        self.regression_features = ['sm_surface', 'lst_celsius', 'NDVI_Landsat', 'lat', 'lng']

    def load_and_preprocess_data(self) -> pd.DataFrame:
        """Load and preprocess the CSV data."""
        self.df = pd.read_csv(self.csv_path)

        # Parse geo coordinates
        self.df['lat'] = self.df['.geo'].apply(self._extract_lat)
        self.df['lng'] = self.df['.geo'].apply(self._extract_lng)

        # Convert LST to Celsius
        self.df['lst_celsius'] = (self.df['LST_Day_1km'] * 0.02) - 273.15

        # Rename NDVI column for clarity
        self.df['NDVI_MODIS'] = self.df['NDVI']

        # Calculate city statistics for NDVI_Sentinel
        self.city_mean_ndvi = self.df['NDVI_Sentinel'].mean()
        self.city_std_ndvi = self.df['NDVI_Sentinel'].std()

        # Assign health labels based on NDVI thresholds
        self.df['health_label'] = self.df['NDVI_Sentinel'].apply(self._classify_health)

        # Calculate Z-scores for NDVI
        self.df['ndvi_z_score'] = (self.df['NDVI_Sentinel'] - self.city_mean_ndvi) / self.city_std_ndvi

        # Assign health scores (0-1 normalized)
        ndvi_min = self.df['NDVI_Sentinel'].min()
        ndvi_max = self.df['NDVI_Sentinel'].max()
        self.df['health_score'] = (self.df['NDVI_Sentinel'] - ndvi_min) / (ndvi_max - ndvi_min)

        # Assign colors
        self.df['color_hex'] = self.df['health_label'].map(self.health_colors)

        self.processed_df = self.df.copy()

        print(f"Loaded {len(self.df)} data points")
        print(f"NDVI Range: {self.df['NDVI_Sentinel'].min():.3f} to {self.df['NDVI_Sentinel'].max():.3f}")
        print(f"City Mean NDVI: {self.city_mean_ndvi:.3f} (±{self.city_std_ndvi:.3f})")

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

    def _classify_health(self, ndvi: float) -> str:
        """Classify vegetation health based on NDVI."""
        if ndvi >= self.HEALTHY_THRESHOLD:
            return 'Healthy'
        elif ndvi >= self.STRESSED_THRESHOLD:
            return 'Stressed'
        else:
            return 'Barren'

    # ========================================================================
    # STEP 1: Random Forest Classifier for Health Classification
    # ========================================================================

    def fit_health_classifier(self, test_size: float = 0.2) -> Dict[str, Any]:
        """
        Train Random Forest Classifier for vegetation health.

        Uses all NDVI sources + LST + soil moisture for classification.
        """
        if self.processed_df is None:
            raise ValueError("Data not loaded. Call load_and_preprocess_data() first.")

        # Features for classification
        feature_cols = ['NDVI_Sentinel', 'NDVI_Landsat', 'NDVI_MODIS', 'lst_celsius', 'sm_surface']
        X = self.processed_df[feature_cols].values
        y = self.processed_df['health_label'].values

        # Encode labels
        self.label_encoder = LabelEncoder()
        y_encoded = self.label_encoder.fit_transform(y)

        # Split data
        X_train, X_test, y_train, y_test = train_test_split(
            X, y_encoded, test_size=test_size, random_state=42, stratify=y_encoded
        )

        # Scale features
        self.feature_scaler = StandardScaler()
        X_train_scaled = self.feature_scaler.fit_transform(X_train)
        X_test_scaled = self.feature_scaler.transform(X_test)

        # Train classifier
        self.rf_classifier = RandomForestClassifier(
            n_estimators=100,
            max_depth=10,
            min_samples_split=5,
            random_state=42,
            n_jobs=-1
        )
        self.rf_classifier.fit(X_train_scaled, y_train)

        # Evaluate
        y_pred = self.rf_classifier.predict(X_test_scaled)
        self.classifier_accuracy = accuracy_score(y_test, y_pred)

        # Store predictions for all data
        X_all_scaled = self.feature_scaler.transform(X)
        self.processed_df['predicted_health'] = self.label_encoder.inverse_transform(
            self.rf_classifier.predict(X_all_scaled)
        )
        self.processed_df['prediction_proba'] = self.rf_classifier.predict_proba(X_all_scaled).max(axis=1)

        print(f"Health Classifier Accuracy: {self.classifier_accuracy:.4f}")

        return {
            'accuracy': round(self.classifier_accuracy, 4),
            'classes': list(self.label_encoder.classes_),
            'train_size': len(X_train),
            'test_size': len(X_test)
        }

    # ========================================================================
    # STEP 2: Z-Score Critical Stress Alerts
    # ========================================================================

    def detect_critical_stress(self, z_threshold: float = -2.0) -> List[StressAlert]:
        """
        Detect critically stressed vegetation using Z-score.

        Points more than 2 std below mean are flagged as critical.
        """
        if self.processed_df is None:
            raise ValueError("Data not loaded.")

        self.z_score_threshold = z_threshold

        # Find critical stress points
        critical_mask = self.processed_df['ndvi_z_score'] <= z_threshold
        critical_df = self.processed_df[critical_mask].copy()

        # Sort by severity (most negative z-score first)
        critical_df = critical_df.sort_values('ndvi_z_score')

        alerts = []
        for _, row in critical_df.iterrows():
            z = row['ndvi_z_score']

            if z <= -3:
                severity = "Critical"
            elif z <= -2.5:
                severity = "High"
            else:
                severity = "Medium"

            alert = StressAlert(
                id=str(row['system:index']),
                lat=round(row['lat'], 6),
                lng=round(row['lng'], 6),
                NDVI_Sentinel=round(row['NDVI_Sentinel'], 4),
                z_score=round(z, 2),
                severity=severity,
                message=f"NDVI {abs(z):.1f} std below city mean — urgent intervention needed"
            )
            alerts.append(alert)

        # Mark alerts in main dataframe
        self.processed_df['is_critical_stress'] = critical_mask

        print(f"Detected {len(alerts)} critical stress alerts (z < {z_threshold})")

        return alerts

    # ========================================================================
    # STEP 3: Random Forest Regressor for Feature Importance
    # ========================================================================

    def fit_ndvi_regressor(self, test_size: float = 0.2) -> Dict[str, Any]:
        """
        Train Random Forest Regressor to predict NDVI_Sentinel.

        This reveals what drives vegetation health in Ahmedabad.
        """
        if self.processed_df is None:
            raise ValueError("Data not loaded.")

        # Features to predict NDVI
        X = self.processed_df[self.regression_features].values
        y = self.processed_df['NDVI_Sentinel'].values

        # Split
        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=test_size, random_state=42
        )

        # Train regressor
        self.rf_regressor = RandomForestRegressor(
            n_estimators=100,
            max_depth=10,
            min_samples_split=5,
            random_state=42,
            n_jobs=-1
        )
        self.rf_regressor.fit(X_train, y_train)

        # Evaluate
        y_pred = self.rf_regressor.predict(X_test)
        self.regressor_r2 = r2_score(y_test, y_pred)

        print(f"NDVI Regressor R² Score: {self.regressor_r2:.4f}")

        return {
            'r2_score': round(self.regressor_r2, 4),
            'train_size': len(X_train),
            'test_size': len(X_test)
        }

    def get_feature_importance(self) -> List[FeatureImportanceItem]:
        """Get feature importance for NDVI drivers."""
        if self.rf_regressor is None:
            raise ValueError("Run fit_ndvi_regressor() first.")

        importances = self.rf_regressor.feature_importances_

        interpretations = {
            'sm_surface': 'Soil moisture is the top driver of green cover',
            'lst_celsius': 'Hotter areas have less vegetation',
            'NDVI_Landsat': 'Consistent cross-sensor vegetation signal',
            'lat': 'Northern areas tend to have different vegetation patterns',
            'lng': 'Eastern industrial areas show less green cover'
        }

        items = []
        for feature, importance in zip(self.regression_features, importances):
            items.append(FeatureImportanceItem(
                feature=feature,
                importance=round(importance, 4),
                interpretation=interpretations.get(feature, "")
            ))

        items.sort(key=lambda x: x.importance, reverse=True)
        return items

    # ========================================================================
    # STEP 4: Plantation Zone Recommender
    # ========================================================================

    def get_plantation_recommendations(self, top_n: int = 20) -> List[PlantationZone]:
        """
        Get top plantation recommendations.

        Prioritizes: Highest LST + Lowest NDVI (most urgent: hot + no green)
        """
        if self.processed_df is None:
            raise ValueError("Data not loaded.")

        # Filter to barren/stressed zones
        candidate_df = self.processed_df[
            self.processed_df['health_label'].isin(['Barren', 'Stressed'])
        ].copy()

        # Create urgency score: high LST + low NDVI
        lst_norm = (candidate_df['lst_celsius'] - candidate_df['lst_celsius'].min()) / \
                   (candidate_df['lst_celsius'].max() - candidate_df['lst_celsius'].min())
        ndvi_norm = (candidate_df['NDVI_Sentinel'] - candidate_df['NDVI_Sentinel'].min()) / \
                    (candidate_df['NDVI_Sentinel'].max() - candidate_df['NDVI_Sentinel'].min())

        candidate_df['urgency_score'] = lst_norm * 0.5 + (1 - ndvi_norm) * 0.5

        # Sort by urgency and take top N
        top_df = candidate_df.nlargest(top_n, 'urgency_score')

        zones = []
        for rank, (_, row) in enumerate(top_df.iterrows(), 1):
            # Determine priority
            if row['urgency_score'] >= 0.8:
                priority = "Critical"
                action = "Immediate tree plantation required"
                trees = 500
            elif row['urgency_score'] >= 0.6:
                priority = "High"
                action = "Urgent plantation recommended within 30 days"
                trees = 300
            else:
                priority = "Medium"
                action = "Plantation recommended within 60 days"
                trees = 200

            # Determine landmark based on location
            landmark = self._get_nearest_landmark(row['lat'], row['lng'])

            zone = PlantationZone(
                rank=rank,
                id=str(row['system:index']),
                lat=round(row['lat'], 6),
                lng=round(row['lng'], 6),
                NDVI_Sentinel=round(row['NDVI_Sentinel'], 4),
                lst_celsius=round(row['lst_celsius'], 2),
                priority=priority,
                action=action,
                estimated_trees_needed=trees,
                nearest_landmark=landmark
            )
            zones.append(zone)

        return zones

    def _get_nearest_landmark(self, lat: float, lng: float) -> str:
        """Generate landmark description based on coordinates."""
        city_center_lat = self.processed_df['lat'].mean()
        city_center_lng = self.processed_df['lng'].mean()

        if lat > city_center_lat + 0.03:
            direction = "North"
        elif lat < city_center_lat - 0.03:
            direction = "South"
        elif lng > city_center_lng + 0.03:
            direction = "East"
        elif lng < city_center_lng - 0.03:
            direction = "West"
        else:
            direction = "Central"

        return f"{direction} Ahmedabad"

    # ========================================================================
    # API RESPONSE METHODS
    # ========================================================================

    def get_vegetation_map_data(self) -> Dict[str, Any]:
        """Get all vegetation points with health classification."""
        if self.processed_df is None:
            raise ValueError("Data not loaded.")

        # Calculate summary
        health_counts = self.processed_df['health_label'].value_counts()
        total = len(self.processed_df)

        summary = {
            'healthy_count': int(health_counts.get('Healthy', 0)),
            'stressed_count': int(health_counts.get('Stressed', 0)),
            'barren_count': int(health_counts.get('Barren', 0)),
            'healthy_pct': round(100 * health_counts.get('Healthy', 0) / total, 1),
            'stressed_pct': round(100 * health_counts.get('Stressed', 0) / total, 1),
            'barren_pct': round(100 * health_counts.get('Barren', 0) / total, 1),
            'city_mean_ndvi': round(self.city_mean_ndvi, 4)
        }

        points = []
        for _, row in self.processed_df.iterrows():
            point = VegetationPoint(
                id=str(row['system:index']),
                lat=round(row['lat'], 6),
                lng=round(row['lng'], 6),
                NDVI_Sentinel=round(row['NDVI_Sentinel'], 4),
                NDVI_Landsat=round(row['NDVI_Landsat'], 4),
                NDVI_MODIS=round(row['NDVI_MODIS'], 2),
                lst_celsius=round(row['lst_celsius'], 2),
                sm_surface=round(row['sm_surface'], 4),
                health_label=row['health_label'],
                health_score=round(row['health_score'], 3),
                color_hex=row['color_hex']
            )
            points.append(asdict(point))

        return {
            'city': 'Ahmedabad',
            'summary': summary,
            'points': points
        }

    def get_alerts_data(self) -> Dict[str, Any]:
        """Get critical stress alerts."""
        alerts = self.detect_critical_stress(self.z_score_threshold)

        threshold_ndvi = self.city_mean_ndvi + (self.z_score_threshold * self.city_std_ndvi)

        return {
            'total_critical_alerts': len(alerts),
            'threshold_used': round(threshold_ndvi, 4),
            'city_mean_ndvi': round(self.city_mean_ndvi, 4),
            'z_score_threshold': self.z_score_threshold,
            'alerts': [asdict(a) for a in alerts]
        }

    def get_plantation_zones_data(self, top_n: int = 10) -> Dict[str, Any]:
        """Get plantation zone recommendations."""
        zones = self.get_plantation_recommendations(top_n)

        return {
            'recommendation_basis': 'Highest LST + Lowest NDVI zones',
            'total_recommendations': len(zones),
            'zones': [asdict(z) for z in zones]
        }

    def get_feature_importance_data(self) -> Dict[str, Any]:
        """Get feature importance API response."""
        importance = self.get_feature_importance()

        return {
            'model': 'Random Forest Regressor',
            'target': 'NDVI_Sentinel',
            'r2_score': round(self.regressor_r2, 4),
            'feature_importance': [asdict(f) for f in importance]
        }

    def train_all_models(self) -> Dict[str, Any]:
        """Train all vegetation models."""
        print("=" * 60)
        print("VEGETATION STRESS MODEL - TRAINING")
        print("=" * 60)

        # Step 1: Load data
        print("\n[1/4] Loading and preprocessing data...")
        self.load_and_preprocess_data()

        # Step 2: Train classifier
        print("\n[2/4] Training health classifier...")
        classifier_metrics = self.fit_health_classifier()

        # Step 3: Detect critical stress
        print("\n[3/4] Detecting critical stress zones...")
        alerts = self.detect_critical_stress()

        # Step 4: Train regressor for feature importance
        print("\n[4/4] Training NDVI regressor for feature importance...")
        regressor_metrics = self.fit_ndvi_regressor()

        print("\n" + "=" * 60)
        print("TRAINING COMPLETE")
        print("=" * 60)

        # Summary
        health_counts = self.processed_df['health_label'].value_counts()

        return {
            'status': 'success',
            'data_points': len(self.processed_df),
            'classifier_accuracy': classifier_metrics['accuracy'],
            'regressor_r2': regressor_metrics['r2_score'],
            'health_distribution': {
                'healthy': int(health_counts.get('Healthy', 0)),
                'stressed': int(health_counts.get('Stressed', 0)),
                'barren': int(health_counts.get('Barren', 0))
            },
            'critical_stress_count': len(alerts),
            'city_statistics': {
                'mean_ndvi': round(self.city_mean_ndvi, 4),
                'std_ndvi': round(self.city_std_ndvi, 4)
            }
        }


# ============================================================================
# STANDALONE EXECUTION
# ============================================================================

if __name__ == "__main__":
    csv_path = "Ahmedabad_MultiSatellite_Data.csv"

    model = VegetationStressModel(csv_path)
    summary = model.train_all_models()

    print("\nTraining Summary:")
    print(json.dumps(summary, indent=2))

    # Sample outputs
    print("\n" + "=" * 60)
    print("SAMPLE API RESPONSES")
    print("=" * 60)

    # Vegetation map summary
    veg_map = model.get_vegetation_map_data()
    print(f"\nVegetation Map: {veg_map['summary']}")

    # Alerts
    alerts = model.get_alerts_data()
    print(f"\nAlerts: {alerts['total_critical_alerts']} critical zones")

    # Plantation zones
    zones = model.get_plantation_zones_data(5)
    print(f"\nTop 5 Plantation Zones:")
    for z in zones['zones']:
        print(f"  {z['rank']}. {z['nearest_landmark']} - NDVI: {z['NDVI_Sentinel']}, LST: {z['lst_celsius']}°C")

    # Feature importance
    importance = model.get_feature_importance_data()
    print(f"\nFeature Importance (R²={importance['r2_score']}):")
    for f in importance['feature_importance']:
        print(f"  - {f['feature']}: {f['importance']*100:.1f}%")
