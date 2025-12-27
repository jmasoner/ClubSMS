package com.clubsms;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.*;

/**
 * MessageHistoryManager - SMS Message History Tracking
 * 
 * Manages the history of sent SMS messages including:
 * - Message content and timestamps
 * - Delivery statistics (sent/failed counts)
 * - Recipient tracking for accountability
 * - Export capabilities for record keeping
 * 
 * Uses SharedPreferences with JSON serialization for persistent storage.
 * Maintains a rolling history with configurable size limits.
 * 
 * @author John (Electrical Engineer)
 * @version 1.0
 * @since 2024-12-24
 */
public class MessageHistoryManager {
    
    // Storage configuration constants
    private static final String PREFS_NAME = "ClubSMS_Messages";
    private static final String KEY_MESSAGE_HISTORY = "message_history";
    private static final String KEY_STATS = "sending_statistics";
    private static final int MAX_HISTORY_SIZE = 100;  // Keep last 100 messages
    
    // Class variables
    private final Context context;
    private final SharedPreferences preferences;
    private final Gson gson;
    private List<MessageRecord> messageHistory;
    private SendingStatistics stats;
    
    /**
     * Constructor - Initialize the message history manager
     * Loads existing message history from persistent storage
     */
    public MessageHistoryManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        
        loadStoredHistory();
        loadStatistics();
        
        Log.d("ClubSMS", "MessageHistoryManager initialized with " + 
              messageHistory.size() + " stored messages");
    }
    
    /**
     * Load previously stored message history
     * Handles JSON deserialization with error recovery
     */
    private void loadStoredHistory() {
        try {
            String historyJson = preferences.getString(KEY_MESSAGE_HISTORY, "[]");
            Type historyType = new TypeToken<List<MessageRecord>>(){}.getType();
            messageHistory = gson.fromJson(historyJson, historyType);
            
            if (messageHistory == null) {
                messageHistory = new ArrayList<>();
            }
            
            // Sort by timestamp (newest first)
            messageHistory.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
            
        } catch (Exception e) {
            Log.e("ClubSMS", "Error loading message history", e);
            messageHistory = new ArrayList<>();
        }
    }
    
    /**
     * Load sending statistics
     * Tracks overall usage metrics
     */
    private void loadStatistics() {
        try {
            String statsJson = preferences.getString(KEY_STATS, "{}");
            stats = gson.fromJson(statsJson, SendingStatistics.class);
            
            if (stats == null) {
                stats = new SendingStatistics();
            }
            
        } catch (Exception e) {
            Log.e("ClubSMS", "Error loading statistics", e);
            stats = new SendingStatistics();
        }
    }
    
    /**
     * Save message history to persistent storage
     * Maintains size limits and updates statistics
     */
    private void saveHistory() {
        try {
            // Trim history to maximum size
            while (messageHistory.size() > MAX_HISTORY_SIZE) {
                messageHistory.remove(messageHistory.size() - 1);
            }
            
            SharedPreferences.Editor editor = preferences.edit();
            
            // Save message history
            String historyJson = gson.toJson(messageHistory);
            editor.putString(KEY_MESSAGE_HISTORY, historyJson);
            
            // Save statistics
            String statsJson = gson.toJson(stats);
            editor.putString(KEY_STATS, statsJson);
            
            editor.apply();
            
            Log.d("ClubSMS", "Message history saved");
            
        } catch (Exception e) {
            Log.e("ClubSMS", "Error saving message history", e);
        }
    }
    
    /**
     * Add a new message to history
     * Called after each broadcast operation
     * 
     * @param messageContent The text content that was sent
     * @param successCount Number of successful deliveries
     * @param failureCount Number of failed deliveries
     */
    public void addMessage(String messageContent, int successCount, int failureCount) {
        MessageRecord record = new MessageRecord(
            messageContent, 
            System.currentTimeMillis(),
            successCount,
            failureCount
        );
        
        // Add to beginning of list (newest first)
        messageHistory.add(0, record);
        
        // Update statistics
        stats.totalMessagesSent++;
        stats.totalRecipients += successCount;
        stats.totalFailures += failureCount;
        stats.lastSentTime = record.getTimestamp();
        
        // Calculate success rate
        int totalAttempts = stats.totalRecipients + stats.totalFailures;
        if (totalAttempts > 0) {
            stats.overallSuccessRate = (double) stats.totalRecipients / totalAttempts;
        }
        
        saveHistory();
        
        Log.i("ClubSMS", String.format("Message added to history: %d sent, %d failed", 
              successCount, failureCount));
    }
    
    /**
     * Get recent message history
     * Returns specified number of most recent messages
     */
    public List<MessageRecord> getRecentMessages(int count) {
        int limit = Math.min(count, messageHistory.size());
        return new ArrayList<>(messageHistory.subList(0, limit));
    }
    
    /**
     * Get all message history
     * Returns complete list for comprehensive review
     */
    public List<MessageRecord> getAllMessages() {
        return new ArrayList<>(messageHistory);
    }
    
    /**
     * Get messages within date range
     * Useful for generating reports for specific periods
     */
    public List<MessageRecord> getMessagesByDateRange(long startTime, long endTime) {
        List<MessageRecord> filteredMessages = new ArrayList<>();
        
        for (MessageRecord record : messageHistory) {
            long timestamp = record.getTimestamp();
            if (timestamp >= startTime && timestamp <= endTime) {
                filteredMessages.add(record);
            }
        }
        
        return filteredMessages;
    }
    
    /**
     * Search messages by content
     * Find messages containing specific text
     */
    public List<MessageRecord> searchMessages(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        List<MessageRecord> results = new ArrayList<>();
        String lowercaseSearch = searchTerm.toLowerCase().trim();
        
        for (MessageRecord record : messageHistory) {
            if (record.getMessageContent().toLowerCase().contains(lowercaseSearch)) {
                results.add(record);
            }
        }
        
        return results;
    }
    
    /**
     * Get sending statistics
     * Provides overview of messaging activity
     */
    public SendingStatistics getStatistics() {
        return stats.copy(); // Return copy to prevent external modification
    }
    
    /**
     * Calculate statistics for date range
     * Useful for periodic reports
     */
    public SendingStatistics getStatisticsForPeriod(long startTime, long endTime) {
        SendingStatistics periodStats = new SendingStatistics();
        List<MessageRecord> periodMessages = getMessagesByDateRange(startTime, endTime);
        
        for (MessageRecord record : periodMessages) {
            periodStats.totalMessagesSent++;
            periodStats.totalRecipients += record.getSuccessCount();
            periodStats.totalFailures += record.getFailureCount();
        }
        
        // Calculate success rate for period
        int totalAttempts = periodStats.totalRecipients + periodStats.totalFailures;
        if (totalAttempts > 0) {
            periodStats.overallSuccessRate = (double) periodStats.totalRecipients / totalAttempts;
        }
        
        // Set time bounds
        if (!periodMessages.isEmpty()) {
            periodStats.lastSentTime = periodMessages.get(0).getTimestamp();
        }
        
        return periodStats;
    }
    
    /**
     * Clear all message history
     * Use carefully - this is irreversible
     */
    public void clearHistory() {
        messageHistory.clear();
        stats = new SendingStatistics();
        saveHistory();
        
        Log.i("ClubSMS", "Message history cleared");
    }
    
    /**
     * Export message history as CSV string
     * Useful for external analysis or backup
     */
    public String exportToCsv() {
        StringBuilder csv = new StringBuilder();
        
        // Header row
        csv.append("Timestamp,Date,Message Content,Recipients,Failures,Success Rate\n");
        
        // Data rows
        for (MessageRecord record : messageHistory) {
            csv.append(record.getTimestamp()).append(",");
            csv.append(record.getFormattedDate()).append(",");
            csv.append("\"").append(record.getMessageContent().replace("\"", "\"\"")).append("\",");
            csv.append(record.getSuccessCount()).append(",");
            csv.append(record.getFailureCount()).append(",");
            csv.append(String.format("%.2f%%", record.getSuccessRate() * 100)).append("\n");
        }
        
        return csv.toString();
    }
    
    /**
     * Export complete data as JSON
     * Includes both history and statistics
     */
    public String exportToJson() {
        Map<String, Object> exportData = new HashMap<>();
        exportData.put("messageHistory", messageHistory);
        exportData.put("statistics", stats);
        exportData.put("exportTime", System.currentTimeMillis());
        exportData.put("version", "1.0");
        
        return gson.toJson(exportData);
    }
    
    /**
     * Import message history from JSON backup
     * Merges with existing data
     */
    public boolean importFromJson(String jsonData) {
        try {
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> importData = gson.fromJson(jsonData, mapType);
            
            // Extract message history
            String historyJson = gson.toJson(importData.get("messageHistory"));
            Type historyType = new TypeToken<List<MessageRecord>>(){}.getType();
            List<MessageRecord> importedHistory = gson.fromJson(historyJson, historyType);
            
            // Extract statistics
            String statsJson = gson.toJson(importData.get("statistics"));
            SendingStatistics importedStats = gson.fromJson(statsJson, SendingStatistics.class);
            
            // Merge data
            if (importedHistory != null && !importedHistory.isEmpty()) {
                // Combine with existing history, avoiding duplicates
                Set<Long> existingTimestamps = new HashSet<>();
                for (MessageRecord existing : messageHistory) {
                    existingTimestamps.add(existing.getTimestamp());
                }
                
                for (MessageRecord imported : importedHistory) {
                    if (!existingTimestamps.contains(imported.getTimestamp())) {
                        messageHistory.add(imported);
                    }
                }
                
                // Re-sort by timestamp
                messageHistory.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
            }
            
            // Update statistics (combine with existing)
            if (importedStats != null) {
                stats.totalMessagesSent += importedStats.totalMessagesSent;
                stats.totalRecipients += importedStats.totalRecipients;
                stats.totalFailures += importedStats.totalFailures;
                stats.lastSentTime = Math.max(stats.lastSentTime, importedStats.lastSentTime);
                
                // Recalculate success rate
                int totalAttempts = stats.totalRecipients + stats.totalFailures;
                if (totalAttempts > 0) {
                    stats.overallSuccessRate = (double) stats.totalRecipients / totalAttempts;
                }
            }
            
            saveHistory();
            Log.i("ClubSMS", "Message history import successful");
            return true;
            
        } catch (Exception e) {
            Log.e("ClubSMS", "Error importing message history", e);
            return false;
        }
    }
    
    /**
     * Get summary report as formatted string
     * Useful for displaying in UI
     */
    public String getSummaryReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("=== Club SMS Activity Summary ===\n\n");
        
        report.append(String.format("Total Messages Sent: %d\n", stats.totalMessagesSent));
        report.append(String.format("Total Recipients: %d\n", stats.totalRecipients));
        report.append(String.format("Failed Deliveries: %d\n", stats.totalFailures));
        report.append(String.format("Overall Success Rate: %.1f%%\n\n", 
                     stats.overallSuccessRate * 100));
        
        if (stats.lastSentTime > 0) {
            Date lastSent = new Date(stats.lastSentTime);
            report.append(String.format("Last Message Sent: %s\n\n", lastSent.toString()));
        }
        
        report.append("=== Recent Messages ===\n");
        List<MessageRecord> recentMessages = getRecentMessages(5);
        
        for (MessageRecord record : recentMessages) {
            report.append(String.format("• %s - %d recipients (%.1f%% success)\n",
                record.getFormattedDate(),
                record.getSuccessCount() + record.getFailureCount(),
                record.getSuccessRate() * 100));
        }
        
        return report.toString();
    }
    
    /**
     * Inner class for overall sending statistics
     * Tracks cumulative metrics across all messages
     */
    public static class SendingStatistics {
        public int totalMessagesSent = 0;
        public int totalRecipients = 0;
        public int totalFailures = 0;
        public double overallSuccessRate = 0.0;
        public long lastSentTime = 0;
        
        public SendingStatistics copy() {
            SendingStatistics copy = new SendingStatistics();
            copy.totalMessagesSent = this.totalMessagesSent;
            copy.totalRecipients = this.totalRecipients;
            copy.totalFailures = this.totalFailures;
            copy.overallSuccessRate = this.overallSuccessRate;
            copy.lastSentTime = this.lastSentTime;
            return copy;
        }
        
        @Override
        public String toString() {
            return String.format("Stats{messages=%d, recipients=%d, failures=%d, rate=%.1f%%}", 
                totalMessagesSent, totalRecipients, totalFailures, overallSuccessRate * 100);
        }
    }
}