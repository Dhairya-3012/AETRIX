"""
Main Entry Point - Environmental Analysis System
=================================================
Run this file to execute all models and generate outputs.

Usage:
    python main.py                    # Run all analysis
    python main.py --accuracy         # Get model accuracy only
    python main.py --api              # Start API server
"""

import argparse
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))


def run_full_analysis(csv_path: str = "Ahmedabad_MultiSatellite_Data.csv"):
    """Run complete environmental analysis."""
    from ml_models import (
        UHIDetectionModel,
        VegetationStressModel,
        PollutionHotspotModel,
        TrendForecastModel,
        ActionPlanGenerator
    )

    print("=" * 70)
    print("AHMEDABAD ENVIRONMENTAL ANALYSIS SYSTEM")
    print("=" * 70)

    os.makedirs("output", exist_ok=True)

    # Feature 2: UHI Detection
    print("\n[1/5] UHI Detection Model...")
    uhi = UHIDetectionModel(csv_path)
    uhi.train_all_models()

    # Feature 3: Vegetation Stress
    print("\n[2/5] Vegetation Stress Model...")
    veg = VegetationStressModel(csv_path)
    veg.train_all_models()

    # Feature 4: Pollution Hotspot
    print("\n[3/5] Pollution Hotspot Model...")
    pol = PollutionHotspotModel(csv_path)
    pol.train_all_models()

    # Feature 5: Trend Forecast
    print("\n[4/5] Trend Forecast Model...")
    forecast = TrendForecastModel(csv_path)
    forecast.train_all_models()

    # Feature 6: Action Plan
    print("\n[5/5] Generating Action Plan...")
    plan_gen = ActionPlanGenerator(csv_path)
    plan_gen.uhi_model = uhi
    plan_gen.vegetation_model = veg
    plan_gen.pollution_model = pol
    plan_gen.forecast_model = forecast
    plan = plan_gen.generate_plan()

    # Save outputs
    print("\n" + "=" * 70)
    print("SAVING OUTPUTS")
    print("=" * 70)

    outputs = {
        "output/uhi_heatmap.json": uhi.get_heatmap_data(),
        "output/uhi_hotspots.json": uhi.get_hotspots_data(),
        "output/uhi_top_hottest.json": uhi.get_top_hottest(10),
        "output/vegetation_map.json": veg.get_vegetation_map_data(),
        "output/vegetation_alerts.json": veg.get_alerts_data(),
        "output/vegetation_plantation.json": veg.get_plantation_zones_data(10),
        "output/pollution_risk_map.json": pol.get_risk_map_data(),
        "output/pollution_hotspots.json": pol.get_hotspots_data(),
        "output/pollution_compliance.json": pol.get_compliance_report(),
        "output/forecast_trend.json": forecast.get_lst_trend_data(),
        "output/forecast_importance.json": forecast.get_feature_importance_data(),
        "output/forecast_breach.json": forecast.analyze_breach_thresholds(35.0),
        "output/action_plan.json": plan,
        "output/action_summary.json": plan_gen.get_summary(),
    }

    for filepath, data in outputs.items():
        with open(filepath, 'w') as f:
            json.dump(data, f, indent=2, default=str)
        print(f"  Saved: {filepath}")

    # Print summary
    print("\n" + "=" * 70)
    print("ANALYSIS COMPLETE - SUMMARY")
    print("=" * 70)
    print(f"\n  Data Points:          {len(uhi.processed_df)}")
    print(f"  UHI Anomalies:        {uhi.processed_df['is_heat_anomaly'].sum()}")
    print(f"  UHI Hotspot Zones:    {len(uhi.hotspot_zones)}")
    print(f"  Vegetation Stressed:  {(veg.processed_df['health_label'] != 'Healthy').sum()}")
    print(f"  Critical Veg Alerts:  {len(veg.get_alerts_data()['alerts'])}")
    print(f"  Pollution Clusters:   {len(pol.hotspot_zones)}")
    print(f"  Action Items:         {plan['total_actions']}")
    print(f"\n  Output files saved to: output/")
    print("=" * 70)


def run_accuracy_report(csv_path: str = "Ahmedabad_MultiSatellite_Data.csv"):
    """Run accuracy evaluation for all models."""
    from ml_models import get_model_accuracy
    metrics = get_model_accuracy(csv_path)
    return metrics


def run_api_server():
    """Start the FastAPI server."""
    try:
        import uvicorn
        from ml_models.api_endpoints import create_app
        app = create_app()
        print("\nStarting API server at http://localhost:8000")
        print("API Docs: http://localhost:8000/docs")
        uvicorn.run(app, host="0.0.0.0", port=8000)
    except ImportError:
        print("Error: Install fastapi and uvicorn first:")
        print("  pip install fastapi uvicorn")


def main():
    parser = argparse.ArgumentParser(description="Environmental Analysis System")
    parser.add_argument("--accuracy", action="store_true", help="Run accuracy report only")
    parser.add_argument("--api", action="store_true", help="Start API server")
    parser.add_argument("--csv", default="Ahmedabad_MultiSatellite_Data.csv", help="Path to CSV file")

    args = parser.parse_args()

    if args.accuracy:
        run_accuracy_report(args.csv)
    elif args.api:
        run_api_server()
    else:
        run_full_analysis(args.csv)


if __name__ == "__main__":
    main()
