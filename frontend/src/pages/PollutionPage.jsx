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
import pollutionService from '../services/pollutionService';
import { formatNumber, formatPercent } from '../utils/formatters';

const PollutionPage = () => {
  const [summary, setSummary] = useState(null);
  const [hotspots, setHotspots] = useState([]);
  const [outliers, setOutliers] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const [summaryRes, hotspotsRes, outliersRes] = await Promise.all([
          pollutionService.getSummary(),
          pollutionService.getHotspots(),
          pollutionService.getOutliers(),
        ]);

        if (summaryRes?.success) setSummary(summaryRes.data);
        if (hotspotsRes?.success) setHotspots(hotspotsRes.data || []);
        if (outliersRes?.success) setOutliers(outliersRes.data || []);
      } catch (err) {
        console.error('Error fetching pollution data:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  if (loading) {
    return (
      <div className="page-container">
        <Header title="Pollution Risk Analysis" />
        <LoadingSpinner />
      </div>
    );
  }

  const riskDistribution = [
    { name: 'Critical', value: summary?.criticalCount || 0, color: '#FF4444' },
    { name: 'High', value: summary?.highCount || 0, color: '#FF8C00' },
    { name: 'Medium', value: summary?.mediumCount || 0, color: '#FFD700' },
    { name: 'Low', value: summary?.lowCount || 0, color: '#00E676' },
  ].filter(item => item.value > 0);

  const hasRiskData = riskDistribution.length > 0;
  const hasHotspotData = hotspots.length > 0;
  const hasOutlierData = outliers.length > 0;

  return (
    <div className="page-container">
      <Header
        title="Pollution Risk Analysis"
        subtitle={`AETRIX — India Environmental Intelligence | ${summary?.city || 'Ahmedabad'}`}
        city={summary?.city}
      />

      {summary?.criticalCount > 0 && (
        <AlertStrip
          type="critical"
          icon="💨"
          message={`${summary.criticalCount} critical pollution zones in ${summary.city} require immediate regulatory action`}
        />
      )}

      <div className="section">
        <div className="grid-4">
          <KpiCard
            label="Mean Risk Score"
            value={formatNumber(summary?.cityMeanRisk, 1)}
            unit="/100"
            icon="📊"
            accent
          />
          <KpiCard
            label="Critical Zones"
            value={summary?.criticalCount || 0}
            change={formatPercent(summary?.criticalPercentage)}
            changeType="negative"
            icon="🚨"
          />
          <KpiCard
            label="High Risk Zones"
            value={summary?.highCount || 0}
            icon="⚠️"
          />
          <KpiCard
            label="Extreme Outliers"
            value={summary?.extremeOutlierCount || 0}
            icon="📍"
          />
        </div>
      </div>

      <div className="section">
        <div className="grid-2">
          <div className="card">
            <div className="card-header">
              <h3 className="card-title">Risk Category Distribution</h3>
            </div>
            {hasRiskData ? (
              <ResponsiveContainer width="100%" height={300}>
                <PieChart>
                  <Pie
                    data={riskDistribution}
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={100}
                    paddingAngle={5}
                    dataKey="value"
                    label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                  >
                    {riskDistribution.map((entry, index) => (
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
                <div style={{ fontSize: '48px', marginBottom: '16px' }}>💨</div>
                <div style={{ fontSize: '14px', color: 'var(--text-muted)' }}>
                  No pollution risk data available
                </div>
              </div>
            )}
          </div>

          <div className="card">
            <div className="card-header">
              <h3 className="card-title">Pollution Hotspots</h3>
            </div>
            {hasHotspotData ? (
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={hotspots} layout="vertical">
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                  <XAxis type="number" domain={[0, 100]} stroke="var(--text-muted)" />
                  <YAxis dataKey="zoneName" type="category" width={120} stroke="var(--text-muted)" tick={{ fontSize: 11 }} />
                  <Tooltip
                    contentStyle={{
                      background: 'var(--bg-card)',
                      border: '1px solid var(--border)',
                      borderRadius: '8px',
                    }}
                  />
                  <Bar dataKey="avgRiskScore" fill="#FF4444" name="Avg Risk Score" radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div style={{ padding: '60px 20px', textAlign: 'center', minHeight: '300px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                <div style={{ fontSize: '48px', marginBottom: '16px' }}>📍</div>
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
          <h2 className="section-title">Hotspot Details & Compliance</h2>
        </div>
        {hasHotspotData ? (
          <div className="card">
            <table className="table">
              <thead>
                <tr>
                  <th>Zone</th>
                  <th>Avg Score</th>
                  <th>Max Score</th>
                  <th>Points</th>
                  <th>Severity</th>
                  <th>Responsible Dept</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {hotspots.map((hotspot) => (
                  <tr key={hotspot.id}>
                    <td style={{ fontWeight: 500 }}>{hotspot.zoneName}</td>
                    <td>{formatNumber(hotspot.avgRiskScore, 1)}</td>
                    <td>{formatNumber(hotspot.maxRiskScore, 1)}</td>
                    <td>{hotspot.pointCount}</td>
                    <td>
                      <span className={`badge badge-${hotspot.severity?.toLowerCase()}`}>
                        {hotspot.severity}
                      </span>
                    </td>
                    <td>{hotspot.responsibleDept}</td>
                    <td style={{ maxWidth: '200px', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                      {hotspot.recommendedAction}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="empty-state">
            <div className="empty-state-icon">📍</div>
            <p className="empty-state-text">No pollution hotspot data available</p>
          </div>
        )}
      </div>

      <div className="section">
        <div className="section-header">
          <h2 className="section-title">Extreme Outlier Points</h2>
        </div>
        {hasOutlierData ? (
          <div className="card">
            <table className="table">
              <thead>
                <tr>
                  <th>Point ID</th>
                  <th>Location</th>
                  <th>Risk Score</th>
                  <th>Category</th>
                </tr>
              </thead>
              <tbody>
                {outliers.slice(0, 10).map((point) => (
                  <tr key={point.pointId}>
                    <td style={{ fontWeight: 500 }}>{point.pointId}</td>
                    <td>{`${point.lat?.toFixed(4)}, ${point.lng?.toFixed(4)}`}</td>
                    <td style={{ color: '#FF4444', fontWeight: 600 }}>
                      {formatNumber(point.riskScore, 1)}/100
                    </td>
                    <td>
                      <span className={`badge badge-${point.riskCategory?.toLowerCase()}`}>
                        {point.riskCategory}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="empty-state">
            <div className="empty-state-icon">🔍</div>
            <p className="empty-state-text">No outlier data available</p>
          </div>
        )}
      </div>

      <div className="section">
        <AiSummaryCard
          title="AI Pollution Analysis"
          featureKey="pollution"
          fetchSummary={pollutionService.getAiSummary}
          regenerateSummary={pollutionService.regenerateAiSummary}
        />
      </div>
    </div>
  );
};

export default PollutionPage;
