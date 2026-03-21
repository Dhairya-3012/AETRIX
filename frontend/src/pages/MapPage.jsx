import React, { useRef, useEffect, useState } from 'react';
import * as maptilersdk from '@maptiler/sdk';
import '@maptiler/sdk/dist/maptiler-sdk.css';
import Header from '../components/Header';
import LoadingSpinner from '../components/LoadingSpinner';
import uhiService from '../services/uhiService';
import vegetationService from '../services/vegetationService';
import pollutionService from '../services/pollutionService';
import dashboardService from '../services/dashboardService';
import { getLstColor, getNdviColor, getRiskColor } from '../utils/colorUtils';

const MapPage = () => {
  const mapContainer = useRef(null);
  const map = useRef(null);
  const markersRef = useRef([]);
  const [activeLayer, setActiveLayer] = useState('uhi');
  const [loading, setLoading] = useState(true);
  const [overview, setOverview] = useState(null);
  const [mapData, setMapData] = useState({
    uhi: [],
    vegetation: [],
    pollution: [],
  });

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const [uhiRes, vegRes, pollRes, overviewRes] = await Promise.all([
          uhiService.getHeatmap(),
          vegetationService.getMap(),
          pollutionService.getMap(),
          dashboardService.getOverview(),
        ]);

        setMapData({
          uhi: uhiRes?.success ? uhiRes.data : [],
          vegetation: vegRes?.success ? vegRes.data : [],
          pollution: pollRes?.success ? pollRes.data : [],
        });

        if (overviewRes?.success) {
          setOverview(overviewRes.data);
        }
      } catch (err) {
        console.error('Error fetching map data:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  useEffect(() => {
    if (map.current) return;

    const apiKey = process.env.REACT_APP_MAPTILER_KEY;
    if (!apiKey) {
      console.error('MapTiler API key not found');
      return;
    }

    maptilersdk.config.apiKey = apiKey;

    map.current = new maptilersdk.Map({
      container: mapContainer.current,
      style: maptilersdk.MapStyle.STREETS.DARK,
      center: [72.571, 23.022],
      zoom: 11,
    });

    return () => {
      if (map.current) {
        map.current.remove();
        map.current = null;
      }
    };
  }, []);

  useEffect(() => {
    if (!map.current || loading) return;

    // Clear existing markers
    markersRef.current.forEach(marker => marker.remove());
    markersRef.current = [];

    const data = mapData[activeLayer] || [];
    const limitedData = data.slice(0, 200); // Limit for performance

    limitedData.forEach(point => {
      if (!point.lat || !point.lng) return;

      let color;
      let popupContent;

      switch (activeLayer) {
        case 'uhi':
          color = getLstColor(point.lstCelsius);
          popupContent = `
            <div style="background: #0D1929; padding: 12px; border-radius: 8px; color: #E8F4F8;">
              <strong>${point.pointId || 'Point'}</strong><br/>
              <span style="color: #7B9DB5;">LST:</span> ${point.lstCelsius?.toFixed(1)}°C<br/>
              <span style="color: #7B9DB5;">Anomaly:</span> ${point.isAnomaly ? 'Yes' : 'No'}<br/>
              <span style="color: #7B9DB5;">Severity:</span> ${point.severity || 'N/A'}
            </div>
          `;
          break;
        case 'vegetation':
          color = getNdviColor(point.ndviSentinel);
          popupContent = `
            <div style="background: #0D1929; padding: 12px; border-radius: 8px; color: #E8F4F8;">
              <strong>${point.pointId || 'Point'}</strong><br/>
              <span style="color: #7B9DB5;">NDVI:</span> ${point.ndviSentinel?.toFixed(3)}<br/>
              <span style="color: #7B9DB5;">Health:</span> ${point.healthLabel || 'N/A'}
            </div>
          `;
          break;
        case 'pollution':
          color = getRiskColor(point.riskScore);
          popupContent = `
            <div style="background: #0D1929; padding: 12px; border-radius: 8px; color: #E8F4F8;">
              <strong>${point.pointId || 'Point'}</strong><br/>
              <span style="color: #7B9DB5;">Risk Score:</span> ${point.riskScore?.toFixed(1)}/100<br/>
              <span style="color: #7B9DB5;">Category:</span> ${point.riskCategory || 'N/A'}
            </div>
          `;
          break;
        default:
          color = '#00D4AA';
          popupContent = `<div>Point</div>`;
      }

      const el = document.createElement('div');
      el.style.width = '12px';
      el.style.height = '12px';
      el.style.borderRadius = '50%';
      el.style.backgroundColor = color;
      el.style.border = '2px solid rgba(255,255,255,0.5)';
      el.style.cursor = 'pointer';

      const popup = new maptilersdk.Popup({ offset: 15, closeButton: false })
        .setHTML(popupContent);

      const marker = new maptilersdk.Marker({ element: el })
        .setLngLat([point.lng, point.lat])
        .setPopup(popup)
        .addTo(map.current);

      markersRef.current.push(marker);
    });
  }, [activeLayer, mapData, loading]);

  const layers = [
    { id: 'uhi', label: 'UHI Heatmap', icon: '🌡️' },
    { id: 'vegetation', label: 'Vegetation', icon: '🌿' },
    { id: 'pollution', label: 'Pollution Risk', icon: '💨' },
  ];

  const legendItems = {
    uhi: [
      { color: '#FF4444', label: '≥40°C (Critical)' },
      { color: '#FF8C00', label: '35-40°C (High)' },
      { color: '#FFD700', label: '30-35°C (Moderate)' },
      { color: '#00E676', label: '<30°C (Normal)' },
    ],
    vegetation: [
      { color: '#00E676', label: 'Healthy (≥0.5)' },
      { color: '#4ADE80', label: 'Moderate (0.3-0.5)' },
      { color: '#FFD700', label: 'Stressed (0.15-0.3)' },
      { color: '#FF4444', label: 'Barren (<0.15)' },
    ],
    pollution: [
      { color: '#FF4444', label: 'Critical (≥75)' },
      { color: '#FF8C00', label: 'High (50-75)' },
      { color: '#FFD700', label: 'Medium (25-50)' },
      { color: '#00E676', label: 'Low (<25)' },
    ],
  };

  const cityBadge = overview
    ? `${overview.city}, ${overview.state} — INDIA | AETRIX`
    : 'AHMEDABAD, GUJARAT — INDIA | AETRIX';

  return (
    <div className="page-container" style={{ padding: 0, paddingTop: '64px' }}>
      <Header
        title="Interactive Environmental Map"
        city={overview?.city}
        state={overview?.state}
      />

      <div className="map-container">
        <div ref={mapContainer} className="map-wrapper" />

        <div className="map-city-badge">
          {cityBadge}
        </div>

        <div className="map-controls">
          {layers.map(layer => (
            <button
              key={layer.id}
              className={`map-control-btn ${activeLayer === layer.id ? 'active' : ''}`}
              onClick={() => setActiveLayer(layer.id)}
            >
              {layer.icon} {layer.label}
            </button>
          ))}
        </div>

        <div className="map-legend">
          <div className="map-legend-title">
            {layers.find(l => l.id === activeLayer)?.label || 'Legend'}
          </div>
          {legendItems[activeLayer]?.map((item, index) => (
            <div key={index} className="map-legend-item">
              <div className="map-legend-color" style={{ backgroundColor: item.color }} />
              <span>{item.label}</span>
            </div>
          ))}
        </div>

        {loading && (
          <div style={{
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            background: 'var(--bg-card)',
            padding: '24px',
            borderRadius: '12px',
            zIndex: 20,
          }}>
            <LoadingSpinner />
            <p style={{ marginTop: '12px', color: 'var(--text-secondary)' }}>Loading map data...</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default MapPage;
