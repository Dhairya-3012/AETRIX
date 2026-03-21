import api from './api';

const llmService = {
  summarize: (feature, city) => api.post('/llm/summarize', { feature, city }),
  regenerate: (feature, city) => api.post('/llm/regenerate', { feature, city }),
  getStatus: () => api.get('/llm/status'),
};

export default llmService;
