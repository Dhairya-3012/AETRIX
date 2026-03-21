"""
Urban Heat Island (UHI) Detection Model
========================================
This module implements ML models for detecting and analyzing Urban Heat Islands:
1. LST Conversion (Kelvin*100 to Celsius)
2. Isolation Forest for anomaly detection
3. DBSCAN for spatial clustering of hotspots
4. Random Forest Regressor for LST prediction and feature importance
"""

import pandas as pd
import numpy as np
import json
from typing import Dict, List, Tuple, Any, Optional
from dataclasses import dataclass, asdict
from sklearn.ensemble import IsolationForest, RandomForestRegressor
from sklearn.cluster import DBSCAN
from sklearn.preprocessing import StandardScaler
from sklearn.model_selection import train_test_split
from sklearn.metrics import r2_score, mean_absolute_error
import warnings
warnings.filterwarnings('ignore')


# ============================================================================
# DATA CLASSES FOR TYPE SAFETY AND CLEAN RESPONSES
# ============================================================================

@dataclass
class HeatmapPoint:
    """Represents a single point on the heatmap."""
    id: str
    lat: float
    lng: float
    lst_celsius: float
    lst_raw: float
    is_anomaly: bool
    anomaly_score: float
    severity: str
    ndvi: float
    soil_moisture: float


@dataclass
class HotspotZone:
    """Represents a cluster of heat island points."""
    zone_id: int
    zone_name: str
    center_lat: float
    center_lng: float
    point_count: int
    avg_lst_celsius: float
    max_lst_celsius: float
    min_lst_celsius: float
    severity: str
    boundary_points: List[Dict[str, float]]
    point_ids: List[str]


@dataclass
class RankedLocation:
    """Represents a ranked hot location."""
    rank: int
    id: str
    lat: float
    lng: float
    lst_celsius: float
    lst_raw: float
    deviation_from_mean: str
    alert_level: str
    ndvi: float
    soil_moisture: float


@dataclass
class FeatureImportanceItem:
    """Represents feature importance from Random Forest."""
    feature: str
    importance: float
    interpretation: str


@dataclass
class PredictionResult:
    """Represents LST prediction result."""
    predicted_lst_celsius: float
    predicted_lst_raw: float
    heat_risk: str
    confidence: float
    message: str
    deviation_from_mean: float


# ============================================================================
# MAIN UHI DETECTION CLASS
# ============================================================================

class UHIDetectionModel:
    """
    Urban Heat Island Detection Model for Ahmedabad.

    This class implements:
    - LST conversion from MODIS format to Celsius
    - Anomaly detection using Isolation Forest
    - Spatial clustering using DBSCAN
    - LST prediction using Random Forest Regressor
    """

    def __init__(self, csv_path: str):
        """
        Initialize the UHI Detection Model.

        Args:
            csv_path: Path to the Ahmedabad multi-satellite CSV file
        """
        self.csv_path = csv_path
        self.df: Optional[pd.DataFrame] = None
        self.processed_df: Optional[pd.DataFrame] = None

        # Models
        self.isolation_forest: Optional[IsolationForest] = None
        self.dbscan: Optional[DBSCAN] = None
        self.random_forest: Optional[RandomForestRegressor] = None

        # Scalers
        self.feature_scaler: Optional[StandardScaler] = None
        self.spatial_scaler: Optional[StandardScaler] = None

        # Model metrics
        self.rf_r2_score: float = 0.0
        self.rf_mae: float = 0.0

        # City statistics
        self.city_mean_lst: float = 0.0
        self.city_std_lst: float = 0.0
        self.city_max_lst: float = 0.0
        self.city_min_lst: float = 0.0

        # Feature names for Random Forest
        self.feature_names = ['NDVI_Sentinel', 'sm_surface', 'lat', 'lng']

        # Hotspot zones
        self.hotspot_zones: List[HotspotZone] = []

    def load_and_preprocess_data(self) -> pd.DataFrame:
        """
        Load CSV and preprocess data.

        Returns:
            Preprocessed DataFrame
        """
        # Load data
        self.df = pd.read_csv(self.csv_path)

        # Parse geo coordinates
        self.df['lat'] = self.df['.geo'].apply(self._extract_lat)
        self.df['lng'] = self.df['.geo'].apply(self._extract_lng)

        # Convert LST to Celsius
        # MODIS LST_Day_1km is in Kelvin * 100 format
        # Formula: LST_celsius = (LST_Day_1km * 0.02) - 273.15
        self.df['lst_celsius'] = (self.df['LST_Day_1km'] * 0.02) - 273.15

        # Store raw LST for reference
        self.df['lst_raw'] = self.df['LST_Day_1km']

        # Clean data - remove any rows with missing essential values
        essential_cols = ['lst_celsius', 'NDVI_Sentinel', 'sm_surface', 'lat', 'lng']
        self.df = self.df.dropna(subset=essential_cols)

        # Calculate city statistics
        self.city_mean_lst = self.df['lst_celsius'].mean()
        self.city_std_lst = self.df['lst_celsius'].std()
        self.city_max_lst = self.df['lst_celsius'].max()
        self.city_min_lst = self.df['lst_celsius'].min()

        self.processed_df = self.df.copy()

        print(f"Loaded {len(self.df)} data points")
        print(f"LST Range: {self.city_min_lst:.2f}°C to {self.city_max_lst:.2f}°C")
        print(f"City Mean LST: {self.city_mean_lst:.2f}°C (±{self.city_std_lst:.2f}°C)")

        return self.processed_df

    def _extract_lat(self, geo_str: str) -> float:
        """Extract latitude from geo JSON string."""
        try:
            geo = json.loads(geo_str)
            return geo['coordinates'][1]  # [lng, lat] format
        except (json.JSONDecodeError, KeyError, IndexError):
            return np.nan

    def _extract_lng(self, geo_str: str) -> float:
        """Extract longitude from geo JSON string."""
        try:
            geo = json.loads(geo_str)
            return geo['coordinates'][0]  # [lng, lat] format
        except (json.JSONDecodeError, KeyError, IndexError):
            return np.nan

    # ========================================================================
    # STEP 1: Isolation Forest - Anomaly Detection
    # ========================================================================

    def fit_isolation_forest(self, contamination: float = 0.15) -> None:
        """
        Fit Isolation Forest to detect heat island anomalies.

        Args:
            contamination: Expected proportion of anomalies (default 15%)
        """
        if self.processed_df is None:
            raise ValueError("Data not loaded. Call load_and_preprocess_data() first.")

        # Use LST values for anomaly detection
        lst_values = self.processed_df[['lst_celsius']].values

        # Initialize and fit Isolation Forest
        self.isolation_forest = IsolationForest(
            n_estimators=100,
            contamination=contamination,
            random_state=42,
            n_jobs=-1
        )

        # Fit and predict
        self.processed_df['anomaly_label'] = self.isolation_forest.fit_predict(lst_values)

        # Get anomaly scores (higher score = more anomalous for hot spots)
        # Isolation Forest returns negative scores for anomalies
        raw_scores = self.isolation_forest.decision_function(lst_values)

        # Normalize scores to 0-1 range where 1 = most anomalous (hottest)
        # For heat islands, we want high LST to have high anomaly scores
        self.processed_df['anomaly_score'] = self._normalize_anomaly_scores(
            raw_scores,
            self.processed_df['lst_celsius'].values
        )

        # Mark heat island anomalies (anomaly_label = -1 AND high LST)
        mean_lst = self.processed_df['lst_celsius'].mean()
        self.processed_df['is_heat_anomaly'] = (
            (self.processed_df['anomaly_label'] == -1) &
            (self.processed_df['lst_celsius'] > mean_lst)
        )

        # Assign severity levels
        self.processed_df['severity'] = self.processed_df.apply(
            self._assign_severity, axis=1
        )

        anomaly_count = self.processed_df['is_heat_anomaly'].sum()
        print(f"Isolation Forest detected {anomaly_count} heat island anomalies")

    def _normalize_anomaly_scores(self, raw_scores: np.ndarray, lst_values: np.ndarray) -> np.ndarray:
        """
        Normalize anomaly scores so higher LST = higher score.

        Args:
            raw_scores: Raw scores from Isolation Forest
            lst_values: LST values in Celsius

        Returns:
            Normalized scores (0-1 range)
        """
        # Combine Isolation Forest score with LST-based score
        # Normalize LST to 0-1 range
        lst_normalized = (lst_values - lst_values.min()) / (lst_values.max() - lst_values.min())

        # Invert and normalize Isolation Forest scores
        # More negative = more anomalous, so we invert
        if_normalized = 1 - (raw_scores - raw_scores.min()) / (raw_scores.max() - raw_scores.min())

        # Combine scores (weight LST higher for heat islands)
        combined_score = 0.6 * lst_normalized + 0.4 * if_normalized

        return np.round(combined_score, 3)

    def _assign_severity(self, row: pd.Series) -> str:
        """Assign severity level based on LST and anomaly status."""
        if not row['is_heat_anomaly']:
            if row['lst_celsius'] >= self.city_mean_lst + self.city_std_lst:
                return "Moderate"
            return "Normal"

        deviation = row['lst_celsius'] - self.city_mean_lst

        if deviation >= 2 * self.city_std_lst:
            return "Critical"
        elif deviation >= 1.5 * self.city_std_lst:
            return "High"
        elif deviation >= self.city_std_lst:
            return "Medium"
        else:
            return "Low"

    # ========================================================================
    # STEP 2: DBSCAN - Spatial Clustering
    # ========================================================================

    def fit_dbscan(self, eps_km: float = 2.0, min_samples: int = 3) -> List[HotspotZone]:
        """
        Fit DBSCAN to cluster nearby hot anomalies into zones.

        Args:
            eps_km: Maximum distance between points in kilometers (default 2km)
            min_samples: Minimum points to form a cluster

        Returns:
            List of HotspotZone objects
        """
        if self.processed_df is None or 'is_heat_anomaly' not in self.processed_df.columns:
            raise ValueError("Run fit_isolation_forest() first.")

        # Get only heat anomaly points
        anomaly_df = self.processed_df[self.processed_df['is_heat_anomaly']].copy()

        if len(anomaly_df) == 0:
            print("No heat anomalies found for clustering")
            return []

        # Prepare spatial features
        spatial_features = anomaly_df[['lat', 'lng']].values

        # Convert eps from km to radians (Earth radius ≈ 6371 km)
        eps_radians = eps_km / 6371.0

        # Fit DBSCAN with haversine metric (requires radians)
        self.dbscan = DBSCAN(eps=eps_radians, min_samples=min_samples, metric='haversine')

        # Convert to radians for haversine metric
        spatial_radians = np.radians(spatial_features)
        cluster_labels = self.dbscan.fit_predict(spatial_radians)

        anomaly_df['cluster_id'] = cluster_labels

        # Update main dataframe
        self.processed_df.loc[anomaly_df.index, 'cluster_id'] = cluster_labels
        self.processed_df['cluster_id'] = self.processed_df['cluster_id'].fillna(-1).astype(int)

        # Create hotspot zones from clusters
        self.hotspot_zones = self._create_hotspot_zones(anomaly_df)

        print(f"DBSCAN found {len(self.hotspot_zones)} hotspot zones")

        return self.hotspot_zones

    def _create_hotspot_zones(self, anomaly_df: pd.DataFrame) -> List[HotspotZone]:
        """Create HotspotZone objects from cluster data."""
        zones = []
        unique_clusters = anomaly_df[anomaly_df['cluster_id'] != -1]['cluster_id'].unique()

        # Zone naming based on location
        zone_names = self._generate_zone_names(anomaly_df, unique_clusters)

        for cluster_id in sorted(unique_clusters):
            cluster_data = anomaly_df[anomaly_df['cluster_id'] == cluster_id]

            # Calculate cluster statistics
            center_lat = cluster_data['lat'].mean()
            center_lng = cluster_data['lng'].mean()
            avg_lst = cluster_data['lst_celsius'].mean()
            max_lst = cluster_data['lst_celsius'].max()
            min_lst = cluster_data['lst_celsius'].min()

            # Calculate boundary points (convex hull approximation)
            boundary = self._calculate_boundary(cluster_data)

            # Determine severity
            severity = self._determine_zone_severity(avg_lst, max_lst)

            zone = HotspotZone(
                zone_id=int(cluster_id),
                zone_name=zone_names.get(cluster_id, f"Zone {cluster_id}"),
                center_lat=round(center_lat, 6),
                center_lng=round(center_lng, 6),
                point_count=len(cluster_data),
                avg_lst_celsius=round(avg_lst, 2),
                max_lst_celsius=round(max_lst, 2),
                min_lst_celsius=round(min_lst, 2),
                severity=severity,
                boundary_points=boundary,
                point_ids=cluster_data['system:index'].tolist()
            )
            zones.append(zone)

        # Sort by severity and size
        severity_order = {'Critical': 0, 'High': 1, 'Medium': 2, 'Low': 3}
        zones.sort(key=lambda z: (severity_order.get(z.severity, 4), -z.point_count))

        return zones

    def _generate_zone_names(self, anomaly_df: pd.DataFrame, clusters: np.ndarray) -> Dict[int, str]:
        """Generate descriptive names for zones based on location."""
        names = {}

        # Ahmedabad area descriptors
        area_descriptors = {
            'north': ['Industrial', 'Manufacturing', 'Commercial'],
            'south': ['Residential', 'Mixed-Use', 'Urban'],
            'east': ['Industrial', 'Commercial', 'Warehouse'],
            'west': ['Residential', 'Suburban', 'Green'],
            'central': ['Business District', 'Commercial Hub', 'Urban Core']
        }

        city_center_lat = anomaly_df['lat'].mean()
        city_center_lng = anomaly_df['lng'].mean()

        for i, cluster_id in enumerate(sorted(clusters)):
            cluster_data = anomaly_df[anomaly_df['cluster_id'] == cluster_id]
            center_lat = cluster_data['lat'].mean()
            center_lng = cluster_data['lng'].mean()

            # Determine direction from city center
            if center_lat > city_center_lat + 0.02:
                direction = 'north'
            elif center_lat < city_center_lat - 0.02:
                direction = 'south'
            elif center_lng > city_center_lng + 0.02:
                direction = 'east'
            elif center_lng < city_center_lng - 0.02:
                direction = 'west'
            else:
                direction = 'central'

            descriptors = area_descriptors[direction]
            descriptor = descriptors[i % len(descriptors)]

            names[cluster_id] = f"{direction.title()} Ahmedabad {descriptor} Zone"

        return names

    def _calculate_boundary(self, cluster_data: pd.DataFrame) -> List[Dict[str, float]]:
        """Calculate approximate boundary points for a cluster."""
        if len(cluster_data) < 3:
            # For small clusters, just return the points themselves
            return [
                {'lat': round(row['lat'], 6), 'lng': round(row['lng'], 6)}
                for _, row in cluster_data.iterrows()
            ]

        # Simple bounding box with buffer
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

    def _determine_zone_severity(self, avg_lst: float, max_lst: float) -> str:
        """Determine zone severity based on average and max LST."""
        deviation = avg_lst - self.city_mean_lst

        if deviation >= 2.5 or max_lst >= self.city_mean_lst + 3:
            return "Critical"
        elif deviation >= 1.5:
            return "High"
        elif deviation >= 0.8:
            return "Medium"
        else:
            return "Low"

    # ========================================================================
    # STEP 3: Random Forest Regressor - LST Prediction
    # ========================================================================

    def fit_random_forest(self, test_size: float = 0.2) -> Dict[str, Any]:
        """
        Fit Random Forest Regressor to predict LST from features.

        Args:
            test_size: Proportion of data for testing

        Returns:
            Dictionary with model metrics
        """
        if self.processed_df is None:
            raise ValueError("Data not loaded. Call load_and_preprocess_data() first.")

        # Prepare features and target
        X = self.processed_df[self.feature_names].values
        y = self.processed_df['lst_celsius'].values

        # Split data
        X_train, X_test, y_train, y_test = train_test_split(
            X, y, test_size=test_size, random_state=42
        )

        # Scale features
        self.feature_scaler = StandardScaler()
        X_train_scaled = self.feature_scaler.fit_transform(X_train)
        X_test_scaled = self.feature_scaler.transform(X_test)

        # Initialize and fit Random Forest
        self.random_forest = RandomForestRegressor(
            n_estimators=100,
            max_depth=10,
            min_samples_split=5,
            min_samples_leaf=2,
            random_state=42,
            n_jobs=-1
        )

        self.random_forest.fit(X_train_scaled, y_train)

        # Make predictions and calculate metrics
        y_pred = self.random_forest.predict(X_test_scaled)
        self.rf_r2_score = r2_score(y_test, y_pred)
        self.rf_mae = mean_absolute_error(y_test, y_pred)

        metrics = {
            'r2_score': round(self.rf_r2_score, 4),
            'mae': round(self.rf_mae, 4),
            'rmse': round(np.sqrt(np.mean((y_test - y_pred) ** 2)), 4),
            'train_size': len(X_train),
            'test_size': len(X_test)
        }

        print(f"Random Forest R² Score: {self.rf_r2_score:.4f}")
        print(f"Random Forest MAE: {self.rf_mae:.4f}°C")

        return metrics

    def get_feature_importance(self) -> List[FeatureImportanceItem]:
        """
        Get feature importance from Random Forest.

        Returns:
            List of FeatureImportanceItem objects
        """
        if self.random_forest is None:
            raise ValueError("Run fit_random_forest() first.")

        importances = self.random_forest.feature_importances_

        # Interpretations for each feature
        interpretations = {
            'NDVI_Sentinel': 'Higher vegetation coverage correlates with lower LST',
            'sm_surface': 'Higher soil moisture correlates with cooler surface',
            'lat': 'Latitude affects sun angle and heat distribution',
            'lng': 'Longitude captures east-west variation in land use'
        }

        feature_importance = []
        for feature, importance in zip(self.feature_names, importances):
            item = FeatureImportanceItem(
                feature=feature,
                importance=round(importance, 4),
                interpretation=interpretations.get(feature, "")
            )
            feature_importance.append(item)

        # Sort by importance
        feature_importance.sort(key=lambda x: x.importance, reverse=True)

        return feature_importance

    def predict_lst(self, lat: float, lng: float,
                    ndvi_sentinel: float, sm_surface: float) -> PredictionResult:
        """
        Predict LST at a given location.

        Args:
            lat: Latitude
            lng: Longitude
            ndvi_sentinel: Sentinel NDVI value
            sm_surface: Surface soil moisture

        Returns:
            PredictionResult object
        """
        if self.random_forest is None or self.feature_scaler is None:
            raise ValueError("Run fit_random_forest() first.")

        # Prepare features
        features = np.array([[ndvi_sentinel, sm_surface, lat, lng]])
        features_scaled = self.feature_scaler.transform(features)

        # Predict
        predicted_lst = self.random_forest.predict(features_scaled)[0]

        # Convert back to raw format
        predicted_raw = (predicted_lst + 273.15) / 0.02

        # Calculate deviation from mean
        deviation = predicted_lst - self.city_mean_lst

        # Determine heat risk
        if deviation >= 2.5:
            heat_risk = "Critical"
        elif deviation >= 1.5:
            heat_risk = "High"
        elif deviation >= 0.8:
            heat_risk = "Medium"
        elif deviation >= 0:
            heat_risk = "Low"
        else:
            heat_risk = "Below Average"

        # Calculate confidence based on feature similarity to training data
        confidence = self._calculate_prediction_confidence(features[0])

        # Generate message
        if deviation > 0:
            message = f"This location is predicted to be {deviation:.1f}°C above city average"
        else:
            message = f"This location is predicted to be {abs(deviation):.1f}°C below city average"

        return PredictionResult(
            predicted_lst_celsius=round(predicted_lst, 2),
            predicted_lst_raw=round(predicted_raw, 2),
            heat_risk=heat_risk,
            confidence=round(confidence, 2),
            message=message,
            deviation_from_mean=round(deviation, 2)
        )

    def _calculate_prediction_confidence(self, features: np.ndarray) -> float:
        """Calculate confidence based on how similar input is to training data."""
        if self.processed_df is None:
            return 0.5

        # Check if features are within training data range
        confidence = 1.0

        for i, (feature_name, value) in enumerate(zip(self.feature_names, features)):
            col_data = self.processed_df[feature_name]
            min_val, max_val = col_data.min(), col_data.max()

            if value < min_val or value > max_val:
                # Outside training range - reduce confidence
                if value < min_val:
                    distance = (min_val - value) / (max_val - min_val)
                else:
                    distance = (value - max_val) / (max_val - min_val)
                confidence -= min(0.2, distance * 0.1)

        return max(0.5, min(1.0, confidence))

    # ========================================================================
    # API RESPONSE METHODS
    # ========================================================================

    def get_heatmap_data(self) -> Dict[str, Any]:
        """
        Get all data for heatmap visualization.

        Returns:
            Dictionary with heatmap data
        """
        if self.processed_df is None or 'is_heat_anomaly' not in self.processed_df.columns:
            raise ValueError("Run all fitting methods first.")

        points = []
        for _, row in self.processed_df.iterrows():
            point = HeatmapPoint(
                id=str(row['system:index']),
                lat=round(row['lat'], 6),
                lng=round(row['lng'], 6),
                lst_celsius=round(row['lst_celsius'], 2),
                lst_raw=round(row['lst_raw'], 2),
                is_anomaly=bool(row['is_heat_anomaly']),
                anomaly_score=round(row['anomaly_score'], 3),
                severity=row['severity'],
                ndvi=round(row['NDVI_Sentinel'], 4),
                soil_moisture=round(row['sm_surface'], 4)
            )
            points.append(asdict(point))

        return {
            'city': 'Ahmedabad',
            'city_mean_lst_celsius': round(self.city_mean_lst, 2),
            'city_max_lst_celsius': round(self.city_max_lst, 2),
            'city_min_lst_celsius': round(self.city_min_lst, 2),
            'city_std_lst_celsius': round(self.city_std_lst, 2),
            'total_points': len(points),
            'total_anomalies': sum(1 for p in points if p['is_anomaly']),
            'points': points
        }

    def get_hotspots_data(self) -> Dict[str, Any]:
        """
        Get hotspot zones data.

        Returns:
            Dictionary with hotspot zones
        """
        return {
            'city': 'Ahmedabad',
            'total_hotspot_zones': len(self.hotspot_zones),
            'hotspots': [asdict(zone) for zone in self.hotspot_zones]
        }

    def get_top_hottest(self, top_n: int = 10) -> Dict[str, Any]:
        """
        Get top N hottest locations.

        Args:
            top_n: Number of locations to return

        Returns:
            Dictionary with ranked locations
        """
        if self.processed_df is None:
            raise ValueError("Data not loaded.")

        # Sort by LST descending
        sorted_df = self.processed_df.nlargest(top_n, 'lst_celsius')

        rankings = []
        for rank, (_, row) in enumerate(sorted_df.iterrows(), 1):
            deviation = row['lst_celsius'] - self.city_mean_lst

            if deviation >= 2.5:
                alert_level = "Critical"
            elif deviation >= 1.5:
                alert_level = "High"
            elif deviation >= 0.8:
                alert_level = "Medium"
            else:
                alert_level = "Low"

            location = RankedLocation(
                rank=rank,
                id=str(row['system:index']),
                lat=round(row['lat'], 6),
                lng=round(row['lng'], 6),
                lst_celsius=round(row['lst_celsius'], 2),
                lst_raw=round(row['lst_raw'], 2),
                deviation_from_mean=f"+{deviation:.2f}°C" if deviation >= 0 else f"{deviation:.2f}°C",
                alert_level=alert_level,
                ndvi=round(row['NDVI_Sentinel'], 4),
                soil_moisture=round(row['sm_surface'], 4)
            )
            rankings.append(asdict(location))

        return {
            'city': 'Ahmedabad',
            'city_mean_lst_celsius': round(self.city_mean_lst, 2),
            'total_ranked': len(rankings),
            'ranking': rankings
        }

    def get_feature_importance_response(self) -> Dict[str, Any]:
        """
        Get feature importance API response.

        Returns:
            Dictionary with feature importance data
        """
        feature_importance = self.get_feature_importance()

        return {
            'city': 'Ahmedabad',
            'model': 'Random Forest Regressor',
            'r2_score': round(self.rf_r2_score, 4),
            'mae_celsius': round(self.rf_mae, 4),
            'feature_importance': [asdict(item) for item in feature_importance]
        }

    def train_all_models(self,
                         contamination: float = 0.15,
                         dbscan_eps_km: float = 2.0,
                         dbscan_min_samples: int = 3) -> Dict[str, Any]:
        """
        Train all models in sequence.

        Args:
            contamination: Isolation Forest contamination parameter
            dbscan_eps_km: DBSCAN eps parameter in kilometers
            dbscan_min_samples: DBSCAN min_samples parameter

        Returns:
            Dictionary with training summary
        """
        print("=" * 60)
        print("URBAN HEAT ISLAND DETECTION MODEL - TRAINING")
        print("=" * 60)

        # Step 1: Load and preprocess data
        print("\n[1/4] Loading and preprocessing data...")
        self.load_and_preprocess_data()

        # Step 2: Fit Isolation Forest
        print("\n[2/4] Training Isolation Forest for anomaly detection...")
        self.fit_isolation_forest(contamination=contamination)

        # Step 3: Fit DBSCAN
        print("\n[3/4] Training DBSCAN for spatial clustering...")
        self.fit_dbscan(eps_km=dbscan_eps_km, min_samples=dbscan_min_samples)

        # Step 4: Fit Random Forest
        print("\n[4/4] Training Random Forest for LST prediction...")
        rf_metrics = self.fit_random_forest()

        print("\n" + "=" * 60)
        print("TRAINING COMPLETE")
        print("=" * 60)

        return {
            'status': 'success',
            'data_points': len(self.processed_df),
            'anomalies_detected': int(self.processed_df['is_heat_anomaly'].sum()),
            'hotspot_zones': len(self.hotspot_zones),
            'random_forest_r2': rf_metrics['r2_score'],
            'random_forest_mae': rf_metrics['mae'],
            'city_statistics': {
                'mean_lst_celsius': round(self.city_mean_lst, 2),
                'max_lst_celsius': round(self.city_max_lst, 2),
                'min_lst_celsius': round(self.city_min_lst, 2),
                'std_lst_celsius': round(self.city_std_lst, 2)
            }
        }


# ============================================================================
# STANDALONE EXECUTION
# ============================================================================

if __name__ == "__main__":
    # Example usage
    csv_path = "Ahmedabad_MultiSatellite_Data.csv"

    # Initialize model
    model = UHIDetectionModel(csv_path)

    # Train all models
    training_summary = model.train_all_models()
    print("\nTraining Summary:")
    print(json.dumps(training_summary, indent=2))

    # Get heatmap data
    print("\n" + "=" * 60)
    print("SAMPLE API RESPONSES")
    print("=" * 60)

    # Sample heatmap response (first 2 points)
    heatmap = model.get_heatmap_data()
    print(f"\nHeatmap: {heatmap['total_points']} points, {heatmap['total_anomalies']} anomalies")

    # Hotspots
    hotspots = model.get_hotspots_data()
    print(f"\nHotspots: {hotspots['total_hotspot_zones']} zones")
    for zone in hotspots['hotspots'][:3]:
        print(f"  - {zone['zone_name']}: {zone['avg_lst_celsius']}°C ({zone['severity']})")

    # Top hottest
    top_hottest = model.get_top_hottest(5)
    print(f"\nTop 5 Hottest Locations:")
    for loc in top_hottest['ranking']:
        print(f"  {loc['rank']}. {loc['lst_celsius']}°C at ({loc['lat']}, {loc['lng']})")

    # Feature importance
    importance = model.get_feature_importance_response()
    print(f"\nFeature Importance (R²={importance['r2_score']}):")
    for feat in importance['feature_importance']:
        print(f"  - {feat['feature']}: {feat['importance']*100:.1f}%")

    # Prediction example
    print("\nSample Prediction:")
    prediction = model.predict_lst(
        lat=23.05,
        lng=72.55,
        ndvi_sentinel=0.18,
        sm_surface=0.147
    )
    print(f"  Predicted LST: {prediction.predicted_lst_celsius}°C")
    print(f"  Heat Risk: {prediction.heat_risk}")
    print(f"  {prediction.message}")
