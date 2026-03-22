"""
Model Accuracy & Performance Metrics
=====================================
This module calculates and reports accuracy metrics for all ML models.
"""

import pandas as pd
import numpy as np
import json
from typing import Dict, Any
from datetime import datetime
from sklearn.metrics import (
    accuracy_score, precision_score, recall_score, f1_score,
    r2_score, mean_absolute_error, mean_squared_error,
    confusion_matrix, classification_report
)

from .uhi_detection import UHIDetectionModel
from .vegetation_stress import VegetationStressModel
from .pollution_hotspot import PollutionHotspotModel
from .trend_forecast import TrendForecastModel


class ModelAccuracyReporter:
    """
    Calculates and reports accuracy metrics for all environmental ML models.
    """

    def __init__(self, csv_path: str):
        self.csv_path = csv_path
        self.metrics: Dict[str, Any] = {}
        self.models_trained = False

    def train_and_evaluate_all(self) -> Dict[str, Any]:
        """Train all models and calculate accuracy metrics."""

        print("=" * 70)
        print("MODEL ACCURACY EVALUATION")
        print("=" * 70)

        # Feature 2: UHI Detection
        print("\n[1/4] Evaluating UHI Detection Model...")
        self.metrics['uhi_detection'] = self._evaluate_uhi_model()

        # Feature 3: Vegetation Stress
        print("\n[2/4] Evaluating Vegetation Stress Model...")
        self.metrics['vegetation_stress'] = self._evaluate_vegetation_model()

        # Feature 4: Pollution Hotspot
        print("\n[3/4] Evaluating Pollution Hotspot Model...")
        self.metrics['pollution_hotspot'] = self._evaluate_pollution_model()

        # Feature 5: Trend Forecast
        print("\n[4/4] Evaluating Trend Forecast Model...")
        self.metrics['trend_forecast'] = self._evaluate_forecast_model()

        self.models_trained = True

        # Calculate overall summary
        self.metrics['summary'] = self._calculate_summary()
        self.metrics['evaluated_at'] = datetime.now().isoformat()

        print("\n" + "=" * 70)
        print("EVALUATION COMPLETE")
        print("=" * 70)

        return self.metrics

    def _evaluate_uhi_model(self) -> Dict[str, Any]:
        """Evaluate UHI Detection Model accuracy."""
        from sklearn.model_selection import cross_val_score, train_test_split

        model = UHIDetectionModel(self.csv_path)
        model.load_and_preprocess_data()
        model.fit_isolation_forest()
        model.fit_dbscan()

        # Random Forest evaluation with cross-validation
        feature_names = ['NDVI_Sentinel', 'sm_surface', 'lat', 'lng']
        X = model.processed_df[feature_names].values
        y = model.processed_df['lst_celsius'].values

        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

        from sklearn.ensemble import RandomForestRegressor
        from sklearn.preprocessing import StandardScaler

        scaler = StandardScaler()
        X_train_scaled = scaler.fit_transform(X_train)
        X_test_scaled = scaler.transform(X_test)

        rf = RandomForestRegressor(n_estimators=100, max_depth=10, random_state=42)
        rf.fit(X_train_scaled, y_train)
        y_pred = rf.predict(X_test_scaled)

        # Cross-validation
        cv_scores = cross_val_score(rf, scaler.fit_transform(X), y, cv=5, scoring='r2')

        metrics = {
            'model_name': 'UHI Detection (Random Forest Regressor)',
            'task_type': 'Regression',
            'test_metrics': {
                'r2_score': round(r2_score(y_test, y_pred), 4),
                'mae': round(mean_absolute_error(y_test, y_pred), 4),
                'rmse': round(np.sqrt(mean_squared_error(y_test, y_pred)), 4),
                'mape': round(np.mean(np.abs((y_test - y_pred) / y_test)) * 100, 2)
            },
            'cross_validation': {
                'cv_folds': 5,
                'cv_r2_mean': round(cv_scores.mean(), 4),
                'cv_r2_std': round(cv_scores.std(), 4),
                'cv_scores': [round(s, 4) for s in cv_scores]
            },
            'anomaly_detection': {
                'total_points': len(model.processed_df),
                'anomalies_detected': int(model.processed_df['is_heat_anomaly'].sum()),
                'anomaly_percentage': round(100 * model.processed_df['is_heat_anomaly'].sum() / len(model.processed_df), 2)
            },
            'clustering': {
                'hotspot_zones': len(model.hotspot_zones),
                'clustered_points': sum(z.point_count for z in model.hotspot_zones)
            }
        }

        print(f"   R² Score: {metrics['test_metrics']['r2_score']}")
        print(f"   MAE: {metrics['test_metrics']['mae']}°C")
        print(f"   CV R² Mean: {metrics['cross_validation']['cv_r2_mean']} ± {metrics['cross_validation']['cv_r2_std']}")

        return metrics

    def _evaluate_vegetation_model(self) -> Dict[str, Any]:
        """Evaluate Vegetation Stress Model accuracy."""
        from sklearn.model_selection import cross_val_score, train_test_split

        model = VegetationStressModel(self.csv_path)
        model.load_and_preprocess_data()

        # Classifier evaluation
        feature_cols = ['NDVI_Sentinel', 'NDVI_Landsat', 'NDVI_MODIS', 'lst_celsius', 'sm_surface']
        X = model.processed_df[feature_cols].values
        y = model.processed_df['health_label'].values

        from sklearn.preprocessing import LabelEncoder, StandardScaler
        from sklearn.ensemble import RandomForestClassifier

        le = LabelEncoder()
        y_encoded = le.fit_transform(y)

        X_train, X_test, y_train, y_test = train_test_split(
            X, y_encoded, test_size=0.2, random_state=42, stratify=y_encoded
        )

        scaler = StandardScaler()
        X_train_scaled = scaler.fit_transform(X_train)
        X_test_scaled = scaler.transform(X_test)

        clf = RandomForestClassifier(n_estimators=100, max_depth=10, random_state=42)
        clf.fit(X_train_scaled, y_train)
        y_pred = clf.predict(X_test_scaled)

        # Cross-validation
        cv_scores = cross_val_score(clf, scaler.fit_transform(X), y_encoded, cv=5, scoring='accuracy')

        # Confusion matrix
        cm = confusion_matrix(y_test, y_pred)

        metrics = {
            'model_name': 'Vegetation Health Classifier (Random Forest)',
            'task_type': 'Classification',
            'classes': list(le.classes_),
            'test_metrics': {
                'accuracy': round(accuracy_score(y_test, y_pred), 4),
                'precision_macro': round(precision_score(y_test, y_pred, average='macro'), 4),
                'recall_macro': round(recall_score(y_test, y_pred, average='macro'), 4),
                'f1_macro': round(f1_score(y_test, y_pred, average='macro'), 4)
            },
            'cross_validation': {
                'cv_folds': 5,
                'cv_accuracy_mean': round(cv_scores.mean(), 4),
                'cv_accuracy_std': round(cv_scores.std(), 4),
                'cv_scores': [round(s, 4) for s in cv_scores]
            },
            'confusion_matrix': cm.tolist(),
            'class_distribution': {
                cls: int((y == cls).sum()) for cls in le.classes_
            }
        }

        # Regressor for feature importance
        model.fit_ndvi_regressor()
        metrics['feature_importance_r2'] = round(model.regressor_r2, 4)

        print(f"   Accuracy: {metrics['test_metrics']['accuracy']}")
        print(f"   F1 Score: {metrics['test_metrics']['f1_macro']}")
        print(f"   CV Accuracy Mean: {metrics['cross_validation']['cv_accuracy_mean']} ± {metrics['cross_validation']['cv_accuracy_std']}")

        return metrics

    def _evaluate_pollution_model(self) -> Dict[str, Any]:
        """Evaluate Pollution Hotspot Model accuracy."""
        from sklearn.model_selection import cross_val_score, train_test_split

        model = PollutionHotspotModel(self.csv_path)
        model.load_and_preprocess_data()
        model.calculate_risk_scores()
        model.fit_isolation_forest()
        model.fit_dbscan()

        # Classifier evaluation
        feature_cols = ['lst_celsius', 'NDVI_Sentinel', 'sm_surface', 'lat', 'lng']
        X = model.processed_df[feature_cols].values
        y = model.processed_df['risk_category'].values

        from sklearn.preprocessing import LabelEncoder, StandardScaler
        from sklearn.ensemble import RandomForestClassifier

        le = LabelEncoder()
        y_encoded = le.fit_transform(y)

        X_train, X_test, y_train, y_test = train_test_split(
            X, y_encoded, test_size=0.2, random_state=42, stratify=y_encoded
        )

        scaler = StandardScaler()
        X_train_scaled = scaler.fit_transform(X_train)
        X_test_scaled = scaler.transform(X_test)

        clf = RandomForestClassifier(n_estimators=100, max_depth=10, random_state=42)
        clf.fit(X_train_scaled, y_train)
        y_pred = clf.predict(X_test_scaled)

        # Cross-validation
        cv_scores = cross_val_score(clf, scaler.fit_transform(X), y_encoded, cv=5, scoring='accuracy')

        metrics = {
            'model_name': 'Pollution Risk Classifier (Random Forest)',
            'task_type': 'Classification',
            'classes': list(le.classes_),
            'test_metrics': {
                'accuracy': round(accuracy_score(y_test, y_pred), 4),
                'precision_macro': round(precision_score(y_test, y_pred, average='macro'), 4),
                'recall_macro': round(recall_score(y_test, y_pred, average='macro'), 4),
                'f1_macro': round(f1_score(y_test, y_pred, average='macro'), 4)
            },
            'cross_validation': {
                'cv_folds': 5,
                'cv_accuracy_mean': round(cv_scores.mean(), 4),
                'cv_accuracy_std': round(cv_scores.std(), 4),
                'cv_scores': [round(s, 4) for s in cv_scores]
            },
            'risk_score_stats': {
                'mean': round(model.processed_df['risk_score'].mean(), 2),
                'std': round(model.processed_df['risk_score'].std(), 2),
                'min': round(model.processed_df['risk_score'].min(), 2),
                'max': round(model.processed_df['risk_score'].max(), 2)
            },
            'anomaly_detection': {
                'extreme_outliers': int(model.processed_df['is_extreme_outlier'].sum())
            },
            'clustering': {
                'pollution_clusters': len(model.hotspot_zones)
            }
        }

        print(f"   Accuracy: {metrics['test_metrics']['accuracy']}")
        print(f"   F1 Score: {metrics['test_metrics']['f1_macro']}")
        print(f"   CV Accuracy Mean: {metrics['cross_validation']['cv_accuracy_mean']} ± {metrics['cross_validation']['cv_accuracy_std']}")

        return metrics

    def _evaluate_forecast_model(self) -> Dict[str, Any]:
        """Evaluate Trend Forecast Model accuracy."""
        from sklearn.model_selection import cross_val_score, train_test_split

        model = TrendForecastModel(self.csv_path)
        model.load_and_preprocess_data()

        # Scenario model evaluation
        feature_names = ['NDVI_Sentinel', 'NDVI_Landsat', 'NDVI', 'sm_surface', 'lat', 'lng']
        X = model.processed_df[feature_names].values
        y = model.processed_df['lst_celsius'].values

        X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

        from sklearn.ensemble import RandomForestRegressor

        rf = RandomForestRegressor(n_estimators=100, max_depth=12, random_state=42)
        rf.fit(X_train, y_train)
        y_pred = rf.predict(X_test)

        # Cross-validation
        cv_scores = cross_val_score(rf, X, y, cv=5, scoring='r2')

        # Feature importance
        importance = dict(zip(feature_names, rf.feature_importances_))

        metrics = {
            'model_name': 'LST Prediction (Random Forest Regressor)',
            'task_type': 'Regression',
            'test_metrics': {
                'r2_score': round(r2_score(y_test, y_pred), 4),
                'mae': round(mean_absolute_error(y_test, y_pred), 4),
                'rmse': round(np.sqrt(mean_squared_error(y_test, y_pred)), 4),
                'mape': round(np.mean(np.abs((y_test - y_pred) / y_test)) * 100, 2)
            },
            'cross_validation': {
                'cv_folds': 5,
                'cv_r2_mean': round(cv_scores.mean(), 4),
                'cv_r2_std': round(cv_scores.std(), 4),
                'cv_scores': [round(s, 4) for s in cv_scores]
            },
            'feature_importance': {k: round(v, 4) for k, v in sorted(importance.items(), key=lambda x: -x[1])},
            'forecast': {
                'method': 'Linear Trend Extrapolation',
                'forecast_steps': 30
            }
        }

        print(f"   R² Score: {metrics['test_metrics']['r2_score']}")
        print(f"   MAE: {metrics['test_metrics']['mae']}°C")
        print(f"   CV R² Mean: {metrics['cross_validation']['cv_r2_mean']} ± {metrics['cross_validation']['cv_r2_std']}")

        return metrics

    def _calculate_summary(self) -> Dict[str, Any]:
        """Calculate overall summary metrics."""

        # Average R² for regression models
        regression_r2 = [
            self.metrics['uhi_detection']['test_metrics']['r2_score'],
            self.metrics['trend_forecast']['test_metrics']['r2_score']
        ]

        # Average accuracy for classification models
        classification_acc = [
            self.metrics['vegetation_stress']['test_metrics']['accuracy'],
            self.metrics['pollution_hotspot']['test_metrics']['accuracy']
        ]

        return {
            'total_models': 4,
            'regression_models': {
                'count': 2,
                'average_r2': round(np.mean(regression_r2), 4),
                'models': ['UHI Detection', 'Trend Forecast']
            },
            'classification_models': {
                'count': 2,
                'average_accuracy': round(np.mean(classification_acc), 4),
                'models': ['Vegetation Stress', 'Pollution Hotspot']
            },
            'overall_performance': 'Good' if np.mean(regression_r2) > 0.7 and np.mean(classification_acc) > 0.9 else 'Moderate'
        }

    def print_report(self) -> None:
        """Print a formatted accuracy report."""
        if not self.models_trained:
            print("Models not evaluated yet. Run train_and_evaluate_all() first.")
            return

        print("\n" + "=" * 70)
        print("MODEL ACCURACY REPORT")
        print("=" * 70)

        # UHI Detection
        uhi = self.metrics['uhi_detection']
        print(f"\n{'─' * 70}")
        print(f"FEATURE 2: {uhi['model_name']}")
        print(f"{'─' * 70}")
        print(f"  R² Score:        {uhi['test_metrics']['r2_score']}")
        print(f"  MAE:             {uhi['test_metrics']['mae']}°C")
        print(f"  RMSE:            {uhi['test_metrics']['rmse']}°C")
        print(f"  MAPE:            {uhi['test_metrics']['mape']}%")
        print(f"  CV R² (5-fold):  {uhi['cross_validation']['cv_r2_mean']} ± {uhi['cross_validation']['cv_r2_std']}")

        # Vegetation
        veg = self.metrics['vegetation_stress']
        print(f"\n{'─' * 70}")
        print(f"FEATURE 3: {veg['model_name']}")
        print(f"{'─' * 70}")
        print(f"  Accuracy:        {veg['test_metrics']['accuracy']}")
        print(f"  Precision:       {veg['test_metrics']['precision_macro']}")
        print(f"  Recall:          {veg['test_metrics']['recall_macro']}")
        print(f"  F1 Score:        {veg['test_metrics']['f1_macro']}")
        print(f"  CV Acc (5-fold): {veg['cross_validation']['cv_accuracy_mean']} ± {veg['cross_validation']['cv_accuracy_std']}")

        # Pollution
        pol = self.metrics['pollution_hotspot']
        print(f"\n{'─' * 70}")
        print(f"FEATURE 4: {pol['model_name']}")
        print(f"{'─' * 70}")
        print(f"  Accuracy:        {pol['test_metrics']['accuracy']}")
        print(f"  Precision:       {pol['test_metrics']['precision_macro']}")
        print(f"  Recall:          {pol['test_metrics']['recall_macro']}")
        print(f"  F1 Score:        {pol['test_metrics']['f1_macro']}")
        print(f"  CV Acc (5-fold): {pol['cross_validation']['cv_accuracy_mean']} ± {pol['cross_validation']['cv_accuracy_std']}")

        # Forecast
        fore = self.metrics['trend_forecast']
        print(f"\n{'─' * 70}")
        print(f"FEATURE 5: {fore['model_name']}")
        print(f"{'─' * 70}")
        print(f"  R² Score:        {fore['test_metrics']['r2_score']}")
        print(f"  MAE:             {fore['test_metrics']['mae']}°C")
        print(f"  RMSE:            {fore['test_metrics']['rmse']}°C")
        print(f"  MAPE:            {fore['test_metrics']['mape']}%")
        print(f"  CV R² (5-fold):  {fore['cross_validation']['cv_r2_mean']} ± {fore['cross_validation']['cv_r2_std']}")

        # Summary
        summ = self.metrics['summary']
        print(f"\n{'=' * 70}")
        print("OVERALL SUMMARY")
        print(f"{'=' * 70}")
        print(f"  Regression Models Avg R²:      {summ['regression_models']['average_r2']}")
        print(f"  Classification Models Avg Acc: {summ['classification_models']['average_accuracy']}")
        print(f"  Overall Performance:           {summ['overall_performance']}")

    def save_report(self, filepath: str = "output/model_accuracy_report.json") -> None:
        """Save metrics to JSON file."""
        import os
        os.makedirs(os.path.dirname(filepath), exist_ok=True)

        with open(filepath, 'w') as f:
            json.dump(self.metrics, f, indent=2)
        print(f"\nReport saved to: {filepath}")


def get_model_accuracy(csv_path: str = "Ahmedabad_TimeSeries_Final.csv") -> Dict[str, Any]:
    """
    Quick function to get all model accuracy metrics.

    Usage:
        from ml_models import get_model_accuracy
        metrics = get_model_accuracy("Ahmedabad_TimeSeries_Final.csv")
    """
    reporter = ModelAccuracyReporter(csv_path)
    metrics = reporter.train_and_evaluate_all()
    reporter.print_report()
    reporter.save_report()
    return metrics


if __name__ == "__main__":
    metrics = get_model_accuracy()
