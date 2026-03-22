"""
Environment Action Plan Generator (Feature 6)
==============================================
This module integrates all ML model outputs to generate actionable
environmental intervention plans with priority scoring.
"""

import pandas as pd
import numpy as np
import json
from typing import Dict, List, Any, Optional
from dataclasses import dataclass, asdict, field
from datetime import datetime, timedelta
import warnings
warnings.filterwarnings('ignore')

# Import other models
from .uhi_detection import UHIDetectionModel
from .vegetation_stress import VegetationStressModel
from .pollution_hotspot import PollutionHotspotModel
from .trend_forecast import TrendForecastModel


# ============================================================================
# DATA CLASSES
# ============================================================================

@dataclass
class ActionLocation:
    """Location details for an action."""
    zone_name: str
    center_lat: Optional[float] = None
    center_lng: Optional[float] = None
    affected_points: int = 0


@dataclass
class ActionTrigger:
    """What triggered this action."""
    model: str
    finding: str
    satellite: str


@dataclass
class ActionItem:
    """Represents a single action item in the plan."""
    action_id: int
    priority: str
    priority_score: float
    title: str
    description: str
    location: ActionLocation
    triggered_by: ActionTrigger
    responsible_dept: str
    deadline: str
    estimated_impact: str
    status: str = "Pending"


@dataclass
class ActionPlanSummary:
    """Summary of the action plan."""
    high_priority: int
    medium_priority: int
    low_priority: int


# ============================================================================
# RULE ENGINE
# ============================================================================

class RuleEngine:
    """
    Maps ML findings to specific actions using predefined rules.
    """

    def __init__(self):
        self.rules = self._define_rules()

    def _define_rules(self) -> List[Dict]:
        """Define mapping rules from findings to actions."""
        return [
            {
                'condition': 'uhi_hotspot_high',
                'action_template': {
                    'title': "Emergency tree plantation in {zone_name}",
                    'description': "{points} locations show critically high LST (avg {lst}°C) combined with NDVI below 0.08. Immediate plantation of minimum {trees} trees required.",
                    'responsible_dept': "Parks Department + PWD",
                    'deadline': "Within 30 days",
                    'estimated_impact': "Could reduce LST by 1.5-2°C in affected zone"
                },
                'priority_weight': 1.0
            },
            {
                'condition': 'pollution_critical',
                'action_template': {
                    'title': "PCB inspection of {zone_name}",
                    'description': "{points} locations show simultaneous high temperature, bare land, and dry soil — environmental signature of active industrial pollution. Immediate on-site inspection required.",
                    'responsible_dept': "Pollution Control Board",
                    'deadline': "Within 7 days",
                    'estimated_impact': "Prevent further land degradation in {points} flagged locations"
                },
                'priority_weight': 0.95
            },
            {
                'condition': 'vegetation_barren_critical',
                'action_template': {
                    'title': "Emergency greening initiative for {zone_name}",
                    'description': "{points} locations classified as barren with NDVI below 0.10. Z-score analysis shows these are {z_deviation} below city average. Urgent plantation drive required.",
                    'responsible_dept': "Forest Department",
                    'deadline': "Within 45 days",
                    'estimated_impact': "Restore vegetation cover in critical zones"
                },
                'priority_weight': 0.85
            },
            {
                'condition': 'soil_moisture_low',
                'action_template': {
                    'title': "Soil moisture recharge programme in {zone_name}",
                    'description': "{points} locations show soil moisture below {threshold} — the lowest quartile for Ahmedabad. Groundwater recharge pits and rainwater harvesting structures recommended.",
                    'responsible_dept': "Water Resources Department",
                    'deadline': "Within 60 days",
                    'estimated_impact': "Improve soil moisture and reduce surface heating"
                },
                'priority_weight': 0.70
            },
            {
                'condition': 'lst_trend_increasing',
                'action_template': {
                    'title': "Green corridor development along high-LST axis",
                    'description': "ARIMA forecast shows LST increasing trend ({trend}). Feature importance shows NDVI is {ndvi_importance}% responsible for temperature variation. A connected green corridor would have maximum city-wide cooling impact.",
                    'responsible_dept': "City Planning Department + Forest Department",
                    'deadline': "Long-term — 6 months planning",
                    'estimated_impact': "Systemic temperature reduction across city"
                },
                'priority_weight': 0.65
            },
            {
                'condition': 'cool_roof_needed',
                'action_template': {
                    'title': "Cool roof mandate for top hottest ward locations",
                    'description': "{points} locations with LST above {threshold}°C in dense urban areas where tree plantation is not feasible. Cool roof coating mandate recommended for all new constructions.",
                    'responsible_dept': "Municipal Corporation — Building Permits",
                    'deadline': "Policy within 90 days",
                    'estimated_impact': "Reduce indoor temperatures and surface heat"
                },
                'priority_weight': 0.60
            },
            {
                'condition': 'monitoring_needed',
                'action_template': {
                    'title': "Monthly satellite monitoring protocol for vegetation recovery",
                    'description': "{pct}% of Ahmedabad shows stressed or barren vegetation. A monthly NDVI monitoring protocol using Sentinel-2 data will track recovery after interventions.",
                    'responsible_dept': "Environmental Monitoring Cell",
                    'deadline': "Ongoing — start within 30 days",
                    'estimated_impact': "Track intervention effectiveness over time"
                },
                'priority_weight': 0.40
            }
        ]

    def apply_rules(self, findings: Dict[str, Any]) -> List[Dict]:
        """Apply rules to findings and generate action candidates."""
        action_candidates = []

        # UHI hotspot rules
        for hotspot in findings.get('uhi_hotspots', []):
            if hotspot['severity'] in ['High', 'Critical']:
                action = self._create_action_from_rule(
                    'uhi_hotspot_high',
                    zone_name=hotspot['zone_name'],
                    points=hotspot['point_count'],
                    lst=hotspot['avg_lst_celsius'],
                    trees=hotspot['point_count'] * 30,
                    model="Isolation Forest + DBSCAN",
                    finding=f"LST anomaly, avg {hotspot['avg_lst_celsius']}°C",
                    satellite="MODIS LST + Sentinel-2",
                    lat=hotspot['center_lat'],
                    lng=hotspot['center_lng'],
                    affected=hotspot['point_count']
                )
                action_candidates.append(action)

        # Pollution cluster rules
        for cluster in findings.get('pollution_clusters', []):
            if cluster['severity'] in ['High', 'Critical']:
                action = self._create_action_from_rule(
                    'pollution_critical',
                    zone_name=cluster['zone_name'],
                    points=cluster['point_count'],
                    model="Isolation Forest + Risk Score",
                    finding=f"Risk score {cluster['avg_risk_score']}/100 — extreme environmental stress",
                    satellite="MODIS LST + Sentinel-2 NDVI + SMAP",
                    lat=cluster['center_lat'],
                    lng=cluster['center_lng'],
                    affected=cluster['point_count']
                )
                action_candidates.append(action)

        # Vegetation stress rules
        veg_summary = findings.get('vegetation_summary', {})
        if veg_summary.get('barren_pct', 0) > 30:
            barren_zones = findings.get('plantation_zones', [])
            if barren_zones:
                top_zone = barren_zones[0]
                action = self._create_action_from_rule(
                    'vegetation_barren_critical',
                    zone_name=top_zone['nearest_landmark'],
                    points=len(barren_zones),
                    z_deviation="significantly",
                    model="RF Classifier + Z-score",
                    finding=f"NDVI {top_zone['NDVI_Sentinel']:.3f}, classified as Barren",
                    satellite="Sentinel-2 NDVI",
                    lat=top_zone['lat'],
                    lng=top_zone['lng'],
                    affected=len(barren_zones)
                )
                action_candidates.append(action)

        # Soil moisture rules
        sm_low_count = findings.get('low_moisture_count', 0)
        if sm_low_count > 20:
            action = self._create_action_from_rule(
                'soil_moisture_low',
                zone_name="Low moisture zones",
                points=sm_low_count,
                threshold="0.146",
                model="Z-score + Feature Importance",
                finding=f"sm_surface contributor to LST variation",
                satellite="SMAP Soil Moisture",
                affected=sm_low_count
            )
            action_candidates.append(action)

        # Trend-based rules
        trend_info = findings.get('trend_info', {})
        if trend_info.get('direction') == 'Increasing':
            action = self._create_action_from_rule(
                'lst_trend_increasing',
                zone_name="City-wide corridor",
                trend=trend_info.get('rate', 'positive'),
                ndvi_importance=round(findings.get('top_feature_importance', 0.42) * 100, 1),
                model="ARIMA + RF Feature Importance",
                finding=f"LST trend {trend_info.get('direction')}, NDVI is top driver",
                satellite="MODIS + Sentinel-2 combined",
                affected=findings.get('total_points', 400)
            )
            action_candidates.append(action)

        # Cool roof rules (for dense urban hottest spots)
        top_hottest = findings.get('top_hottest', [])
        if len(top_hottest) >= 10:
            action = self._create_action_from_rule(
                'cool_roof_needed',
                zone_name="Multiple wards — see coordinates list",
                points=10,
                threshold=36.5,
                model="Isolation Forest + Top Hottest Ranking",
                finding="High LST in dense urban areas",
                satellite="MODIS LST + Landsat Thermal",
                affected=10
            )
            action_candidates.append(action)

        # Monitoring rule
        stressed_pct = veg_summary.get('stressed_pct', 0) + veg_summary.get('barren_pct', 0)
        if stressed_pct > 50:
            action = self._create_action_from_rule(
                'monitoring_needed',
                zone_name="City-wide",
                pct=round(stressed_pct, 1),
                model="RF Classifier Summary",
                finding=f"{stressed_pct}% stressed/barren classification",
                satellite="Sentinel-2 NDVI",
                affected=findings.get('total_points', 400)
            )
            action_candidates.append(action)

        return action_candidates

    def _create_action_from_rule(self, condition: str, **kwargs) -> Dict:
        """Create action dict from rule and parameters."""
        rule = next((r for r in self.rules if r['condition'] == condition), None)
        if not rule:
            return {}

        template = rule['action_template']

        return {
            'condition': condition,
            'title': template['title'].format(**kwargs),
            'description': template['description'].format(**kwargs),
            'responsible_dept': template['responsible_dept'],
            'deadline': template['deadline'],
            'estimated_impact': template.get('estimated_impact', '').format(**kwargs),
            'priority_weight': rule['priority_weight'],
            'model': kwargs.get('model', ''),
            'finding': kwargs.get('finding', ''),
            'satellite': kwargs.get('satellite', ''),
            'lat': kwargs.get('lat'),
            'lng': kwargs.get('lng'),
            'zone_name': kwargs.get('zone_name', 'Unknown Zone'),
            'affected_points': kwargs.get('affected', 0)
        }


# ============================================================================
# PRIORITY SCORER
# ============================================================================

class PriorityScorer:
    """Calculates priority scores for actions."""

    def calculate_score(self, action: Dict, findings: Dict) -> float:
        """
        Calculate priority score based on:
        - Base weight from rule
        - Points affected
        - Trend multiplier
        """
        base_weight = action.get('priority_weight', 0.5)
        points_affected = action.get('affected_points', 1)
        total_points = findings.get('total_points', 400)

        # Normalize affected points (0-1)
        points_factor = min(points_affected / (total_points * 0.1), 1.0)

        # Trend multiplier
        trend_info = findings.get('trend_info', {})
        if trend_info.get('direction') == 'Increasing':
            trend_multiplier = 1.2
        elif trend_info.get('direction') == 'Decreasing':
            trend_multiplier = 0.8
        else:
            trend_multiplier = 1.0

        # Calculate final score (0-100)
        score = base_weight * 70 + points_factor * 20 + (trend_multiplier - 1) * 10
        score = min(100, max(0, score * 1.2))  # Scale and clamp

        return round(score, 1)

    def assign_priority_level(self, score: float) -> str:
        """Assign priority level based on score."""
        if score >= 75:
            return "High"
        elif score >= 50:
            return "Medium"
        else:
            return "Low"


# ============================================================================
# MAIN ACTION PLAN GENERATOR
# ============================================================================

class ActionPlanGenerator:
    """
    Generates comprehensive environmental action plans by integrating
    outputs from all ML models.
    """

    def __init__(self, csv_path: str):
        """Initialize with data path."""
        self.csv_path = csv_path

        # Initialize all models
        self.uhi_model: Optional[UHIDetectionModel] = None
        self.vegetation_model: Optional[VegetationStressModel] = None
        self.pollution_model: Optional[PollutionHotspotModel] = None
        self.forecast_model: Optional[TrendForecastModel] = None

        # Engines
        self.rule_engine = RuleEngine()
        self.priority_scorer = PriorityScorer()

        # Generated plan
        self.actions: List[ActionItem] = []
        self.generated_at: Optional[datetime] = None

        # Aggregated findings
        self.findings: Dict[str, Any] = {}

    def initialize_models(self) -> None:
        """Initialize and train all underlying models."""
        print("=" * 60)
        print("ACTION PLAN GENERATOR - INITIALIZING ALL MODELS")
        print("=" * 60)

        # Initialize UHI model
        print("\n[1/4] Training UHI Detection Model...")
        self.uhi_model = UHIDetectionModel(self.csv_path)
        self.uhi_model.train_all_models()

        # Initialize Vegetation model
        print("\n[2/4] Training Vegetation Stress Model...")
        self.vegetation_model = VegetationStressModel(self.csv_path)
        self.vegetation_model.train_all_models()

        # Initialize Pollution model
        print("\n[3/4] Training Pollution Hotspot Model...")
        self.pollution_model = PollutionHotspotModel(self.csv_path)
        self.pollution_model.train_all_models()

        # Initialize Forecast model
        print("\n[4/4] Training Trend Forecast Model...")
        self.forecast_model = TrendForecastModel(self.csv_path)
        self.forecast_model.train_all_models()

        print("\n" + "=" * 60)
        print("ALL MODELS INITIALIZED")
        print("=" * 60)

    def aggregate_findings(self) -> Dict[str, Any]:
        """Aggregate findings from all models."""
        self.findings = {
            'total_points': len(self.uhi_model.processed_df),

            # UHI findings
            'uhi_hotspots': [asdict(z) for z in self.uhi_model.hotspot_zones],
            'total_heat_anomalies': int(self.uhi_model.processed_df['is_heat_anomaly'].sum()),
            'top_hottest': self.uhi_model.get_top_hottest(10)['ranking'],

            # Vegetation findings
            'vegetation_summary': self.vegetation_model.get_vegetation_map_data()['summary'],
            'critical_stress_alerts': len(self.vegetation_model.get_alerts_data()['alerts']),
            'plantation_zones': [asdict(z) for z in self.vegetation_model.get_plantation_recommendations(10)],

            # Pollution findings
            'pollution_clusters': [asdict(z) for z in self.pollution_model.hotspot_zones],
            'extreme_outliers': self.pollution_model.get_extreme_outliers_data()['total_flagged'],
            'city_mean_risk': self.pollution_model.city_mean_risk,

            # Trend findings
            'trend_info': {
                'direction': self.forecast_model.get_trend_direction()[0],
                'rate': self.forecast_model.get_trend_direction()[1]
            },
            'top_feature_importance': self.forecast_model.get_feature_importance()[0].importance
                if self.forecast_model.get_feature_importance() else 0,

            # Soil moisture (low quartile count)
            'low_moisture_count': int((
                self.uhi_model.processed_df['sm_surface'] <
                self.uhi_model.processed_df['sm_surface'].quantile(0.25)
            ).sum())
        }

        return self.findings

    def generate_plan(self, include_features: List[str] = None) -> Dict[str, Any]:
        """
        Generate complete action plan.

        Args:
            include_features: List of features to include ['uhi', 'vegetation', 'pollution', 'forecast']
        """
        if include_features is None:
            include_features = ['uhi', 'vegetation', 'pollution', 'forecast']

        if not self.uhi_model:
            self.initialize_models()

        # Aggregate findings
        self.aggregate_findings()

        # Apply rules to get action candidates
        action_candidates = self.rule_engine.apply_rules(self.findings)

        # Score and prioritize actions
        self.actions = []
        for i, candidate in enumerate(action_candidates, 1):
            score = self.priority_scorer.calculate_score(candidate, self.findings)
            priority = self.priority_scorer.assign_priority_level(score)

            action = ActionItem(
                action_id=i,
                priority=priority,
                priority_score=score,
                title=candidate['title'],
                description=candidate['description'],
                location=ActionLocation(
                    zone_name=candidate['zone_name'],
                    center_lat=candidate.get('lat'),
                    center_lng=candidate.get('lng'),
                    affected_points=candidate['affected_points']
                ),
                triggered_by=ActionTrigger(
                    model=candidate['model'],
                    finding=candidate['finding'],
                    satellite=candidate['satellite']
                ),
                responsible_dept=candidate['responsible_dept'],
                deadline=candidate['deadline'],
                estimated_impact=candidate['estimated_impact']
            )
            self.actions.append(action)

        # Sort by priority score
        self.actions.sort(key=lambda x: x.priority_score, reverse=True)

        # Reassign IDs after sorting
        for i, action in enumerate(self.actions, 1):
            action.action_id = i

        self.generated_at = datetime.now()

        # Create response
        summary = ActionPlanSummary(
            high_priority=sum(1 for a in self.actions if a.priority == 'High'),
            medium_priority=sum(1 for a in self.actions if a.priority == 'Medium'),
            low_priority=sum(1 for a in self.actions if a.priority == 'Low')
        )

        return {
            'city': 'Ahmedabad',
            'generated_at': self.generated_at.isoformat(),
            'satellite_sources': ['MODIS LST', 'Sentinel-2 NDVI', 'Landsat NDVI', 'SMAP Soil Moisture'],
            'total_actions': len(self.actions),
            'summary': asdict(summary),
            'actions': [self._serialize_action(a) for a in self.actions]
        }

    def _serialize_action(self, action: ActionItem) -> Dict:
        """Serialize action item for JSON response."""
        return {
            'action_id': action.action_id,
            'priority': action.priority,
            'priority_score': action.priority_score,
            'title': action.title,
            'description': action.description,
            'location': asdict(action.location),
            'triggered_by': asdict(action.triggered_by),
            'responsible_dept': action.responsible_dept,
            'deadline': action.deadline,
            'estimated_impact': action.estimated_impact,
            'status': action.status
        }

    def get_summary(self) -> Dict[str, Any]:
        """Get quick summary for dashboard widget."""
        if not self.actions:
            return {'error': 'No plan generated yet'}

        return {
            'city': 'Ahmedabad',
            'last_generated': self.generated_at.isoformat() if self.generated_at else None,
            'total_actions': len(self.actions),
            'high_priority': sum(1 for a in self.actions if a.priority == 'High'),
            'medium_priority': sum(1 for a in self.actions if a.priority == 'Medium'),
            'low_priority': sum(1 for a in self.actions if a.priority == 'Low'),
            'top_action': self.actions[0].title if self.actions else None,
            'satellite_findings': f"{self.findings.get('total_heat_anomalies', 0)} heat anomalies, {self.findings.get('critical_stress_alerts', 0)} critical vegetation stress points, {len(self.findings.get('pollution_clusters', []))} pollution clusters detected"
        }

    def update_action_status(self, action_id: int, status: str,
                            notes: str = "", updated_by: str = "") -> Dict[str, Any]:
        """Update status of an action."""
        action = next((a for a in self.actions if a.action_id == action_id), None)

        if not action:
            return {'error': f'Action {action_id} not found'}

        previous_status = action.status
        action.status = status

        return {
            'action_id': action_id,
            'previous_status': previous_status,
            'new_status': status,
            'updated_at': datetime.now().isoformat(),
            'updated_by': updated_by,
            'notes': notes
        }

    def export_pdf_data(self) -> Dict[str, Any]:
        """
        Get data for PDF export.
        Note: Actual PDF generation requires reportlab package.
        """
        return {
            'title': f"Environmental Action Plan - Ahmedabad",
            'generated_at': self.generated_at.isoformat() if self.generated_at else datetime.now().isoformat(),
            'satellite_sources': ['MODIS LST', 'Sentinel-2 NDVI', 'Landsat NDVI', 'SMAP Soil Moisture'],
            'summary': {
                'total_actions': len(self.actions),
                'high_priority': sum(1 for a in self.actions if a.priority == 'High'),
                'medium_priority': sum(1 for a in self.actions if a.priority == 'Medium'),
                'low_priority': sum(1 for a in self.actions if a.priority == 'Low')
            },
            'key_findings': {
                'heat_anomalies': self.findings.get('total_heat_anomalies', 0),
                'vegetation_stress': self.findings.get('critical_stress_alerts', 0),
                'pollution_clusters': len(self.findings.get('pollution_clusters', [])),
                'trend': self.findings.get('trend_info', {}).get('direction', 'Unknown')
            },
            'actions': [self._serialize_action(a) for a in self.actions]
        }


# ============================================================================
# STANDALONE EXECUTION
# ============================================================================

if __name__ == "__main__":
    csv_path = "Ahmedabad_TimeSeries_Final.csv"

    generator = ActionPlanGenerator(csv_path)

    # Generate plan
    plan = generator.generate_plan()

    print("\n" + "=" * 60)
    print("GENERATED ACTION PLAN")
    print("=" * 60)

    print(f"\nCity: {plan['city']}")
    print(f"Generated: {plan['generated_at']}")
    print(f"Total Actions: {plan['total_actions']}")
    print(f"Summary: {plan['summary']}")

    print("\nActions:")
    for action in plan['actions']:
        print(f"\n  [{action['priority']}] {action['action_id']}. {action['title']}")
        print(f"     Score: {action['priority_score']}")
        print(f"     Dept: {action['responsible_dept']}")
        print(f"     Deadline: {action['deadline']}")

    # Summary
    summary = generator.get_summary()
    print("\n" + "=" * 60)
    print("QUICK SUMMARY")
    print("=" * 60)
    print(json.dumps(summary, indent=2))
