# AETRIX - Satellite Environmental Intelligence Platform for Smart Cities

**All-India Environmental Telemetry & Risk Intelligence X**

A comprehensive satellite-based environmental monitoring platform developed for the **Satimage Hackathon** organized by **ISRO** and **Ahmedabad University**.

---

## Problem Statement

> **Build an intelligent platform that uses multi-satellite data to detect, predict, and mitigate environmental risks** like Urban Heat Islands (UHI), vegetation stress, and pollution hotspots in Indian cities using machine learning models.

### Key Objectives
- Integrate multi-satellite environmental data (MODIS, Landsat, Sentinel-2, SMAP)
- Detect Urban Heat Islands using Land Surface Temperature (LST) analysis
- Monitor vegetation health through multi-source NDVI analysis
- Identify pollution hotspots and environmental risk zones
- Forecast environmental trends using ML models
- Generate actionable recommendations for urban planners

---

## Solution Architecture

```
+------------------+     +------------------+     +------------------+
|    Frontend      |     |     Backend      |     |    ML Module     |
|   (React.js)     |<--->| (Spring Boot)    |<--->|    (Python)      |
|                  |     |                  |     |                  |
| - MapTiler Maps  |     | - REST APIs      |     | - UHI Detection  |
| - Interactive UI |     | - PostgreSQL     |     | - NDVI Analysis  |
| - Recharts       |     | - Caffeine Cache |     | - Pollution ML   |
| - Dashboard      |     | - xAI Integration|     | - Forecasting    |
+------------------+     +------------------+     +------------------+
```

---

## Features

### 1. Urban Heat Island (UHI) Detection
- Machine Learning models for heat anomaly classification
- Heatmap visualization with temperature gradients
- Hotspot zone identification and clustering
- Top-N hottest location analysis

### 2. Vegetation Health Monitoring
- Multi-satellite NDVI fusion (Landsat + Sentinel-2)
- Vegetation stress classification (Healthy/Stressed/Critical)
- Alert system for vegetation degradation
- Priority plantation zone recommendations

### 3. Pollution Hotspot Analysis
- Risk zone mapping using clustering algorithms
- Compliance reporting against environmental standards
- Industrial vs. residential pollution differentiation
- Real-time pollution level tracking

### 4. Trend Forecasting
- Time-series prediction for LST trends
- Feature importance analysis
- Threshold breach predictions
- Seasonal pattern recognition

### 5. AI-Powered Action Plans
- Automated prioritization of interventions
- Zone-specific recommendations
- Integration with Grok LLM for intelligent summaries
- Cost-benefit analysis for proposed actions

---

## Tech Stack

| Layer | Technology | Version |
|-------|------------|---------|
| **Frontend** | React.js | 18.2 |
| **Mapping** | MapTiler SDK | 2.0.3 |
| **Charts** | Recharts | 2.12 |
| **Backend** | Spring Boot | 3.5.12 |
| **Database** | PostgreSQL | 42.7.3 |
| **Cache** | Caffeine | 3.1.8 |
| **API Docs** | SpringDoc OpenAPI | 2.8.5 |
| **ML Framework** | scikit-learn | 1.0+ |
| **ML API** | FastAPI | 0.68+ |
| **Language** | Java 17 / Python 3.10+ | - |

---

## Data Sources

| Satellite | Product | Parameter | Resolution |
|-----------|---------|-----------|------------|
| **MODIS** | MOD11A1 | Land Surface Temperature (LST) | 1 km |
| **Landsat 8/9** | Collection 2 | NDVI | 30 m |
| **Sentinel-2** | Level-2A | NDVI | 10 m |
| **SMAP** | L4 | Soil Moisture | 9 km |

---

## Project Structure

```
Aetrix/
├── frontend/                   # React.js Frontend
│   ├── src/
│   │   ├── components/         # UI Components
│   │   │   ├── Header.jsx
│   │   │   ├── Sidebar.jsx
│   │   │   ├── KpiCard.jsx
│   │   │   ├── AiSummaryCard.jsx
│   │   │   └── ActionCard.jsx
│   │   ├── pages/              # Page Components
│   │   │   ├── DashboardPage.jsx
│   │   │   ├── MapPage.jsx
│   │   │   ├── UhiPage.jsx
│   │   │   ├── VegetationPage.jsx
│   │   │   ├── PollutionPage.jsx
│   │   │   ├── ForecastPage.jsx
│   │   │   └── ActionPlanPage.jsx
│   │   ├── services/           # API Services
│   │   ├── context/            # React Context
│   │   └── utils/              # Utility Functions
│   └── package.json
│
├── backend/                    # Spring Boot Backend
│   ├── src/main/java/com/aetrix/
│   │   ├── controller/         # REST Controllers
│   │   │   ├── DashboardController.java
│   │   │   ├── UhiController.java
│   │   │   ├── VegetationController.java
│   │   │   ├── PollutionController.java
│   │   │   ├── ForecastController.java
│   │   │   └── ActionPlanController.java
│   │   ├── service/            # Business Logic
│   │   ├── repository/         # Data Access
│   │   ├── entity/             # JPA Entities
│   │   ├── dto/                # Data Transfer Objects
│   │   └── config/             # Configuration
│   └── pom.xml
│
├── ML/                         # Machine Learning Module
│   ├── main.py                 # Entry Point
│   ├── ml_models/
│   │   ├── uhi_detection.py    # UHI ML Model
│   │   ├── vegetation_stress.py # Vegetation Analysis
│   │   ├── pollution_hotspot.py # Pollution Detection
│   │   ├── trend_forecast.py   # Time-series Forecasting
│   │   ├── action_plan.py      # Action Plan Generator
│   │   ├── accuracy_metrics.py # Model Evaluation
│   │   └── api_endpoints.py    # FastAPI Endpoints
│   ├── output/                 # Generated JSON Outputs
│   └── Ahmedabad_MultiSatellite_Data.csv
│
└── README.md
```

---

## Installation & Setup

### Prerequisites
- Node.js 18+ and npm
- Java 17+
- Python 3.10+
- PostgreSQL 14+
- Maven 3.8+

### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/aetrix.git
cd aetrix
```

### 2. Frontend Setup
```bash
cd frontend

# Install dependencies
npm install

# Create environment file
cp .env.example .env

# Edit .env and add your MapTiler API key
# REACT_APP_MAPTILER_KEY=your_api_key

# Start development server
npm start
```

### 3. Backend Setup
```bash
cd backend

# Configure database in application.properties
# spring.datasource.url=jdbc:postgresql://localhost:5432/aetrix
# spring.datasource.username=your_username
# spring.datasource.password=your_password

# Build and run
mvn clean install
mvn spring-boot:run
```

### 4. ML Module Setup
```bash
cd ML

# Create virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r ml_models/requirements.txt

# Run full analysis
python main.py

# Or start API server
python main.py --api
```

---

## Usage

### Running Full Analysis
```bash
cd ML
python main.py
```

This will:
1. Load satellite data from CSV
2. Train all ML models
3. Generate heatmaps, hotspot zones, alerts
4. Create action plan recommendations
5. Save outputs to `output/` directory

### Getting Model Accuracy
```bash
python main.py --accuracy
```

### Starting ML API Server
```bash
python main.py --api
# API available at http://localhost:8000
# Docs at http://localhost:8000/docs
```

---

## API Endpoints

### Backend (Spring Boot) - Port 8080
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/dashboard` | GET | Dashboard KPIs |
| `/api/uhi/heatmap` | GET | UHI Heatmap Data |
| `/api/uhi/hotspots` | GET | UHI Hotspot Zones |
| `/api/vegetation/map` | GET | Vegetation Health Map |
| `/api/vegetation/alerts` | GET | Vegetation Alerts |
| `/api/pollution/risk-map` | GET | Pollution Risk Zones |
| `/api/forecast/trends` | GET | LST Trend Predictions |
| `/api/action-plan` | GET | Action Recommendations |

### ML API (FastAPI) - Port 8000
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/health` | GET | Health Check |
| `/predict/uhi` | POST | UHI Prediction |
| `/predict/vegetation` | POST | Vegetation Analysis |
| `/predict/pollution` | POST | Pollution Risk Score |
| `/forecast` | POST | Trend Forecast |

---

## ML Models

### UHI Detection
- **Algorithm**: Support Vector Machine (SVM), Random Forest
- **Features**: LST, NDVI, Soil Moisture, Geolocation
- **Output**: Heat anomaly classification, Hotspot clustering

### Vegetation Stress
- **Algorithm**: Gradient Boosting, K-Means Clustering
- **Features**: Multi-satellite NDVI fusion
- **Output**: Health classification (Healthy/Stressed/Critical)

### Pollution Hotspot
- **Algorithm**: DBSCAN Clustering, Random Forest
- **Features**: Derived environmental indices
- **Output**: Risk zones, Compliance reports

### Trend Forecast
- **Algorithm**: ARIMA, LightGBM
- **Features**: Historical LST, NDVI time-series
- **Output**: 30-day LST predictions, Breach alerts

---

## Output Files

| File | Description |
|------|-------------|
| `uhi_heatmap.json` | LST heatmap data for visualization |
| `uhi_hotspots.json` | Clustered UHI hotspot zones |
| `vegetation_map.json` | NDVI health classification map |
| `vegetation_alerts.json` | Critical vegetation alerts |
| `pollution_risk_map.json` | Pollution risk zone mapping |
| `pollution_compliance.json` | Environmental compliance report |
| `forecast_trend.json` | LST trend predictions |
| `action_plan.json` | Prioritized action recommendations |

---

## Team

**Team Name**: Sanskari Coders

Developed for the AETRIX Hackathon by PDEU College

---

## License

This project is developed for hackathon purposes. All rights reserved.

---

## Acknowledgments

- **ISRO** - For providing satellite data access and guidance
- **Pandit Deendayal Energy University** - For organizing the hackathon
- **Google Earth Engine** - For satellite data processing
- **OpenStreetMap** - For base map data
