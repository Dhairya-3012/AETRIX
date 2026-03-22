import api from './api';

const forecastService = {
  getTrend: (city) => api.get('/forecast/trend', { params: { city } }),
  getBreach: (city) => api.get('/forecast/breach', { params: { city } }),
  getHistorical: (city) => api.get('/forecast/historical', { params: { city } }),
  getPredicted: (city) => api.get('/forecast/predicted', { params: { city } }),
  getAiSummary: (city) => api.get('/forecast/ai-summary', { params: { city } }),
  regenerateAiSummary: (city) => api.post('/forecast/ai-summary/regenerate', null, { params: { city } }),
};

export default forecastService;
