import api from './api';

const pollutionService = {
  getSummary: (city) => api.get('/pollution/summary', { params: { city } }),
  getMap: (city) => api.get('/pollution/map', { params: { city } }),
  getHotspots: (city) => api.get('/pollution/hotspots', { params: { city } }),
  getCompliance: (city) => api.get('/pollution/compliance', { params: { city } }),
  getOutliers: (city) => api.get('/pollution/outliers', { params: { city } }),
  getAiSummary: (city) => api.get('/pollution/ai-summary', { params: { city } }),
  regenerateAiSummary: (city) => api.post('/pollution/ai-summary/regenerate', null, { params: { city } }),
};

export default pollutionService;
