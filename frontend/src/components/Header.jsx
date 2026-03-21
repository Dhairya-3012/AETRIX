import React from 'react';
import { getTrendColor } from '../utils/colorUtils';

const Header = ({ title, subtitle, city, state, trend, trendDescription }) => {
  const cityTag = city && state ? `${city}, ${state}` : city || 'AHMEDABAD, GJ';

  return (
    <header className="header">
      <div className="header-left">
        <div>
          <h1 className="header-title">{title}</h1>
          <p className="header-subtitle">
            {subtitle || 'AETRIX — India Environmental Intelligence'}
          </p>
        </div>
      </div>

      <div className="header-right">
        <span className="header-city-tag">{cityTag}</span>
        {trend && (
          <span
            className={`header-trend-badge ${trend}`}
            style={{ borderColor: getTrendColor(trend) + '50' }}
          >
            <span style={{ color: getTrendColor(trend) }}>●</span>
            {trend?.toUpperCase()}
          </span>
        )}
      </div>
    </header>
  );
};

export default Header;
