package com.clubsms.bridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.clubsms.MainActivity;
import com.clubsms.R;

import org.json.JSONObject;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * BridgeService - WebSocket Server for Desktop Connection
 * 
 * This foreground service runs a WebSocket server that allows the 
 * ClubSMS desktop application to connect and send commands for 
 * SMS broadcasting.
 * 
 * Features:
 * - WebSocket server on port 8765
 * - Foreground service with persistent notification
 * - Displays phone IP address for easy connection
 * - Handles connection lifecycle
 * 
 * @author Claude (Bridge Engineer)
 * @version 2.0
 * @since 2025-12
 */
public class BridgeService extends Service {
    
    private static final String TAG = "ClubSMS_Bridge";
    private static final int NOTIFICATION_ID = 2001;
    private static final String CHANNEL_ID = "clubsms_bridge";
    private static final int WEBSOCKET_PORT = 8765;
    
    // Service state
    private BridgeWebSocketServer wsServer;
    private boolean isRunning = false;
    private String currentIP = "0.0.0.0";
    private PowerManager.WakeLock wakeLock;
    private NotificationManager notificationManager;
    
    // Connection state
    private boolean desktopConnected = false;
    private String desktopIP = null;
    
    // Binder for local service binding
    private final IBinder binder = new BridgeBinder();
    
    // Opt-out receiver
    private BroadcastReceiver optOutReceiver;
    
    /**
     * Binder class for local service access
     */
    public class BridgeBinder extends Binder {
        public BridgeService getService() {
            return BridgeService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Bridge service creating...");
        
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        // Create notification channel (required for Android 8.0+)
        createNotificationChannel();
        
        // Get current IP address
        currentIP = getLocalIPAddress();
        
        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification("Initializing...", false));
        
        // Acquire wake lock to keep CPU running
        acquireWakeLock();
        
        // Register opt-out receiver
        registerOptOutReceiver();
        
        // Start WebSocket server
        startWebSocketServer();
    }
    
    /**
     * Register receiver for opt-out notifications from SmsReceiver
     */
    private void registerOptOutReceiver() {
        optOutReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String phone = intent.getStringExtra("phone_number");
                String message = intent.getStringExtra("original_message");
                Log.i(TAG, "Opt-out received: " + phone);
                
                // Send to desktop if connected
                if (wsServer != null && desktopConnected) {
                    try {
                        JSONObject optOut = new JSONObject();
                        optOut.put("type", "OPT_OUT");
                        optOut.put("phone_number", phone);
                        optOut.put("original_message", message);
                        optOut.put("timestamp", java.time.Instant.now().toString());
                        wsServer.sendToClient(optOut);
                    } catch (Exception e) {
                        Log.e(TAG, "Error sending opt-out to desktop", e);
                    }
                }
            }
        };
        
        IntentFilter filter = new IntentFilter("com.clubsms.OPT_OUT_RECEIVED");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(optOutReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(optOutReceiver, filter);
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Bridge service start command received");
        
        // Ensure WebSocket server is running
        if (!isRunning) {
            startWebSocketServer();
        }
        
        // Update notification with current IP
        updateNotification("Ready - IP: " + currentIP + ":" + WEBSOCKET_PORT, false);
        
        return START_STICKY; // Restart if killed by system
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public void onDestroy() {
        Log.i(TAG, "Bridge service destroying...");
        
        // Unregister opt-out receiver
        if (optOutReceiver != null) {
            try {
                unregisterReceiver(optOutReceiver);
            } catch (Exception e) {
                Log.w(TAG, "Error unregistering opt-out receiver", e);
            }
        }
        
        // Stop WebSocket server
        stopWebSocketServer();
        
        // Release wake lock
        releaseWakeLock();
        
        isRunning = false;
        super.onDestroy();
    }
    
    /**
     * Create notification channel for Android 8.0+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.bridge_channel_name),
                NotificationManager.IMPORTANCE_LOW // Low importance = no sound
            );
            channel.setDescription(getString(R.string.bridge_channel_description));
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    /**
     * Create the foreground service notification
     */
    private Notification createNotification(String status, boolean connected) {
        // Intent to open app when notification tapped
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.bridge_notification_title))
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_notification) // You may need to create this
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Cannot be dismissed
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE);
        
        // Show connection status
        if (connected) {
            builder.setSubText("Desktop connected");
        } else {
            builder.setSubText("IP: " + currentIP + ":" + WEBSOCKET_PORT);
        }
        
        return builder.build();
    }
    
    /**
     * Update the notification with new status
     */
    public void updateNotification(String status, boolean connected) {
        Notification notification = createNotification(status, connected);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }
    
    /**
     * Start the WebSocket server
     */
    private void startWebSocketServer() {
        if (wsServer != null && wsServer.isRunning()) {
            Log.w(TAG, "WebSocket server already running");
            return;
        }
        
        try {
            Log.i(TAG, "Starting WebSocket server on port " + WEBSOCKET_PORT);
            
            wsServer = new BridgeWebSocketServer(this, WEBSOCKET_PORT);
            wsServer.start();
            
            isRunning = true;
            Log.i(TAG, "WebSocket server started successfully");
            
            updateNotification("Ready - Waiting for desktop", false);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start WebSocket server", e);
            isRunning = false;
            updateNotification("Failed to start: " + e.getMessage(), false);
        }
    }
    
    /**
     * Stop the WebSocket server
     */
    private void stopWebSocketServer() {
        if (wsServer != null) {
            try {
                Log.i(TAG, "Stopping WebSocket server...");
                wsServer.stop(1000); // 1 second timeout
                wsServer = null;
                isRunning = false;
                Log.i(TAG, "WebSocket server stopped");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping WebSocket server", e);
            }
        }
    }
    
    /**
     * Acquire partial wake lock to keep service running
     */
    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ClubSMS::BridgeWakeLock"
        );
        wakeLock.acquire(10 * 60 * 1000L); // 10 minutes max, will re-acquire as needed
        Log.d(TAG, "Wake lock acquired");
    }
    
    /**
     * Release wake lock
     */
    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.d(TAG, "Wake lock released");
        }
    }
    
    /**
     * Get the local WiFi IP address
     */
    private String getLocalIPAddress() {
        try {
            // First, try to get WiFi IP
            WifiManager wifiManager = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
            
            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
                if (ipAddress != 0) {
                    return String.format("%d.%d.%d.%d",
                        (ipAddress & 0xff),
                        (ipAddress >> 8 & 0xff),
                        (ipAddress >> 16 & 0xff),
                        (ipAddress >> 24 & 0xff));
                }
            }
            
            // Fallback: iterate network interfaces
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                 en != null && en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
                     enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        String ip = inetAddress.getHostAddress();
                        if (ip != null && isLocalNetworkIP(ip)) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting local IP address", e);
        }
        
        return "0.0.0.0";
    }
    
    /**
     * Check if IP is on local network
     */
    private boolean isLocalNetworkIP(String ip) {
        return ip.startsWith("192.168.") ||
               ip.startsWith("10.") ||
               ip.startsWith("172.16.") ||
               ip.startsWith("172.17.") ||
               ip.startsWith("172.18.") ||
               ip.startsWith("172.19.") ||
               ip.startsWith("172.2") ||
               ip.startsWith("172.30.") ||
               ip.startsWith("172.31.");
    }
    
    // Public methods for status checking
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public boolean isDesktopConnected() {
        return desktopConnected;
    }
    
    public String getCurrentIP() {
        return currentIP;
    }
    
    public int getPort() {
        return WEBSOCKET_PORT;
    }
    
    public String getConnectionURL() {
        return "ws://" + currentIP + ":" + WEBSOCKET_PORT + "/clubsms";
    }
    
    /**
     * Called by WebSocket server when desktop connects
     */
    public void onDesktopConnected(String clientIP) {
        desktopConnected = true;
        desktopIP = clientIP;
        updateNotification("Desktop connected from " + clientIP, true);
        Log.i(TAG, "Desktop connected from: " + clientIP);
    }
    
    /**
     * Called by WebSocket server when desktop disconnects
     */
    public void onDesktopDisconnected() {
        desktopConnected = false;
        desktopIP = null;
        updateNotification("Ready - Waiting for desktop", false);
        Log.i(TAG, "Desktop disconnected");
    }
    
    /**
     * Called when sending messages
     */
    public void onSendingMessages(int current, int total) {
        updateNotification(String.format("Sending %d of %d...", current, total), true);
    }
    
    /**
     * Called when broadcast completes
     */
    public void onBroadcastComplete(int sent, int failed) {
        updateNotification(String.format("Complete: %d sent, %d failed", sent, failed), true);
    }
}

