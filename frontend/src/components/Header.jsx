import React from 'react';
import { getTrendColor } from '../utils/colorUtils';
import { useCity } from '../context/CityContext';

const Header = ({ title, subtitle, city, state, trend, trendDescription }) => {
  const { selectedCity, setSelectedCity, cities } = useCity();
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
        <div className="header-city-selector">
          <label className="header-city-label">City</label>
          <select
            value={selectedCity}
            onChange={(e) => setSelectedCity(e.target.value)}
            className="header-city-select"
          >
            {cities.map(cityName => (
              <option key={cityName} value={cityName}>{cityName}</option>
            ))}
          </select>
        </div>
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
