export const getSeverityColor = (severity) => {
  const colors = {
    critical: '#FF4444',
    high: '#FF8C00',
    moderate: '#FFD700',
    medium: '#FFD700',
    low: '#00E676',
    healthy: '#00E676',
    stressed: '#FFD700',
    barren: '#FF4444',
  };
  return colors[severity?.toLowerCase()] || '#7B9DB5';
};

export const getRiskColor = (riskScore) => {
  if (riskScore >= 75) return '#FF4444';
  if (riskScore >= 50) return '#FF8C00';
  if (riskScore >= 25) return '#FFD700';
  return '#00E676';
};

export const getHealthColor = (healthLabel) => {
  const colors = {
    Healthy: '#00E676',
    Stressed: '#FFD700',
    Barren: '#FF4444',
  };
  return colors[healthLabel] || '#7B9DB5';
};

export const getLstColor = (lstCelsius) => {
  if (lstCelsius >= 40) return '#FF4444';
  if (lstCelsius >= 35) return '#FF8C00';
  if (lstCelsius >= 30) return '#FFD700';
  return '#00E676';
};

export const getNdviColor = (ndvi) => {
  if (ndvi >= 0.5) return '#00E676';
  if (ndvi >= 0.3) return '#4ADE80';
  if (ndvi >= 0.15) return '#FFD700';
  return '#FF4444';
};

export const getTrendColor = (trend) => {
  const colors = {
    critical: '#FF4444',
    degrading: '#FF8C00',
    stable: '#4A9EFF',
    improving: '#00E676',
  };
  return colors[trend?.toLowerCase()] || '#7B9DB5';
};

export const getPriorityColor = (priority) => {
  const colors = {
    high: '#FF4444',
    medium: '#FF8C00',
    low: '#00E676',
  };
  return colors[priority?.toLowerCase()] || '#7B9DB5';
};

export const getStatusColor = (status) => {
  const colors = {
    pending: '#4A9EFF',
    in_progress: '#FF8C00',
    completed: '#00E676',
  };
  return colors[status?.toLowerCase()] || '#7B9DB5';
};

export const interpolateColor = (value, min, max, colorStart, colorEnd) => {
  const ratio = Math.min(Math.max((value - min) / (max - min), 0), 1);

  const hexToRgb = (hex) => {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ? {
      r: parseInt(result[1], 16),
      g: parseInt(result[2], 16),
      b: parseInt(result[3], 16)
    } : null;
  };

  const rgbToHex = (r, g, b) => {
    return '#' + [r, g, b].map(x => {
      const hex = Math.round(x).toString(16);
      return hex.length === 1 ? '0' + hex : hex;
    }).join('');
  };

  const start = hexToRgb(colorStart);
  const end = hexToRgb(colorEnd);

  if (!start || !end) return colorStart;

  return rgbToHex(
    start.r + ratio * (end.r - start.r),
    start.g + ratio * (end.g - start.g),
    start.b + ratio * (end.b - start.b)
  );
};
