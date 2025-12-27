package com.clubsms.bridge;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * StatusReporter - Send Status Updates to Desktop
 * 
 * Handles sending progress updates, delivery status, and 
 * broadcast completion messages to the connected desktop client.
 * 
 * Message Types:
 * - PROGRESS: Periodic broadcast progress updates
 * - DELIVERY_STATUS: Individual recipient status
 * - BROADCAST_COMPLETE: Final summary when broadcast finishes
 * 
 * @author Claude (Bridge Engineer)
 * @version 2.0
 * @since 2025-12
 */
public class StatusReporter {
    
    private static final String TAG = "StatusReporter";
    
    private final BridgeWebSocketServer wsServer;
    
    // Progress update throttling
    private static final long MIN_PROGRESS_INTERVAL_MS = 500; // Max 2 updates per second
    private long lastProgressTime = 0;
    
    public StatusReporter(BridgeWebSocketServer wsServer) {
        this.wsServer = wsServer;
    }
    
    /**
     * Send broadcast progress update
     * 
     * @param messageId Broadcast message ID
     * @param total Total number of recipients
     * @param current Current recipient being processed (0-based index)
     * @param sent Number successfully sent
     * @param failed Number failed
     */
    public void sendProgress(String messageId, int total, int current, int sent, int failed) {
        // Throttle progress updates
        long now = System.currentTimeMillis();
        if (now - lastProgressTime < MIN_PROGRESS_INTERVAL_MS && current < total - 1) {
            return; // Skip this update (not the last one)
        }
        lastProgressTime = now;
        
        try {
            JSONObject progress = new JSONObject();
            progress.put("type", "PROGRESS");
            progress.put("message_id", messageId);
            progress.put("timestamp", java.time.Instant.now().toString());
            
            // Statistics
            JSONObject statistics = new JSONObject();
            statistics.put("total", total);
            statistics.put("queued", total - current - 1);
            statistics.put("sending", 1);
            statistics.put("sent", sent);
            statistics.put("failed", failed);
            progress.put("statistics", statistics);
            
            // Progress percentage
            int percentComplete = total > 0 ? (int) ((current + 1) * 100.0 / total) : 0;
            progress.put("percent_complete", percentComplete);
            
            // Estimated time remaining (rough calculation)
            if (current > 0) {
                long elapsed = now - (lastProgressTime - MIN_PROGRESS_INTERVAL_MS);
                int remaining = total - current - 1;
                int estimatedSeconds = (int) ((elapsed / (current + 1)) * remaining / 1000);
                progress.put("estimated_remaining_seconds", estimatedSeconds);
            }
            
            wsServer.sendToClient(progress);
            Log.d(TAG, String.format("Progress: %d/%d (%d%%)", current + 1, total, percentComplete));
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending progress", e);
        }
    }
    
    /**
     * Send individual delivery status for a recipient
     * 
     * @param messageId Broadcast message ID
     * @param phone Recipient phone number
     * @param name Recipient name
     * @param contactId Contact ID (may be null)
     * @param status Status: PENDING, SENDING, SENT, FAILED
     * @param errorCode Error code (null if no error)
     * @param errorMessage Error message (null if no error)
     */
    public void sendDeliveryStatus(String messageId, String phone, String name, 
                                   String contactId, String status, 
                                   Integer errorCode, String errorMessage) {
        try {
            JSONObject delivery = new JSONObject();
            delivery.put("type", "DELIVERY_STATUS");
            delivery.put("message_id", messageId);
            delivery.put("timestamp", java.time.Instant.now().toString());
            
            // Recipient info
            JSONObject recipient = new JSONObject();
            recipient.put("phone", phone);
            recipient.put("name", name);
            if (contactId != null) {
                recipient.put("contact_id", contactId);
            }
            delivery.put("recipient", recipient);
            
            delivery.put("status", status);
            
            if (errorCode != null) {
                delivery.put("error_code", errorCode);
            }
            if (errorMessage != null) {
                delivery.put("error_message", errorMessage);
            }
            
            wsServer.sendToClient(delivery);
            Log.d(TAG, String.format("Delivery status: %s -> %s", phone, status));
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending delivery status", e);
        }
    }
    
    /**
     * Send broadcast completion summary
     * 
     * @param messageId Broadcast message ID
     * @param total Total recipients
     * @param sent Successfully sent count
     * @param failed Failed count
     * @param failedRecipients List of failed recipient details
     * @param durationSeconds Total broadcast duration
     */
    public void sendBroadcastComplete(String messageId, int total, int sent, int failed,
                                      List<JSONObject> failedRecipients, int durationSeconds) {
        try {
            JSONObject complete = new JSONObject();
            complete.put("type", "BROADCAST_COMPLETE");
            complete.put("message_id", messageId);
            complete.put("timestamp", java.time.Instant.now().toString());
            
            // Final statistics
            JSONObject finalStats = new JSONObject();
            finalStats.put("total", total);
            finalStats.put("sent", sent);
            finalStats.put("failed", failed);
            complete.put("final_statistics", finalStats);
            
            // Failed recipients list
            JSONArray failedArray = new JSONArray();
            if (failedRecipients != null) {
                for (JSONObject failedRecip : failedRecipients) {
                    failedArray.put(failedRecip);
                }
            }
            complete.put("failed_recipients", failedArray);
            
            complete.put("duration_seconds", durationSeconds);
            
            wsServer.sendToClient(complete);
            Log.i(TAG, String.format("Broadcast complete: %d sent, %d failed in %ds", 
                sent, failed, durationSeconds));
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending broadcast complete", e);
        }
    }
    
    /**
     * Send a generic status message
     */
    public void sendStatus(String statusType, String message) {
        try {
            JSONObject status = new JSONObject();
            status.put("type", statusType);
            status.put("message", message);
            status.put("timestamp", java.time.Instant.now().toString());
            
            wsServer.sendToClient(status);
            Log.d(TAG, "Status sent: " + statusType + " - " + message);
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending status", e);
        }
    }
}

