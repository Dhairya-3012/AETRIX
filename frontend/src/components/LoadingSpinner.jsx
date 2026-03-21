import React from 'react';

const LoadingSpinner = ({ size = 24 }) => {
  return (
    <div className="loading-container">
      <div
        className="loading-spinner"
        style={{ width: size, height: size }}
      ></div>
    </div>
  );
};

export default LoadingSpinner;
