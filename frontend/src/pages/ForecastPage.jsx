import React, { useState, useEffect } from 'react';
import {
  AreaChart,
  Area,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  ReferenceLine,
  Legend,
} from 'recharts';
import Header from '../components/Header';
import KpiCard from '../components/KpiCard';
import AlertStrip from '../components/AlertStrip';
import AiSummaryCard from '../components/AiSummaryCard';
import LoadingSpinner from '../components/LoadingSpinner';
import forecastService from '../services/forecastService';
import { formatTemperature, formatPercent, formatNumber } from '../utils/formatters';

const ForecastPage = () => {
  const [trend, setTrend] = useState(null);
  const [breach, setBreach] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const [trendRes, breachRes] = await Promise.all([
          forecastService.getTrend(),
          forecastService.getBreach(),
        ]);

        if (trendRes?.success) setTrend(trendRes.data);
        if (breachRes?.success) setBreach(breachRes.data);
      } catch (err) {
        console.error('Error fetching forecast data:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  if (loading) {
    return (
      <div className="page-container">
        <Header title="Temperature Forecast" />
        <LoadingSpinner />
      </div>
    );
  }

  // Prepare combined chart data
  const historicalData = trend?.historical?.filter(item => item.valueCelsius != null).map(item => ({
    step: item.step,
    historical: item.valueCelsius,
    type: 'historical',
  })) || [];

  const predictedData = trend?.predicted?.filter(item => item.predictedCelsius != null).map(item => ({
    step: item.step + (historicalData.length > 0 ? historicalData[historicalData.length - 1].step : 0),
    predicted: item.predictedCelsius,
    lower: item.lowerBound,
    upper: item.upperBound,
    type: 'predicted',
  })) || [];

  const combinedData = [...historicalData.slice(-50), ...predictedData];
  const hasData = combinedData.length > 0;

  const getTrendIcon = (direction) => {
    switch (direction) {
      case 'increasing': return '📈';
      case 'decreasing': return '📉';
      default: return '➡️';
    }
  };

  const getTrendColor = (direction) => {
    switch (direction) {
      case 'increasing': return '#FF4444';
      case 'decreasing': return '#00E676';
      default: return '#4A9EFF';
    }
  };

  return (
    <div className="page-container">
      <Header
        title="Temperature Forecast"
        subtitle={`AETRIX — India Environmental Intelligence | ${trend?.city || 'Ahmedabad'}`}
        city={trend?.city}
      />

      {breach?.breachCount > 0 && breach?.riskLevel === 'critical' && (
        <AlertStrip
          type="critical"
          icon="🌡️"
          message={`${breach.breachCount} locations (${formatPercent(breach.breachPercentage)}) exceed ${breach.dangerThreshold}°C danger threshold`}
        />
      )}

      <div className="section">
        <div className="grid-4">
          <KpiCard
            label="Predicted Mean"
            value={formatTemperature(trend?.predictedMean)}
            icon="🌡️"
            accent
          />
          <KpiCard
            label="Trend Direction"
            value={trend?.trendDirection?.toUpperCase() || 'N/A'}
            icon={getTrendIcon(trend?.trendDirection)}
            change={`Rate: ${formatNumber(trend?.trendRate, 3)}`}
          />
          <KpiCard
            label="Breach Count"
            value={breach?.breachCount || 0}
            change={formatPercent(breach?.breachPercentage)}
            changeType={breach?.breachPercentage > 20 ? 'negative' : 'positive'}
            icon="⚠️"
          />
          <KpiCard
            label="Risk Level"
            value={breach?.riskLevel?.toUpperCase() || 'N/A'}
            icon="📊"
          />
        </div>
      </div>

      <div className="section">
        <div className="card">
          <div className="card-header">
            <h3 className="card-title">Historical & Predicted Temperature (ARIMA Model)</h3>
            <span className="badge badge-pending">{trend?.modelType || 'ARIMA(2,1,2)'}</span>
          </div>
          {hasData ? (
            <ResponsiveContainer width="100%" height={400}>
              <AreaChart data={combinedData}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                <XAxis dataKey="step" stroke="var(--text-muted)" />
                <YAxis stroke="var(--text-muted)" domain={['auto', 'auto']} />
                <Tooltip
                  contentStyle={{
                    background: 'var(--bg-card)',
                    border: '1px solid var(--border)',
                    borderRadius: '8px',
                  }}
                />
                <Legend />
                <ReferenceLine
                  y={breach?.dangerThreshold || 35}
                  stroke="#FF4444"
                  strokeDasharray="5 5"
                  label={{ value: `Danger: ${breach?.dangerThreshold}°C`, fill: '#FF4444', fontSize: 11 }}
                />
                <Area
                  type="monotone"
                  dataKey="historical"
                  stroke="#4A9EFF"
                  fill="rgba(74, 158, 255, 0.2)"
                  name="Historical (°C)"
                  connectNulls
                />
                <Area
                  type="monotone"
                  dataKey="upper"
                  stroke="transparent"
                  fill="rgba(255, 140, 0, 0.1)"
                  name="Upper Bound"
                  connectNulls
                />
                <Area
                  type="monotone"
                  dataKey="lower"
                  stroke="transparent"
                  fill="rgba(255, 140, 0, 0.1)"
                  name="Lower Bound"
                  connectNulls
                />
                <Line
                  type="monotone"
                  dataKey="predicted"
                  stroke="#FF8C00"
                  strokeWidth={2}
                  dot={false}
                  name="Predicted (°C)"
                  connectNulls
                />
              </AreaChart>
            </ResponsiveContainer>
          ) : (
            <div style={{ padding: '60px 20px', textAlign: 'center' }}>
              <div style={{ fontSize: '48px', marginBottom: '16px' }}>📊</div>
              <div style={{ fontSize: '16px', color: 'var(--text-muted)' }}>
                No forecast data available. Please ensure the database is populated with forecast data.
              </div>
            </div>
          )}
        </div>
      </div>

      <div className="section">
        <div className="grid-2">
          <div className="card">
            <div className="card-header">
              <h3 className="card-title">Forecast Summary</h3>
            </div>
            <div style={{ padding: '20px' }}>
              <div style={{ marginBottom: '16px' }}>
                <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginBottom: '4px' }}>
                  PREDICTION RANGE
                </div>
                <div style={{ fontSize: '24px', fontWeight: '700', color: 'var(--text-primary)' }}>
                  {formatTemperature(trend?.predictedMin)} — {formatTemperature(trend?.predictedMax)}
                </div>
              </div>
              <div style={{ marginBottom: '16px' }}>
                <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginBottom: '4px' }}>
                  TREND
                </div>
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  color: getTrendColor(trend?.trendDirection),
                }}>
                  <span style={{ fontSize: '24px' }}>{getTrendIcon(trend?.trendDirection)}</span>
                  <span style={{ fontSize: '18px', fontWeight: '600' }}>
                    {trend?.trendDirection?.charAt(0).toUpperCase() + trend?.trendDirection?.slice(1)}
                  </span>
                </div>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginBottom: '4px' }}>
                  MODEL
                </div>
                <div style={{ fontSize: '14px', color: 'var(--text-secondary)' }}>
                  {trend?.modelType || 'ARIMA(2,1,2)'}
                </div>
              </div>
            </div>
          </div>

          <div className="card">
            <div className="card-header">
              <h3 className="card-title">What-If Scenarios</h3>
            </div>
            <div style={{ padding: '20px' }}>
              <div style={{
                background: 'rgba(255, 68, 68, 0.1)',
                border: '1px solid rgba(255, 68, 68, 0.3)',
                borderRadius: '8px',
                padding: '16px',
                marginBottom: '12px',
              }}>
                <div style={{ fontSize: '12px', color: '#FF4444', marginBottom: '4px', fontWeight: '600' }}>
                  IF TEMPERATURES RISE +2°C
                </div>
                <div style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
                  {breach?.whatIfWarmer || 'Analysis not available'}
                </div>
              </div>
              <div style={{
                background: 'rgba(0, 230, 118, 0.1)',
                border: '1px solid rgba(0, 230, 118, 0.3)',
                borderRadius: '8px',
                padding: '16px',
              }}>
                <div style={{ fontSize: '12px', color: '#00E676', marginBottom: '4px', fontWeight: '600' }}>
                  IF TEMPERATURES DROP -2°C
                </div>
                <div style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
                  {breach?.whatIfCooler || 'Analysis not available'}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="section">
        <AiSummaryCard
          title="AI Forecast Analysis"
          featureKey="forecast"
          fetchSummary={forecastService.getAiSummary}
          regenerateSummary={forecastService.regenerateAiSummary}
        />
      </div>
    </div>
  );
};

export default ForecastPage;
