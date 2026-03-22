import React, { useState, useEffect } from 'react';
import {
  AreaChart,
  Area,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import Header from '../components/Header';
import KpiCard from '../components/KpiCard';
import AlertStrip from '../components/AlertStrip';
import AiSummaryCard from '../components/AiSummaryCard';
import DataSourcesStrip from '../components/DataSourcesStrip';
import LoadingSpinner from '../components/LoadingSpinner';
import { useCity } from '../context/CityContext';
import dashboardService from '../services/dashboardService';
import uhiService from '../services/uhiService';
import vegetationService from '../services/vegetationService';
import pollutionService from '../services/pollutionService';
import forecastService from '../services/forecastService';
import actionService from '../services/actionService';
import { formatNumber, formatTemperature, formatPercent } from '../utils/formatters';

const DashboardPage = () => {
  const { selectedCity, cityInfo } = useCity();
  const [overview, setOverview] = useState(null);
  const [pollutionHotspots, setPollutionHotspots] = useState([]);
  const [forecastTrend, setForecastTrend] = useState(null);
  const [highPriorityActions, setHighPriorityActions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const [overviewRes, pollutionRes, forecastRes, actionsRes] = await Promise.all([
          dashboardService.getOverview(selectedCity),
          pollutionService.getHotspots(selectedCity),
          forecastService.getTrend(selectedCity),
          actionService.getHighPriority(),
        ]);

        if (overviewRes?.success) setOverview(overviewRes.data);
        if (pollutionRes?.success) setPollutionHotspots(pollutionRes.data || []);
        if (forecastRes?.success) setForecastTrend(forecastRes.data);
        if (actionsRes?.success) setHighPriorityActions(actionsRes.data || []);
      } catch (err) {
        console.error('Error fetching dashboard data:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [selectedCity]);

  if (loading) {
    return (
      <div className="page-container">
        <Header title="Environmental Intelligence Dashboard" city={selectedCity} state={cityInfo.state} />
        <LoadingSpinner />
      </div>
    );
  }

  const chartData = forecastTrend?.historical?.slice(-20).map((item, index) => ({
    step: item.step,
    lst: item.valueCelsius,
  })) || [];

  const CustomTooltip = ({ active, payload, label }) => {
    if (active && payload && payload.length) {
      return (
        <div className="custom-tooltip">
          <p className="custom-tooltip-label">Step {label}</p>
          {payload.map((entry, index) => (
            <p key={index} className="custom-tooltip-item" style={{ color: entry.color }}>
              {entry.name}: {formatNumber(entry.value)}
            </p>
          ))}
        </div>
      );
    }
    return null;
  };

  return (
    <div className="page-container">
      <Header
        title="Environmental Intelligence Dashboard"
        subtitle="AETRIX — All-India Satellite Monitoring Platform"
        city={overview?.city}
        state={overview?.state}
        trend={overview?.overallTrend}
      />

      {overview?.pollutionCriticalCount > 0 && (
        <AlertStrip
          type="critical"
          icon="🔴"
          message={`${overview.pollutionCriticalCount} critical pollution zones in ${overview.city} require immediate attention`}
          city={overview.city}
        />
      )}

      <div className="section">
        <div className="grid-6">
          <KpiCard
            label="City Mean LST"
            value={formatTemperature(overview?.cityMeanLst)}
            icon="🌡️"
            accent
          />
          <KpiCard
            label="Mean NDVI"
            value={formatNumber(overview?.cityMeanNdvi, 3)}
            icon="🌿"
          />
          <KpiCard
            label="Pollution Risk"
            value={formatNumber(overview?.cityMeanRisk, 1)}
            unit="/100"
            icon="💨"
          />
          <KpiCard
            label="Critical Zones"
            value={overview?.pollutionCriticalCount || 0}
            icon="⚠️"
          />
          <KpiCard
            label="Pending Actions"
            value={overview?.pendingActions || 0}
            icon="📋"
          />
          <KpiCard
            label="Data Points"
            value={overview?.totalDataPoints || 0}
            icon="📊"
          />
        </div>
      </div>

      <div className="section">
        <div className="grid-2">
          <div className="card">
            <div className="card-header">
              <h3 className="card-title">Temperature Trend</h3>
            </div>
            <ResponsiveContainer width="100%" height={250}>
              <AreaChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                <XAxis dataKey="step" stroke="var(--text-muted)" />
                <YAxis stroke="var(--text-muted)" />
                <Tooltip content={<CustomTooltip />} />
                <Area
                  type="monotone"
                  dataKey="lst"
                  stroke="#FF8C00"
                  fill="rgba(255, 140, 0, 0.2)"
                  name="LST (°C)"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>

          <div className="card">
            <div className="card-header">
              <h3 className="card-title">Pollution Hotspots</h3>
            </div>
            <ResponsiveContainer width="100%" height={250}>
              <BarChart data={pollutionHotspots.slice(0, 7)}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                <XAxis
                  dataKey="zoneName"
                  stroke="var(--text-muted)"
                  tick={{ fontSize: 10 }}
                  angle={-45}
                  textAnchor="end"
                  height={80}
                />
                <YAxis stroke="var(--text-muted)" />
                <Tooltip content={<CustomTooltip />} />
                <Bar dataKey="avgRiskScore" fill="#FF4444" name="Risk Score" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      <div className="section">
        <div className="section-header">
          <h2 className="section-title">Urgent Actions</h2>
        </div>
        <div className="card">
          <table className="table">
            <thead>
              <tr>
                <th>Action</th>
                <th>Zone</th>
                <th>Department</th>
                <th>Priority</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {highPriorityActions.slice(0, 5).map((action) => (
                <tr key={action.id}>
                  <td style={{ fontWeight: 500 }}>{action.title}</td>
                  <td>{action.zoneName || 'City-wide'}</td>
                  <td>{action.responsibleDept}</td>
                  <td>
                    <span className={`badge badge-${action.priority?.toLowerCase()}`}>
                      {action.priority}
                    </span>
                  </td>
                  <td>
                    <span className={`badge badge-${action.status}`}>
                      {action.status?.replace('_', ' ')}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div className="section">
        <div className="section-header">
          <h2 className="section-title">AI Intelligence Summaries</h2>
        </div>
        <div className="grid-2">
          <AiSummaryCard
            title="UHI Analysis"
            featureKey="uhi"
            fetchSummary={uhiService.getAiSummary}
            regenerateSummary={uhiService.regenerateAiSummary}
          />
          <AiSummaryCard
            title="Vegetation Health"
            featureKey="vegetation"
            fetchSummary={vegetationService.getAiSummary}
            regenerateSummary={vegetationService.regenerateAiSummary}
          />
          <AiSummaryCard
            title="Pollution Risk"
            featureKey="pollution"
            fetchSummary={pollutionService.getAiSummary}
            regenerateSummary={pollutionService.regenerateAiSummary}
          />
          <AiSummaryCard
            title="Temperature Forecast"
            featureKey="forecast"
            fetchSummary={forecastService.getAiSummary}
            regenerateSummary={forecastService.regenerateAiSummary}
          />
        </div>
      </div>

      <DataSourcesStrip
        sources={overview?.dataSources || ['MODIS LST', 'Sentinel-2 NDVI', 'Landsat NDVI', 'SMAP Soil Moisture']}
        coverage="Pan-India Coverage"
        lastUpdated={overview?.lastUpdated}
      />
    </div>
  );
};

export default DashboardPage;
