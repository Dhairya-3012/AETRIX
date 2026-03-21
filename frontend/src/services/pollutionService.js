import api from './api';

const pollutionService = {
  getSummary: () => api.get('/pollution/summary'),
  getMap: () => api.get('/pollution/map'),
  getHotspots: () => api.get('/pollution/hotspots'),
  getCompliance: () => api.get('/pollution/compliance'),
  getOutliers: () => api.get('/pollution/outliers'),
  getAiSummary: () => api.get('/pollution/ai-summary'),
  regenerateAiSummary: () => api.post('/pollution/ai-summary/regenerate'),
};

export default pollutionService;
