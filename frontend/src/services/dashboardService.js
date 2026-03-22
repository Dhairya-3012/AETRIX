import api from './api';

const dashboardService = {
  getOverview: (city) => api.get('/dashboard/overview', { params: { city } }),
  getCities: () => api.get('/dashboard/cities'),
};

export default dashboardService;
