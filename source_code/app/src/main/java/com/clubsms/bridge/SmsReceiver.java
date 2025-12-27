package com.clubsms.bridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.clubsms.ClubContactManager;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * SmsReceiver - Listens for incoming SMS messages
 * 
 * Detects opt-out keywords (STOP, UNSUBSCRIBE, etc.) and automatically
 * marks the sender as opted-out in ClubSMS.
 * 
 * @author Claude (Bridge Engineer)
 * @version 2.0
 * @since 2025-12
 */
public class SmsReceiver extends BroadcastReceiver {
    
    private static final String TAG = "SmsReceiver";
    
    // Opt-out keywords (case insensitive)
    private static final Set<String> OPT_OUT_KEYWORDS = new HashSet<>();
    static {
        OPT_OUT_KEYWORDS.add("stop");
        OPT_OUT_KEYWORDS.add("unsubscribe");
        OPT_OUT_KEYWORDS.add("cancel");
        OPT_OUT_KEYWORDS.add("quit");
        OPT_OUT_KEYWORDS.add("end");
        OPT_OUT_KEYWORDS.add("optout");
        OPT_OUT_KEYWORDS.add("opt-out");
        OPT_OUT_KEYWORDS.add("opt out");
        OPT_OUT_KEYWORDS.add("remove");
        OPT_OUT_KEYWORDS.add("take me off");
    }
    
    // Pattern to match opt-out at start of message or as entire message
    private static final Pattern OPT_OUT_PATTERN = Pattern.compile(
        "^\\s*(stop|unsubscribe|cancel|quit|end|optout|opt-out|opt out|remove)\\s*$",
        Pattern.CASE_INSENSITIVE
    );
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        
        if (!intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            return;
        }
        
        Bundle bundle = intent.getExtras();
        if (bundle == null) return;
        
        try {
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus == null || pdus.length == 0) return;
            
            String format = bundle.getString("format");
            
            for (Object pdu : pdus) {
                SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu, format);
                if (sms == null) continue;
                
                String sender = sms.getOriginatingAddress();
                String body = sms.getMessageBody();
                
                if (sender == null || body == null) continue;
                
                Log.d(TAG, "SMS received from: " + sender);
                
                // Check for opt-out keywords
                if (isOptOutMessage(body)) {
                    Log.i(TAG, "Opt-out detected from: " + sender);
                    handleOptOut(context, sender, body);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing incoming SMS", e);
        }
    }
    
    /**
     * Check if message contains opt-out keywords
     */
    private boolean isOptOutMessage(String message) {
        if (message == null) return false;
        
        String trimmed = message.trim().toLowerCase();
        
        // Check if entire message is just an opt-out keyword
        if (OPT_OUT_KEYWORDS.contains(trimmed)) {
            return true;
        }
        
        // Check pattern match (keyword at start)
        if (OPT_OUT_PATTERN.matcher(trimmed).matches()) {
            return true;
        }
        
        // Check if message starts with opt-out keyword
        for (String keyword : OPT_OUT_KEYWORDS) {
            if (trimmed.equals(keyword) || trimmed.startsWith(keyword + " ") || 
                trimmed.startsWith(keyword + ".") || trimmed.startsWith(keyword + "!")) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Handle opt-out request
     */
    private void handleOptOut(Context context, String phoneNumber, String message) {
        try {
            // Normalize phone number
            String normalizedPhone = normalizePhoneNumber(phoneNumber);
            
            // Mark as opted out in contact manager
            ClubContactManager contactManager = new ClubContactManager(context);
            boolean found = contactManager.markAsOptedOut(normalizedPhone);
            
            if (found) {
                Log.i(TAG, "Contact marked as opted out: " + normalizedPhone);
            } else {
                Log.i(TAG, "Opt-out from unknown number: " + normalizedPhone);
            }
            
            // Notify the bridge service (if running) to inform desktop
            Intent optOutIntent = new Intent("com.clubsms.OPT_OUT_RECEIVED");
            optOutIntent.putExtra("phone_number", normalizedPhone);
            optOutIntent.putExtra("original_message", message);
            optOutIntent.setPackage(context.getPackageName());
            context.sendBroadcast(optOutIntent);
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling opt-out", e);
        }
    }
    
    /**
     * Normalize phone number for comparison
     */
    private String normalizePhoneNumber(String phone) {
        if (phone == null) return "";
        
        // Remove all non-digit characters except leading +
        String normalized = phone.replaceAll("[^0-9+]", "");
        
        // Handle US numbers
        if (normalized.startsWith("+1")) {
            normalized = normalized.substring(2);
        } else if (normalized.startsWith("1") && normalized.length() == 11) {
            normalized = normalized.substring(1);
        }
        
        return normalized;
    }
}

