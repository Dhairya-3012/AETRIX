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
  ScatterChart,
  Scatter,
  ZAxis,
} from 'recharts';
import Header from '../components/Header';
import KpiCard from '../components/KpiCard';
import AlertStrip from '../components/AlertStrip';
import AiSummaryCard from '../components/AiSummaryCard';
import LoadingSpinner from '../components/LoadingSpinner';
import uhiService from '../services/uhiService';
import { formatTemperature, formatPercent, formatNumber } from '../utils/formatters';
import { getSeverityColor } from '../utils/colorUtils';

const UhiPage = () => {
  const [summary, setSummary] = useState(null);
  const [heatmap, setHeatmap] = useState([]);
  const [hotspots, setHotspots] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const [summaryRes, heatmapRes, hotspotsRes] = await Promise.all([
          uhiService.getSummary(),
          uhiService.getHeatmap(),
          uhiService.getHotspots(),
        ]);

        if (summaryRes?.success) setSummary(summaryRes.data);
        if (heatmapRes?.success) setHeatmap(heatmapRes.data || []);
        if (hotspotsRes?.success) setHotspots(hotspotsRes.data || []);
      } catch (err) {
        console.error('Error fetching UHI data:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  if (loading) {
    return (
      <div className="page-container">
        <Header title="Urban Heat Island Analysis" />
        <LoadingSpinner />
      </div>
    );
  }

  const lstDistribution = heatmap.reduce((acc, point) => {
    const range = Math.floor(point.lstCelsius / 5) * 5;
    const key = `${range}-${range + 5}`;
    acc[key] = (acc[key] || 0) + 1;
    return acc;
  }, {});

  const lstChartData = Object.entries(lstDistribution).map(([range, count]) => ({
    range,
    count,
  })).sort((a, b) => {
    const aStart = parseInt(a.range.split('-')[0]);
    const bStart = parseInt(b.range.split('-')[0]);
    return aStart - bStart;
  });

  const hasLstData = lstChartData.length > 0;
  const hasHotspotData = hotspots.length > 0;

  const scatterData = heatmap.slice(0, 100).map(point => ({
    x: point.lng,
    y: point.lat,
    z: point.lstCelsius,
    id: point.pointId,
  }));

  return (
    <div className="page-container">
      <Header
        title="Urban Heat Island Analysis"
        subtitle={`AETRIX — India Environmental Intelligence | ${summary?.city || 'Ahmedabad'}`}
        city={summary?.city}
      />

      {summary?.anomalyCount > 0 && (
        <AlertStrip
          type="critical"
          icon="🔥"
          message={`${summary.anomalyCount} locations in ${summary.city} are anomalously hot (${formatPercent(summary.anomalyPercentage)} of total)`}
        />
      )}

      <div className="section">
        <div className="grid-4">
          <KpiCard
            label="City Mean LST"
            value={formatTemperature(summary?.cityMeanLst)}
            icon="🌡️"
            accent
          />
          <KpiCard
            label="Max Temperature"
            value={formatTemperature(summary?.cityMaxLst)}
            icon="🔥"
          />
          <KpiCard
            label="Anomalous Points"
            value={summary?.anomalyCount || 0}
            change={formatPercent(summary?.anomalyPercentage)}
            changeType="negative"
            icon="⚠️"
          />
          <KpiCard
            label="Critical Zones"
            value={summary?.criticalCount || 0}
            icon="🚨"
          />
        </div>
      </div>

      <div className="section">
        <div className="grid-2">
          <div className="card">
            <div className="card-header">
              <h3 className="card-title">Temperature Distribution</h3>
            </div>
            {hasLstData ? (
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={lstChartData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                  <XAxis dataKey="range" stroke="var(--text-muted)" />
                  <YAxis stroke="var(--text-muted)" />
                  <Tooltip
                    contentStyle={{
                      background: 'var(--bg-card)',
                      border: '1px solid var(--border)',
                      borderRadius: '8px',
                    }}
                  />
                  <Bar dataKey="count" fill="#FF8C00" name="Points" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div style={{ padding: '60px 20px', textAlign: 'center' }}>
                <div style={{ fontSize: '48px', marginBottom: '16px' }}>🌡️</div>
                <div style={{ fontSize: '14px', color: 'var(--text-muted)' }}>
                  No temperature data available
                </div>
              </div>
            )}
          </div>

          <div className="card">
            <div className="card-header">
              <h3 className="card-title">Hotspot Zones</h3>
            </div>
            {hasHotspotData ? (
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={hotspots} layout="vertical">
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                  <XAxis type="number" stroke="var(--text-muted)" />
                  <YAxis dataKey="zoneName" type="category" width={120} stroke="var(--text-muted)" tick={{ fontSize: 11 }} />
                  <Tooltip
                    contentStyle={{
                      background: 'var(--bg-card)',
                      border: '1px solid var(--border)',
                      borderRadius: '8px',
                    }}
                  />
                  <Bar dataKey="avgLstCelsius" fill="#FF4444" name="Avg LST (°C)" radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div style={{ padding: '60px 20px', textAlign: 'center' }}>
                <div style={{ fontSize: '48px', marginBottom: '16px' }}>🔥</div>
                <div style={{ fontSize: '14px', color: 'var(--text-muted)' }}>
                  No hotspot data available
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="section">
        <div className="section-header">
          <h2 className="section-title">Hotspot Details</h2>
        </div>
        {hasHotspotData ? (
          <div className="card">
            <table className="table">
              <thead>
                <tr>
                  <th>Zone</th>
                  <th>Avg LST</th>
                  <th>Max LST</th>
                  <th>Points</th>
                  <th>Severity</th>
                </tr>
              </thead>
              <tbody>
                {hotspots.map((hotspot) => (
                  <tr key={hotspot.id}>
                    <td style={{ fontWeight: 500 }}>{hotspot.zoneName}</td>
                    <td>{formatTemperature(hotspot.avgLstCelsius)}</td>
                    <td>{formatTemperature(hotspot.maxLstCelsius)}</td>
                    <td>{hotspot.pointCount}</td>
                    <td>
                      <span
                        className={`badge badge-${hotspot.severity?.toLowerCase()}`}
                      >
                        {hotspot.severity}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="empty-state">
            <div className="empty-state-icon">🔥</div>
            <p className="empty-state-text">No hotspot data available</p>
          </div>
        )}
      </div>

      <div className="section">
        <AiSummaryCard
          title="AI UHI Analysis"
          featureKey="uhi"
          fetchSummary={uhiService.getAiSummary}
          regenerateSummary={uhiService.regenerateAiSummary}
        />
      </div>
    </div>
  );
};

export default UhiPage;
