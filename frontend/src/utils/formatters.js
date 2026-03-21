export const formatNumber = (num, decimals = 1) => {
  if (num === null || num === undefined) return 'N/A';
  return Number(num).toFixed(decimals);
};

export const formatPercent = (num, decimals = 1) => {
  if (num === null || num === undefined) return 'N/A';
  return `${Number(num).toFixed(decimals)}%`;
};

export const formatTemperature = (num, decimals = 1) => {
  if (num === null || num === undefined) return 'N/A';
  return `${Number(num).toFixed(decimals)}°C`;
};

export const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  const date = new Date(dateString);
  return date.toLocaleDateString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  });
};

export const formatDateTime = (dateString) => {
  if (!dateString) return 'N/A';
  const date = new Date(dateString);
  return date.toLocaleString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

export const formatLargeNumber = (num) => {
  if (num === null || num === undefined) return 'N/A';
  if (num >= 1000000) return `${(num / 1000000).toFixed(1)}M`;
  if (num >= 1000) return `${(num / 1000).toFixed(1)}K`;
  return num.toString();
};
