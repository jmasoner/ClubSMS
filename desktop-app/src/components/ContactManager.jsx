import React, { useState, useMemo } from 'react';

const ContactManager = ({ contacts, selectedContacts, onSelectionChange, connected, onRefresh }) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [isRefreshing, setIsRefreshing] = useState(false);

  // Filter contacts based on search term
  const filteredContacts = useMemo(() => {
    const term = searchTerm.toLowerCase();
    return contacts.filter(contact =>
      contact.name.toLowerCase().includes(term) ||
      contact.phone.includes(term)
    );
  }, [contacts, searchTerm]);

  const handleContactToggle = (contactId) => {
    const newSelection = new Set(selectedContacts);
    if (newSelection.has(contactId)) {
      newSelection.delete(contactId);
    } else {
      newSelection.add(contactId);
    }
    onSelectionChange(newSelection);
  };

  const handleSelectAll = () => {
    const newSelection = new Set(filteredContacts.map(c => c.id));
    onSelectionChange(newSelection);
  };

  const handleSelectNone = () => {
    onSelectionChange(new Set());
  };

  const handleRefresh = async () => {
    if (!connected) return;

    setIsRefreshing(true);
    try {
      await onRefresh();
    } catch (error) {
      console.error('Refresh error:', error);
    }
    setIsRefreshing(false);
  };

  const getInitials = (name) => {
    return name.split(' ').map(n => n[0]).join('').substring(0, 2).toUpperCase();
  };

  const escapeHtml = (text) => {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  };

  return (
    <div className="right-panel">
      <div className="contacts-header">
        <div className="section-title">Recipients</div>
        <button
          className="btn btn-secondary"
          onClick={handleRefresh}
          disabled={!connected || isRefreshing}
          style={{ padding: '6px 12px', fontSize: '12px' }}
        >
          {isRefreshing && <div className="loading-spinner" style={{ width: '12px', height: '12px' }}></div>}
          ↻ {isRefreshing ? 'Loading...' : 'Refresh'}
        </button>
      </div>

      <input
        type="text"
        className="contact-search"
        placeholder="🔍 Search contacts..."
        value={searchTerm}
        onChange={(e) => setSearchTerm(e.target.value)}
      />

      <div style={{ display: 'flex', gap: '8px', marginBottom: '8px' }}>
        <button
          className="btn btn-secondary"
          onClick={handleSelectAll}
          style={{ flex: 1, padding: '8px', fontSize: '12px' }}
        >
          Select All
        </button>
        <button
          className="btn btn-secondary"
          onClick={handleSelectNone}
          style={{ flex: 1, padding: '8px', fontSize: '12px' }}
        >
          Select None
        </button>
      </div>

      <div className="contacts-list">
        {!connected ? (
          <div className="empty-state">
            <div className="empty-state-icon">📱</div>
            <p>Connect to phone to load contacts</p>
          </div>
        ) : filteredContacts.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">{searchTerm ? '🔍' : '👥'}</div>
            <p>
              {searchTerm
                ? 'No contacts match your search'
                : 'No contacts loaded. Try refreshing.'}
            </p>
          </div>
        ) : (
          filteredContacts.map(contact => {
            const isSelected = selectedContacts.has(contact.id);
            const initials = getInitials(contact.name);

            return (
              <div
                key={contact.id}
                className={`contact-item ${isSelected ? 'selected' : ''}`}
                onClick={() => handleContactToggle(contact.id)}
              >
                <input
                  type="checkbox"
                  className="contact-checkbox"
                  checked={isSelected}
                  onChange={() => {}} // Handled by parent div click
                />
                <div className="contact-avatar">{initials}</div>
                <div className="contact-info">
                  <div className="contact-name" dangerouslySetInnerHTML={{ __html: escapeHtml(contact.name) }}></div>
                  <div className="contact-phone">{contact.phone}</div>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};

export default ContactManager;
