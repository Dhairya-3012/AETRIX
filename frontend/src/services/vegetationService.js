import api from './api';

const vegetationService = {
  getSummary: (city) => api.get('/vegetation/summary', { params: { city } }),
  getMap: (city) => api.get('/vegetation/map', { params: { city } }),
  getAlerts: (city) => api.get('/vegetation/alerts', { params: { city } }),
  getPlantation: (city) => api.get('/vegetation/plantation', { params: { city } }),
  getAiSummary: (city) => api.get('/vegetation/ai-summary', { params: { city } }),
  regenerateAiSummary: (city) => api.post('/vegetation/ai-summary/regenerate', null, { params: { city } }),
};

export default vegetationService;
