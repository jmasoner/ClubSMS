/**
 * ClubSMS Desktop App - Preload Script
 * 
 * Exposes safe APIs to the renderer process.
 * Acts as a bridge between Electron and React.
 */

const { contextBridge, ipcRenderer } = require('electron');

// Expose protected methods to renderer
contextBridge.exposeInMainWorld('clubsms', {
    // Connection
    connect: (phoneIP) => ipcRenderer.invoke('connect-to-phone', phoneIP),
    disconnect: () => ipcRenderer.invoke('disconnect-from-phone'),
    getConnectionStatus: () => ipcRenderer.invoke('get-connection-status'),
    
    // SMS
    sendSMS: (recipients, content) => ipcRenderer.invoke('send-sms', { recipients, content }),
    
    // Contacts
    getContacts: () => ipcRenderer.invoke('get-contacts'),
    
    // Status
    getPhoneStatus: () => ipcRenderer.invoke('get-phone-status'),
    ping: () => ipcRenderer.invoke('ping-phone'),
    
    // Event listeners
    onPhoneConnected: (callback) => {
        ipcRenderer.on('phone-connected', (event, data) => callback(data));
    },
    onConnectionStatus: (callback) => {
        ipcRenderer.on('connection-status', (event, data) => callback(data));
    },
    onBroadcastProgress: (callback) => {
        ipcRenderer.on('broadcast-progress', (event, data) => callback(data));
    },
    onDeliveryStatus: (callback) => {
        ipcRenderer.on('delivery-status', (event, data) => callback(data));
    },
    onBroadcastComplete: (callback) => {
        ipcRenderer.on('broadcast-complete', (event, data) => callback(data));
    },
    onContactsList: (callback) => {
        ipcRenderer.on('contacts-list', (event, data) => callback(data));
    },
    onPhoneStatus: (callback) => {
        ipcRenderer.on('phone-status', (event, data) => callback(data));
    },
    onError: (callback) => {
        ipcRenderer.on('error', (event, data) => callback(data));
    },
    onOptOut: (callback) => {
        ipcRenderer.on('opt-out', (event, data) => callback(data));
    },
    
    // Remove listeners
    removeAllListeners: () => {
        ipcRenderer.removeAllListeners('phone-connected');
        ipcRenderer.removeAllListeners('connection-status');
        ipcRenderer.removeAllListeners('broadcast-progress');
        ipcRenderer.removeAllListeners('delivery-status');
        ipcRenderer.removeAllListeners('broadcast-complete');
        ipcRenderer.removeAllListeners('contacts-list');
        ipcRenderer.removeAllListeners('phone-status');
        ipcRenderer.removeAllListeners('error');
        ipcRenderer.removeAllListeners('opt-out');
    }
});

