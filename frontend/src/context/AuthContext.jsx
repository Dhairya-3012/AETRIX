import React, { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext(null);

export const ROLES = {
  MUNICIPAL_OFFICER: 'Municipal Officer',
  ENVIRONMENTAL_RESEARCHER: 'Environmental Researcher',
  REGULATOR: 'Regulator',
  CITY_PLANNER: 'City Planner',
  POLLUTION_CONTROL_OFFICER: 'Pollution Control Officer',
  FIELD_INSPECTOR: 'Field Inspector',
};

export const ROLE_COLORS = {
  'Municipal Officer': '#4A9EFF',
  'Environmental Researcher': '#00E676',
  'Regulator': '#FF4444',
  'City Planner': '#A78BFA',
  'Pollution Control Officer': '#FF8C00',
  'Field Inspector': '#FFD700',
};

export const DEMO_USERS = [
  {
    id: 1,
    name: 'Priya Sharma',
    role: 'Municipal Officer',
    entity: 'AMC — Ahmedabad',
    email: 'priya@amc.gov.in',
    avatar: 'PS',
  },
  {
    id: 2,
    name: 'Dr. Rahul Mehta',
    role: 'Environmental Researcher',
    entity: 'ISRO / SAC',
    email: 'rahul@isro.gov.in',
    avatar: 'RM',
  },
  {
    id: 3,
    name: 'Kavita Patel',
    role: 'Regulator',
    entity: 'CPCB — New Delhi',
    email: 'kavita@cpcb.gov.in',
    avatar: 'KP',
  },
  {
    id: 4,
    name: 'Arjun Nair',
    role: 'City Planner',
    entity: 'AUDA — Ahmedabad',
    email: 'arjun@auda.org.in',
    avatar: 'AN',
  },
  {
    id: 5,
    name: 'Suresh Kumar',
    role: 'Pollution Control Officer',
    entity: 'GPCB — Gujarat',
    email: 'suresh@gpcb.gov.in',
    avatar: 'SK',
  },
  {
    id: 6,
    name: 'Meera Joshi',
    role: 'Field Inspector',
    entity: 'MoEFCC — India',
    email: 'meera@moefcc.gov.in',
    avatar: 'MJ',
  },
];

export function AuthProvider({ children }) {
  const [currentUser, setCurrentUser] = useState(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);

  useEffect(() => {
    const savedUser = sessionStorage.getItem('aetrix_user');
    if (savedUser) {
      try {
        const user = JSON.parse(savedUser);
        setCurrentUser(user);
        setIsAuthenticated(true);
      } catch (e) {
        sessionStorage.removeItem('aetrix_user');
      }
    }
  }, []);

  const login = (user) => {
    setCurrentUser(user);
    setIsAuthenticated(true);
    sessionStorage.setItem('aetrix_user', JSON.stringify(user));
  };

  const logout = () => {
    setCurrentUser(null);
    setIsAuthenticated(false);
    sessionStorage.removeItem('aetrix_user');
  };

  const value = {
    currentUser,
    isAuthenticated,
    login,
    logout,
    DEMO_USERS,
    ROLES,
    ROLE_COLORS,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
