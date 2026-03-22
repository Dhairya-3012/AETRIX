# AETRIX

## Satellite Environmental Intelligence Platform for Smart Cities

**All-India Environmental Telemetry & Risk Intelligence X**

Developed for the **AETRIX Hackathon** by **Science and Technical Committee** **PDEU**

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
- Multi-city support for 4 major Indian metropolitan areas

### Supported Cities

| City | State | Coordinates |
|------|-------|-------------|
| **Ahmedabad** | Gujarat | 23.022°N, 72.571°E |
| **Bangalore** | Karnataka | 12.972°N, 77.594°E |
| **Delhi** | Delhi | 28.644°N, 77.216°E |
| **Mumbai** | Maharashtra | 19.076°N, 72.877°E |

---

## Tech Stack

| Layer | Technology | Version |
|-------|------------|---------|
| **Frontend** | React.js | 18.2 |
| **Mapping** | MapTiler SDK | 2.0.3 |
| **Charts** | Recharts | 2.12 |
| **Backend** | Spring Boot | 3.5.12 |
| **Database** | PostgreSQL | 14+ |
| **Cache** | Caffeine | 3.1.8 |
| **ML Framework** | scikit-learn | 1.0+ |
| **ML API** | FastAPI | 0.68+ |
| **AI/LLM** | Groq (Llama 3.3 70B) | - |
| **Languages** | Java 17, Python 3.10+, JavaScript | - |

### Data Sources

| Satellite | Product | Parameter | Resolution |
|-----------|---------|-----------|------------|
| **MODIS** | MOD11A1 | Land Surface Temperature | 1 km |
| **Landsat 8/9** | Collection 2 | NDVI | 30 m |
| **Sentinel-2** | Level-2A | NDVI | 10 m |
| **SMAP** | L4 | Soil Moisture | 9 km |

---

## Setup & Run Instructions

### Prerequisites

- Node.js 18+ and npm
- Java 17+
- Python 3.10+
- PostgreSQL 14+
- Maven 3.8+

### Quick Start

#### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/aetrix.git
cd aetrix
```

#### 2. Database Setup

```bash
psql -U postgres -c "CREATE DATABASE aetrix_db;"
psql -U postgres -c "CREATE USER aetrix_user WITH PASSWORD 'aetrix_pass';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE aetrix_db TO aetrix_user;"
```

#### 3. Backend Setup

```bash
cd backend

# Configure environment
cp .env.example .env
# Edit .env with your database credentials and GROQ API key

# Build and run
mvn clean install -DskipTests
mvn spring-boot:run
```

Backend runs at: `http://localhost:8080`

#### 4. Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Configure environment
cp .env.example .env
# Add your MapTiler API key to .env

# Start development server
npm start
```

Frontend runs at: `http://localhost:3000`

#### 5. ML Module Setup

```bash
cd ML

# Create virtual environment
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# Install dependencies
pip install -r ml_models/requirements.txt

# Run full analysis for all cities
python main.py

# Or start API server
python main.py --api
```

ML API runs at: `http://localhost:8000`

### Required API Keys

| Service | Variable | Get it from |
|---------|----------|-------------|
| **MapTiler** | `REACT_APP_MAPTILER_KEY` | [cloud.maptiler.com](https://cloud.maptiler.com/account/keys/) |
| **Groq LLM** | `GROQ_API_KEY` | [console.groq.com](https://console.groq.com/keys) |

---

## Features

### 1. Urban Heat Island (UHI) Detection
- ML-based heat anomaly classification
- Heatmap visualization with temperature gradients
- Hotspot zone identification and clustering

### 2. Vegetation Health Monitoring
- Multi-satellite NDVI fusion (Landsat + Sentinel-2)
- Vegetation stress classification (Healthy/Stressed/Critical)
- Priority plantation zone recommendations

### 3. Pollution Hotspot Analysis
- Risk zone mapping using clustering algorithms
- Environmental compliance reporting
- Industrial vs. residential pollution differentiation

### 4. Trend Forecasting
- Time-series prediction for LST trends
- Threshold breach predictions
- 30-day temperature forecasts

### 5. AI-Powered Action Plans
- Automated prioritization of interventions
- Zone-specific recommendations
- LLM-generated intelligent summaries

---

## Architecture

```
+------------------+     +------------------+     +------------------+
|    Frontend      |     |     Backend      |     |    ML Module     |
|   (React.js)     |<--->| (Spring Boot)    |<--->|    (Python)      |
|                  |     |                  |     |                  |
| - MapTiler Maps  |     | - REST APIs      |     | - UHI Detection  |
| - City Selector  |     | - PostgreSQL     |     | - NDVI Analysis  |
| - Interactive UI |     | - Caffeine Cache |     | - Pollution ML   |
| - Recharts       |     | - Groq LLM       |     | - Forecasting    |
+------------------+     +------------------+     +------------------+
```

---

## Project Structure

```
Aetrix/
├── frontend/                   # React.js Frontend
│   ├── src/
│   │   ├── components/         # UI Components
│   │   ├── pages/              # Page Components
│   │   ├── services/           # API Services
│   │   └── context/            # React Context
│   └── package.json
│
├── backend/                    # Spring Boot Backend
│   ├── src/main/java/com/aetrix/
│   │   ├── controller/         # REST Controllers
│   │   ├── service/            # Business Logic
│   │   ├── repository/         # Data Access
│   │   ├── entity/             # JPA Entities
│   │   └── dto/                # Data Transfer Objects
│   └── pom.xml
│
├── ML/                         # Machine Learning Module
│   ├── main.py                 # Entry Point
│   ├── ml_models/              # ML Model Implementations
│   └── output/                 # Generated JSON Outputs
│
└── README.md
```

---

## Team

### Team Name: Sanskari Coders

| Name | Role |
|------|------|
| **Preet Makadiya** | Full Stack Developer |
| **Himadri Patel** | ML Engineer |
| **Ved Shrimali** | Backend Developer |
| **Dhairyasinh Parmar** | Frontend Developer |

**Institution**: Pandit Deendayal Energy University (PDEU)

---

## Acknowledgments

- **ISRO** - For satellite data access and guidance
- **PDEU** - For organizing the Satimage Hackathon
- **Google Earth Engine** - For satellite data processing
- **OpenStreetMap** - For base map data

---

## License

This project is developed for hackathon purposes. All rights reserved.
