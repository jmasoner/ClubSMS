package com.clubsms;

import java.util.Date;

/**
 * ClubMember - Data Model for Club Members
 * 
 * Represents a single club member with their contact information,
 * status, and relevant metadata for SMS broadcasting system.
 * 
 * This class handles:
 * - Member identification and contact details
 * - Opt-out status tracking
 * - Data validation for SMS compatibility
 * - Serialization for persistent storage
 * 
 * @author John (Electrical Engineer)
 * @version 1.0  
 * @since 2024-12-24
 */
public class ClubMember {
    
    // Core member information
    private String memberId;          // Unique identifier (often contact ID)
    private String name;              // Display name
    private String phoneNumber;       // Primary SMS number
    private String email;             // Optional email address
    private boolean isActive;         // Active membership status
    
    // SMS-specific settings
    private boolean optedOut;         // SMS opt-out status
    private Date optOutDate;          // When they opted out (null if active)
    private String preferredName;     // How they want to be addressed in messages
    
    // Metadata
    private Date joinDate;            // When added to system
    private Date lastContactDate;     // Last successful SMS sent
    private String notes;             // Admin notes about member
    private MemberSource source;      // How they were added to system
    
    /**
     * Enum for tracking how members were added
     * Useful for data management and reporting
     */
    public enum MemberSource {
        PHONE_IMPORT,      // Imported from phone contacts
        MANUAL_ENTRY,      // Manually added by user
        CSV_IMPORT,        // Imported from CSV file
        OTHER_APP,         // Added by another app
        UNKNOWN            // Source not tracked
    }
    
    /**
     * Default constructor for JSON deserialization
     * Initialize with safe default values
     */
    public ClubMember() {
        this.isActive = true;
        this.optedOut = false;
        this.joinDate = new Date();
        this.source = MemberSource.UNKNOWN;
    }
    
    /**
     * Primary constructor for creating new members
     * Used when importing contacts or adding manually
     * 
     * @param memberId Unique identifier (can be phone contact ID)
     * @param name Display name for the member
     * @param phoneNumber SMS-capable phone number
     */
    public ClubMember(String memberId, String name, String phoneNumber) {
        this();
        this.memberId = memberId;
        this.name = name != null ? name.trim() : "";
        this.phoneNumber = cleanPhoneNumber(phoneNumber);
        this.preferredName = this.name; // Default to full name
        this.source = MemberSource.PHONE_IMPORT; // Most common source
    }
    
    /**
     * Full constructor with all optional parameters
     * Used for comprehensive member creation
     */
    public ClubMember(String memberId, String name, String phoneNumber, 
                     String email, String preferredName, MemberSource source) {
        this(memberId, name, phoneNumber);
        this.email = email != null ? email.trim() : "";
        this.preferredName = preferredName != null ? preferredName.trim() : this.name;
        this.source = source != null ? source : MemberSource.MANUAL_ENTRY;
    }
    
    /**
     * Clean phone number format for SMS compatibility
     * Removes formatting and ensures consistent format
     */
    private String cleanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";
        
        // Remove all non-digit characters except + at start
        String cleaned = phoneNumber.replaceAll("[^\\d+]", "");
        
        // Add US country code if missing (assuming US-based club)
        if (cleaned.length() == 10 && !cleaned.startsWith("+")) {
            cleaned = "+1" + cleaned;
        }
        
        return cleaned;
    }
    
    /**
     * Validate member data integrity
     * Ensures member has minimum required information for SMS sending
     */
    public boolean isValid() {
        // Must have name and valid phone number
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        // Phone number must have at least 10 digits
        String digitsOnly = phoneNumber.replaceAll("[^\\d]", "");
        return digitsOnly.length() >= 10;
    }
    
    /**
     * Check if member can receive SMS messages
     * Considers both active status and opt-out preference
     */
    public boolean canReceiveMessages() {
        return isActive && !optedOut && isValid();
    }
    
    /**
     * Mark member as opted out with timestamp
     * Records when they chose to stop receiving messages
     */
    public void setOptedOut(boolean optedOut) {
        this.optedOut = optedOut;
        if (optedOut) {
            this.optOutDate = new Date();
        } else {
            this.optOutDate = null; // Clear date when opting back in
        }
    }
    
    /**
     * Update last contact date
     * Called when SMS is successfully sent to member
     */
    public void recordSuccessfulContact() {
        this.lastContactDate = new Date();
    }
    
    // Getter methods with null-safe returns
    
    public String getMemberId() {
        return memberId != null ? memberId : "";
    }
    
    public String getName() {
        return name != null ? name : "";
    }
    
    public String getPhoneNumber() {
        return phoneNumber != null ? phoneNumber : "";
    }
    
    public String getEmail() {
        return email != null ? email : "";
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public boolean hasOptedOut() {
        return optedOut;
    }
    
    public Date getOptOutDate() {
        return optOutDate;
    }
    
    public String getPreferredName() {
        return preferredName != null && !preferredName.isEmpty() ? preferredName : name;
    }
    
    public Date getJoinDate() {
        return joinDate;
    }
    
    public Date getLastContactDate() {
        return lastContactDate;
    }
    
    public String getNotes() {
        return notes != null ? notes : "";
    }
    
    public MemberSource getSource() {
        return source != null ? source : MemberSource.UNKNOWN;
    }
    
    // Setter methods with validation
    
    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }
    
    public void setName(String name) {
        this.name = name != null ? name.trim() : "";
        // Update preferred name if it was using the old name
        if (preferredName == null || preferredName.isEmpty()) {
            this.preferredName = this.name;
        }
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = cleanPhoneNumber(phoneNumber);
    }
    
    public void setEmail(String email) {
        this.email = email != null ? email.trim() : "";
    }
    
    public void setActive(boolean active) {
        this.isActive = active;
    }
    
    public void setPreferredName(String preferredName) {
        this.preferredName = preferredName != null ? preferredName.trim() : "";
    }
    
    public void setJoinDate(Date joinDate) {
        this.joinDate = joinDate != null ? joinDate : new Date();
    }
    
    public void setNotes(String notes) {
        this.notes = notes != null ? notes.trim() : "";
    }
    
    public void setSource(MemberSource source) {
        this.source = source != null ? source : MemberSource.UNKNOWN;
    }
    
    /**
     * Get formatted phone number for display
     * Shows user-friendly format while keeping raw number for SMS
     */
    public String getFormattedPhoneNumber() {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return "";
        }
        
        // Format US numbers: +1 (555) 123-4567
        if (phoneNumber.startsWith("+1") && phoneNumber.length() == 12) {
            String digits = phoneNumber.substring(2);
            return String.format("+1 (%s) %s-%s", 
                digits.substring(0, 3), 
                digits.substring(3, 6), 
                digits.substring(6));
        }
        
        // For other formats, return as-is
        return phoneNumber;
    }
    
    /**
     * Generate display name for UI
     * Shows preferred name with fallback to full name
     */
    public String getDisplayName() {
        String display = getPreferredName();
        if (display.isEmpty()) {
            display = getName();
        }
        
        // Add status indicators
        if (!isActive) {
            display += " (Inactive)";
        } else if (optedOut) {
            display += " (Opted Out)";
        }
        
        return display;
    }
    
    /**
     * Create summary string for member
     * Useful for logging and debugging
     */
    public String getSummary() {
        return String.format("%s (%s) - %s - %s", 
            getName(), 
            getFormattedPhoneNumber(),
            isActive ? "Active" : "Inactive",
            optedOut ? "Opted Out" : "Available");
    }
    
    /**
     * Check if member data has been recently updated
     * Useful for detecting stale records
     */
    public boolean isRecentlyActive(int dayThreshold) {
        if (lastContactDate == null) {
            return false; // Never contacted
        }
        
        long daysSinceContact = (System.currentTimeMillis() - lastContactDate.getTime()) 
                               / (1000 * 60 * 60 * 24);
        return daysSinceContact <= dayThreshold;
    }
    
    /**
     * Create a copy of this member
     * Useful for editing without affecting original
     */
    public ClubMember copy() {
        ClubMember copy = new ClubMember(memberId, name, phoneNumber, email, preferredName, source);
        copy.isActive = this.isActive;
        copy.optedOut = this.optedOut;
        copy.optOutDate = this.optOutDate;
        copy.joinDate = this.joinDate;
        copy.lastContactDate = this.lastContactDate;
        copy.notes = this.notes;
        return copy;
    }
    
    /**
     * String representation for debugging
     * Shows key information without sensitive details
     */
    @Override
    public String toString() {
        return String.format("ClubMember{name='%s', phone='%s', active=%b, optedOut=%b}", 
            name, 
            phoneNumber != null && phoneNumber.length() > 4 ? 
                phoneNumber.substring(0, phoneNumber.length() - 4) + "****" : "****",
            isActive, 
            optedOut);
    }
    
    /**
     * Equality comparison based on phone number
     * Two members are equal if they have the same phone number
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ClubMember other = (ClubMember) obj;
        return phoneNumber != null && phoneNumber.equals(other.phoneNumber);
    }
    
    /**
     * Hash code based on phone number
     * Consistent with equals() method
     */
    @Override
    public int hashCode() {
        return phoneNumber != null ? phoneNumber.hashCode() : 0;
    }
}