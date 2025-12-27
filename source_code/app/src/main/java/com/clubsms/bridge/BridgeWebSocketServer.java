package com.clubsms.bridge;

import android.os.BatteryManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * BridgeWebSocketServer - WebSocket Server Implementation
 * 
 * Handles WebSocket connections from the desktop application and
 * processes incoming commands for SMS broadcasting.
 * 
 * Protocol: ws://[PHONE_IP]:8765/clubsms
 * 
 * @author Claude (Bridge Engineer)
 * @version 2.0
 * @since 2025-12
 */
public class BridgeWebSocketServer extends WebSocketServer {
    
    private static final String TAG = "BridgeWebSocket";
    
    private final BridgeService bridgeService;
    private final CommandProcessor commandProcessor;
    private final StatusReporter statusReporter;
    
    private WebSocket currentClient = null;
    private String sessionId = null;
    private boolean isRunning = false;
    
    /**
     * Constructor
     * 
     * @param bridgeService Parent service for callbacks
     * @param port Port to listen on
     */
    public BridgeWebSocketServer(BridgeService bridgeService, int port) {
        super(new InetSocketAddress(port));
        this.bridgeService = bridgeService;
        this.commandProcessor = new CommandProcessor(bridgeService, this);
        this.statusReporter = new StatusReporter(this);
        
        // Configure server
        setReuseAddr(true);
        setConnectionLostTimeout(30); // 30 second timeout
    }
    
    @Override
    public void onStart() {
        Log.i(TAG, "WebSocket server started on port " + getPort());
        isRunning = true;
    }
    
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String clientIP = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        Log.i(TAG, "Client connecting from: " + clientIP);
        
        // Validate local network connection
        if (!isLocalConnection(clientIP)) {
            Log.w(TAG, "Rejecting non-local connection from: " + clientIP);
            conn.close(1008, "Only local network connections allowed");
            return;
        }
        
        // Allow only one client at a time
        if (currentClient != null && currentClient.isOpen()) {
            Log.w(TAG, "Rejecting additional connection - already have a client");
            conn.close(1008, "Another client is already connected");
            return;
        }
        
        // Accept connection
        currentClient = conn;
        sessionId = "session-" + UUID.randomUUID().toString().substring(0, 8);
        
        Log.i(TAG, "Desktop client connected: " + clientIP + " (session: " + sessionId + ")");
        
        // Notify service
        bridgeService.onDesktopConnected(clientIP);
        
        // Send connection confirmation
        sendConnectedMessage(conn);
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            Log.d(TAG, "Received: " + truncateForLog(message));
            
            JSONObject command = new JSONObject(message);
            String type = command.optString("type", "UNKNOWN");
            
            // Process command
            commandProcessor.processCommand(conn, command);
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing message", e);
            sendError(conn, 4000, "Failed to process command: " + e.getMessage(), null);
        }
    }
    
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String clientIP = conn.getRemoteSocketAddress() != null ? 
            conn.getRemoteSocketAddress().getAddress().getHostAddress() : "unknown";
        
        Log.i(TAG, String.format("Client disconnected: %s (code=%d, reason=%s, remote=%b)",
            clientIP, code, reason, remote));
        
        if (conn == currentClient) {
            currentClient = null;
            sessionId = null;
            bridgeService.onDesktopDisconnected();
        }
    }
    
    @Override
    public void onError(WebSocket conn, Exception ex) {
        Log.e(TAG, "WebSocket error", ex);
        
        if (conn != null && conn == currentClient) {
            currentClient = null;
            sessionId = null;
            bridgeService.onDesktopDisconnected();
        }
    }
    
    /**
     * Check if connection is from local network
     */
    private boolean isLocalConnection(String clientIP) {
        return clientIP.startsWith("192.168.") ||
               clientIP.startsWith("10.") ||
               clientIP.startsWith("172.16.") ||
               clientIP.startsWith("172.17.") ||
               clientIP.startsWith("172.18.") ||
               clientIP.startsWith("172.19.") ||
               clientIP.startsWith("172.2") ||
               clientIP.startsWith("172.30.") ||
               clientIP.startsWith("172.31.") ||
               clientIP.equals("127.0.0.1");
    }
    
    /**
     * Send CONNECTED message to client
     */
    private void sendConnectedMessage(WebSocket conn) {
        try {
            JSONObject response = new JSONObject();
            response.put("type", "CONNECTED");
            response.put("session_id", sessionId);
            response.put("timestamp", getCurrentTimestamp());
            
            // Phone info
            JSONObject phoneInfo = new JSONObject();
            phoneInfo.put("model", Build.MODEL);
            phoneInfo.put("manufacturer", Build.MANUFACTURER);
            phoneInfo.put("android_version", Build.VERSION.RELEASE);
            phoneInfo.put("app_version", "2.0.0");
            phoneInfo.put("ip_address", bridgeService.getCurrentIP());
            phoneInfo.put("battery_level", getBatteryLevel());
            
            // Try to get carrier info
            try {
                TelephonyManager tm = (TelephonyManager) bridgeService
                    .getSystemService(android.content.Context.TELEPHONY_SERVICE);
                if (tm != null) {
                    phoneInfo.put("carrier", tm.getNetworkOperatorName());
                }
            } catch (Exception e) {
                phoneInfo.put("carrier", "Unknown");
            }
            
            response.put("phone_info", phoneInfo);
            
            // Capabilities
            JSONArray capabilities = new JSONArray();
            capabilities.put("sms");
            capabilities.put("contacts");
            response.put("capabilities", capabilities);
            
            conn.send(response.toString());
            Log.d(TAG, "Sent CONNECTED response");
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending CONNECTED message", e);
        }
    }
    
    /**
     * Send error message to client
     */
    public void sendError(WebSocket conn, int errorCode, String errorMessage, String relatedMessageId) {
        try {
            JSONObject error = new JSONObject();
            error.put("type", "ERROR");
            error.put("error_code", errorCode);
            error.put("error_message", errorMessage);
            error.put("timestamp", getCurrentTimestamp());
            
            if (relatedMessageId != null) {
                error.put("related_message_id", relatedMessageId);
            }
            
            conn.send(error.toString());
            Log.d(TAG, "Sent ERROR: " + errorCode + " - " + errorMessage);
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending error message", e);
        }
    }
    
    /**
     * Send message to current client
     */
    public void sendToClient(String message) {
        if (currentClient != null && currentClient.isOpen()) {
            currentClient.send(message);
        } else {
            Log.w(TAG, "Cannot send - no client connected");
        }
    }
    
    /**
     * Send JSON object to current client
     */
    public void sendToClient(JSONObject json) {
        sendToClient(json.toString());
    }
    
    /**
     * Check if a client is connected
     */
    public boolean hasClient() {
        return currentClient != null && currentClient.isOpen();
    }
    
    /**
     * Get current client WebSocket
     */
    public WebSocket getCurrentClient() {
        return currentClient;
    }
    
    /**
     * Get status reporter for sending updates
     */
    public StatusReporter getStatusReporter() {
        return statusReporter;
    }
    
    /**
     * Check if server is running
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Get battery level
     */
    private int getBatteryLevel() {
        try {
            BatteryManager bm = (BatteryManager) bridgeService
                .getSystemService(android.content.Context.BATTERY_SERVICE);
            if (bm != null) {
                return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not get battery level", e);
        }
        return -1;
    }
    
    /**
     * Get current ISO 8601 timestamp
     */
    private String getCurrentTimestamp() {
        return java.time.Instant.now().toString();
    }
    
    /**
     * Truncate message for logging (hide sensitive data)
     */
    private String truncateForLog(String message) {
        if (message == null) return "null";
        if (message.length() <= 200) return message;
        return message.substring(0, 200) + "... (" + message.length() + " chars)";
    }
}

