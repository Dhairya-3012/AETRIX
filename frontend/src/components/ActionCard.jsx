import React from 'react';
import { getPriorityColor, getStatusColor } from '../utils/colorUtils';

const ActionCard = ({ action, onStatusChange }) => {
  const handleStatusChange = (e) => {
    if (onStatusChange) {
      onStatusChange(action.id, e.target.value);
    }
  };

  return (
    <div className="action-card">
      <div style={{ display: 'flex' }}>
        <div
          className="action-priority-indicator"
          style={{ backgroundColor: getPriorityColor(action.priority), marginRight: '16px' }}
        ></div>
        <div style={{ flex: 1 }}>
          <div className="action-header">
            <div>
              <div className="action-title">{action.title}</div>
              <span
                className={`badge badge-${action.priority?.toLowerCase()}`}
                style={{ marginRight: '8px' }}
              >
                {action.priority}
              </span>
              <span
                className="badge"
                style={{
                  backgroundColor: getStatusColor(action.status) + '20',
                  color: getStatusColor(action.status),
                  borderColor: getStatusColor(action.status) + '40',
                }}
              >
                {action.status?.replace('_', ' ')}
              </span>
            </div>
            <select
              value={action.status || 'pending'}
              onChange={handleStatusChange}
              style={{
                background: 'var(--bg-secondary)',
                border: '1px solid var(--border)',
                color: 'var(--text-primary)',
                padding: '6px 12px',
                borderRadius: '6px',
                fontSize: '12px',
              }}
            >
              <option value="pending">Pending</option>
              <option value="in_progress">In Progress</option>
              <option value="completed">Completed</option>
            </select>
          </div>

          <p className="action-description">{action.description}</p>

          <div className="action-meta">
            {action.actionId && (
              <span className="action-meta-item">
                🆔 {action.actionId}
              </span>
            )}
            {action.zoneName && (
              <span className="action-meta-item">
                📍 {action.zoneName}
              </span>
            )}
            {action.affectedCount && action.affectedCount > 0 && (
              <span className="action-meta-item">
                🎯 {action.affectedCount} locations
              </span>
            )}
            {action.responsibleDept && (
              <span className="action-meta-item">
                🏛️ {action.responsibleDept}
              </span>
            )}
            {action.deadline && (
              <span className="action-meta-item">
                📅 {action.deadline}
              </span>
            )}
          </div>

          {action.keyFinding && (
            <div style={{ marginTop: '12px', padding: '12px', background: 'var(--bg-secondary)', borderRadius: '8px' }}>
              <div style={{ fontSize: '11px', color: 'var(--text-muted)', marginBottom: '4px', fontWeight: '600' }}>KEY FINDING</div>
              <div style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>{action.keyFinding}</div>
            </div>
          )}

          {action.expectedImpact && (
            <div style={{ marginTop: '12px', padding: '12px', background: 'rgba(0, 230, 118, 0.1)', border: '1px solid rgba(0, 230, 118, 0.2)', borderRadius: '8px' }}>
              <div style={{ fontSize: '11px', color: '#00E676', marginBottom: '4px', fontWeight: '600' }}>EXPECTED IMPACT</div>
              <div style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>{action.expectedImpact}</div>
            </div>
          )}

          {action.modelTriggeredBy && (
            <div style={{ marginTop: '12px', fontSize: '11px', color: 'var(--text-muted)' }}>
              <span style={{ fontWeight: '600' }}>Triggered by:</span> {action.modelTriggeredBy}
              {action.satelliteSources && <span> • Sources: {action.satelliteSources}</span>}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ActionCard;
