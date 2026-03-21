import React from 'react';
import { useAuth, ROLE_COLORS } from '../context/AuthContext';

const Sidebar = ({ currentPage, onNavigate }) => {
  const { currentUser, logout } = useAuth();

  const navItems = [
    { id: 'dashboard', icon: '🏠', label: 'Dashboard', section: 'OVERVIEW' },
    { id: 'uhi', icon: '🌡️', label: 'UHI Analysis', section: 'ANALYSIS' },
    { id: 'vegetation', icon: '🌿', label: 'Vegetation', section: 'ANALYSIS' },
    { id: 'pollution', icon: '💨', label: 'Pollution', section: 'ANALYSIS' },
    { id: 'forecast', icon: '📈', label: 'Forecast', section: 'ANALYSIS' },
    { id: 'map', icon: '🗺️', label: 'Interactive Map', section: 'MAP' },
    { id: 'actions', icon: '📋', label: 'Action Plan', section: 'OUTPUT', badge: true },
  ];

  const sections = [...new Set(navItems.map(item => item.section))];

  return (
    <div className="sidebar">
      <div className="sidebar-brand">
        <h1>AETRIX</h1>
        <p>All-India Env. Intelligence</p>
      </div>

      <nav className="sidebar-nav">
        {sections.map(section => (
          <div key={section} className="sidebar-section">
            <div className="sidebar-section-title">{section}</div>
            {navItems
              .filter(item => item.section === section)
              .map(item => (
                <div
                  key={item.id}
                  className={`sidebar-item ${currentPage === item.id ? 'active' : ''}`}
                  onClick={() => onNavigate(item.id)}
                >
                  <span className="sidebar-item-icon">{item.icon}</span>
                  <span>{item.label}</span>
                  {item.badge && (
                    <span className="sidebar-item-badge">3</span>
                  )}
                </div>
              ))}
          </div>
        ))}
      </nav>

      {currentUser && (
        <div className="sidebar-user">
          <div
            className="sidebar-user-avatar"
            style={{ backgroundColor: ROLE_COLORS[currentUser.role] + '20', color: ROLE_COLORS[currentUser.role] }}
          >
            {currentUser.avatar}
          </div>
          <div className="sidebar-user-info">
            <div className="sidebar-user-name">{currentUser.name}</div>
            <div className="sidebar-user-role">{currentUser.entity}</div>
          </div>
          <span className="sidebar-logout" onClick={logout} title="Logout">
            🚪
          </span>
        </div>
      )}
    </div>
  );
};

export default Sidebar;
