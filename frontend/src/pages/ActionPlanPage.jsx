import React, { useState, useEffect, useCallback } from 'react';
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
import ActionCard from '../components/ActionCard';
import AiSummaryCard from '../components/AiSummaryCard';
import LoadingSpinner from '../components/LoadingSpinner';
import actionService from '../services/actionService';
import { formatDate } from '../utils/formatters';

const ActionPlanPage = () => {
  const [plan, setPlan] = useState(null);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState('all');

  const fetchData = useCallback(async () => {
    setLoading(true);
    try {
      const response = await actionService.getAll();
      if (response?.success) {
        setPlan(response.data);
      }
    } catch (err) {
      console.error('Error fetching action plan:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const handleStatusChange = async (actionId, newStatus) => {
    try {
      await actionService.updateStatus(actionId, newStatus);
      fetchData();
    } catch (err) {
      console.error('Error updating action status:', err);
    }
  };

  if (loading) {
    return (
      <div className="page-container">
        <Header title="Environment Action Plan" />
        <LoadingSpinner />
      </div>
    );
  }

  const statusDistribution = [
    { name: 'Pending', value: plan?.pendingCount || 0, color: '#4A9EFF' },
    { name: 'In Progress', value: plan?.inProgressCount || 0, color: '#FF8C00' },
    { name: 'Completed', value: plan?.completedCount || 0, color: '#00E676' },
  ].filter(item => item.value > 0);

  const priorityDistribution = [
    { name: 'High', value: plan?.highPriorityCount || 0, color: '#FF4444' },
    { name: 'Medium', value: plan?.mediumPriorityCount || 0, color: '#FFD700' },
    { name: 'Low', value: plan?.lowPriorityCount || 0, color: '#00E676' },
  ].filter(item => item.value > 0);

  const hasStatusData = statusDistribution.length > 0;
  const hasPriorityData = priorityDistribution.length > 0;

  const filteredActions = plan?.actions?.filter(action => {
    if (filter === 'all') return true;
    if (filter === 'high') return action.priority === 'high';
    if (filter === 'pending') return action.status === 'pending';
    if (filter === 'in_progress') return action.status === 'in_progress';
    return true;
  }) || [];

  return (
    <div className="page-container">
      <Header
        title={`${plan?.city || 'City'} Environment Action Plan — AETRIX India`}
        subtitle={`AETRIX — India Environmental Intelligence | ${plan?.city || 'Ahmedabad'}`}
        city={plan?.city}
      />

      <div className="section">
        <div className="grid-4">
          <KpiCard
            label="Total Actions"
            value={plan?.totalActions || 0}
            icon="📋"
            accent
          />
          <KpiCard
            label="High Priority"
            value={plan?.highPriorityCount || 0}
            icon="🔴"
          />
          <KpiCard
            label="Pending"
            value={plan?.pendingCount || 0}
            icon="⏳"
          />
          <KpiCard
            label="Completed"
            value={plan?.completedCount || 0}
            icon="✅"
          />
        </div>
      </div>

      <div className="section">
        <div className="grid-2">
          <div className="card">
            <div className="card-header">
              <h3 className="card-title">Status Distribution</h3>
            </div>
            {hasStatusData ? (
              <ResponsiveContainer width="100%" height={250}>
                <PieChart>
                  <Pie
                    data={statusDistribution}
                    cx="50%"
                    cy="50%"
                    innerRadius={50}
                    outerRadius={80}
                    paddingAngle={5}
                    dataKey="value"
                  >
                    {statusDistribution.map((entry, index) => (
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
              <div style={{ padding: '60px 20px', textAlign: 'center', minHeight: '250px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                <div style={{ fontSize: '48px', marginBottom: '16px' }}>📊</div>
                <div style={{ fontSize: '14px', color: 'var(--text-muted)' }}>
                  No action items yet
                </div>
              </div>
            )}
          </div>

          <div className="card">
            <div className="card-header">
              <h3 className="card-title">Priority Distribution</h3>
            </div>
            {hasPriorityData ? (
              <ResponsiveContainer width="100%" height={250}>
                <BarChart data={priorityDistribution}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                  <XAxis dataKey="name" stroke="var(--text-muted)" />
                  <YAxis stroke="var(--text-muted)" />
                  <Tooltip
                    contentStyle={{
                      background: 'var(--bg-card)',
                      border: '1px solid var(--border)',
                      borderRadius: '8px',
                    }}
                  />
                  <Bar dataKey="value" name="Actions" radius={[4, 4, 0, 0]}>
                    {priorityDistribution.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div style={{ padding: '60px 20px', textAlign: 'center', minHeight: '250px', display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
                <div style={{ fontSize: '48px', marginBottom: '16px' }}>📋</div>
                <div style={{ fontSize: '14px', color: 'var(--text-muted)' }}>
                  No priority data available
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="section">
        <div className="section-header">
          <h2 className="section-title">Action Items</h2>
          <div style={{ display: 'flex', gap: '8px' }}>
            {['all', 'high', 'pending', 'in_progress'].map(f => (
              <button
                key={f}
                className={`btn btn-sm ${filter === f ? 'btn-primary' : 'btn-outline'}`}
                onClick={() => setFilter(f)}
              >
                {f === 'all' ? 'All' : f === 'high' ? 'High Priority' : f === 'in_progress' ? 'In Progress' : 'Pending'}
              </button>
            ))}
          </div>
        </div>

        {filteredActions.map(action => (
          <ActionCard
            key={action.id}
            action={action}
            onStatusChange={handleStatusChange}
          />
        ))}

        {filteredActions.length === 0 && (
          <div className="empty-state">
            <div className="empty-state-icon">📋</div>
            <p className="empty-state-text">No actions found for the selected filter</p>
          </div>
        )}
      </div>

      <div className="section">
        <AiSummaryCard
          title="AI Action Plan Summary"
          featureKey="action-plan"
          fetchSummary={actionService.getAiSummary}
          regenerateSummary={actionService.regenerateAiSummary}
        />
      </div>

      <div style={{
        marginTop: '32px',
        padding: '16px',
        background: 'var(--bg-secondary)',
        borderRadius: '8px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        fontSize: '12px',
        color: 'var(--text-muted)',
      }}>
        <span>
          Generated: {formatDate(new Date().toISOString())} | Coverage: {plan?.city}, India
        </span>
        <span>
          Sources: MODIS LST · Sentinel-2 NDVI · Landsat · SMAP
        </span>
      </div>
    </div>
  );
};

export default ActionPlanPage;
