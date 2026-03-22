import api from './api';

const uhiService = {
  getSummary: (city) => api.get('/uhi/summary', { params: { city } }),
  getHeatmap: (city) => api.get('/uhi/heatmap', { params: { city } }),
  getHotspots: (city) => api.get('/uhi/hotspots', { params: { city } }),
  getHotspotById: (id) => api.get(`/uhi/hotspots/${id}`),
  getAnomalies: (city) => api.get('/uhi/anomalies', { params: { city } }),
  getAiSummary: (city) => api.get('/uhi/ai-summary', { params: { city } }),
  regenerateAiSummary: (city) => api.post('/uhi/ai-summary/regenerate', null, { params: { city } }),
};

export default uhiService;
