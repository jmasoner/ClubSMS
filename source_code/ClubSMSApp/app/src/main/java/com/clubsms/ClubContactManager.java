package com.clubsms;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.*;

/**
 * ClubContactManager - Contact Management System
 * 
 * Handles all contact-related operations for the Club SMS app including:
 * - Adding and removing club members
 * - Managing opt-out status
 * - Persistent storage of member data
 * - Contact validation and cleanup
 * 
 * Uses SharedPreferences for local data storage with JSON serialization
 * for complex data structures.
 * 
 * @author John (Electrical Engineer)  
 * @version 1.0
 * @since 2024-12-24
 */
public class ClubContactManager {
    
    // Storage constants - using descriptive keys
    private static final String PREFS_NAME = "ClubSMS_Contacts";
    private static final String KEY_MEMBERS = "club_members";
    private static final String KEY_OPTED_OUT = "opted_out_members";
    private static final String KEY_LAST_UPDATE = "last_update_timestamp";
    
    // Class variables
    private final Context context;
    private final SharedPreferences preferences;
    private final Gson gson;                    // JSON serialization
    private List<ClubMember> allMembers;       // Complete member list
    private Set<String> optedOutNumbers;       // Phone numbers that opted out
    
    /**
     * Constructor - Initialize the contact manager
     * Sets up persistent storage and loads existing data
     */
    public ClubContactManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.allMembers = new ArrayList<>();
        this.optedOutNumbers = new HashSet<>();
        
        // Load existing data on initialization
        loadStoredData();
    }
    
    /**
     * Load previously stored member data from SharedPreferences
     * Handles JSON deserialization with error recovery
     */
    private void loadStoredData() {
        try {
            // Load club members
            String membersJson = preferences.getString(KEY_MEMBERS, "[]");
            Type memberListType = new TypeToken<List<ClubMember>>(){}.getType();
            allMembers = gson.fromJson(membersJson, memberListType);
            
            if (allMembers == null) {
                allMembers = new ArrayList<>();
            }
            
            // Load opted-out numbers
            String optedOutJson = preferences.getString(KEY_OPTED_OUT, "[]");
            Type stringSetType = new TypeToken<Set<String>>(){}.getType();
            optedOutNumbers = gson.fromJson(optedOutJson, stringSetType);
            
            if (optedOutNumbers == null) {
                optedOutNumbers = new HashSet<>();
            }
            
            Log.d("ClubSMS", "Loaded " + allMembers.size() + " members, " + 
                  optedOutNumbers.size() + " opted out");
            
        } catch (Exception e) {
            Log.e("ClubSMS", "Error loading stored data", e);
            // Initialize with empty data on error
            allMembers = new ArrayList<>();
            optedOutNumbers = new HashSet<>();
        }
    }
    
    /**
     * Save current member data to persistent storage
     * Uses JSON serialization for complex data structures
     */
    private void saveData() {
        try {
            SharedPreferences.Editor editor = preferences.edit();
            
            // Save members list
            String membersJson = gson.toJson(allMembers);
            editor.putString(KEY_MEMBERS, membersJson);
            
            // Save opted-out numbers
            String optedOutJson = gson.toJson(optedOutNumbers);
            editor.putString(KEY_OPTED_OUT, optedOutJson);
            
            // Update timestamp
            editor.putLong(KEY_LAST_UPDATE, System.currentTimeMillis());
            
            // Commit changes
            editor.apply();
            
            Log.d("ClubSMS", "Data saved successfully");
            
        } catch (Exception e) {
            Log.e("ClubSMS", "Error saving data", e);
        }
    }
    
    /**
     * Add a single new member to the club
     * Validates data and prevents duplicates
     */
    public boolean addMember(ClubMember member) {
        if (member == null || !member.isValid()) {
            Log.w("ClubSMS", "Invalid member data provided");
            return false;
        }
        
        // Check for duplicates by phone number
        String phoneNumber = member.getPhoneNumber();
        for (ClubMember existing : allMembers) {
            if (existing.getPhoneNumber().equals(phoneNumber)) {
                Log.i("ClubSMS", "Member already exists: " + member.getName());
                return false; // Already exists
            }
        }
        
        // Add new member
        allMembers.add(member);
        saveData();
        
        Log.i("ClubSMS", "Added new member: " + member.getName());
        return true;
    }
    
    /**
     * Add multiple members in batch operation
     * More efficient than adding one by one
     */
    public int addMembers(List<ClubMember> members) {
        if (members == null || members.isEmpty()) {
            return 0;
        }
        
        int addedCount = 0;
        Set<String> existingNumbers = new HashSet<>();
        
        // Build set of existing phone numbers for faster lookup
        for (ClubMember existing : allMembers) {
            existingNumbers.add(existing.getPhoneNumber());
        }
        
        // Add each valid, non-duplicate member
        for (ClubMember member : members) {
            if (member != null && member.isValid() && 
                !existingNumbers.contains(member.getPhoneNumber())) {
                
                allMembers.add(member);
                existingNumbers.add(member.getPhoneNumber());
                addedCount++;
            }
        }
        
        // Save once after all additions
        if (addedCount > 0) {
            saveData();
        }
        
        Log.i("ClubSMS", "Added " + addedCount + " new members");
        return addedCount;
    }
    
    /**
     * Remove a member from the club
     * Handles removal by phone number (most reliable identifier)
     */
    public boolean removeMember(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        // Find and remove member with matching phone number
        Iterator<ClubMember> iterator = allMembers.iterator();
        while (iterator.hasNext()) {
            ClubMember member = iterator.next();
            if (member.getPhoneNumber().equals(phoneNumber)) {
                iterator.remove();
                saveData();
                Log.i("ClubSMS", "Removed member: " + member.getName());
                return true;
            }
        }
        
        Log.w("ClubSMS", "Member not found for removal: " + phoneNumber);
        return false;
    }
    
    /**
     * Mark a member as opted out
     * They remain in the system but won't receive messages
     */
    public void markOptedOut(String phoneNumber) {
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            optedOutNumbers.add(phoneNumber);
            saveData();
            Log.i("ClubSMS", "Member opted out: " + phoneNumber);
        }
    }
    
    /**
     * Remove opt-out status (member wants to receive messages again)
     * Allows previously opted-out members to rejoin
     */
    public void removeOptOut(String phoneNumber) {
        if (phoneNumber != null && optedOutNumbers.contains(phoneNumber)) {
            optedOutNumbers.remove(phoneNumber);
            saveData();
            Log.i("ClubSMS", "Member opted back in: " + phoneNumber);
        }
    }
    
    /**
     * Check if a phone number has opted out
     * Used before sending messages
     */
    public boolean hasOptedOut(String phoneNumber) {
        return phoneNumber != null && optedOutNumbers.contains(phoneNumber);
    }
    
    /**
     * Get all members who are active (not opted out)
     * This is the list used for sending messages
     */
    public List<ClubMember> getAllActiveMembers() {
        List<ClubMember> activeMembers = new ArrayList<>();
        
        for (ClubMember member : allMembers) {
            if (!hasOptedOut(member.getPhoneNumber())) {
                activeMembers.add(member);
            }
        }
        
        return activeMembers;
    }
    
    /**
     * Get complete member list (including opted-out members)
     * Useful for contact management interface
     */
    public List<ClubMember> getAllMembers() {
        return new ArrayList<>(allMembers); // Return copy to prevent external modification
    }
    
    /**
     * Get list of opted-out members
     * Useful for management and re-engagement
     */
    public List<ClubMember> getOptedOutMembers() {
        List<ClubMember> optedOutMembers = new ArrayList<>();
        
        for (ClubMember member : allMembers) {
            if (hasOptedOut(member.getPhoneNumber())) {
                optedOutMembers.add(member);
            }
        }
        
        return optedOutMembers;
    }
    
    /**
     * Find a member by phone number
     * Returns null if not found
     */
    public ClubMember findMemberByPhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return null;
        }
        
        for (ClubMember member : allMembers) {
            if (member.getPhoneNumber().equals(phoneNumber)) {
                return member;
            }
        }
        
        return null;
    }
    
    /**
     * Update member information
     * Useful for correcting names or phone numbers
     */
    public boolean updateMember(String oldPhoneNumber, ClubMember updatedMember) {
        if (oldPhoneNumber == null || updatedMember == null || !updatedMember.isValid()) {
            return false;
        }
        
        for (int i = 0; i < allMembers.size(); i++) {
            if (allMembers.get(i).getPhoneNumber().equals(oldPhoneNumber)) {
                allMembers.set(i, updatedMember);
                saveData();
                Log.i("ClubSMS", "Updated member: " + updatedMember.getName());
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Clean up invalid or duplicate entries
     * Maintenance function to keep data clean
     */
    public int cleanupMembers() {
        int removedCount = 0;
        Set<String> seenNumbers = new HashSet<>();
        Iterator<ClubMember> iterator = allMembers.iterator();
        
        while (iterator.hasNext()) {
            ClubMember member = iterator.next();
            
            // Remove if invalid or duplicate
            if (!member.isValid() || seenNumbers.contains(member.getPhoneNumber())) {
                iterator.remove();
                removedCount++;
            } else {
                seenNumbers.add(member.getPhoneNumber());
            }
        }
        
        if (removedCount > 0) {
            saveData();
            Log.i("ClubSMS", "Cleaned up " + removedCount + " invalid/duplicate members");
        }
        
        return removedCount;
    }
    
    /**
     * Get statistics about the member database
     * Useful for reporting and monitoring
     */
    public ContactStats getStats() {
        ContactStats stats = new ContactStats();
        stats.totalMembers = allMembers.size();
        stats.activeMembers = getAllActiveMembers().size();
        stats.optedOutMembers = optedOutNumbers.size();
        stats.lastUpdateTime = preferences.getLong(KEY_LAST_UPDATE, 0);
        
        return stats;
    }
    
    /**
     * Export member data for backup or migration
     * Returns JSON string of all data
     */
    public String exportData() {
        Map<String, Object> exportData = new HashMap<>();
        exportData.put("members", allMembers);
        exportData.put("optedOut", optedOutNumbers);
        exportData.put("exportTime", System.currentTimeMillis());
        exportData.put("version", "1.0");
        
        return gson.toJson(exportData);
    }
    
    /**
     * Import member data from backup
     * Merges with existing data, avoiding duplicates
     */
    public boolean importData(String jsonData) {
        try {
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> importData = gson.fromJson(jsonData, mapType);
            
            // Extract members list
            String membersJson = gson.toJson(importData.get("members"));
            Type memberListType = new TypeToken<List<ClubMember>>(){}.getType();
            List<ClubMember> importedMembers = gson.fromJson(membersJson, memberListType);
            
            // Extract opted-out numbers
            String optedOutJson = gson.toJson(importData.get("optedOut"));
            Type stringSetType = new TypeToken<Set<String>>(){}.getType();
            Set<String> importedOptedOut = gson.fromJson(optedOutJson, stringSetType);
            
            // Merge data
            if (importedMembers != null) {
                addMembers(importedMembers);
            }
            
            if (importedOptedOut != null) {
                optedOutNumbers.addAll(importedOptedOut);
                saveData();
            }
            
            Log.i("ClubSMS", "Data import successful");
            return true;
            
        } catch (Exception e) {
            Log.e("ClubSMS", "Error importing data", e);
            return false;
        }
    }
    
    /**
     * Inner class for contact statistics
     * Provides summary information about the member database
     */
    public static class ContactStats {
        public int totalMembers;
        public int activeMembers;
        public int optedOutMembers;
        public long lastUpdateTime;
        
        @Override
        public String toString() {
            return String.format("Total: %d, Active: %d, Opted Out: %d", 
                totalMembers, activeMembers, optedOutMembers);
        }
    }
}