package com.clubsms.companion;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.clubsms.MainActivity;
import com.clubsms.R;
import com.clubsms.bridge.BridgeService;

/**
 * CompanionService - Bridge Lifecycle Manager
 * 
 * This service manages the lifecycle of the BridgeService to ensure
 * reliable desktop connectivity. It handles:
 * - Starting/stopping the bridge service
 * - Monitoring bridge health
 * - Recovering from crashes
 * - Persisting desktop mode preference
 * 
 * @author Claude (Companion Service Developer)
 * @version 2.0
 * @since 2025-12
 */
public class CompanionService extends Service {
    
    private static final String TAG = "ClubSMS_Companion";
    private static final int NOTIFICATION_ID = 2002;
    private static final String CHANNEL_ID = "clubsms_companion";
    
    private static final String PREFS_NAME = "ClubSMS_Settings";
    private static final String KEY_DESKTOP_MODE_ENABLED = "desktop_mode_enabled";
    
    // Service state
    private boolean isRunning = false;
    private BridgeService bridgeService = null;
    private boolean bridgeBound = false;
    
    // Handlers and callbacks
    private Handler handler;
    private Runnable healthCheckRunnable;
    private static final long HEALTH_CHECK_INTERVAL_MS = 30000; // 30 seconds
    
    // Binder for local access
    private final IBinder binder = new CompanionBinder();
    
    public class CompanionBinder extends Binder {
        public CompanionService getService() {
            return CompanionService.this;
        }
    }
    
    /**
     * Service connection for binding to BridgeService
     */
    private final ServiceConnection bridgeConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BridgeService.BridgeBinder binder = (BridgeService.BridgeBinder) service;
            bridgeService = binder.getService();
            bridgeBound = true;
            Log.i(TAG, "Connected to BridgeService");
            
            updateNotification("Bridge active - " + bridgeService.getConnectionURL());
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            bridgeService = null;
            bridgeBound = false;
            Log.w(TAG, "Disconnected from BridgeService");
            
            // Attempt to restart bridge
            handler.postDelayed(() -> startBridgeService(), 5000);
        }
    };
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Companion service creating...");
        
        handler = new Handler(Looper.getMainLooper());
        
        // Create notification channel
        createNotificationChannel();
        
        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification("Initializing..."));
        
        // Start bridge service
        startBridgeService();
        
        // Start health checks
        startHealthChecks();
        
        isRunning = true;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Companion service start command");
        
        // Ensure bridge is running
        if (!bridgeBound) {
            startBridgeService();
        }
        
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public void onDestroy() {
        Log.i(TAG, "Companion service destroying...");
        
        // Stop health checks
        stopHealthChecks();
        
        // Unbind from bridge
        if (bridgeBound) {
            unbindService(bridgeConnection);
            bridgeBound = false;
        }
        
        // Stop bridge service
        stopBridgeService();
        
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
                "Desktop Mode",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Desktop mode status and connectivity");
            channel.setShowBadge(false);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    /**
     * Create the foreground service notification
     */
    private Notification createNotification(String status) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ClubSMS Desktop Mode")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build();
    }
    
    /**
     * Update notification with new status
     */
    private void updateNotification(String status) {
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, createNotification(status));
    }
    
    /**
     * Start the BridgeService
     */
    private void startBridgeService() {
        Log.i(TAG, "Starting BridgeService...");
        
        Intent bridgeIntent = new Intent(this, BridgeService.class);
        
        // Start as foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(bridgeIntent);
        } else {
            startService(bridgeIntent);
        }
        
        // Bind to get reference
        bindService(bridgeIntent, bridgeConnection, Context.BIND_AUTO_CREATE);
    }
    
    /**
     * Stop the BridgeService
     */
    private void stopBridgeService() {
        Log.i(TAG, "Stopping BridgeService...");
        
        Intent bridgeIntent = new Intent(this, BridgeService.class);
        stopService(bridgeIntent);
    }
    
    /**
     * Start periodic health checks
     */
    private void startHealthChecks() {
        healthCheckRunnable = new Runnable() {
            @Override
            public void run() {
                performHealthCheck();
                handler.postDelayed(this, HEALTH_CHECK_INTERVAL_MS);
            }
        };
        
        handler.postDelayed(healthCheckRunnable, HEALTH_CHECK_INTERVAL_MS);
        Log.d(TAG, "Health checks started");
    }
    
    /**
     * Stop periodic health checks
     */
    private void stopHealthChecks() {
        if (healthCheckRunnable != null) {
            handler.removeCallbacks(healthCheckRunnable);
            healthCheckRunnable = null;
        }
        Log.d(TAG, "Health checks stopped");
    }
    
    /**
     * Perform health check on bridge service
     */
    private void performHealthCheck() {
        Log.d(TAG, "Performing health check...");
        
        if (!bridgeBound || bridgeService == null) {
            Log.w(TAG, "Bridge not bound, attempting restart...");
            startBridgeService();
            return;
        }
        
        if (!bridgeService.isRunning()) {
            Log.w(TAG, "Bridge not running, attempting restart...");
            stopBridgeService();
            handler.postDelayed(() -> startBridgeService(), 2000);
            return;
        }
        
        // Update notification with current status
        String status;
        if (bridgeService.isDesktopConnected()) {
            status = "Desktop connected";
        } else {
            status = "Ready - " + bridgeService.getConnectionURL();
        }
        updateNotification(status);
        
        Log.d(TAG, "Health check passed: " + status);
    }
    
    // Public methods for external access
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public boolean isBridgeRunning() {
        return bridgeBound && bridgeService != null && bridgeService.isRunning();
    }
    
    public boolean isDesktopConnected() {
        return bridgeBound && bridgeService != null && bridgeService.isDesktopConnected();
    }
    
    public String getConnectionURL() {
        if (bridgeBound && bridgeService != null) {
            return bridgeService.getConnectionURL();
        }
        return null;
    }
    
    // Static helper methods for managing desktop mode
    
    /**
     * Check if desktop mode is enabled in preferences
     */
    public static boolean isDesktopModeEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_DESKTOP_MODE_ENABLED, false);
    }
    
    /**
     * Enable desktop mode and start services
     */
    public static void enableDesktopMode(Context context) {
        // Save preference
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_DESKTOP_MODE_ENABLED, true).apply();
        
        // Start companion service
        Intent intent = new Intent(context, CompanionService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
        
        Log.i(TAG, "Desktop mode enabled");
    }
    
    /**
     * Disable desktop mode and stop services
     */
    public static void disableDesktopMode(Context context) {
        // Save preference
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_DESKTOP_MODE_ENABLED, false).apply();
        
        // Stop companion service (which will stop bridge)
        Intent intent = new Intent(context, CompanionService.class);
        context.stopService(intent);
        
        // Also stop bridge directly
        Intent bridgeIntent = new Intent(context, BridgeService.class);
        context.stopService(bridgeIntent);
        
        Log.i(TAG, "Desktop mode disabled");
    }
}

