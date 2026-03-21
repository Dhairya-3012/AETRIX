import api from './api';

const dashboardService = {
  getOverview: () => api.get('/dashboard/overview'),
};

export default dashboardService;
