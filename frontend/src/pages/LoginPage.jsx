import React, { useState } from 'react';
import { useAuth, DEMO_USERS, ROLE_COLORS } from '../context/AuthContext';

const LoginPage = () => {
  const { login } = useAuth();
  const [selectedUser, setSelectedUser] = useState(null);

  const handleLogin = () => {
    if (selectedUser) {
      login(selectedUser);
    }
  };

  return (
    <div className="login-container">
      <div className="login-content">
        <div className="login-header">
          <h1 className="login-logo">AETRIX</h1>
          <p className="login-title">All-India Environmental Telemetry & Risk Intelligence X</p>
          <p className="login-subtitle">
            Pan-India satellite environmental intelligence platform — cities, districts, states
          </p>
        </div>

        <div className="role-grid">
          {DEMO_USERS.map((user) => (
            <div
              key={user.id}
              className={`role-card ${selectedUser?.id === user.id ? 'selected' : ''}`}
              onClick={() => setSelectedUser(user)}
              style={{
                borderColor: selectedUser?.id === user.id ? ROLE_COLORS[user.role] : undefined,
                boxShadow: selectedUser?.id === user.id
                  ? `0 0 20px ${ROLE_COLORS[user.role]}30`
                  : undefined,
              }}
            >
              <div
                className="role-avatar"
                style={{
                  backgroundColor: ROLE_COLORS[user.role] + '20',
                  color: ROLE_COLORS[user.role],
                }}
              >
                {user.avatar}
              </div>
              <div className="role-name">{user.name}</div>
              <div className="role-title">{user.role}</div>
              <div className="role-entity">{user.entity}</div>
            </div>
          ))}
        </div>

        <button
          className="btn btn-primary"
          onClick={handleLogin}
          disabled={!selectedUser}
          style={{ padding: '14px 48px', fontSize: '16px' }}
        >
          Launch Dashboard
        </button>
      </div>
    </div>
  );
};

export default LoginPage;
