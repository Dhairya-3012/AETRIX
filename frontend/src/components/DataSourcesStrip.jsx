import React from 'react';

const DataSourcesStrip = ({ sources, lastUpdated, coverage }) => {
  return (
    <div className="data-sources">
      <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>Data Sources:</span>
      {sources?.map((source, index) => (
        <span key={index} className="data-source-tag">{source}</span>
      ))}
      {coverage && (
        <span className="data-source-tag accent">{coverage}</span>
      )}
      {lastUpdated && (
        <span style={{ marginLeft: 'auto', fontSize: '11px', color: 'var(--text-muted)' }}>
          Updated: {new Date(lastUpdated).toLocaleString()}
        </span>
      )}
    </div>
  );
};

export default DataSourcesStrip;
