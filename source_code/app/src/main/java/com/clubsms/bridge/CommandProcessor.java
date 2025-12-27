package com.clubsms.bridge;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.clubsms.ClubContactManager;
import com.clubsms.ClubMember;
import com.clubsms.MessageHistoryManager;

import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * CommandProcessor - Handles Desktop Commands
 * 
 * Processes incoming commands from the desktop application and
 * executes the appropriate actions (sending SMS, fetching contacts, etc.)
 * 
 * Supported Commands:
 * - SEND_SMS: Send SMS broadcast to recipients
 * - GET_CONTACTS: Fetch contacts from phone
 * - GET_STATUS: Get phone/bridge status
 * - PING: Keep-alive ping
 * 
 * @author Claude (Bridge Engineer)
 * @version 2.0
 * @since 2025-12
 */
public class CommandProcessor {
    
    private static final String TAG = "CommandProcessor";
    
    // Rate limiting: max 30 SMS per minute
    private static final int RATE_LIMIT_PER_MINUTE = 30;
    private static final long RATE_LIMIT_DELAY_MS = 2000; // 2 seconds between messages
    
    private final BridgeService bridgeService;
    private final BridgeWebSocketServer wsServer;
    private final Context context;
    
    // Managers from existing v1.0 code
    private ClubContactManager contactManager;
    private MessageHistoryManager historyManager;
    
    // Broadcast state
    private volatile boolean isBroadcasting = false;
    private volatile boolean cancelRequested = false;
    
    public CommandProcessor(BridgeService bridgeService, BridgeWebSocketServer wsServer) {
        this.bridgeService = bridgeService;
        this.wsServer = wsServer;
        this.context = bridgeService.getApplicationContext();
        
        // Initialize managers
        this.contactManager = new ClubContactManager(context);
        this.historyManager = new MessageHistoryManager(context);
    }
    
    /**
     * Process incoming command from desktop
     */
    public void processCommand(WebSocket conn, JSONObject command) {
        String type = command.optString("type", "UNKNOWN");
        
        Log.d(TAG, "Processing command: " + type);
        
        try {
            switch (type) {
                case "SEND_SMS":
                    handleSendSMS(conn, command);
                    break;
                    
                case "GET_CONTACTS":
                    handleGetContacts(conn, command);
                    break;
                    
                case "GET_STATUS":
                    handleGetStatus(conn, command);
                    break;
                    
                case "PING":
                    handlePing(conn, command);
                    break;
                    
                case "CANCEL_BROADCAST":
                    handleCancelBroadcast(conn, command);
                    break;
                    
                default:
                    wsServer.sendError(conn, 4000, "Unknown command type: " + type, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing command: " + type, e);
            wsServer.sendError(conn, 4000, "Command failed: " + e.getMessage(), 
                command.optString("message_id", null));
        }
    }
    
    /**
     * Handle SEND_SMS command
     */
    private void handleSendSMS(WebSocket conn, JSONObject command) throws Exception {
        String messageId = command.getString("message_id");
        JSONArray recipients = command.getJSONArray("recipients");
        String content = command.getString("content");
        
        Log.i(TAG, String.format("SEND_SMS: %d recipients, %d chars", 
            recipients.length(), content.length()));
        
        // Validate SMS permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) 
                != PackageManager.PERMISSION_GRANTED) {
            wsServer.sendError(conn, 4001, "SMS permission not granted", messageId);
            return;
        }
        
        // Validate content
        if (content == null || content.trim().isEmpty()) {
            wsServer.sendError(conn, 2002, "Message content cannot be empty", messageId);
            return;
        }
        
        if (content.length() > 1000) {
            wsServer.sendError(conn, 2002, "Message too long (max 1000 characters)", messageId);
            return;
        }
        
        // Validate recipients
        if (recipients.length() == 0) {
            wsServer.sendError(conn, 2003, "No recipients specified", messageId);
            return;
        }
        
        // Check if already broadcasting
        if (isBroadcasting) {
            wsServer.sendError(conn, 4000, "Another broadcast is in progress", messageId);
            return;
        }
        
        // Start broadcast in background thread
        new Thread(() -> {
            executeSMSBroadcast(conn, messageId, recipients, content);
        }).start();
    }
    
    /**
     * Execute SMS broadcast to all recipients
     */
    private void executeSMSBroadcast(WebSocket conn, String messageId, 
                                     JSONArray recipients, String content) {
        isBroadcasting = true;
        cancelRequested = false;
        
        SmsManager smsManager = SmsManager.getDefault();
        StatusReporter reporter = wsServer.getStatusReporter();
        
        int total = recipients.length();
        int sent = 0;
        int failed = 0;
        List<JSONObject> failedRecipients = new ArrayList<>();
        
        long startTime = System.currentTimeMillis();
        
        Log.i(TAG, "Starting broadcast to " + total + " recipients");
        
        try {
            for (int i = 0; i < total && !cancelRequested; i++) {
                JSONObject recipient = recipients.getJSONObject(i);
                String phone = recipient.getString("phone");
                String name = recipient.optString("name", "Unknown");
                String contactId = recipient.optString("contact_id", null);
                
                // Send progress update
                reporter.sendProgress(messageId, total, i, sent, failed);
                bridgeService.onSendingMessages(i + 1, total);
                
                try {
                    // Validate phone number
                    if (!isValidPhoneNumber(phone)) {
                        failed++;
                        Log.w(TAG, "Invalid phone number: " + phone);
                        
                        JSONObject failedRecip = new JSONObject();
                        failedRecip.put("phone", phone);
                        failedRecip.put("name", name);
                        failedRecip.put("error_code", 2001);
                        failedRecip.put("error_message", "Invalid phone number format");
                        failedRecipients.add(failedRecip);
                        
                        reporter.sendDeliveryStatus(messageId, phone, name, contactId, 
                            "FAILED", 2001, "Invalid phone number format");
                        continue;
                    }
                    
                    // Send SMS
                    reporter.sendDeliveryStatus(messageId, phone, name, contactId, 
                        "SENDING", null, null);
                    
                    // Handle long messages (split into parts)
                    if (content.length() > 160) {
                        ArrayList<String> parts = smsManager.divideMessage(content);
                        smsManager.sendMultipartTextMessage(phone, null, parts, null, null);
                    } else {
                        smsManager.sendTextMessage(phone, null, content, null, null);
                    }
                    
                    sent++;
                    Log.d(TAG, "SMS sent to: " + phone);
                    
                    reporter.sendDeliveryStatus(messageId, phone, name, contactId, 
                        "SENT", null, null);
                    
                    // Rate limiting delay
                    if (i < total - 1) {
                        Thread.sleep(RATE_LIMIT_DELAY_MS);
                    }
                    
                } catch (Exception e) {
                    failed++;
                    Log.e(TAG, "Failed to send to: " + phone, e);
                    
                    JSONObject failedRecip = new JSONObject();
                    failedRecip.put("phone", phone);
                    failedRecip.put("name", name);
                    failedRecip.put("error_code", 5001);
                    failedRecip.put("error_message", e.getMessage());
                    failedRecipients.add(failedRecip);
                    
                    reporter.sendDeliveryStatus(messageId, phone, name, contactId, 
                        "FAILED", 5001, e.getMessage());
                }
            }
            
            long duration = (System.currentTimeMillis() - startTime) / 1000;
            
            // Send completion message
            reporter.sendBroadcastComplete(messageId, total, sent, failed, 
                failedRecipients, (int) duration);
            
            bridgeService.onBroadcastComplete(sent, failed);
            
            // Save to history
            historyManager.addMessage(content, sent, failed);
            
            Log.i(TAG, String.format("Broadcast complete: %d sent, %d failed in %d seconds", 
                sent, failed, duration));
            
        } catch (Exception e) {
            Log.e(TAG, "Broadcast failed", e);
            wsServer.sendError(wsServer.getCurrentClient(), 4000, 
                "Broadcast failed: " + e.getMessage(), messageId);
        } finally {
            isBroadcasting = false;
            cancelRequested = false;
        }
    }
    
    /**
     * Handle GET_CONTACTS command
     */
    private void handleGetContacts(WebSocket conn, JSONObject command) throws Exception {
        String requestId = command.optString("request_id", "unknown");
        
        Log.d(TAG, "GET_CONTACTS request: " + requestId);
        
        // Get contacts from existing manager
        List<ClubMember> allMembers = contactManager.getAllMembers();
        List<ClubMember> activeMembers = contactManager.getAllActiveMembers();
        
        // Build response
        JSONObject response = new JSONObject();
        response.put("type", "CONTACTS_LIST");
        response.put("request_id", requestId);
        response.put("timestamp", java.time.Instant.now().toString());
        
        // Convert members to JSON array
        JSONArray contactsArray = new JSONArray();
        for (ClubMember member : allMembers) {
            JSONObject contact = new JSONObject();
            contact.put("id", member.getMemberId());
            contact.put("name", member.getName());
            contact.put("phone", member.getPhoneNumber());
            contact.put("active", member.isActive());
            contact.put("opted_out", member.hasOptedOut());
            contactsArray.put(contact);
        }
        
        response.put("contacts", contactsArray);
        response.put("total_count", allMembers.size());
        response.put("active_count", activeMembers.size());
        response.put("opted_out_count", allMembers.size() - activeMembers.size());
        
        conn.send(response.toString());
        Log.d(TAG, "Sent " + allMembers.size() + " contacts");
    }
    
    /**
     * Handle GET_STATUS command
     */
    private void handleGetStatus(WebSocket conn, JSONObject command) throws Exception {
        String requestId = command.optString("request_id", "unknown");
        
        Log.d(TAG, "GET_STATUS request: " + requestId);
        
        JSONObject response = new JSONObject();
        response.put("type", "PHONE_STATUS");
        response.put("request_id", requestId);
        response.put("timestamp", java.time.Instant.now().toString());
        
        response.put("bridge_running", bridgeService.isRunning());
        response.put("wifi_connected", true); // We're connected, so WiFi must be working
        response.put("cellular_connected", true);
        response.put("battery_level", getBatteryLevel());
        
        // Check SMS permission
        boolean smsPermission = ContextCompat.checkSelfPermission(context, 
            Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
        response.put("sms_permission_granted", smsPermission);
        
        // Contact count
        response.put("contacts_count", contactManager.getAllMembers().size());
        response.put("pending_messages", 0); // Not implemented yet
        response.put("is_broadcasting", isBroadcasting);
        
        conn.send(response.toString());
        Log.d(TAG, "Sent PHONE_STATUS");
    }
    
    /**
     * Handle PING command
     */
    private void handlePing(WebSocket conn, JSONObject command) throws Exception {
        JSONObject response = new JSONObject();
        response.put("type", "PONG");
        response.put("timestamp", java.time.Instant.now().toString());
        
        conn.send(response.toString());
        Log.d(TAG, "Sent PONG");
    }
    
    /**
     * Handle CANCEL_BROADCAST command
     */
    private void handleCancelBroadcast(WebSocket conn, JSONObject command) throws Exception {
        if (isBroadcasting) {
            cancelRequested = true;
            Log.i(TAG, "Broadcast cancellation requested");
            
            JSONObject response = new JSONObject();
            response.put("type", "BROADCAST_CANCELLED");
            response.put("timestamp", java.time.Instant.now().toString());
            conn.send(response.toString());
        } else {
            wsServer.sendError(conn, 4000, "No broadcast in progress", null);
        }
    }
    
    /**
     * Validate phone number format
     */
    private boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }
        
        // Remove non-digit characters except +
        String cleaned = phoneNumber.replaceAll("[^\\d+]", "");
        
        // Must have at least 10 digits
        String digitsOnly = cleaned.replaceAll("[^\\d]", "");
        return digitsOnly.length() >= 10;
    }
    
    /**
     * Get battery level
     */
    private int getBatteryLevel() {
        try {
            android.os.BatteryManager bm = (android.os.BatteryManager) context
                .getSystemService(Context.BATTERY_SERVICE);
            if (bm != null) {
                return bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not get battery level", e);
        }
        return -1;
    }
    
    /**
     * Check if currently broadcasting
     */
    public boolean isBroadcasting() {
        return isBroadcasting;
    }
}

