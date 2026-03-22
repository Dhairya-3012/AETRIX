import React, { useState, useEffect } from 'react';
import {
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';
import Header from '../components/Header';
import KpiCard from '../components/KpiCard';
import AlertStrip from '../components/AlertStrip';
import AiSummaryCard from '../components/AiSummaryCard';
import LoadingSpinner from '../components/LoadingSpinner';
import { useCity } from '../context/CityContext';
import vegetationService from '../services/vegetationService';
import { formatNumber, formatPercent } from '../utils/formatters';
import { getHealthColor } from '../utils/colorUtils';

const VegetationPage = () => {
  const { selectedCity, cityInfo } = useCity();
  const [summary, setSummary] = useState(null);
  const [alerts, setAlerts] = useState([]);
  const [plantation, setPlantation] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const [summaryRes, alertsRes, plantationRes] = await Promise.all([
          vegetationService.getSummary(selectedCity),
          vegetationService.getAlerts(selectedCity),
          vegetationService.getPlantation(selectedCity),
        ]);

        if (summaryRes?.success) setSummary(summaryRes.data);
        if (alertsRes?.success) setAlerts(alertsRes.data || []);
        if (plantationRes?.success) setPlantation(plantationRes.data || []);
      } catch (err) {
        console.error('Error fetching vegetation data:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [selectedCity]);

  if (loading) {
    return (
      <div className="page-container">
        <Header title="Vegetation Health Analysis" city={selectedCity} state={cityInfo.state} />
        <LoadingSpinner />
      </div>
    );
  }

  const healthDistribution = [
    { name: 'Healthy', value: summary?.healthyCount || 0, color: '#00E676' },
    { name: 'Stressed', value: summary?.stressedCount || 0, color: '#FFD700' },
    { name: 'Barren', value: summary?.barrenCount || 0, color: '#FF4444' },
  ].filter(item => item.value > 0);

  const hasHealthData = healthDistribution.length > 0;
  const hasAlertData = alerts.length > 0;

  // Transform alerts for bar chart - use absolute z-score for visualization
  // Handle both 'zScore' and 'zscore' field names (backend may use either)
  const alertChartData = alerts.slice(0, 10).map(alert => ({
    ...alert,
    absZScore: Math.abs(alert.zScore ?? alert.zscore ?? 0),
    displayId: alert.pointId?.substring(0, 8) || alert.id?.toString().substring(0, 8) || 'N/A'
  }));

  return (
    <div className="page-container">
      <Header
        title="Vegetation Health Analysis"
        subtitle={`AETRIX — India Environmental Intelligence | ${selectedCity}`}
        city={selectedCity}
        state={cityInfo.state}
      />

      {alerts.length > 0 && (
        <AlertStrip
          type="warn"
          icon="🌿"
          message={`${alerts.length} critical vegetation stress alerts detected in ${summary?.city}`}
        />
      )}

      <div className="section">
        <div className="grid-4">
          <KpiCard
            label="Mean NDVI"
            value={formatNumber(summary?.cityMeanNdvi, 3)}
            icon="🌿"
            accent
          />
          <KpiCard
            label="Healthy Zones"
            value={summary?.healthyCount || 0}
            change={formatPercent(summary?.healthyPercentage)}
            changeType="positive"
            icon="✅"
          />
          <KpiCard
            label="Stressed Zones"
            value={summary?.stressedCount || 0}
            change={formatPercent(summary?.stressedPercentage)}
            changeType="negative"
            icon="⚠️"
          />
          <KpiCard
            label="Critical Alerts"
            value={alerts.length}
            icon="🚨"
          />
        </div>
      </div>

      <div className="section">
        <div className="grid-2">
          <div className="card">
            <div className="card-header">
              <h3 className="card-title">Health Distribution</h3>
            </div>
            {hasHealthData ? (
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={healthDistribution}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={100}
                    paddingAngle={5}
                    dataKey="value"
                    label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                  >
                    {healthDistribution.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{
                      background: 'var(--bg-card)',
                      border: '1px solid var(--border)',
                      borderRadius: '8px',
                    }}
                  />
                  <Legend />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div style={{ padding: '60px 20px', textAlign: 'center', minHeight: '300px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                <div style={{ fontSize: '48px', marginBottom: '16px' }}>🌿</div>
                <div style={{ fontSize: '14px', color: 'var(--text-muted)' }}>
                  No vegetation health data available
                </div>
              </div>
            )}
          </div>

          <div className="card">
            <div className="card-header">
              <h3 className="card-title">Stress Alerts by Z-Score</h3>
              <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                Deviation below city mean (higher = more stressed)
              </span>
            </div>
            {hasAlertData ? (
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={alertChartData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                  <XAxis dataKey="displayId" stroke="var(--text-muted)" tick={{ fontSize: 10 }} />
                  <YAxis stroke="var(--text-muted)" domain={[0, 'auto']} />
                  <Tooltip
                    contentStyle={{
                      background: 'var(--bg-card)',
                      border: '1px solid var(--border)',
                      borderRadius: '8px',
                    }}
                    formatter={(value, name) => [`${value.toFixed(2)} std`, 'Deviation']}
                    labelFormatter={(label) => `Point: ${label}`}
                  />
                  <Bar dataKey="absZScore" fill="#FFD700" name="Z-Score Magnitude" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div style={{ padding: '60px 20px', textAlign: 'center', minHeight: '300px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                <div style={{ fontSize: '48px', marginBottom: '16px' }}>📊</div>
                <div style={{ fontSize: '14px', color: 'var(--text-muted)' }}>
                  No stress alerts to display
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="section">
        <div className="section-header">
          <h2 className="section-title">Vegetation Stress Alerts</h2>
        </div>
        <div className="card">
          <table className="table">
            <thead>
              <tr>
                <th>Point ID</th>
                <th>Location</th>
                <th>NDVI</th>
                <th>Z-Score</th>
                <th>Severity</th>
                <th>Message</th>
              </tr>
            </thead>
            <tbody>
              {alerts.map((alert) => (
                <tr key={alert.id}>
                  <td style={{ fontWeight: 500 }}>{alert.pointId}</td>
                  <td>{`${alert.lat?.toFixed(4)}, ${alert.lng?.toFixed(4)}`}</td>
                  <td>{formatNumber(alert.ndviSentinel, 3)}</td>
                  <td>{formatNumber(alert.zScore ?? alert.zscore, 2)}</td>
                  <td>
                    <span className={`badge badge-${alert.severity?.toLowerCase()}`}>
                      {alert.severity}
                    </span>
                  </td>
                  <td style={{ maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {alert.message}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="section">
        <div className="section-header">
          <h2 className="section-title">Plantation Recommendations</h2>
        </div>
        <div className="card">
          <table className="table">
            <thead>
              <tr>
                <th>Point ID</th>
                <th>Location</th>
                <th>Current NDVI</th>
                <th>Health Status</th>
              </tr>
            </thead>
            <tbody>
              {plantation.slice(0, 10).map((point) => (
                <tr key={point.pointId}>
                  <td>{point.pointId}</td>
                  <td>{`${point.lat?.toFixed(4)}, ${point.lng?.toFixed(4)}`}</td>
                  <td>{formatNumber(point.ndviSentinel, 3)}</td>
                  <td>
                    <span
                      style={{
                        color: getHealthColor(point.healthLabel),
                        fontWeight: 500,
                      }}
                    >
                      {point.healthLabel}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="section">
        <AiSummaryCard
          title="AI Vegetation Analysis"
          featureKey="vegetation"
          fetchSummary={() => vegetationService.getAiSummary(selectedCity)}
          regenerateSummary={() => vegetationService.regenerateAiSummary(selectedCity)}
        />
      </div>
    </div>
  );
};

export default VegetationPage;
