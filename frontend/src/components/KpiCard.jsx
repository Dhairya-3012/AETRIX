import React from 'react';

const KpiCard = ({ label, value, unit, change, changeType, accent, icon }) => {
  return (
    <div className="kpi-card">
      <div className="kpi-label">
        {icon && <span style={{ marginRight: '6px' }}>{icon}</span>}
        {label}
      </div>
      <div className={`kpi-value ${accent ? 'accent' : ''}`}>
        {value}
        {unit && <span style={{ fontSize: '16px', fontWeight: '500', marginLeft: '4px' }}>{unit}</span>}
      </div>
      {change && (
        <div className={`kpi-change ${changeType || ''}`}>
          {changeType === 'positive' && '↑'}
          {changeType === 'negative' && '↓'}
          {change}
        </div>
      )}
    </div>
  );
};

export default KpiCard;
