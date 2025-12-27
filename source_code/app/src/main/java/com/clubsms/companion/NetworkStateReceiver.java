package com.clubsms.companion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

/**
 * NetworkStateReceiver - WiFi Connectivity Monitor
 * 
 * Monitors WiFi connectivity changes and manages the bridge service
 * accordingly. When WiFi is available, ensures the bridge is running.
 * When WiFi is lost, the bridge pauses (desktop can't connect anyway).
 * 
 * @author Claude (Companion Service Developer)
 * @version 2.0
 * @since 2025-12
 */
public class NetworkStateReceiver extends BroadcastReceiver {
    
    private static final String TAG = "NetworkStateReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (action == null) return;
        
        Log.d(TAG, "Network state change: " + action);
        
        // Only act if desktop mode is enabled
        if (!CompanionService.isDesktopModeEnabled(context)) {
            Log.d(TAG, "Desktop mode not enabled, ignoring network change");
            return;
        }
        
        switch (action) {
            case ConnectivityManager.CONNECTIVITY_ACTION:
                handleConnectivityChange(context, intent);
                break;
                
            case WifiManager.WIFI_STATE_CHANGED_ACTION:
                handleWifiStateChange(context, intent);
                break;
        }
    }
    
    /**
     * Handle connectivity changes
     */
    private void handleConnectivityChange(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager) 
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (cm == null) return;
        
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        
        if (activeNetwork != null && activeNetwork.isConnected()) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                Log.i(TAG, "WiFi connected - ensuring bridge service is running");
                ensureBridgeRunning(context);
            } else {
                Log.w(TAG, "Non-WiFi connection (mobile data) - bridge may not be accessible");
                // Still keep service running, but it won't be accessible from desktop
            }
        } else {
            Log.w(TAG, "No network connection");
            // Keep service running - it will recover when network returns
        }
    }
    
    /**
     * Handle WiFi state changes
     */
    private void handleWifiStateChange(Context context, Intent intent) {
        int wifiState = intent.getIntExtra(
            WifiManager.EXTRA_WIFI_STATE, 
            WifiManager.WIFI_STATE_UNKNOWN
        );
        
        switch (wifiState) {
            case WifiManager.WIFI_STATE_ENABLED:
                Log.i(TAG, "WiFi enabled");
                // Wait a bit for connection to establish
                new android.os.Handler(android.os.Looper.getMainLooper())
                    .postDelayed(() -> ensureBridgeRunning(context), 3000);
                break;
                
            case WifiManager.WIFI_STATE_DISABLED:
                Log.w(TAG, "WiFi disabled - desktop connection not possible");
                // Keep service running for when WiFi is re-enabled
                break;
                
            case WifiManager.WIFI_STATE_DISABLING:
                Log.d(TAG, "WiFi disabling...");
                break;
                
            case WifiManager.WIFI_STATE_ENABLING:
                Log.d(TAG, "WiFi enabling...");
                break;
        }
    }
    
    /**
     * Ensure the bridge service is running
     */
    private void ensureBridgeRunning(Context context) {
        if (CompanionService.isDesktopModeEnabled(context)) {
            Intent intent = new Intent(context, CompanionService.class);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
            
            Log.i(TAG, "Companion service start requested");
        }
    }
    
    /**
     * Check if WiFi is currently connected
     */
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) 
            context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (cm == null) return false;
        
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && 
               activeNetwork.isConnected() && 
               activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
    }
}

