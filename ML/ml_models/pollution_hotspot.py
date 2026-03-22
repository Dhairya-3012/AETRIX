"""
Air Quality & Pollution Hotspot Finder (Feature 4)
===================================================
This module implements ML models for pollution risk analysis:
1. Pollution Risk Score calculation (composite of LST, NDVI, soil moisture)
2. Isolation Forest for extreme risk detection
3. DBSCAN for spatial clustering of pollution hotspots
4. Random Forest Classifier for risk categorization
"""

import pandas as pd
import numpy as np
import json
from typing import Dict, List, Any, Optional
from dataclasses import dataclass, asdict
from sklearn.ensemble import IsolationForest, RandomForestClassifier
from sklearn.cluster import DBSCAN
from sklearn.preprocessing import StandardScaler, LabelEncoder
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score
from datetime import datetime
import warnings
warnings.filterwarnings('ignore')


# ============================================================================
# DATA CLASSES
# ============================================================================

@dataclass
class PollutionPoint:
    """Represents a pollution risk data point."""
    id: str
    lat: float
    lng: float
    lst_celsius: float
    NDVI_Sentinel: float
    sm_surface: float
    risk_score: float
    risk_category: str
    is_extreme_outlier: bool
    color_hex: str


@dataclass
class PollutionHotspot:
    """Represents a pollution cluster zone."""
    zone_id: int
    zone_name: str
    center_lat: float
    center_lng: float
    point_count: int
    avg_risk_score: float
    max_risk_score: float
    severity: str
    recommended_action: str
    responsible_dept: str
    boundary_points: List[Dict[str, float]]
    point_ids: List[str]


@dataclass
class ExtremeOutlier:
    """Represents an extreme risk outlier."""
    id: str
    lat: float
    lng: float
    lst_celsius: float
    NDVI_Sentinel: float
    sm_surface: float
    anomaly_score: float
    flag: str
    alert_level: str


@dataclass
class ComplianceZone:
    """Represents a zone for compliance reporting."""
    zone_name: str
    risk_score: float
    compliance_status: str
    satellite_evidence: str
    action_required: str
    deadline: str


# ============================================================================
# MAIN POLLUTION MODEL
# ============================================================================

class PollutionHotspotModel:
    """
    Air Quality & Pollution Hotspot Finder for Ahmedabad.

    Implements:
    - Composite pollution risk score calculation
    - Isolation Forest for extreme risk detection
    - DBSCAN for spatial clustering
    - Random Forest for risk categorization
    """

    def __init__(self, csv_path: str):
        """Initialize the model."""
        self.csv_path = csv_path
        self.df: Optional[pd.DataFrame] = None
        self.processed_df: Optional[pd.DataFrame] = None

        # Models
        self.isolation_forest: Optional[IsolationForest] = None
        self.dbscan: Optional[DBSCAN] = None
        self.rf_classifier: Optional[RandomForestClassifier] = None
        self.feature_scaler: Optional[StandardScaler] = None

        # Model metrics
        self.classifier_accuracy: float = 0.0

        # City statistics
        self.city_mean_risk: float = 0.0

        # Historical baselines for normalization
        self.historical_lst_min: float = 0.0
        self.historical_lst_max: float = 50.0
        self.historical_ndvi_min: float = 0.0
        self.historical_ndvi_max: float = 1.0
        self.historical_sm_min: float = 0.0
        self.historical_sm_max: float = 1.0

        # Hotspot zones
        self.hotspot_zones: List[PollutionHotspot] = []

        # Risk color mapping
        self.risk_colors = {
            'Critical': '#8B0000',   # Dark Red
            'High': '#DC143C',       # Crimson
            'Medium': '#FF8C00',     # Dark Orange
            'Low': '#32CD32'         # Lime Green
        }

    def load_and_preprocess_data(self) -> pd.DataFrame:
        """Load and preprocess data."""
        self.df = pd.read_csv(self.csv_path)

        # Parse geo coordinates
        self.df['lat'] = self.df['.geo'].apply(self._extract_lat)
        self.df['lng'] = self.df['.geo'].apply(self._extract_lng)

        # Convert LST to Celsius
        self.df['lst_celsius'] = (self.df['LST_Day_1km'] * 0.02) - 273.15

        # Clean data
        self.df = self.df.dropna(subset=['lst_celsius', 'NDVI_Sentinel', 'sm_surface', 'lat', 'lng'])

        # Store historical min/max for normalization BEFORE filtering
        # This ensures risk scores are comparable across time
        self.historical_lst_min = self.df['lst_celsius'].min()
        self.historical_lst_max = self.df['lst_celsius'].max()
        self.historical_ndvi_min = self.df['NDVI_Sentinel'].min()
        self.historical_ndvi_max = self.df['NDVI_Sentinel'].max()
        self.historical_sm_min = self.df['sm_surface'].min()
        self.historical_sm_max = self.df['sm_surface'].max()

        # If time series data, filter to latest date AFTER calculating historical stats
        if 'date' in self.df.columns:
            latest_date = self.df['date'].max()
            total_dates = self.df['date'].nunique()
            self.df = self.df[self.df['date'] == latest_date].copy()
            print(f"Time series detected — using latest date: {latest_date} ({len(self.df)} points)")
            print(f"Historical baseline: {total_dates} dates used for normalization")

        self.processed_df = self.df.copy()

        print(f"Analyzing {len(self.df)} data points")

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
    # STEP 1: Pollution Risk Score Calculation
    # ========================================================================

    def calculate_risk_scores(self) -> None:
        """
        Calculate composite pollution risk score.

        Formula: risk_score = (normalized_LST × 0.4) +
                              ((1 - normalized_NDVI) × 0.4) +
                              ((1 - normalized_sm) × 0.2)

        High temperature + Low vegetation + Dry soil = High pollution risk
        """
        if self.processed_df is None:
            raise ValueError("Data not loaded.")

        # Normalize features to 0-1 range using HISTORICAL min/max for consistent comparison
        lst_norm = (self.processed_df['lst_celsius'] - self.historical_lst_min) / \
                   (self.historical_lst_max - self.historical_lst_min)
        ndvi_norm = (self.processed_df['NDVI_Sentinel'] - self.historical_ndvi_min) / \
                    (self.historical_ndvi_max - self.historical_ndvi_min)
        sm_norm = (self.processed_df['sm_surface'] - self.historical_sm_min) / \
                  (self.historical_sm_max - self.historical_sm_min)

        # Clip to 0-1 range in case current values exceed historical range
        lst_norm = lst_norm.clip(0, 1)
        ndvi_norm = ndvi_norm.clip(0, 1)
        sm_norm = sm_norm.clip(0, 1)

        # Calculate risk score (0-100)
        self.processed_df['risk_score'] = (
            (lst_norm * 0.4) +
            ((1 - ndvi_norm) * 0.4) +
            ((1 - sm_norm) * 0.2)
        ) * 100

        self.city_mean_risk = self.processed_df['risk_score'].mean()

        # Assign risk categories based on score
        self.processed_df['risk_category'] = self.processed_df['risk_score'].apply(
            self._categorize_risk
        )

        # Assign colors
        self.processed_df['color_hex'] = self.processed_df['risk_category'].map(self.risk_colors)

        print(f"Risk Score Range: {self.processed_df['risk_score'].min():.1f} - {self.processed_df['risk_score'].max():.1f}")
        print(f"City Mean Risk Score: {self.city_mean_risk:.1f}")

    def _categorize_risk(self, score: float) -> str:
        """Categorize risk based on score."""
        if score >= 75:
            return 'Critical'
        elif score >= 55:
            return 'High'
        elif score >= 35:
            return 'Medium'
        else:
            return 'Low'

    # ========================================================================
    # STEP 2: Isolation Forest for Extreme Risk Detection
    # ========================================================================

    def fit_isolation_forest(self, contamination: float = 0.1) -> int:
        """
        Detect extreme risk outliers using Isolation Forest.

        Finds locations that are simultaneously hot, bare, and dry.
        """
        if self.processed_df is None:
            raise ValueError("Data not loaded.")

        # Features: LST, NDVI (inverted), soil moisture (inverted)
        features = self.processed_df[['lst_celsius', 'NDVI_Sentinel', 'sm_surface']].values

        # For pollution detection: high LST, low NDVI, low moisture
        # We invert NDVI and sm_surface so outliers = high pollution risk
        features_transformed = features.copy()
        features_transformed[:, 1] = 1 - (features[:, 1] / features[:, 1].max())  # Invert NDVI
        features_transformed[:, 2] = 1 - (features[:, 2] / features[:, 2].max())  # Invert sm

        # Scale features
        scaler = StandardScaler()
        features_scaled = scaler.fit_transform(features_transformed)

        # Fit Isolation Forest
        self.isolation_forest = IsolationForest(
            n_estimators=100,
            contamination=contamination,
            random_state=42,
            n_jobs=-1
        )

        # Predict outliers
        outlier_labels = self.isolation_forest.fit_predict(features_scaled)
        anomaly_scores = -self.isolation_forest.decision_function(features_scaled)

        # Normalize anomaly scores to 0-1
        anomaly_scores = (anomaly_scores - anomaly_scores.min()) / (anomaly_scores.max() - anomaly_scores.min())

        self.processed_df['is_extreme_outlier'] = outlier_labels == -1
        self.processed_df['anomaly_score'] = anomaly_scores

        # Only flag as extreme if also high risk
        self.processed_df['is_extreme_outlier'] = (
            self.processed_df['is_extreme_outlier'] &
            (self.processed_df['risk_score'] >= 60)
        )

        outlier_count = self.processed_df['is_extreme_outlier'].sum()
        print(f"Isolation Forest detected {outlier_count} extreme risk outliers")

        return outlier_count

    # ========================================================================
    # STEP 3: DBSCAN for Hotspot Clustering
    # ========================================================================

    def fit_dbscan(self, eps_km: float = 5.0, min_samples: int = 2) -> List[PollutionHotspot]:
        """
        Cluster high-risk points into named pollution zones.
        """
        if self.processed_df is None:
            raise ValueError("Data not loaded.")

        # Filter to high-risk points (score >= 55)
        high_risk_df = self.processed_df[self.processed_df['risk_score'] >= 55].copy()

        if len(high_risk_df) == 0:
            print("No high-risk points found for clustering")
            return []

        # Spatial features
        spatial_features = high_risk_df[['lat', 'lng']].values

        # Convert eps to radians
        eps_radians = eps_km / 6371.0

        # Fit DBSCAN
        self.dbscan = DBSCAN(eps=eps_radians, min_samples=min_samples, metric='haversine')
        spatial_radians = np.radians(spatial_features)
        cluster_labels = self.dbscan.fit_predict(spatial_radians)

        high_risk_df['cluster_id'] = cluster_labels

        # Update main dataframe
        self.processed_df.loc[high_risk_df.index, 'cluster_id'] = cluster_labels
        self.processed_df['cluster_id'] = self.processed_df['cluster_id'].fillna(-1).astype(int)

        # Create hotspot zones
        self.hotspot_zones = self._create_hotspot_zones(high_risk_df)

        print(f"DBSCAN found {len(self.hotspot_zones)} pollution hotspot clusters")

        return self.hotspot_zones

    def _create_hotspot_zones(self, high_risk_df: pd.DataFrame) -> List[PollutionHotspot]:
        """Create PollutionHotspot objects from clusters."""
        zones = []
        unique_clusters = high_risk_df[high_risk_df['cluster_id'] != -1]['cluster_id'].unique()

        zone_names = self._generate_zone_names(high_risk_df, unique_clusters)

        for cluster_id in sorted(unique_clusters):
            cluster_data = high_risk_df[high_risk_df['cluster_id'] == cluster_id]

            center_lat = cluster_data['lat'].mean()
            center_lng = cluster_data['lng'].mean()
            avg_risk = cluster_data['risk_score'].mean()
            max_risk = cluster_data['risk_score'].max()

            # Determine severity and action
            if avg_risk >= 75:
                severity = "Critical"
                action = "PCB inspection required immediately"
                dept = "Pollution Control Board"
            elif avg_risk >= 60:
                severity = "High"
                action = "Monthly monitoring required"
                dept = "Environmental Monitoring Cell"
            else:
                severity = "Medium"
                action = "Quarterly assessment recommended"
                dept = "Municipal Corporation"

            boundary = self._calculate_boundary(cluster_data)

            zone = PollutionHotspot(
                zone_id=int(cluster_id),
                zone_name=zone_names.get(cluster_id, f"Zone {cluster_id}"),
                center_lat=round(center_lat, 6),
                center_lng=round(center_lng, 6),
                point_count=len(cluster_data),
                avg_risk_score=round(avg_risk, 1),
                max_risk_score=round(max_risk, 1),
                severity=severity,
                recommended_action=action,
                responsible_dept=dept,
                boundary_points=boundary,
                point_ids=cluster_data['system:index'].tolist()
            )
            zones.append(zone)

        # Sort by severity
        severity_order = {'Critical': 0, 'High': 1, 'Medium': 2, 'Low': 3}
        zones.sort(key=lambda z: (severity_order.get(z.severity, 4), -z.avg_risk_score))

        return zones

    def _generate_zone_names(self, df: pd.DataFrame, clusters: np.ndarray) -> Dict[int, str]:
        """Generate industrial zone names."""
        names = {}
        zone_types = [
            'Industrial Cluster', 'Chemical Zone', 'Manufacturing Area',
            'Commercial Zone', 'Mixed Industrial', 'Warehouse District'
        ]

        city_center_lat = df['lat'].mean()
        city_center_lng = df['lng'].mean()

        for i, cluster_id in enumerate(sorted(clusters)):
            cluster_data = df[df['cluster_id'] == cluster_id]
            center_lat = cluster_data['lat'].mean()
            center_lng = cluster_data['lng'].mean()

            # Direction from center
            if center_lat > city_center_lat + 0.02:
                area = 'Naroda' if center_lng > city_center_lng else 'Chandkheda'
            elif center_lat < city_center_lat - 0.02:
                area = 'Vatva' if center_lng > city_center_lng else 'Narol'
            elif center_lng > city_center_lng + 0.02:
                area = 'Odhav'
            elif center_lng < city_center_lng - 0.02:
                area = 'Sanand'
            else:
                area = 'Central'

            zone_type = zone_types[i % len(zone_types)]
            names[cluster_id] = f"{area} {zone_type}"

        return names

    def _calculate_boundary(self, cluster_data: pd.DataFrame) -> List[Dict[str, float]]:
        """Calculate boundary points for a cluster."""
        lat_min = cluster_data['lat'].min() - 0.005
        lat_max = cluster_data['lat'].max() + 0.005
        lng_min = cluster_data['lng'].min() - 0.005
        lng_max = cluster_data['lng'].max() + 0.005

        return [
            {'lat': round(lat_min, 6), 'lng': round(lng_min, 6)},
            {'lat': round(lat_min, 6), 'lng': round(lng_max, 6)},
            {'lat': round(lat_max, 6), 'lng': round(lng_max, 6)},
            {'lat': round(lat_max, 6), 'lng': round(lng_min, 6)},
        ]

    # ========================================================================
    # STEP 4: Random Forest Classifier for Risk Category
    # ========================================================================

    def fit_risk_classifier(self, test_size: float = 0.2) -> Dict[str, Any]:
        """
        Train Random Forest to classify risk categories.
        """
        if self.processed_df is None:
            raise ValueError("Data not loaded.")

        # Features
        feature_cols = ['lst_celsius', 'NDVI_Sentinel', 'sm_surface', 'lat', 'lng']
        X = self.processed_df[feature_cols].values
        y = self.processed_df['risk_category'].values

        # Encode labels
        label_encoder = LabelEncoder()
        y_encoded = label_encoder.fit_transform(y)

        # Split
        X_train, X_test, y_train, y_test = train_test_split(
            X, y_encoded, test_size=test_size, random_state=42, stratify=y_encoded
        )

        # Scale
        self.feature_scaler = StandardScaler()
        X_train_scaled = self.feature_scaler.fit_transform(X_train)
        X_test_scaled = self.feature_scaler.transform(X_test)

        # Train
        self.rf_classifier = RandomForestClassifier(
            n_estimators=100,
            max_depth=10,
            random_state=42,
            n_jobs=-1
        )
        self.rf_classifier.fit(X_train_scaled, y_train)

        # Evaluate
        y_pred = self.rf_classifier.predict(X_test_scaled)
        self.classifier_accuracy = accuracy_score(y_test, y_pred)

        print(f"Risk Classifier Accuracy: {self.classifier_accuracy:.4f}")

        return {
            'accuracy': round(self.classifier_accuracy, 4),
            'classes': list(label_encoder.classes_)
        }

    # ========================================================================
    # API RESPONSE METHODS
    # ========================================================================

    def get_risk_map_data(self) -> Dict[str, Any]:
        """Get pollution risk map data."""
        if self.processed_df is None:
            raise ValueError("Data not loaded.")

        risk_counts = self.processed_df['risk_category'].value_counts()

        summary = {
            'critical_count': int(risk_counts.get('Critical', 0)),
            'high_count': int(risk_counts.get('High', 0)),
            'medium_count': int(risk_counts.get('Medium', 0)),
            'low_count': int(risk_counts.get('Low', 0)),
            'city_mean_risk_score': round(self.city_mean_risk, 1)
        }

        points = []
        for _, row in self.processed_df.iterrows():
            point = PollutionPoint(
                id=str(row['system:index']),
                lat=round(row['lat'], 6),
                lng=round(row['lng'], 6),
                lst_celsius=round(row['lst_celsius'], 2),
                NDVI_Sentinel=round(row['NDVI_Sentinel'], 4),
                sm_surface=round(row['sm_surface'], 4),
                risk_score=round(row['risk_score'], 1),
                risk_category=row['risk_category'],
                is_extreme_outlier=bool(row['is_extreme_outlier']),
                color_hex=row['color_hex']
            )
            points.append(asdict(point))

        return {
            'city': 'Ahmedabad',
            'risk_summary': summary,
            'points': points
        }

    def get_hotspots_data(self) -> Dict[str, Any]:
        """Get pollution hotspot clusters."""
        return {
            'total_clusters': len(self.hotspot_zones),
            'hotspots': [asdict(z) for z in self.hotspot_zones]
        }

    def get_extreme_outliers_data(self) -> Dict[str, Any]:
        """Get extreme risk outliers."""
        outlier_df = self.processed_df[self.processed_df['is_extreme_outlier']].copy()
        outlier_df = outlier_df.sort_values('anomaly_score', ascending=False)

        outliers = []
        for _, row in outlier_df.iterrows():
            if row['risk_score'] >= 75:
                alert_level = "Critical"
            elif row['risk_score'] >= 60:
                alert_level = "High"
            else:
                alert_level = "Medium"

            outlier = ExtremeOutlier(
                id=str(row['system:index']),
                lat=round(row['lat'], 6),
                lng=round(row['lng'], 6),
                lst_celsius=round(row['lst_celsius'], 2),
                NDVI_Sentinel=round(row['NDVI_Sentinel'], 4),
                sm_surface=round(row['sm_surface'], 4),
                anomaly_score=round(row['anomaly_score'], 3),
                flag="Extreme environmental stress — likely industrial zone",
                alert_level=alert_level
            )
            outliers.append(asdict(outlier))

        return {
            'model': 'Isolation Forest',
            'contamination_rate': 0.1,
            'total_flagged': len(outliers),
            'outliers': outliers
        }

    def get_compliance_report(self) -> Dict[str, Any]:
        """Generate compliance report for regulators."""
        non_compliant = []

        for zone in self.hotspot_zones:
            if zone.severity in ['Critical', 'High']:
                # Get sample point from zone
                zone_df = self.processed_df[
                    self.processed_df['system:index'].isin(zone.point_ids)
                ]

                avg_lst = zone_df['lst_celsius'].mean()
                avg_ndvi = zone_df['NDVI_Sentinel'].mean()
                avg_sm = zone_df['sm_surface'].mean()

                compliance = ComplianceZone(
                    zone_name=zone.zone_name,
                    risk_score=zone.avg_risk_score,
                    compliance_status="Non-Compliant" if zone.severity == "Critical" else "Needs Review",
                    satellite_evidence=f"LST {avg_lst:.1f}°C + NDVI {avg_ndvi:.2f} + Soil moisture {avg_sm:.3f}",
                    action_required=zone.recommended_action,
                    deadline="Within 7 days" if zone.severity == "Critical" else "Within 30 days"
                )
                non_compliant.append(asdict(compliance))

        return {
            'report_date': datetime.now().strftime('%Y-%m-%d'),
            'city': 'Ahmedabad',
            'total_non_compliant_zones': len(non_compliant),
            'zones': non_compliant
        }

    def train_all_models(self) -> Dict[str, Any]:
        """Train all pollution models."""
        print("=" * 60)
        print("POLLUTION HOTSPOT MODEL - TRAINING")
        print("=" * 60)

        # Step 1: Load data
        print("\n[1/5] Loading and preprocessing data...")
        self.load_and_preprocess_data()

        # Step 2: Calculate risk scores
        print("\n[2/5] Calculating pollution risk scores...")
        self.calculate_risk_scores()

        # Step 3: Isolation Forest
        print("\n[3/5] Training Isolation Forest for extreme risk detection...")
        outlier_count = self.fit_isolation_forest()

        # Step 4: DBSCAN clustering
        print("\n[4/5] Clustering high-risk zones with DBSCAN...")
        self.fit_dbscan()

        # Step 5: Risk classifier
        print("\n[5/5] Training risk classifier...")
        classifier_metrics = self.fit_risk_classifier()

        print("\n" + "=" * 60)
        print("TRAINING COMPLETE")
        print("=" * 60)

        risk_counts = self.processed_df['risk_category'].value_counts()

        return {
            'status': 'success',
            'data_points': len(self.processed_df),
            'extreme_outliers': outlier_count,
            'hotspot_clusters': len(self.hotspot_zones),
            'classifier_accuracy': classifier_metrics['accuracy'],
            'risk_distribution': {
                'critical': int(risk_counts.get('Critical', 0)),
                'high': int(risk_counts.get('High', 0)),
                'medium': int(risk_counts.get('Medium', 0)),
                'low': int(risk_counts.get('Low', 0))
            },
            'city_mean_risk_score': round(self.city_mean_risk, 1)
        }


# ============================================================================
# STANDALONE EXECUTION
# ============================================================================

if __name__ == "__main__":
    csv_path = "Ahmedabad_TimeSeries_Final.csv"

    model = PollutionHotspotModel(csv_path)
    summary = model.train_all_models()

    print("\nTraining Summary:")
    print(json.dumps(summary, indent=2))

    # Sample outputs
    print("\n" + "=" * 60)
    print("SAMPLE API RESPONSES")
    print("=" * 60)

    # Risk map
    risk_map = model.get_risk_map_data()
    print(f"\nRisk Summary: {risk_map['risk_summary']}")

    # Hotspots
    hotspots = model.get_hotspots_data()
    print(f"\nHotspot Clusters: {hotspots['total_clusters']}")
    for h in hotspots['hotspots'][:3]:
        print(f"  - {h['zone_name']}: Risk {h['avg_risk_score']} ({h['severity']})")

    # Outliers
    outliers = model.get_extreme_outliers_data()
    print(f"\nExtreme Outliers: {outliers['total_flagged']}")

    # Compliance
    compliance = model.get_compliance_report()
    print(f"\nNon-Compliant Zones: {compliance['total_non_compliant_zones']}")
