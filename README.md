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
- **Multi-city support** for analyzing multiple Indian metropolitan areas

---

## Supported Cities

AETRIX currently supports environmental monitoring for **4 major Indian cities**:

| City | State | Coordinates |
|------|-------|-------------|
| **Ahmedabad** | Gujarat | 23.022°N, 72.571°E |
| **Bangalore** | Karnataka | 12.972°N, 77.594°E |
| **Delhi** | Delhi | 28.644°N, 77.216°E |
| **Mumbai** | Maharashtra | 19.076°N, 72.877°E |

Users can switch between cities using the city selector in the application header.

---

## Solution Architecture

```
+------------------+     +------------------+     +------------------+
|    Frontend      |     |     Backend      |     |    ML Module     |
|   (React.js)     |<--->| (Spring Boot)    |<--->|    (Python)      |
|                  |     |                  |     |                  |
| - MapTiler Maps  |     | - REST APIs      |     | - UHI Detection  |
| - City Selector  |     | - PostgreSQL     |     | - NDVI Analysis  |
| - Interactive UI |     | - Multi-city DB  |     | - Pollution ML   |
| - Recharts       |     | - Caffeine Cache |     | - Multi-city     |
| - Dashboard      |     | - xAI Integration|     | - Forecasting    |
+------------------+     +------------------+     +------------------+
```

---

## Features

### 1. Multi-City Environmental Monitoring
- City selector for switching between Ahmedabad, Bangalore, Delhi, and Mumbai
- City-specific data isolation in database
- Per-city ML analysis and predictions
- Comparative insights across cities

### 2. Urban Heat Island (UHI) Detection
- Machine Learning models for heat anomaly classification
- Heatmap visualization with temperature gradients
- Hotspot zone identification and clustering
- Top-N hottest location analysis per city

### 3. Vegetation Health Monitoring
- Multi-satellite NDVI fusion (Landsat + Sentinel-2)
- Vegetation stress classification (Healthy/Stressed/Critical)
- Alert system for vegetation degradation
- Priority plantation zone recommendations

### 4. Pollution Hotspot Analysis
- Risk zone mapping using clustering algorithms
- Compliance reporting against environmental standards
- Industrial vs. residential pollution differentiation
- Real-time pollution level tracking

### 5. Trend Forecasting
- Time-series prediction for LST trends
- Feature importance analysis
- Threshold breach predictions
- Seasonal pattern recognition

### 6. AI-Powered Action Plans
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
├── .env.example                # Root environment template
├── .gitignore                  # Git ignore rules
├── README.md
│
├── frontend/                   # React.js Frontend
│   ├── .env.example            # Frontend environment template
│   ├── src/
│   │   ├── components/         # UI Components
│   │   │   ├── Header.jsx      # Includes city selector
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
│   │   │   └── CityContext.jsx # City selection state
│   │   └── utils/              # Utility Functions
│   └── package.json
│
├── backend/                    # Spring Boot Backend
│   ├── .env.example            # Backend environment template
│   ├── src/main/java/com/aetrix/
│   │   ├── controller/         # REST Controllers
│   │   │   ├── DashboardController.java
│   │   │   ├── UhiController.java
│   │   │   ├── VegetationController.java
│   │   │   ├── PollutionController.java
│   │   │   ├── ForecastController.java
│   │   │   └── ActionPlanController.java
│   │   ├── service/            # Business Logic
│   │   ├── repository/         # Data Access (city-filtered queries)
│   │   ├── entity/             # JPA Entities (with city column)
│   │   ├── dto/                # Data Transfer Objects
│   │   └── config/             # Configuration
│   ├── src/main/resources/
│   │   ├── application.yml     # Spring Boot config
│   │   └── data/               # City-specific JSON data
│   │       ├── cities.json     # Available cities list
│   │       ├── ahmedabad/      # Ahmedabad data files
│   │       ├── bangalore/      # Bangalore data files
│   │       ├── delhi/          # Delhi data files
│   │       └── mumbai/         # Mumbai data files
│   └── pom.xml
│
├── ML/                         # Machine Learning Module
│   ├── .env.example            # ML environment template
│   ├── main.py                 # Entry Point (multi-city support)
│   ├── ml_models/
│   │   ├── uhi_detection.py    # UHI ML Model
│   │   ├── vegetation_stress.py # Vegetation Analysis
│   │   ├── pollution_hotspot.py # Pollution Detection
│   │   ├── trend_forecast.py   # Time-series Forecasting
│   │   ├── action_plan.py      # Action Plan Generator
│   │   ├── accuracy_metrics.py # Model Evaluation
│   │   └── api_endpoints.py    # FastAPI Endpoints
│   ├── output/                 # Generated JSON Outputs
│   │   ├── cities.json         # Cities metadata
│   │   ├── ahmedabad/          # Ahmedabad outputs
│   │   ├── bangalore/          # Bangalore outputs
│   │   ├── delhi/              # Delhi outputs
│   │   └── mumbai/             # Mumbai outputs
│   ├── Ahmedabad_TimeSeries_Final.csv
│   ├── Bangalore_TimeSeries_Final.csv
│   ├── Delhi_TimeSeries_Final.csv
│   └── Mumbai_TimeSeries_Final.csv
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

### 2. Environment Configuration

Copy the example environment files and configure them with your values:

```bash
# Root level (for combined deployment)
cp .env.example .env

# Individual services
cp backend/.env.example backend/.env
cp frontend/.env.example frontend/.env
cp ML/.env.example ML/.env
```

#### Required API Keys

| Service | Variable | Get it from |
|---------|----------|-------------|
| **MapTiler** | `REACT_APP_MAPTILER_KEY` | [cloud.maptiler.com](https://cloud.maptiler.com/account/keys/) |
| **Groq LLM** | `GROQ_API_KEY` | [console.groq.com](https://console.groq.com/keys) |

> **Note:** The Groq API is used with the `llama-3.3-70b-versatile` model for generating AI-powered environmental summaries and action plan recommendations.

### 3. Database Setup
```bash
# Create PostgreSQL database
psql -U postgres -c "CREATE DATABASE aetrix_db;"
psql -U postgres -c "CREATE USER aetrix_user WITH PASSWORD 'your_password';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE aetrix_db TO aetrix_user;"
```

### 4. Frontend Setup
```bash
cd frontend

# Install dependencies
npm install

# Configure environment (edit .env with your MapTiler API key)
cp .env.example .env

# Start development server
npm start
```

### 5. Backend Setup
```bash
cd backend

# Configure environment
cp .env.example .env
# Edit .env with your database credentials and GROQ API key

# Build and run
mvn clean install
mvn spring-boot:run
```

### 6. ML Module Setup
```bash
cd ML

# Create virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r ml_models/requirements.txt
pip install python-dotenv  # For environment variable support

# Configure environment (optional)
cp .env.example .env

# Run full analysis for all cities
python main.py

# Or start API server
python main.py --api
```

---

## Environment Variables

### Backend (`backend/.env`)

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Server port | `8080` |
| `POSTGRES_HOST` | Database host | `localhost` |
| `POSTGRES_PORT` | Database port | `5432` |
| `POSTGRES_DB` | Database name | `aetrix_db` |
| `POSTGRES_USER` | Database user | `aetrix_user` |
| `POSTGRES_PASSWORD` | Database password | - |
| `DB_POOL_SIZE` | Connection pool size | `10` |
| `DB_POOL_MIN_IDLE` | Minimum idle connections | `2` |
| `DB_CONNECTION_TIMEOUT` | Connection timeout (ms) | `30000` |
| `JPA_DDL_AUTO` | Hibernate DDL mode | `update` |
| `JPA_SHOW_SQL` | Show SQL queries | `false` |
| `GROQ_API_KEY` | Groq LLM API key | - |
| `GROQ_API_URL` | Groq API endpoint | `https://api.groq.com/openai/v1/chat/completions` |
| `GROQ_MODEL` | LLM model to use | `llama-3.3-70b-versatile` |
| `GROQ_MAX_TOKENS` | Max response tokens | `500` |
| `GROQ_TEMPERATURE` | Model temperature | `0.3` |
| `GROQ_TIMEOUT_SECONDS` | API timeout | `30` |
| `LOG_LEVEL_APP` | Application log level | `INFO` |
| `LOG_LEVEL_HIBERNATE` | Hibernate log level | `WARN` |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins | `http://localhost:3000,http://localhost:5173` |
| `CACHE_MAX_SIZE` | Max cache entries | `200` |
| `CACHE_EXPIRE_MINUTES` | Cache TTL (minutes) | `60` |

### Frontend (`frontend/.env`)

| Variable | Description | Default |
|----------|-------------|---------|
| `REACT_APP_MAPTILER_KEY` | MapTiler API key | - |
| `REACT_APP_API_URL` | Backend API URL | `http://localhost:8080` |
| `REACT_APP_ML_API_URL` | ML Service URL | `http://localhost:8000` |
| `REACT_APP_DEFAULT_CITY` | Default city selection | `Ahmedabad` |
| `REACT_APP_REFRESH_INTERVAL` | Data refresh interval (ms) | `300000` |
| `REACT_APP_ENABLE_ANALYTICS` | Enable analytics | `false` |
| `REACT_APP_ENABLE_DEBUG` | Enable debug mode | `false` |

### ML Service (`ML/.env`)

| Variable | Description | Default |
|----------|-------------|---------|
| `ML_API_HOST` | API host | `0.0.0.0` |
| `ML_API_PORT` | API port | `8000` |
| `OUTPUT_DIR` | Output directory | `output` |
| `DEFAULT_CITY` | Default city for operations | `Ahmedabad` |
| `LOG_LEVEL` | Logging level | `INFO` |
| `WORKERS` | Uvicorn workers | `1` |
| `ENABLE_UHI_MODEL` | Enable UHI model | `true` |
| `ENABLE_VEGETATION_MODEL` | Enable vegetation model | `true` |
| `ENABLE_POLLUTION_MODEL` | Enable pollution model | `true` |
| `ENABLE_FORECAST_MODEL` | Enable forecast model | `true` |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins | `http://localhost:3000,http://localhost:5173,http://localhost:8080` |

---

## Deployment

### Railway Deployment

1. **Create a new project** on [Railway](https://railway.app)

2. **Add PostgreSQL** service from the Railway dashboard

3. **Deploy Backend**:
   - Connect your GitHub repository
   - Set root directory to `backend`
   - Add environment variables:
     ```
     POSTGRES_HOST=<from Railway PostgreSQL>
     POSTGRES_PORT=5432
     POSTGRES_DB=railway
     POSTGRES_USER=postgres
     POSTGRES_PASSWORD=<from Railway>
     GROQ_API_KEY=<your key>
     CORS_ALLOWED_ORIGINS=https://your-frontend.up.railway.app
     ```

4. **Deploy Frontend**:
   - Add another service from the same repo
   - Set root directory to `frontend`
   - Add environment variables:
     ```
     REACT_APP_MAPTILER_KEY=<your key>
     REACT_APP_API_URL=https://your-backend.up.railway.app
     ```

5. **Deploy ML Service** (optional):
   - Add another service
   - Set root directory to `ML`
   - Set start command: `python main.py --api`

### Docker Deployment

```bash
# Build and run all services
docker-compose up -d

# Or build individually
docker build -t aetrix-backend ./backend
docker build -t aetrix-frontend ./frontend
docker build -t aetrix-ml ./ML
```

### Production Checklist

- [ ] Set strong `POSTGRES_PASSWORD`
- [ ] Configure `CORS_ALLOWED_ORIGINS` with production URLs
- [ ] Set `JPA_DDL_AUTO=validate` (not `update`)
- [ ] Set `LOG_LEVEL_APP=INFO` or `WARN`
- [ ] Set `JPA_SHOW_SQL=false`
- [ ] Configure appropriate `DB_POOL_SIZE` for expected load
- [ ] Enable HTTPS for all services
- [ ] Rotate API keys periodically
- [ ] Set `WORKERS` > 1 for ML service in production

---

## Usage

### Running Full Analysis (All Cities)
```bash
cd ML
python main.py
```

This will:
1. Load satellite data from CSV files for all 4 cities
2. Train ML models for each city
3. Generate city-specific heatmaps, hotspot zones, alerts
4. Create action plan recommendations per city
5. Save outputs to `output/<city>/` directories

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

All endpoints support the `city` query parameter for city-specific data.

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/cities` | GET | List available cities |
| `/api/dashboard?city=Ahmedabad` | GET | Dashboard KPIs for city |
| `/api/uhi/heatmap?city=Ahmedabad` | GET | UHI Heatmap Data |
| `/api/uhi/hotspots?city=Ahmedabad` | GET | UHI Hotspot Zones |
| `/api/vegetation/map?city=Ahmedabad` | GET | Vegetation Health Map |
| `/api/vegetation/alerts?city=Ahmedabad` | GET | Vegetation Alerts |
| `/api/pollution/risk-map?city=Ahmedabad` | GET | Pollution Risk Zones |
| `/api/pollution/hotspots?city=Ahmedabad` | GET | Pollution Hotspots |
| `/api/forecast/trends?city=Ahmedabad` | GET | LST Trend Predictions |
| `/api/action-plan?city=Ahmedabad` | GET | Action Recommendations |

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

Each city has its own output directory (`output/<city>/`):

| File | Description |
|------|-------------|
| `uhi_heatmap.json` | LST heatmap data for visualization |
| `uhi_hotspots.json` | Clustered UHI hotspot zones |
| `uhi_top_hottest.json` | Top N hottest locations |
| `vegetation_map.json` | NDVI health classification map |
| `vegetation_alerts.json` | Critical vegetation alerts |
| `vegetation_plantation.json` | Recommended plantation zones |
| `pollution_risk_map.json` | Pollution risk zone mapping |
| `pollution_hotspots.json` | Pollution hotspot clusters |
| `pollution_compliance.json` | Environmental compliance report |
| `forecast_trend.json` | LST trend predictions |
| `forecast_breach.json` | Threshold breach predictions |
| `forecast_importance.json` | Feature importance analysis |
| `action_plan.json` | Prioritized action recommendations |
| `action_summary.json` | Summary of all actions |

---

## Database Schema

All entities include a `city` column for multi-city data isolation:

- `uhi_heatmap_points` - UHI temperature data points
- `uhi_hotspots` - Clustered heat island zones
- `vegetation_points` - NDVI health data
- `vegetation_alerts` - Vegetation degradation alerts
- `pollution_points` - Pollution risk data
- `pollution_hotspots` - Pollution zone clusters
- `forecast_steps` - Time-series forecast data
- `action_items` - Recommended actions
- `llm_summaries` - AI-generated summaries

---

## Team

**Team Name**: Sanskari Coders

Developed for the Satimage Hackathon by **Pandit Deendayal Energy University (PDEU)**

---

## License

This project is developed for hackathon purposes. All rights reserved.

---

## Acknowledgments

- **ISRO** - For providing satellite data access and guidance
- **Ahmedabad University** - For organizing the hackathon
- **Google Earth Engine** - For satellite data processing
- **OpenStreetMap** - For base map data
