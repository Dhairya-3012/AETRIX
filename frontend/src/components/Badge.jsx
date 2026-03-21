import React from 'react';

const Badge = ({ type, children }) => {
  const badgeClass = type
    ? `badge badge-${type.toLowerCase()}`
    : 'badge';

  return <span className={badgeClass}>{children}</span>;
};

export default Badge;
