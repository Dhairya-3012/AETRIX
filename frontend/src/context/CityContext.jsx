import React, { createContext, useContext, useState, useEffect } from 'react';
import dashboardService from '../services/dashboardService';

const CityContext = createContext();

// City coordinates mapping
const CITY_COORDINATES = {
  Ahmedabad: { lat: 23.022, lng: 72.571, state: 'Gujarat' },
  Bangalore: { lat: 12.972, lng: 77.594, state: 'Karnataka' },
  Delhi: { lat: 28.644, lng: 77.216, state: 'Delhi' },
  Mumbai: { lat: 19.076, lng: 72.877, state: 'Maharashtra' },
};

export const CityProvider = ({ children }) => {
  const [selectedCity, setSelectedCity] = useState('Ahmedabad');
  const [cities, setCities] = useState(['Ahmedabad', 'Bangalore', 'Delhi', 'Mumbai']);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const fetchCities = async () => {
      try {
        const response = await dashboardService.getCities();
        if (response?.success && response.data) {
          setCities(response.data);
        }
      } catch (error) {
        console.log('Using default cities list');
      }
    };
    fetchCities();
  }, []);

  const getCityCoordinates = (cityName) => {
    return CITY_COORDINATES[cityName] || CITY_COORDINATES.Ahmedabad;
  };

  const value = {
    selectedCity,
    setSelectedCity,
    cities,
    loading,
    getCityCoordinates,
    cityInfo: getCityCoordinates(selectedCity),
  };

  return (
    <CityContext.Provider value={value}>
      {children}
    </CityContext.Provider>
  );
};

export const useCity = () => {
  const context = useContext(CityContext);
  if (!context) {
    throw new Error('useCity must be used within a CityProvider');
  }
  return context;
};

export default CityContext;
