"""
Main Entry Point - Environmental Analysis System
=================================================
Run this file to execute all models and generate outputs.

Usage:
    python main.py                    # Run all analysis for default city
    python main.py --city Bangalore   # Run for specific city
    python main.py --all-cities       # Run for all available cities
    python main.py --accuracy         # Get model accuracy only
    python main.py --api              # Start API server
"""

import argparse
import json
import os
import sys

# Load environment variables from .env file
try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    pass  # python-dotenv not installed, use system env vars

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

# Supported cities with their coordinates
SUPPORTED_CITIES = {
    "Ahmedabad": {"lat": 23.022, "lng": 72.571, "state": "Gujarat"},
    "Bangalore": {"lat": 12.972, "lng": 77.594, "state": "Karnataka"},
    "Delhi": {"lat": 28.644, "lng": 77.216, "state": "Delhi"},
    "Mumbai": {"lat": 19.076, "lng": 72.877, "state": "Maharashtra"},
}


def get_city_from_csv(csv_path: str) -> str:
    """Extract city name from CSV filename."""
    basename = os.path.basename(csv_path)
    for city in SUPPORTED_CITIES.keys():
        if city.lower() in basename.lower():
            return city
    return "Unknown"


def run_full_analysis(csv_path: str, city: str = None):
    """Run complete environmental analysis for a city."""
    from ml_models import (
        UHIDetectionModel,
        VegetationStressModel,
        PollutionHotspotModel,
        TrendForecastModel,
        ActionPlanGenerator
    )

    if city is None:
        city = get_city_from_csv(csv_path)

    city_info = SUPPORTED_CITIES.get(city, {"state": "India"})

    print("=" * 70)
    print(f"{city.upper()} ENVIRONMENTAL ANALYSIS SYSTEM")
    print("=" * 70)

    # Create city-specific output directory
    output_dir = f"output/{city.lower()}"
    os.makedirs(output_dir, exist_ok=True)

    # Feature 2: UHI Detection
    print("\n[1/5] UHI Detection Model...")
    uhi = UHIDetectionModel(csv_path)
    uhi.city_name = city
    uhi.train_all_models()

    # Feature 3: Vegetation Stress
    print("\n[2/5] Vegetation Stress Model...")
    veg = VegetationStressModel(csv_path)
    veg.city_name = city
    veg.train_all_models()

    # Feature 4: Pollution Hotspot
    print("\n[3/5] Pollution Hotspot Model...")
    pol = PollutionHotspotModel(csv_path)
    pol.city_name = city
    pol.train_all_models()

    # Feature 5: Trend Forecast
    print("\n[4/5] Trend Forecast Model...")
    forecast = TrendForecastModel(csv_path)
    forecast.city_name = city
    forecast.train_all_models()

    # Feature 6: Action Plan
    print("\n[5/5] Generating Action Plan...")
    plan_gen = ActionPlanGenerator(csv_path)
    plan_gen.city_name = city
    plan_gen.uhi_model = uhi
    plan_gen.vegetation_model = veg
    plan_gen.pollution_model = pol
    plan_gen.forecast_model = forecast
    plan = plan_gen.generate_plan()

    # Add city metadata to plan
    plan["city"] = city
    plan["state"] = city_info.get("state", "India")
    plan["coordinates"] = {
        "lat": city_info.get("lat"),
        "lng": city_info.get("lng")
    }

    # Save outputs
    print("\n" + "=" * 70)
    print("SAVING OUTPUTS")
    print("=" * 70)

    outputs = {
        f"{output_dir}/uhi_heatmap.json": uhi.get_heatmap_data(),
        f"{output_dir}/uhi_hotspots.json": uhi.get_hotspots_data(),
        f"{output_dir}/uhi_top_hottest.json": uhi.get_top_hottest(10),
        f"{output_dir}/vegetation_map.json": veg.get_vegetation_map_data(),
        f"{output_dir}/vegetation_alerts.json": veg.get_alerts_data(),
        f"{output_dir}/vegetation_plantation.json": veg.get_plantation_zones_data(10),
        f"{output_dir}/pollution_risk_map.json": pol.get_risk_map_data(),
        f"{output_dir}/pollution_hotspots.json": pol.get_hotspots_data(),
        f"{output_dir}/pollution_compliance.json": pol.get_compliance_report(),
        f"{output_dir}/forecast_trend.json": forecast.get_lst_trend_data(),
        f"{output_dir}/forecast_importance.json": forecast.get_feature_importance_data(),
        f"{output_dir}/forecast_breach.json": forecast.analyze_breach_thresholds(35.0),
        f"{output_dir}/action_plan.json": plan,
        f"{output_dir}/action_summary.json": plan_gen.get_summary(),
    }

    # Add city info to each output
    for filepath, data in outputs.items():
        if isinstance(data, dict):
            data["city"] = city
            data["state"] = city_info.get("state", "India")
        with open(filepath, 'w') as f:
            json.dump(data, f, indent=2, default=str)
        print(f"  Saved: {filepath}")

    # Print summary
    print("\n" + "=" * 70)
    print(f"ANALYSIS COMPLETE - {city.upper()} SUMMARY")
    print("=" * 70)
    print(f"\n  Data Points:          {len(uhi.processed_df)}")
    print(f"  UHI Anomalies:        {uhi.processed_df['is_heat_anomaly'].sum()}")
    print(f"  UHI Hotspot Zones:    {len(uhi.hotspot_zones)}")
    print(f"  Vegetation Stressed:  {(veg.processed_df['health_label'] != 'Healthy').sum()}")
    print(f"  Critical Veg Alerts:  {len(veg.get_alerts_data()['alerts'])}")
    print(f"  Pollution Clusters:   {len(pol.hotspot_zones)}")
    print(f"  Action Items:         {plan['total_actions']}")
    print(f"\n  Output files saved to: {output_dir}/")
    print("=" * 70)

    return city


def run_all_cities():
    """Run analysis for all supported cities."""
    print("\n" + "=" * 70)
    print("MULTI-CITY ENVIRONMENTAL ANALYSIS")
    print("=" * 70)
    print(f"\nProcessing {len(SUPPORTED_CITIES)} cities: {', '.join(SUPPORTED_CITIES.keys())}\n")

    results = {}
    for city in SUPPORTED_CITIES.keys():
        csv_file = f"{city}_TimeSeries_Final.csv"
        if os.path.exists(csv_file):
            print(f"\n{'='*70}")
            print(f"Processing: {city}")
            print(f"{'='*70}")
            run_full_analysis(csv_file, city)
            results[city] = "Success"
        else:
            print(f"\n  WARNING: {csv_file} not found, skipping {city}")
            results[city] = "Skipped - No data file"

    # Save city index
    city_index = {
        "cities": list(SUPPORTED_CITIES.keys()),
        "city_info": SUPPORTED_CITIES,
        "results": results
    }
    with open("output/cities.json", 'w') as f:
        json.dump(city_index, f, indent=2)
    print(f"\n  Saved: output/cities.json")

    print("\n" + "=" * 70)
    print("ALL CITIES PROCESSED")
    print("=" * 70)
    for city, status in results.items():
        print(f"  {city}: {status}")


def run_accuracy_report(csv_path: str = "Ahmedabad_TimeSeries_Final.csv"):
    """Run accuracy evaluation for all models."""
    from ml_models import get_model_accuracy
    metrics = get_model_accuracy(csv_path)
    return metrics


def run_api_server():
    """Start the FastAPI server."""
    try:
        import uvicorn
        from ml_models.api_endpoints import create_app

        host = os.getenv("ML_API_HOST", "0.0.0.0")
        port = int(os.getenv("ML_API_PORT", "8000"))

        app = create_app()
        print(f"\nStarting API server at http://{host}:{port}")
        print(f"API Docs: http://{host}:{port}/docs")
        uvicorn.run(app, host=host, port=port)
    except ImportError:
        print("Error: Install fastapi and uvicorn first:")
        print("  pip install fastapi uvicorn")


def main():
    default_csv = os.getenv("CSV_DATA_PATH", "Ahmedabad_TimeSeries_Final.csv")

    parser = argparse.ArgumentParser(description="Environmental Analysis System")
    parser.add_argument("--accuracy", action="store_true", help="Run accuracy report only")
    parser.add_argument("--api", action="store_true", help="Start API server")
    parser.add_argument("--csv", default=default_csv, help="Path to CSV file")
    parser.add_argument("--city", choices=list(SUPPORTED_CITIES.keys()), help="City to analyze")
    parser.add_argument("--all-cities", action="store_true", help="Run analysis for all cities")

    args = parser.parse_args()

    if args.accuracy:
        run_accuracy_report(args.csv)
    elif args.api:
        run_api_server()
    elif args.all_cities:
        run_all_cities()
    elif args.city:
        csv_file = f"{args.city}_TimeSeries_Final.csv"
        run_full_analysis(csv_file, args.city)
    else:
        run_full_analysis(args.csv)


if __name__ == "__main__":
    main()
