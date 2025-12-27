/**
 * ClubSMS Desktop App - Main Electron Process
 * 
 * This is the main entry point for the Electron app.
 * Handles window creation, IPC communication, and WebSocket bridge.
 * 
 * @author Claude
 * @version 2.0
 */

const { app, BrowserWindow, ipcMain, Notification } = require('electron');
const path = require('path');
const WebSocket = require('ws');

// Keep a global reference of the window object
let mainWindow = null;
let wsConnection = null;
let isConnected = false;

/**
 * Create the main application window
 */
function createWindow() {
    mainWindow = new BrowserWindow({
        width: 1200,
        height: 800,
        minWidth: 900,
        minHeight: 600,
        title: 'ClubSMS Desktop',
        backgroundColor: '#0a0a0f',
        webPreferences: {
            nodeIntegration: false,
            contextIsolation: true,
            preload: path.join(__dirname, 'preload.js')
        }
    });

    // Load the app
    mainWindow.loadFile('index.html');

    // Open DevTools in development
    if (process.argv.includes('--dev')) {
        mainWindow.webContents.openDevTools();
    }

    mainWindow.on('closed', () => {
        mainWindow = null;
        disconnectFromPhone();
    });
}

// App ready
app.whenReady().then(createWindow);

// Quit when all windows are closed (except on macOS)
app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') {
        app.quit();
    }
});

app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
        createWindow();
    }
});

// ==================== WebSocket Connection ====================

/**
 * Connect to phone's WebSocket server
 */
function connectToPhone(phoneIP) {
    return new Promise((resolve, reject) => {
        // Clean up input - extract just the IP address
        let cleanIP = phoneIP.trim();
        cleanIP = cleanIP.replace(/^ws:\/\//, '');  // Remove ws:// prefix
        cleanIP = cleanIP.replace(/^wss:\/\//, ''); // Remove wss:// prefix
        cleanIP = cleanIP.replace(/:8765.*$/, '');  // Remove port and path
        cleanIP = cleanIP.replace(/\/.*$/, '');     // Remove any path
        
        const url = `ws://${cleanIP}:8765`;
        console.log('Connecting to:', url);

        try {
            wsConnection = new WebSocket(url);

            wsConnection.on('open', () => {
                console.log('Connected to phone!');
                isConnected = true;
                resolve({ success: true });
            });

            wsConnection.on('message', (data) => {
                try {
                    const message = JSON.parse(data.toString());
                    handlePhoneMessage(message);
                } catch (e) {
                    console.error('Failed to parse message:', e);
                }
            });

            wsConnection.on('close', (code, reason) => {
                console.log('Disconnected from phone:', code, reason);
                isConnected = false;
                sendToRenderer('connection-status', { connected: false, reason: reason.toString() });
            });

            wsConnection.on('error', (error) => {
                console.error('WebSocket error:', error.message);
                isConnected = false;
                reject({ success: false, error: error.message });
            });

            // Connection timeout
            setTimeout(() => {
                if (!isConnected) {
                    wsConnection.close();
                    reject({ success: false, error: 'Connection timeout' });
                }
            }, 10000);

        } catch (error) {
            reject({ success: false, error: error.message });
        }
    });
}

/**
 * Disconnect from phone
 */
function disconnectFromPhone() {
    if (wsConnection) {
        wsConnection.close();
        wsConnection = null;
    }
    isConnected = false;
}

/**
 * Send message to phone
 */
function sendToPhone(message) {
    if (wsConnection && isConnected) {
        wsConnection.send(JSON.stringify(message));
        return true;
    }
    return false;
}

/**
 * Handle incoming messages from phone
 */
function handlePhoneMessage(message) {
    console.log('Phone message:', message.type);
    
    switch (message.type) {
        case 'CONNECTED':
            sendToRenderer('phone-connected', message);
            showNotification('Phone Connected', `Connected to ${message.phone_info?.model || 'Phone'}`);
            break;

        case 'PROGRESS':
            sendToRenderer('broadcast-progress', message);
            break;

        case 'DELIVERY_STATUS':
            sendToRenderer('delivery-status', message);
            break;

        case 'BROADCAST_COMPLETE':
            sendToRenderer('broadcast-complete', message);
            showNotification('Broadcast Complete', 
                `Sent: ${message.final_statistics.sent}, Failed: ${message.final_statistics.failed}`);
            break;

        case 'CONTACTS_LIST':
            sendToRenderer('contacts-list', message);
            break;

        case 'PHONE_STATUS':
            sendToRenderer('phone-status', message);
            break;

        case 'PONG':
            sendToRenderer('pong', message);
            break;

        case 'ERROR':
            sendToRenderer('error', message);
            showNotification('Error', message.error_message);
            break;

        case 'OPT_OUT':
            sendToRenderer('opt-out', message);
            showNotification('Opt-Out Received', `${message.phone_number} replied STOP`);
            break;

        default:
            sendToRenderer('unknown-message', message);
    }
}

/**
 * Send message to renderer process
 */
function sendToRenderer(channel, data) {
    if (mainWindow && mainWindow.webContents) {
        mainWindow.webContents.send(channel, data);
    }
}

/**
 * Show system notification
 */
function showNotification(title, body) {
    if (Notification.isSupported()) {
        new Notification({ title, body }).show();
    }
}

// ==================== IPC Handlers ====================

// Connect to phone
ipcMain.handle('connect-to-phone', async (event, phoneIP) => {
    try {
        const result = await connectToPhone(phoneIP);
        return result;
    } catch (error) {
        return error;
    }
});

// Disconnect from phone
ipcMain.handle('disconnect-from-phone', async () => {
    disconnectFromPhone();
    return { success: true };
});

// Check connection status
ipcMain.handle('get-connection-status', () => {
    return { connected: isConnected };
});

// Send SMS broadcast
ipcMain.handle('send-sms', async (event, data) => {
    const { recipients, content } = data;
    const messageId = `msg-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    
    const command = {
        type: 'SEND_SMS',
        message_id: messageId,
        recipients: recipients,
        content: content,
        timestamp: new Date().toISOString()
    };
    
    const sent = sendToPhone(command);
    return { success: sent, message_id: messageId };
});

// Get contacts from phone
ipcMain.handle('get-contacts', async () => {
    const command = {
        type: 'GET_CONTACTS',
        request_id: `req-${Date.now()}`
    };
    
    const sent = sendToPhone(command);
    return { success: sent };
});

// Get phone status
ipcMain.handle('get-phone-status', async () => {
    const command = {
        type: 'GET_STATUS',
        request_id: `req-${Date.now()}`
    };
    
    const sent = sendToPhone(command);
    return { success: sent };
});

// Ping phone
ipcMain.handle('ping-phone', async () => {
    const command = {
        type: 'PING',
        timestamp: new Date().toISOString()
    };
    
    const sent = sendToPhone(command);
    return { success: sent };
});

