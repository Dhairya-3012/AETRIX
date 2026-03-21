import api from './api';

const actionService = {
  getAll: () => api.get('/actions'),
  getSummary: () => api.get('/actions/summary'),
  getHighPriority: () => api.get('/actions/high-priority'),
  getById: (id) => api.get(`/actions/${id}`),
  updateStatus: (id, status) => api.put(`/actions/${id}/status`, { status }),
  getAiSummary: () => api.get('/actions/ai-summary'),
  regenerateAiSummary: () => api.post('/actions/regenerate'),
};

export default actionService;
