import React, { useState, useEffect } from 'react';

const StatusArea = () => {
  const [messages, setMessages] = useState([]);

  useEffect(() => {
    // Listen for error events from Electron
    window.clubsms.onError((data) => {
      addMessage('Error: ' + data.error_message, 'error');
    });

    // Listen for broadcast complete events
    window.clubsms.onBroadcastComplete((data) => {
      const { final_statistics } = data;
      const { sent, failed } = final_statistics;
      const type = failed > 0 ? 'error' : 'success';
      const text = `Broadcast complete! Sent: ${sent}, Failed: ${failed}`;
      addMessage(text, type);
    });

    // Cleanup on unmount
    return () => {
      // Event listeners are managed by the main app
    };
  }, []);

  const addMessage = (text, type = 'info') => {
    const message = {
      id: Date.now() + Math.random(),
      text,
      type,
      timestamp: new Date()
    };

    setMessages(prev => [message, ...prev].slice(0, 5)); // Keep only last 5

    // Auto-remove after 10 seconds
    setTimeout(() => {
      setMessages(prev => prev.filter(m => m.id !== message.id));
    }, 10000);
  };

  const getMessageIcon = (type) => {
    switch (type) {
      case 'success': return '✓';
      case 'error': return '✗';
      default: return 'ℹ';
    }
  };

  const escapeHtml = (text) => {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  };

  return (
    <div className="status-area">
      {messages.map(message => (
        <div key={message.id} className={`status-message ${message.type}`}>
          <span>{getMessageIcon(message.type)}</span>
          <span dangerouslySetInnerHTML={{ __html: escapeHtml(message.text) }}></span>
        </div>
      ))}
    </div>
  );
};

export default StatusArea;
