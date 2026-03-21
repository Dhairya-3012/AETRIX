import React, { useState } from 'react';
import { useAuth } from './context/AuthContext';
import Sidebar from './components/Sidebar';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import MapPage from './pages/MapPage';
import UhiPage from './pages/UhiPage';
import VegetationPage from './pages/VegetationPage';
import PollutionPage from './pages/PollutionPage';
import ForecastPage from './pages/ForecastPage';
import ActionPlanPage from './pages/ActionPlanPage';

function App() {
  const { isAuthenticated } = useAuth();
  const [currentPage, setCurrentPage] = useState('dashboard');

  if (!isAuthenticated) {
    return <LoginPage />;
  }

  const renderPage = () => {
    switch (currentPage) {
      case 'dashboard':
        return <DashboardPage />;
      case 'map':
        return <MapPage />;
      case 'uhi':
        return <UhiPage />;
      case 'vegetation':
        return <VegetationPage />;
      case 'pollution':
        return <PollutionPage />;
      case 'forecast':
        return <ForecastPage />;
      case 'actions':
        return <ActionPlanPage />;
      default:
        return <DashboardPage />;
    }
  };

  return (
    <div className="app-container">
      <Sidebar currentPage={currentPage} onNavigate={setCurrentPage} />
      <main className="main-content">
        {renderPage()}
      </main>
    </div>
  );
}

export default App;
