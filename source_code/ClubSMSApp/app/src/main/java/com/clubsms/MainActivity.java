package com.clubsms;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

/**
 * ClubSMS - Main Activity
 * 
 * This is the primary interface for the Club SMS Broadcasting app.
 * Handles contact management, SMS broadcasting, and user interactions.
 * 
 * Features:
 * - Import contacts from phone
 * - Manage club member list with add/remove functionality
 * - Broadcast SMS messages to all active members
 * - Handle opt-out requests automatically
 * - Track message history and delivery status
 * 
 * @author John (Electrical Engineer)
 * @version 1.0
 * @since 2024-12-24
 */
public class MainActivity extends AppCompatActivity {
    
    // Permission request codes - using unique identifiers
    private static final int PERMISSION_REQUEST_CONTACTS = 1001;
    private static final int PERMISSION_REQUEST_SMS = 1002;
    private static final int PERMISSION_REQUEST_ALL = 1003;
    
    // UI Components - organized by functionality
    private EditText messageEditText;           // Main message input field
    private Button sendButton;                  // Broadcast message button
    private Button importContactsButton;        // Import from phone contacts
    private Button manageContactsButton;        // Open contact management
    private TextView statusTextView;            // Display operation status
    private TextView contactCountTextView;      // Show active member count
    private RecyclerView recentMessagesRecycler; // Message history display
    
    // Data Management
    private ClubContactManager contactManager;   // Handles all contact operations
    private MessageHistoryManager historyManager; // Tracks sent messages
    private List<ClubMember> activeMembers;     // Current active club members
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize core components
        initializeComponents();
        setupEventListeners();
        checkPermissions();
        
        // Load existing data
        loadExistingMembers();
        updateContactCount();
    }
    
    /**
     * Initialize all UI components and data managers
     * Sets up the foundation for the app's functionality
     */
    private void initializeComponents() {
        // Link UI elements to code variables
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        importContactsButton = findViewById(R.id.importContactsButton);
        manageContactsButton = findViewById(R.id.manageContactsButton);
        statusTextView = findViewById(R.id.statusTextView);
        contactCountTextView = findViewById(R.id.contactCountTextView);
        recentMessagesRecycler = findViewById(R.id.recentMessagesRecycler);
        
        // Initialize data management classes
        contactManager = new ClubContactManager(this);
        historyManager = new MessageHistoryManager(this);
        activeMembers = new ArrayList<>();
        
        // Set up recycler view for message history
        recentMessagesRecycler.setLayoutManager(new LinearLayoutManager(this));
        
        // Set initial UI state
        statusTextView.setText("Ready to send messages to your club members");
        updateSendButtonState();
    }
    
    /**
     * Set up all button click listeners and UI interactions
     * Centralizes event handling for better maintainability
     */
    private void setupEventListeners() {
        // Send Message Button - Main broadcasting functionality
        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString().trim();
            if (!message.isEmpty()) {
                broadcastMessage(message);
            } else {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Import Contacts Button - Load contacts from phone
        importContactsButton.setOnClickListener(v -> importContactsFromPhone());
        
        // Manage Contacts Button - Open contact management interface
        manageContactsButton.setOnClickListener(v -> openContactManagement());
        
        // Message input field - Enable/disable send button based on content
        messageEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSendButtonState();
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }
    
    /**
     * Check and request necessary permissions for SMS and Contacts
     * Android requires explicit permission for these sensitive operations
     */
    private void checkPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        
        // Check SMS permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.SEND_SMS);
        }
        
        // Check Contacts permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_CONTACTS);
        }
        
        // Request missing permissions
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsNeeded.toArray(new String[0]), 
                PERMISSION_REQUEST_ALL);
        }
    }
    
    /**
     * Handle permission request results
     * Critical for app functionality - SMS and Contacts access required
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        switch (requestCode) {
            case PERMISSION_REQUEST_ALL:
                boolean allGranted = true;
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allGranted = false;
                        break;
                    }
                }
                
                if (allGranted) {
                    statusTextView.setText("Permissions granted - Ready to use");
                    importContactsButton.setEnabled(true);
                } else {
                    statusTextView.setText("Permissions required for full functionality");
                    showPermissionExplanation();
                }
                break;
        }
    }
    
    /**
     * Import contacts from phone's contact list
     * Filters and processes contacts for club member management
     */
    private void importContactsFromPhone() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) 
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Contacts permission required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        statusTextView.setText("Importing contacts...");
        
        // Run contact import in background thread to avoid UI freezing
        new Thread(() -> {
            List<ClubMember> importedContacts = readPhoneContacts();
            
            // Update UI on main thread
            runOnUiThread(() -> {
                if (!importedContacts.isEmpty()) {
                    // Show contact selection dialog
                    showContactSelectionDialog(importedContacts);
                } else {
                    statusTextView.setText("No contacts found");
                    Toast.makeText(this, "No phone contacts available", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
    
    /**
     * Read contacts from phone's contact database
     * Extracts name and phone number for potential club members
     */
    private List<ClubMember> readPhoneContacts() {
        List<ClubMember> contacts = new ArrayList<>();
        ContentResolver contentResolver = getContentResolver();
        
        // Query phone contacts database
        Cursor cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            new String[]{
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID
            },
            null, null, 
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        );
        
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                String phoneNumber = cursor.getString(1);
                String contactId = cursor.getString(2);
                
                // Clean and validate phone number
                phoneNumber = cleanPhoneNumber(phoneNumber);
                
                if (isValidPhoneNumber(phoneNumber)) {
                    ClubMember member = new ClubMember(contactId, name, phoneNumber);
                    contacts.add(member);
                }
            }
            cursor.close();
        }
        
        return contacts;
    }
    
    /**
     * Clean phone number format for consistent storage and SMS sending
     * Removes formatting characters and standardizes format
     */
    private String cleanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return "";
        
        // Remove all non-digit characters except + at the beginning
        String cleaned = phoneNumber.replaceAll("[^\\d+]", "");
        
        // Ensure US numbers have country code
        if (cleaned.length() == 10 && !cleaned.startsWith("+")) {
            cleaned = "+1" + cleaned;
        }
        
        return cleaned;
    }
    
    /**
     * Validate phone number format
     * Ensures number is suitable for SMS sending
     */
    private boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) return false;
        
        // Basic validation - should have at least 10 digits
        String digitsOnly = phoneNumber.replaceAll("[^\\d]", "");
        return digitsOnly.length() >= 10;
    }
    
    /**
     * Show dialog for selecting which imported contacts to add as club members
     * Allows user to cherry-pick relevant contacts
     */
    private void showContactSelectionDialog(List<ClubMember> importedContacts) {
        // This will open a new activity with checkboxes for contact selection
        // For now, showing a simple confirmation
        new android.app.AlertDialog.Builder(this)
            .setTitle("Import Contacts")
            .setMessage("Found " + importedContacts.size() + " contacts. Import all as club members?")
            .setPositiveButton("Import All", (dialog, which) -> {
                contactManager.addMembers(importedContacts);
                loadExistingMembers();
                updateContactCount();
                statusTextView.setText("Imported " + importedContacts.size() + " club members");
            })
            .setNegativeButton("Select Specific", (dialog, which) -> {
                // TODO: Open detailed selection activity
                Toast.makeText(this, "Detailed selection coming in next version", Toast.LENGTH_SHORT).show();
            })
            .setNeutralButton("Cancel", null)
            .show();
    }
    
    /**
     * Open contact management interface
     * Allows adding, removing, and editing club member information
     */
    private void openContactManagement() {
        // TODO: Start ContactManagementActivity
        Toast.makeText(this, "Opening contact management...", Toast.LENGTH_SHORT).show();
        // Intent intent = new Intent(this, ContactManagementActivity.class);
        // startActivityForResult(intent, CONTACT_MANAGEMENT_REQUEST);
    }
    
    /**
     * Broadcast SMS message to all active club members
     * Core functionality - sends message to entire member list
     */
    private void broadcastMessage(String message) {
        if (activeMembers.isEmpty()) {
            Toast.makeText(this, "No club members to send to", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) 
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show confirmation dialog with member count
        new android.app.AlertDialog.Builder(this)
            .setTitle("Send Message")
            .setMessage("Send message to " + activeMembers.size() + " club members?")
            .setPositiveButton("Send", (dialog, which) -> {
                performBroadcast(message);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    /**
     * Perform the actual SMS broadcast operation
     * Handles sending, error tracking, and status updates
     */
    private void performBroadcast(String message) {
        statusTextView.setText("Sending messages...");
        sendButton.setEnabled(false);
        
        // Track sending statistics
        final int totalMembers = activeMembers.size();
        final int[] sentCount = {0};
        final int[] failedCount = {0};
        
        // Send messages in background thread
        new Thread(() -> {
            SmsManager smsManager = SmsManager.getDefault();
            
            for (ClubMember member : activeMembers) {
                try {
                    // Check if member has opted out
                    if (member.hasOptedOut()) {
                        continue; // Skip opted-out members
                    }
                    
                    // Send SMS
                    smsManager.sendTextMessage(
                        member.getPhoneNumber(),
                        null,
                        message,
                        null,
                        null
                    );
                    
                    sentCount[0]++;
                    
                    // Small delay to avoid overwhelming SMS service
                    Thread.sleep(100);
                    
                } catch (Exception e) {
                    failedCount[0]++;
                    // Log error for debugging
                    android.util.Log.e("ClubSMS", "Failed to send to " + member.getName(), e);
                }
            }
            
            // Update UI with results
            runOnUiThread(() -> {
                sendButton.setEnabled(true);
                String statusMessage = String.format("Sent: %d, Failed: %d", 
                    sentCount[0], failedCount[0]);
                statusTextView.setText(statusMessage);
                
                // Clear message field after successful send
                if (sentCount[0] > 0) {
                    messageEditText.setText("");
                    
                    // Save message to history
                    historyManager.addMessage(message, sentCount[0], failedCount[0]);
                }
                
                // Show completion notification
                Toast.makeText(this, "Broadcast complete: " + statusMessage, 
                    Toast.LENGTH_LONG).show();
            });
        }).start();
    }
    
    /**
     * Load existing club members from local storage
     * Populates the active members list on app startup
     */
    private void loadExistingMembers() {
        activeMembers = contactManager.getAllActiveMembers();
    }
    
    /**
     * Update the contact count display
     * Shows current number of active club members
     */
    private void updateContactCount() {
        String countText = "Club Members: " + activeMembers.size();
        contactCountTextView.setText(countText);
    }
    
    /**
     * Update send button state based on message content and member count
     * Ensures button is only enabled when message can be sent
     */
    private void updateSendButtonState() {
        boolean hasMessage = !messageEditText.getText().toString().trim().isEmpty();
        boolean hasMembers = !activeMembers.isEmpty();
        sendButton.setEnabled(hasMessage && hasMembers);
    }
    
    /**
     * Show explanation dialog for required permissions
     * Helps user understand why permissions are needed
     */
    private void showPermissionExplanation() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("This app needs:\n\n" +
                "• SMS Permission - To send messages to your club members\n" +
                "• Contacts Permission - To import members from your phone\n\n" +
                "No personal data is shared outside this app.")
            .setPositiveButton("Grant Permissions", (dialog, which) -> checkPermissions())
            .setNegativeButton("Continue Limited", null)
            .show();
    }
}