import React from 'react';

const ProgressPanel = ({ broadcast }) => {
  const { statistics, percent_complete = 0 } = broadcast;
  const { sent = 0, failed = 0, queued = 0 } = statistics || {};

  return (
    <div className="progress-card">
      <div className="progress-header">
        <span className="composer-title">📤 Sending...</span>
        <span>{percent_complete}%</span>
      </div>

      <div className="progress-bar-container">
        <div
          className="progress-bar"
          style={{ width: `${percent_complete}%` }}
        ></div>
      </div>

      <div className="progress-stats">
        <div className="stat-item">
          <div className="stat-value sent">{sent}</div>
          <div className="stat-label">Sent</div>
        </div>
        <div className="stat-item">
          <div className="stat-value failed">{failed}</div>
          <div className="stat-label">Failed</div>
        </div>
        <div className="stat-item">
          <div className="stat-value pending">{queued}</div>
          <div className="stat-label">Pending</div>
        </div>
      </div>
    </div>
  );
};

export default ProgressPanel;
