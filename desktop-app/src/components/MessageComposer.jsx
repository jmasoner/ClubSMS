import React, { useState } from 'react';

const MessageComposer = ({ connected, selectedCount, onSend, disabled }) => {
  const [message, setMessage] = useState('');
  const [isSending, setIsSending] = useState(false);

  const handleSend = async () => {
    if (!message.trim() || disabled || isSending) return;

    setIsSending(true);
    try {
      await onSend(message.trim());
      setMessage(''); // Clear message after sending
    } catch (error) {
      console.error('Send error:', error);
    }
    setIsSending(false);
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && e.ctrlKey) {
      handleSend();
    }
  };

  return (
    <div className="composer-card">
      <div className="composer-header">
        <span className="composer-title">📝 Compose Message</span>
        <span className="char-count">
          <span id="char-count">{message.length}</span>/1000
        </span>
      </div>

      <textarea
        className="message-textarea"
        value={message}
        onChange={(e) => setMessage(e.target.value)}
        onKeyPress={handleKeyPress}
        placeholder={`Type your message here...

Include URLs for clickable links. Example:
Join us tonight! Details: https://yourclub.com/event`}
        maxLength="1000"
        disabled={disabled || !connected}
      />

      <div className="composer-footer">
        <span className="recipient-count">
          Sending to <strong>{selectedCount}</strong> {selectedCount === 1 ? 'recipient' : 'recipients'}
        </span>
        <button
          className="btn btn-success"
          onClick={handleSend}
          disabled={disabled || !connected || !message.trim() || selectedCount === 0 || isSending}
        >
          {isSending && <div className="loading-spinner"></div>}
          🚀 {isSending ? 'Sending...' : 'Send Broadcast'}
        </button>
      </div>
    </div>
  );
};

export default MessageComposer;
