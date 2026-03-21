import api from './api';

const uhiService = {
  getSummary: () => api.get('/uhi/summary'),
  getHeatmap: () => api.get('/uhi/heatmap'),
  getHotspots: () => api.get('/uhi/hotspots'),
  getHotspotById: (id) => api.get(`/uhi/hotspots/${id}`),
  getAnomalies: () => api.get('/uhi/anomalies'),
  getAiSummary: () => api.get('/uhi/ai-summary'),
  regenerateAiSummary: () => api.post('/uhi/ai-summary/regenerate'),
};

export default uhiService;
