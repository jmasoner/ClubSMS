# AGENT BRIEFING #2: Android v2.0 Lead
## Assignment: Upgrade Android App with MMS Support

### Your Role
Upgrade the existing ClubSMS Android v1.0 app to v2.0 by adding MMS support, improving UI, and preparing for desktop integration. You're building on the solid foundation of v1.0 while adding critical multimedia capabilities.

### Technology Stack
- **Language:** Java (existing codebase) or Kotlin (if refactoring)
- **Min SDK:** Android 21 (Lollipop)
- **Target SDK:** Android 33 (Tiramisu) 
- **Test Device:** Samsung Galaxy S20 5G (Android 13, One UI 5.1)
- **Build Tools:** Gradle 8.0+, Android Studio 2023.1+

### Base Code Location
- **Existing v1.0 code:** `/home/user/ClubSMSApp/`
- **Your branch:** `feature/android-v2-mms`
- **Save to:** `C:\Users\john\OneDrive\MyProjects\ClubSMS\android-app-v2\`

### New Features to Add

#### 1. MMS Support (CRITICAL - Sprint 1 & 2)
- **Media Picker Integration:**
  - Gallery image selection using `Intent.ACTION_PICK`
  - Camera capture integration for new photos
  - Support PNG, JPEG formats only
  - File size validation before processing

- **Image Compression Engine:**
  - Target size: 300KB maximum (carrier-safe)
  - Maintain aspect ratio during compression
  - Quality degradation algorithm (90% → 70% → 50% → 30%)
  - Progress indicator during compression

- **MMS Sending Implementation:**
  - Use `SmsManager.sendMultimediaMessage()` for Android 5.0+
  - Fallback to Intent-based sending for compatibility
  - Handle different carrier MMS configurations
  - Delivery receipt processing for MMS

- **Image Preview System:**
  - Thumbnail generation and caching
  - Full-size preview modal
  - Remove/replace image functionality
  - Image rotation and basic editing

#### 2. Enhanced SMS/MMS Toggle UI (Sprint 1)
- **Toggle Control:**
  - Material Design Switch component
  - Clear visual distinction between SMS/MMS modes
  - Cost estimation display (SMS: $0.01, MMS: $0.10)
  - Data usage warning for MMS mode

- **Dynamic UI Changes:**
  - Show/hide image picker based on toggle state
  - Update character counter (SMS: 160 chars, MMS: 1000 chars)
  - Change send button text and color
  - Display estimated delivery time difference

#### 3. Improved Contact Management (Sprint 2)
- **Performance Optimization:**
  - Implement contact loading in chunks (100 contacts per batch)
  - Background threading for contact operations
  - Search index creation for fast filtering
  - Memory usage optimization for large contact lists

- **Batch Import Operations:**
  - CSV import with progress dialog
  - Duplicate detection and merge options
  - Error handling for malformed data
  - Import history and rollback capability

- **Enhanced Contact Display:**
  - Contact photos from phone contacts
  - Last message sent timestamp
  - Delivery success rate per contact
  - Group/category assignment

#### 4. Advanced Message History (Sprint 2 & 3)
- **Media Message History:**
  - Separate view for SMS vs MMS messages
  - Thumbnail grid for MMS images
  - Image viewer for sent MMS content
  - Storage management for media files

- **Enhanced Filtering:**
  - Filter by message type (SMS/MMS/All)
  - Date range picker
  - Recipient-based filtering
  - Success/failure status filtering

### Updated Code Architecture

```
app/src/main/java/com/clubsms/
├── MainActivity.java (MAJOR UPDATE)
│   ├── Add MMS toggle handling
│   ├── Image picker integration
│   ├── Enhanced UI for v2.0 features
│   └── Bridge service communication
├── ClubContactManager.java (UPDATE)
│   ├── Batch import optimization
│   ├── Performance improvements
│   └── Contact photo integration
├── MessageHistoryManager.java (UPDATE)
│   ├── MMS record support
│   ├── Media file management
│   └── Enhanced search capabilities
├── ClubMember.java (existing - minimal changes)
├── MessageRecord.java (UPDATE)
│   ├── Add media_path field
│   ├── Add media_type field
│   └── Add file_size field
├── MMSSender.java (NEW)
│   ├── MMS composition and sending
│   ├── Carrier compatibility layer
│   └── Delivery tracking
├── MediaCompressor.java (NEW)
│   ├── Image compression algorithms
│   ├── Size optimization
│   └── Quality management
├── BridgeService.java (NEW - Phase 2)
│   ├── WebSocket server for desktop
│   ├── Command processing
│   └── Status reporting
└── ui/
    ├── ImagePreviewActivity.java (NEW)
    ├── MediaPickerFragment.java (NEW)
    └── AdvancedHistoryActivity.java (NEW)
```

### Critical Implementation Details

#### Android Manifest Updates
```xml
<!-- Existing permissions -->
<uses-permission android:name="android.permission.SEND_SMS"/>
<uses-permission android:name="android.permission.READ_SMS"/>
<uses-permission android:name="android.permission.RECEIVE_SMS"/>
<uses-permission android:name="android.permission.READ_CONTACTS"/>

<!-- NEW MMS Permissions -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
                 android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
                 android:maxSdkVersion="28" />

<!-- NEW Bridge Service Permissions -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

<!-- Service Declaration -->
<service android:name=".BridgeService" 
         android:exported="false" 
         android:enabled="true" />
```

#### MMSSender.java Implementation
```java
public class MMSSender {
    private static final String TAG = "MMSSender";
    private static final int MAX_MMS_SIZE = 300 * 1024; // 300KB
    private static final int MAX_MMS_WIDTH = 1024;
    private static final int MAX_MMS_HEIGHT = 1024;
    
    public static class MMSResult {
        public boolean success;
        public String messageId;
        public String errorMessage;
        public int finalFileSize;
    }
    
    public static MMSResult sendMMS(Context context, String phoneNumber, 
                                   String message, Uri imageUri) {
        MMSResult result = new MMSResult();
        
        try {
            // Step 1: Compress image to acceptable size
            Bitmap compressedBitmap = MediaCompressor.compressImage(
                context, imageUri, MAX_MMS_SIZE, MAX_MMS_WIDTH, MAX_MMS_HEIGHT);
            
            if (compressedBitmap == null) {
                result.success = false;
                result.errorMessage = "Failed to compress image";
                return result;
            }
            
            // Step 2: Save compressed image temporarily
            File tempFile = saveBitmapToTemp(context, compressedBitmap);
            Uri compressedUri = FileProvider.getUriForFile(
                context, "com.clubsms.fileprovider", tempFile);
            result.finalFileSize = (int) tempFile.length();
            
            // Step 3: Send MMS using SmsManager (Android 5.0+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                result.messageId = sendMMSLollipop(context, phoneNumber, message, compressedUri);
            } else {
                // Fallback to Intent method
                result.messageId = sendMMSIntent(context, phoneNumber, message, compressedUri);
            }
            
            result.success = true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to send MMS", e);
            result.success = false;
            result.errorMessage = "MMS send failed: " + e.getMessage();
        }
        
        return result;
    }
    
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static String sendMMSLollipop(Context context, String phone, 
                                         String text, Uri imageUri) {
        // Implementation using SmsManager.sendMultimediaMessage()
        SmsManager smsManager = SmsManager.getDefault();
        String messageId = UUID.randomUUID().toString();
        
        // Create MMS parts
        ArrayList<String> parts = new ArrayList<>();
        parts.add(text);
        
        // Add image part
        // Note: Full implementation requires complex MMS PDU construction
        // This is a simplified version - production code needs more work
        
        return messageId;
    }
    
    private static String sendMMSIntent(Context context, String phone, 
                                       String text, Uri imageUri) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/*");
        intent.putExtra("address", phone);
        intent.putExtra("sms_body", text);
        intent.putExtra(Intent.EXTRA_STREAM, imageUri);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        // This will open the default messaging app
        context.startActivity(Intent.createChooser(intent, "Send MMS"));
        
        return UUID.randomUUID().toString();
    }
}
```

#### MediaCompressor.java Implementation
```java
public class MediaCompressor {
    private static final String TAG = "MediaCompressor";
    
    public static Bitmap compressImage(Context context, Uri imageUri, 
                                     int maxSize, int maxWidth, int maxHeight) {
        try {
            // Step 1: Get image dimensions without loading full image
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            
            // Step 2: Calculate sample size to reduce memory usage
            int sampleSize = calculateSampleSize(options, maxWidth, maxHeight);
            
            // Step 3: Load and scale image
            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;
            options.inPreferredConfig = Bitmap.Config.RGB_565; // Use less memory
            
            inputStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            
            if (bitmap == null) {
                Log.e(TAG, "Failed to decode bitmap");
                return null;
            }
            
            // Step 4: Compress to target file size
            return compressToSize(bitmap, maxSize);
            
        } catch (Exception e) {
            Log.e(TAG, "Error compressing image", e);
            return null;
        }
    }
    
    private static Bitmap compressToSize(Bitmap bitmap, int maxSize) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        int quality = 90; // Start with high quality
        
        do {
            stream.reset();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
            quality -= 10;
        } while (stream.toByteArray().length > maxSize && quality > 10);
        
        if (stream.toByteArray().length > maxSize) {
            // If still too large, scale down the bitmap
            float scaleFactor = (float) Math.sqrt((double) maxSize / stream.toByteArray().length);
            int newWidth = Math.round(bitmap.getWidth() * scaleFactor);
            int newHeight = Math.round(bitmap.getHeight() * scaleFactor);
            
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            if (scaledBitmap != bitmap) {
                bitmap.recycle(); // Free memory
            }
            
            stream.reset();
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
            return scaledBitmap;
        }
        
        return bitmap;
    }
    
    private static int calculateSampleSize(BitmapFactory.Options options, 
                                          int maxWidth, int maxHeight) {
        int width = options.outWidth;
        int height = options.outHeight;
        int sampleSize = 1;
        
        if (height > maxHeight || width > maxWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            
            while ((halfHeight / sampleSize) >= maxHeight && 
                   (halfWidth / sampleSize) >= maxWidth) {
                sampleSize *= 2;
            }
        }
        
        return sampleSize;
    }
}
```

#### Updated activity_main.xml Layout
```xml
<!-- Add after existing message composition area -->

<!-- SMS/MMS Toggle Section -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:padding="16dp"
    android:background="@color/toggle_background">
    
    <TextView
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:text="Message Type:"
        android:textSize="16sp"
        android:textColor="@color/primary_text" />
        
    <Switch
        android:id="@+id/sms_mms_toggle"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="MMS (with image)"
        android:textSize="14sp"
        android:checked="false"
        android:thumbTint="@color/switch_thumb"
        android:trackTint="@color/switch_track" />
</LinearLayout>

<!-- Cost Estimation Display -->
<TextView
    android:id="@+id/cost_estimate"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="8dp"
    android:text="Estimated cost: $0.01 per SMS"
    android:textSize="12sp"
    android:textColor="@color/secondary_text"
    android:background="@color/info_background" />

<!-- MMS Image Selection (Initially Hidden) -->
<LinearLayout
    android:id="@+id/mms_section"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="16dp"
    android:visibility="gone">
    
    <Button
        android:id="@+id/pick_image_button"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="📷 Select Image"
        android:textSize="16sp"
        android:background="@drawable/button_secondary"
        android:textColor="@color/button_text" />
        
    <!-- Image Preview Area -->
    <FrameLayout
        android:id="@+id/image_preview_container"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:visibility="gone">
        
        <ImageView
            android:id="@+id/image_preview"
            android:layout_width="match_parent"
            android:layout_height="200dp"
            android:scaleType="centerCrop"
            android:background="@drawable/image_preview_border" />
            
        <!-- Remove Image Button -->
        <ImageButton
            android:id="@+id/remove_image_button"
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:layout_gravity="top|end"
            android:layout_margin="8dp"
            android:background="@drawable/circle_button_red"
            android:src="@drawable/ic_close_white"
            android:contentDescription="Remove image" />
            
        <!-- Image Size Info -->
        <TextView
            android:id="@+id/image_size_info"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_gravity="bottom|start"
            android:layout_margin="8dp"
            android:background="@drawable/text_background_semi_transparent"
            android:padding="4dp"
            android:text="256 KB"
            android:textColor="@android:color/white"
            android:textSize="10sp" />
    </FrameLayout>
</LinearLayout>
```

### Bridge Integration (Phase 2)

#### BridgeService.java Structure
```java
public class BridgeService extends Service {
    private static final String TAG = "BridgeService";
    private static final int WEBSOCKET_PORT = 8443;
    private WebSocketServer wsServer;
    private boolean isRunning = false;
    
    public class BridgeBinder extends Binder {
        BridgeService getService() {
            return BridgeService.this;
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return new BridgeBinder();
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        startWebSocketServer();
        createNotificationChannel();
        startForeground(1, createServiceNotification());
    }
    
    private void startWebSocketServer() {
        try {
            wsServer = new WebSocketServer(new InetSocketAddress(WEBSOCKET_PORT)) {
                @Override
                public void onOpen(WebSocket conn, ClientHandshake handshake) {
                    Log.d(TAG, "Desktop client connected: " + conn.getRemoteSocketAddress());
                    sendDeviceInfo(conn);
                }
                
                @Override
                public void onMessage(WebSocket conn, String message) {
                    handleDesktopCommand(conn, message);
                }
                
                @Override
                public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                    Log.d(TAG, "Desktop client disconnected: " + reason);
                }
                
                @Override
                public void onError(WebSocket conn, Exception ex) {
                    Log.e(TAG, "WebSocket error", ex);
                }
                
                @Override
                public void onStart() {
                    Log.i(TAG, "WebSocket server started on port " + WEBSOCKET_PORT);
                    isRunning = true;
                }
            };
            
            wsServer.start();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start WebSocket server", e);
        }
    }
    
    private void handleDesktopCommand(WebSocket conn, String message) {
        try {
            JSONObject command = new JSONObject(message);
            String type = command.getString("type");
            
            switch (type) {
                case "CONNECT":
                    handleConnectCommand(conn, command);
                    break;
                case "SEND_SMS":
                    handleSendSMSCommand(conn, command);
                    break;
                case "SEND_MMS":
                    handleSendMMSCommand(conn, command);
                    break;
                case "GET_CONTACTS":
                    handleGetContactsCommand(conn, command);
                    break;
                default:
                    sendError(conn, "Unknown command type: " + type);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling desktop command", e);
            sendError(conn, "Command processing failed: " + e.getMessage());
        }
    }
}
```

### Testing Strategy

#### Samsung Galaxy S20 5G Testing Checklist:
1. **MMS Functionality:**
   - [ ] Send MMS with image to another Android phone
   - [ ] Send MMS with image to iPhone
   - [ ] Verify image compression keeps files under 300KB
   - [ ] Test with different image formats (PNG, JPEG)
   - [ ] Test with various image sizes (small, medium, large)

2. **Carrier Compatibility:**
   - [ ] Test on Verizon network
   - [ ] Test on AT&T network  
   - [ ] Test on T-Mobile network
   - [ ] Verify delivery receipts work

3. **Performance Testing:**
   - [ ] Import 1000+ contacts without app crashes
   - [ ] Send broadcast to 100+ recipients
   - [ ] UI remains responsive during operations
   - [ ] Memory usage stays reasonable

4. **Integration Testing:**
   - [ ] WebSocket server starts correctly
   - [ ] Desktop can connect to bridge service
   - [ ] Commands from desktop execute properly

### Dependencies & Build Configuration

#### Updated build.gradle (app module):
```gradle
android {
    compileSdk 33
    
    defaultConfig {
        applicationId "com.clubsms"
        minSdk 21
        targetSdk 33
        versionCode 2
        versionName "2.0.0"
        
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }
    
    buildFeatures {
        viewBinding true
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    // Existing dependencies
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.10.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'com.google.code.gson:gson:2.10.1'
    
    // NEW for MMS and media handling
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    annotationProcessor 'com.github.bumptech.glide:compiler:4.16.0'
    implementation 'androidx.activity:activity:1.8.0' // For new photo picker
    
    // NEW for Bridge service
    implementation 'org.java-websocket:Java-WebSocket:1.5.4'
    implementation 'androidx.work:work-runtime:2.8.1' // For background tasks
    
    // Testing
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}
```

### Integration Dependencies

**You depend on:**
- **API_SPECIFICATION_v2.0.md:** For exact WebSocket message formats
- **Network Bridge (Agent #3):** For desktop-to-phone communication protocol
- **Phone Companion (Agent #4):** For service integration patterns

**Who depends on you:**
- **Desktop App (Agent #1):** Needs MMS working to test end-to-end flows
- **Integration Team:** For final system testing

### Deliverables & Timeline

#### Sprint 1 (8:30am - 10:30am): MMS Foundation
- ✅ MMS permissions added to AndroidManifest.xml
- ✅ UI updated with SMS/MMS toggle switch
- ✅ Image picker functionality implemented
- ✅ Basic MediaCompressor class created
- ✅ Cost estimation display working

#### Sprint 2 (10:45am - 12:45pm): MMS Implementation
- ✅ MMSSender class fully implemented
- ✅ Image compression working and tested
- ✅ MMS sending functional on Samsung S20
- ✅ Image preview and removal working
- ✅ Contact import performance improved

#### Sprint 3 (1:15pm - 3:15pm): Bridge & Polish
- ✅ BridgeService skeleton created and running
- ✅ WebSocket server accepting connections
- ✅ Message history updated for MMS records
- ✅ All v1.0 features verified still working
- ✅ Ready for integration with desktop app

### Critical Constraints

1. **Device Compatibility:** Must work flawlessly on Samsung Galaxy S20 5G
2. **Carrier Support:** Test MMS on all major US carriers
3. **Backward Compatibility:** All v1.0 features must continue working
4. **File Size Management:** Keep APK under 10MB, compressed images under 300KB
5. **API Compliance:** Follow API_SPECIFICATION_v2.0.md exactly for bridge communication

### File Management

#### Git Workflow:
```bash
# Start from existing v1.0 codebase
git checkout main
git pull origin main
git checkout -b feature/android-v2-mms

# Regular commits with clear messages
git add .
git commit -m "feat: add MMS toggle UI and image picker"
git push origin feature/android-v2-mms

# Create APK builds for testing
./gradlew assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk /path/to/onedrive/builds/
```

#### OneDrive Organization:
```
C:\Users\john\OneDrive\MyProjects\ClubSMS\
├── android-app-v2/          # Your source code
├── builds/                  # APK files for testing
│   ├── app-v2.0-debug.apk
│   └── app-v2.0-release.apk
├── test-media/             # Sample images for testing
└── docs/                   # API specs and briefings
```

### Escalation Procedures

#### Immediate Escalation (Contact John):
- MMS not sending on Samsung S20 after multiple attempts
- WebSocket server won't start or accept connections
- App crashes when handling large contact lists
- API specification unclear or contradictory

#### Sync Point Status Reports:
- **10:30am:** "MMS UI complete, image picker working, starting compression"
- **12:45pm:** "MMS sending successful, performance improved, starting bridge"
- **3:15pm:** "All features complete, bridge service running, ready for integration"

### Success Criteria

#### Functional Requirements:
✅ SMS sending works exactly as v1.0 (no regressions)  
✅ MMS with image attachment sends successfully  
✅ Image compression produces files under 300KB consistently  
✅ SMS/MMS toggle changes UI and behavior appropriately  
✅ Contact import handles 1000+ contacts without crashes  
✅ Bridge service starts and accepts WebSocket connections  

#### Technical Requirements:
✅ Follows exact API specification for bridge communication  
✅ Proper error handling for all failure scenarios  
✅ No memory leaks with large images or contact lists  
✅ Professional UI that matches v1.0 design language  

#### Testing Requirements:
✅ Verified working on actual Samsung Galaxy S20 5G device  
✅ Tested with real carrier networks (not just emulator)  
✅ All unit tests pass  
✅ Integration tests with bridge service work  

---

## 🚨 CRITICAL REMINDERS

1. **REAL DEVICE TESTING:** Use actual Samsung S20, not emulator - MMS behavior is completely different
2. **CARRIER TESTING:** Test on real carrier networks - MMS routing varies significantly
3. **API COMPLIANCE:** Follow API_SPECIFICATION_v2.0.md exactly - no creative interpretations
4. **BACKWARD COMPATIBILITY:** Don't break any v1.0 functionality - users depend on it
5. **PERFORMANCE:** Large contact lists must remain fast - club organizers have thousands of members

**Questions? Ask immediately. Blockers? Escalate instantly. Assumptions? Don't make them.**

**Your MMS implementation is critical - the entire desktop integration depends on it working flawlessly.** 🚀