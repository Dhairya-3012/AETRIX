import api from './api';

const forecastService = {
  getTrend: () => api.get('/forecast/trend'),
  getBreach: () => api.get('/forecast/breach'),
  getHistorical: () => api.get('/forecast/historical'),
  getPredicted: () => api.get('/forecast/predicted'),
  getAiSummary: () => api.get('/forecast/ai-summary'),
  regenerateAiSummary: () => api.post('/forecast/ai-summary/regenerate'),
};

export default forecastService;
