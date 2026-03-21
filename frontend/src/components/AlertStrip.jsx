import React from 'react';

const AlertStrip = ({ type, icon, message, count, city }) => {
  const alertClass = type === 'warn' ? 'alert-strip warn' : type === 'info' ? 'alert-strip info' : 'alert-strip';

  return (
    <div className={alertClass}>
      <span className="alert-icon">{icon || '⚠️'}</span>
      <span className="alert-text">
        {message || `${count} locations in ${city || 'the city'} require immediate attention`}
      </span>
    </div>
  );
};

export default AlertStrip;
