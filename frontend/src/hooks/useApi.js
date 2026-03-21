import { useState, useEffect, useCallback } from 'react';

export function useApi(apiFunction, dependencies = [], autoFetch = true) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(autoFetch);
  const [error, setError] = useState(null);

  const fetchData = useCallback(async (...args) => {
    setLoading(true);
    setError(null);
    try {
      const response = await apiFunction(...args);
      if (response.success) {
        setData(response.data);
      } else {
        setError(response.message || 'An error occurred');
      }
      return response;
    } catch (err) {
      setError(err.message || 'An error occurred');
      return null;
    } finally {
      setLoading(false);
    }
  }, [apiFunction]);

  useEffect(() => {
    if (autoFetch) {
      fetchData();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, dependencies);

  return { data, loading, error, refetch: fetchData };
}

export function useDashboard() {
  const [overview, setOverview] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const fetchOverview = useCallback(async () => {
    setLoading(true);
    try {
      const dashboardService = (await import('../services/dashboardService')).default;
      const response = await dashboardService.getOverview();
      if (response.success) {
        setOverview(response.data);
      } else {
        setError(response.message);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchOverview();
  }, [fetchOverview]);

  return { overview, loading, error, refetch: fetchOverview };
}
