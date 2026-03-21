import api from './api';

const vegetationService = {
  getSummary: () => api.get('/vegetation/summary'),
  getMap: () => api.get('/vegetation/map'),
  getAlerts: () => api.get('/vegetation/alerts'),
  getPlantation: () => api.get('/vegetation/plantation'),
  getAiSummary: () => api.get('/vegetation/ai-summary'),
  regenerateAiSummary: () => api.post('/vegetation/ai-summary/regenerate'),
};

export default vegetationService;
