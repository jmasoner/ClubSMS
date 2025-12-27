import React, { useState } from 'react';

const ConnectionPanel = ({ connected, phoneInfo, onConnect }) => {
  const [phoneIP, setPhoneIP] = useState('');
  const [isConnecting, setIsConnecting] = useState(false);

  const handleConnect = async () => {
    if (!phoneIP.trim()) {
      alert('Please enter phone IP address');
      return;
    }

    setIsConnecting(true);
    try {
      const result = await window.clubsms.connect(phoneIP.trim());
      if (result.success) {
        onConnect && onConnect();
      } else {
        alert('Connection failed: ' + (result.error || 'Unknown error'));
      }
    } catch (error) {
      alert('Connection error: ' + error.message);
    }
    setIsConnecting(false);
  };

  const handleDisconnect = async () => {
    await window.clubsms.disconnect();
  };

  return (
    <div>
      <div className="section-title">Phone Connection</div>

      {!connected ? (
        <div className="connect-form">
          <div className="input-group">
            <label className="input-label">Phone IP Address</label>
            <input
              type="text"
              placeholder="192.168.1.xxx"
              value={phoneIP}
              onChange={(e) => setPhoneIP(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && handleConnect()}
            />
          </div>
          <button
            className="btn btn-primary"
            onClick={handleConnect}
            disabled={isConnecting}
          >
            {isConnecting && <div className="loading-spinner"></div>}
            {isConnecting ? 'Connecting...' : 'Connect to Phone'}
          </button>
        </div>
      ) : (
        <div>
          <div className="phone-info">
            <div className="phone-info-row">
              <span className="phone-info-label">Model</span>
              <span className="phone-info-value">{phoneInfo?.model || '-'}</span>
            </div>
            <div className="phone-info-row">
              <span className="phone-info-label">Android</span>
              <span className="phone-info-value">{phoneInfo?.android_version || '-'}</span>
            </div>
            <div className="phone-info-row">
              <span className="phone-info-label">Carrier</span>
              <span className="phone-info-value">{phoneInfo?.carrier || '-'}</span>
            </div>
            <div className="phone-info-row">
              <span className="phone-info-label">Battery</span>
              <span className="phone-info-value">{phoneInfo?.battery_level ? `${phoneInfo.battery_level}%` : '-'}</span>
            </div>
          </div>
          <button
            className="btn btn-danger"
            onClick={handleDisconnect}
            style={{ marginTop: '16px', width: '100%' }}
          >
            Disconnect
          </button>
        </div>
      )}
    </div>
  );
};

export default ConnectionPanel;
