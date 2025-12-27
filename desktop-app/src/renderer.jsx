import React, { useState, useEffect } from 'react';
import { createRoot } from 'react-dom/client';
import ConnectionPanel from './components/ConnectionPanel';
import MessageComposer from './components/MessageComposer';
import ContactManager from './components/ContactManager';
import ProgressPanel from './components/ProgressPanel';
import StatusArea from './components/StatusArea';
import './styles/globals.css';

function App() {
  const [connected, setConnected] = useState(false);
  const [phoneInfo, setPhoneInfo] = useState(null);
  const [contacts, setContacts] = useState([]);
  const [selectedContacts, setSelectedContacts] = useState(new Set());
  const [isSending, setIsSending] = useState(false);
  const [currentBroadcast, setCurrentBroadcast] = useState(null);

  // Set up Electron event listeners
  useEffect(() => {
    // Connection events
    window.clubsms.onPhoneConnected((data) => {
      setConnected(true);
      setPhoneInfo(data.phone_info);
      // Auto-load contacts when connected
      window.clubsms.getContacts();
    });

    window.clubsms.onConnectionStatus((data) => {
      if (!data.connected) {
        setConnected(false);
        setPhoneInfo(null);
        setContacts([]);
        setSelectedContacts(new Set());
      }
    });

    // Contact events
    window.clubsms.onContactsList((data) => {
      setContacts(data.contacts || []);
    });

    // Broadcast events
    window.clubsms.onBroadcastProgress((data) => {
      setCurrentBroadcast(data);
    });

    window.clubsms.onBroadcastComplete((data) => {
      setIsSending(false);
      setCurrentBroadcast(null);
    });

    window.clubsms.onError((data) => {
      // Error handling will be done in StatusArea component
    });

    // Cleanup on unmount
    return () => {
      window.clubsms.removeAllListeners();
    };
  }, []);

  const handleSendBroadcast = async (message) => {
    if (isSending) return;

    const recipients = contacts
      .filter(c => selectedContacts.has(c.id))
      .map(c => ({
        phone: c.phone,
        name: c.name,
        contact_id: c.id
      }));

    if (!message || recipients.length === 0) return;

    setIsSending(true);

    try {
      const result = await window.clubsms.sendSMS(recipients, message);
      if (!result.success) {
        setIsSending(false);
      }
    } catch (error) {
      setIsSending(false);
    }
  };

  return (
    <div className="app">
      <header className="header">
        <div className="header-left">
          <div className="logo">ClubSMS</div>
          <ConnectionPanel connected={connected} phoneInfo={phoneInfo} />
        </div>
      </header>

      <div className="main-content">
        {/* Left Sidebar - Connection */}
        <div className="sidebar">
          <ConnectionPanel
            connected={connected}
            phoneInfo={phoneInfo}
            onConnect={() => window.clubsms.getContacts()}
          />
        </div>

        {/* Center - Message Composer */}
        <div className="center-panel">
          <MessageComposer
            connected={connected}
            selectedCount={selectedContacts.size}
            onSend={handleSendBroadcast}
            disabled={isSending}
          />

          {isSending && currentBroadcast && (
            <ProgressPanel broadcast={currentBroadcast} />
          )}

          <StatusArea />
        </div>

        {/* Right Panel - Contacts */}
        <ContactManager
          contacts={contacts}
          selectedContacts={selectedContacts}
          onSelectionChange={setSelectedContacts}
          connected={connected}
          onRefresh={() => window.clubsms.getContacts()}
        />
      </div>
    </div>
  );
}

// Render the app
const container = document.getElementById('app');
const root = createRoot(container);
root.render(<App />);
