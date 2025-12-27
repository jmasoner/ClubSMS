package com.clubsms;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * MessageRecord - Individual Message History Entry
 * 
 * Represents a single SMS broadcast event with:
 * - Message content and timing information
 * - Delivery success/failure statistics  
 * - Formatted display methods for UI
 * 
 * This class is designed for JSON serialization and works with
 * MessageHistoryManager for persistent storage.
 * 
 * @author John (Electrical Engineer)
 * @version 1.0
 * @since 2024-12-24
 */
public class MessageRecord {
    
    // Core message data
    private String messageContent;      // The SMS text that was sent
    private long timestamp;             // When message was sent (Unix timestamp)
    private int successCount;           // Number of successful deliveries
    private int failureCount;           // Number of failed deliveries
    
    // Optional metadata
    private String messageId;           // Unique identifier for this broadcast
    private int estimatedLength;        // Character count (for SMS segment estimation)
    private long sendDurationMs;        // How long the broadcast took
    
    // Static formatter for consistent date display
    private static final SimpleDateFormat DATE_FORMAT = 
        new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault());
    
    /**
     * Default constructor for JSON deserialization
     * Initializes with safe default values
     */
    public MessageRecord() {
        this.timestamp = System.currentTimeMillis();
        this.messageContent = "";
        this.successCount = 0;
        this.failureCount = 0;
    }
    
    /**
     * Primary constructor for creating message records
     * Used when logging a completed broadcast
     * 
     * @param messageContent The SMS text that was sent
     * @param timestamp When the message was sent
     * @param successCount Number of successful deliveries
     * @param failureCount Number of failed deliveries
     */
    public MessageRecord(String messageContent, long timestamp, 
                        int successCount, int failureCount) {
        this.messageContent = messageContent != null ? messageContent : "";
        this.timestamp = timestamp;
        this.successCount = Math.max(0, successCount);  // Ensure non-negative
        this.failureCount = Math.max(0, failureCount);  // Ensure non-negative
        
        // Calculate derived values
        this.estimatedLength = this.messageContent.length();
        this.messageId = generateMessageId();
    }
    
    /**
     * Full constructor with all optional parameters
     * Used for detailed logging scenarios
     */
    public MessageRecord(String messageContent, long timestamp, 
                        int successCount, int failureCount,
                        String messageId, long sendDurationMs) {
        this(messageContent, timestamp, successCount, failureCount);
        this.messageId = messageId != null ? messageId : generateMessageId();
        this.sendDurationMs = Math.max(0, sendDurationMs);
    }
    
    /**
     * Generate unique message ID for tracking
     * Creates timestamp-based ID with random component
     */
    private String generateMessageId() {
        return String.format("MSG_%d_%04d", 
            timestamp / 1000,  // Unix timestamp in seconds
            (int)(Math.random() * 10000));  // Random 4-digit suffix
    }
    
    // Getter methods with null-safe returns
    
    public String getMessageContent() {
        return messageContent != null ? messageContent : "";
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public int getSuccessCount() {
        return successCount;
    }
    
    public int getFailureCount() {
        return failureCount;
    }
    
    public String getMessageId() {
        return messageId != null ? messageId : "";
    }
    
    public int getEstimatedLength() {
        return estimatedLength;
    }
    
    public long getSendDurationMs() {
        return sendDurationMs;
    }
    
    // Setter methods with validation
    
    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent != null ? messageContent : "";
        this.estimatedLength = this.messageContent.length();
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public void setSuccessCount(int successCount) {
        this.successCount = Math.max(0, successCount);
    }
    
    public void setFailureCount(int failureCount) {
        this.failureCount = Math.max(0, failureCount);
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public void setSendDurationMs(long sendDurationMs) {
        this.sendDurationMs = Math.max(0, sendDurationMs);
    }
    
    // Derived value calculations
    
    /**
     * Get total number of delivery attempts
     * Sum of successful and failed deliveries
     */
    public int getTotalAttempts() {
        return successCount + failureCount;
    }
    
    /**
     * Calculate success rate as decimal (0.0 to 1.0)
     * Returns 0.0 if no attempts were made
     */
    public double getSuccessRate() {
        int total = getTotalAttempts();
        return total > 0 ? (double) successCount / total : 0.0;
    }
    
    /**
     * Get success rate as percentage string
     * Formatted for display in UI
     */
    public String getSuccessRatePercent() {
        return String.format("%.1f%%", getSuccessRate() * 100);
    }
    
    /**
     * Estimate SMS segments needed
     * Standard SMS is 160 characters, longer messages split into segments
     */
    public int getEstimatedSmsSegments() {
        if (estimatedLength == 0) return 0;
        if (estimatedLength <= 160) return 1;
        
        // Multi-part SMS has 153 characters per segment (7 chars for header)
        return (int) Math.ceil((double) estimatedLength / 153);
    }
    
    /**
     * Check if this was a successful broadcast
     * True if at least 80% of messages were delivered
     */
    public boolean wasSuccessful() {
        return getSuccessRate() >= 0.8 && successCount > 0;
    }
    
    /**
     * Check if this broadcast had significant failures
     * True if more than 20% of messages failed
     */
    public boolean hadSignificantFailures() {
        return getSuccessRate() < 0.8 && getTotalAttempts() > 0;
    }
    
    // Display formatting methods
    
    /**
     * Get formatted date string for display
     * Shows user-friendly date and time
     */
    public String getFormattedDate() {
        return DATE_FORMAT.format(new Date(timestamp));
    }
    
    /**
     * Get short date format for compact display
     * Shows just date without time
     */
    public String getShortDate() {
        SimpleDateFormat shortFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
        return shortFormat.format(new Date(timestamp));
    }
    
    /**
     * Get time only for display
     * Shows just the time component
     */
    public String getFormattedTime() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        return timeFormat.format(new Date(timestamp));
    }
    
    /**
     * Get truncated message content for list display
     * Limits length to prevent UI overflow
     */
    public String getTruncatedMessage(int maxLength) {
        if (messageContent.length() <= maxLength) {
            return messageContent;
        }
        
        return messageContent.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Get summary string for quick overview
     * Shows key metrics in compact format
     */
    public String getSummary() {
        return String.format("%s - %d sent (%s success)", 
            getShortDate(), 
            getTotalAttempts(), 
            getSuccessRatePercent());
    }
    
    /**
     * Get detailed summary for full display
     * Shows comprehensive information about the broadcast
     */
    public String getDetailedSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("Message: ").append(getTruncatedMessage(50)).append("\n");
        summary.append("Sent: ").append(getFormattedDate()).append("\n");
        summary.append("Recipients: ").append(successCount).append(" successful, ");
        summary.append(failureCount).append(" failed\n");
        summary.append("Success Rate: ").append(getSuccessRatePercent()).append("\n");
        summary.append("Message Length: ").append(estimatedLength).append(" characters\n");
        summary.append("SMS Segments: ").append(getEstimatedSmsSegments());
        
        if (sendDurationMs > 0) {
            summary.append("\nSend Duration: ").append(sendDurationMs).append("ms");
        }
        
        return summary.toString();
    }
    
    /**
     * Get status indicator for UI display
     * Returns color-coded status based on success rate
     */
    public MessageStatus getStatus() {
        if (getTotalAttempts() == 0) {
            return MessageStatus.NO_RECIPIENTS;
        } else if (getSuccessRate() >= 0.95) {
            return MessageStatus.EXCELLENT;
        } else if (getSuccessRate() >= 0.8) {
            return MessageStatus.GOOD;
        } else if (getSuccessRate() >= 0.5) {
            return MessageStatus.POOR;
        } else {
            return MessageStatus.FAILED;
        }
    }
    
    /**
     * Check if this record is recent
     * True if sent within specified number of hours
     */
    public boolean isRecent(int hours) {
        long hoursInMs = hours * 60 * 60 * 1000L;
        return (System.currentTimeMillis() - timestamp) <= hoursInMs;
    }
    
    /**
     * Create a copy of this message record
     * Useful for editing without affecting original
     */
    public MessageRecord copy() {
        return new MessageRecord(
            messageContent, timestamp, successCount, failureCount,
            messageId, sendDurationMs
        );
    }
    
    /**
     * Enum for message status classification
     * Used for UI display and filtering
     */
    public enum MessageStatus {
        EXCELLENT("Excellent", "#4CAF50"),      // Green - 95%+ success
        GOOD("Good", "#8BC34A"),                // Light Green - 80-94% success
        POOR("Poor", "#FF9800"),                // Orange - 50-79% success
        FAILED("Failed", "#F44336"),            // Red - <50% success
        NO_RECIPIENTS("No Recipients", "#9E9E9E"); // Gray - 0 attempts
        
        private final String displayName;
        private final String colorCode;
        
        MessageStatus(String displayName, String colorCode) {
            this.displayName = displayName;
            this.colorCode = colorCode;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getColorCode() {
            return colorCode;
        }
    }
    
    /**
     * String representation for debugging
     * Shows key information without full message content
     */
    @Override
    public String toString() {
        return String.format("MessageRecord{id='%s', timestamp=%d, sent=%d, failed=%d, rate=%.1f%%}", 
            messageId, timestamp, successCount, failureCount, getSuccessRate() * 100);
    }
    
    /**
     * Equality comparison based on message ID and timestamp
     * Two records are equal if they have the same ID or same timestamp with content
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        MessageRecord other = (MessageRecord) obj;
        
        // Primary comparison by message ID
        if (messageId != null && other.messageId != null) {
            return messageId.equals(other.messageId);
        }
        
        // Fallback comparison by timestamp and content
        return timestamp == other.timestamp && 
               messageContent.equals(other.messageContent);
    }
    
    /**
     * Hash code based on message ID or timestamp+content
     * Consistent with equals() method
     */
    @Override
    public int hashCode() {
        if (messageId != null) {
            return messageId.hashCode();
        }
        
        return Long.hashCode(timestamp) + messageContent.hashCode();
    }
}