package com.clubsms.companion;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * BootCompleteReceiver - Auto-Start on Device Boot
 * 
 * Automatically starts the ClubSMS bridge service when the device
 * boots up, if desktop mode was previously enabled.
 * 
 * Also handles app updates (MY_PACKAGE_REPLACED) to restart the service.
 * 
 * @author Claude (Companion Service Developer)
 * @version 2.0
 * @since 2025-12
 */
public class BootCompleteReceiver extends BroadcastReceiver {
    
    private static final String TAG = "BootCompleteReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (action == null) return;
        
        Log.i(TAG, "Received broadcast: " + action);
        
        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED:
                handleBootCompleted(context);
                break;
                
            case Intent.ACTION_MY_PACKAGE_REPLACED:
                handlePackageReplaced(context);
                break;
        }
    }
    
    /**
     * Handle device boot completed
     */
    private void handleBootCompleted(Context context) {
        Log.i(TAG, "Device boot completed");
        
        // Check if desktop mode was enabled before boot
        if (CompanionService.isDesktopModeEnabled(context)) {
            Log.i(TAG, "Desktop mode is enabled - starting companion service");
            startCompanionService(context);
        } else {
            Log.d(TAG, "Desktop mode not enabled - not starting service");
        }
    }
    
    /**
     * Handle app package replaced (updated)
     */
    private void handlePackageReplaced(Context context) {
        Log.i(TAG, "App package replaced (updated)");
        
        // Restart service if desktop mode was enabled
        if (CompanionService.isDesktopModeEnabled(context)) {
            Log.i(TAG, "Restarting companion service after app update");
            startCompanionService(context);
        }
    }
    
    /**
     * Start the companion service
     */
    private void startCompanionService(Context context) {
        try {
            Intent serviceIntent = new Intent(context, CompanionService.class);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            
            Log.i(TAG, "Companion service started successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start companion service", e);
        }
    }
}

