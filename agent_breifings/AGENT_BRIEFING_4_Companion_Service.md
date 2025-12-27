# AGENT BRIEFING #4: Phone Companion Service Developer
## Assignment: Build Lightweight Background Service & Lifecycle Manager

### Your Role
You are responsible for building the **service lifecycle management layer** that ensures the ClubSMS bridge remains available and responsive to desktop connections. This is the reliability layer that makes the desktop integration "just work" for users.

### Technology Stack
- **Language:** Java (maintain consistency with existing codebase)
- **Architecture:** Android Foreground Service + Broadcast Receivers
- **Integration:** Manages Network Bridge (Agent #3) lifecycle
- **Target Device:** Samsung Galaxy S20 5G (Android 13)

### Core Architecture

```
CompanionService (your work)
    ├─ Lifecycle Manager → Starts/stops BridgeService
    ├─ Network Monitor → WiFi state changes
    ├─ Connection Status → Desktop connectivity tracking  
    ├─ Notification Manager → User status updates
    ├─ Auto-Start System → Boot receiver, crash recovery
    └─ Battery Optimizer → Doze mode handling
```

### Critical Design Requirements

#### 1. Service Integration Strategy
**IMPORTANT:** You are NOT duplicating the Bridge functionality. Agent #3 builds the WebSocket server. You manage its lifecycle.

```java
public class CompanionService extends Service {
    private static final String TAG = "ClubSMS_Companion";
    private static final int NOTIFICATION_ID = 2001;
    private static final String CHANNEL_ID = "clubsms_companion";
    
    private Intent bridgeServiceIntent;
    private boolean bridgeServiceRunning = false;
    private ConnectionStatusManager statusManager;
    private NetworkStateReceiver networkReceiver;
    private NotificationManager notificationManager;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Companion Service initializing...");
        
        // Initialize components
        statusManager = new ConnectionStatusManager();
        networkReceiver = new NetworkStateReceiver();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        // Set up bridge service intent
        bridgeServiceIntent = new Intent(this, BridgeService.class);
        
        // Create notification channel and start foreground
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createServiceNotification("Initializing..."));
        
        // Register for network state changes
        IntentFilter networkFilter = new IntentFilter();
        networkFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        networkFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        registerReceiver(networkReceiver, networkFilter);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Companion service start command received");
        
        // Ensure bridge service is running
        ensureBridgeServiceRunning();
        
        // Monitor network connectivity
        scheduleNetworkMonitoring();
        
        // Request battery optimization exemption if needed
        requestBatteryOptimizations();
        
        // Return START_STICKY for automatic restart
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return new CompanionBinder();
    }
    
    public class CompanionBinder extends Binder {
        public CompanionService getService() {
            return CompanionService.this;
        }
    }
}
```

#### 2. Bridge Service Lifecycle Management

```java
private void ensureBridgeServiceRunning() {
    if (!isBridgeServiceRunning()) {
        Log.i(TAG, "Starting Bridge Service...");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(bridgeServiceIntent);
        } else {
            startService(bridgeServiceIntent);
        }
        
        bridgeServiceRunning = true;
        updateNotification("Bridge service started");
        
        // Verify bridge actually started
        new Handler().postDelayed(this::verifyBridgeStartup, 3000);
    }
}

private boolean isBridgeServiceRunning() {
    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    
    for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
        if (BridgeService.class.getName().equals(service.service.getClassName())) {
            return true;
        }
    }
    
    return false;
}

private void verifyBridgeStartup() {
    if (isBridgeServiceRunning()) {
        Log.i(TAG, "Bridge service verified running");
        updateNotification("Ready - Waiting for desktop connection");
        statusManager.setBridgeStatus(true);
    } else {
        Log.e(TAG, "Bridge service failed to start, retrying...");
        updateNotification("Bridge startup failed, retrying...");
        
        // Retry after delay
        new Handler().postDelayed(this::ensureBridgeServiceRunning, 5000);
    }
}

private void restartBridgeService() {
    Log.w(TAG, "Restarting bridge service...");
    
    // Stop existing service
    stopService(bridgeServiceIntent);
    bridgeServiceRunning = false;
    
    // Wait for cleanup
    new Handler().postDelayed(this::ensureBridgeServiceRunning, 2000);
}
```

#### 3. Network State Monitoring

```java
private class NetworkStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Network state change: " + action);
        
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            handleConnectivityChange();
        } else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 
                                               WifiManager.WIFI_STATE_UNKNOWN);
            handleWifiStateChange(wifiState);
        }
    }
}

private void handleConnectivityChange() {
    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    
    if (activeNetwork != null && activeNetwork.isConnected()) {
        if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
            Log.i(TAG, "WiFi connected - ensuring bridge service ready");
            
            // WiFi connected - ensure bridge is running
            new Handler().postDelayed(this::ensureBridgeServiceRunning, 1000);
            updateNotification("WiFi connected - Ready for desktop");
            
        } else {
            Log.w(TAG, "Non-WiFi connection active");
            updateNotification("WiFi needed for desktop connection");
        }
    } else {
        Log.w(TAG, "No network connection");
        updateNotification("Waiting for WiFi connection");
        statusManager.setNetworkStatus(false);
    }
}

private void handleWifiStateChange(int wifiState) {
    switch (wifiState) {
        case WifiManager.WIFI_STATE_ENABLED:
            Log.i(TAG, "WiFi enabled");
            statusManager.setNetworkStatus(true);
            break;
        case WifiManager.WIFI_STATE_DISABLED:
            Log.w(TAG, "WiFi disabled");
            updateNotification("WiFi disabled - Enable WiFi for desktop connection");
            statusManager.setNetworkStatus(false);
            break;
        case WifiManager.WIFI_STATE_DISABLING:
            Log.w(TAG, "WiFi disabling");
            updateNotification("WiFi disconnecting...");
            break;
    }
}
```

#### 4. Connection Status Management

```java
public class ConnectionStatusManager {
    private static final String TAG = "ConnectionStatus";
    private final List<ConnectionStatusListener> listeners = new ArrayList<>();
    
    private boolean bridgeRunning = false;
    private boolean networkAvailable = false;
    private boolean desktopConnected = false;
    private String desktopIP = null;
    private long lastConnectionTime = 0;
    
    public void setBridgeStatus(boolean running) {
        if (this.bridgeRunning != running) {
            this.bridgeRunning = running;
            Log.d(TAG, "Bridge status changed: " + running);
            notifyStatusChange();
        }
    }
    
    public void setNetworkStatus(boolean available) {
        if (this.networkAvailable != available) {
            this.networkAvailable = available;
            Log.d(TAG, "Network status changed: " + available);
            notifyStatusChange();
        }
    }
    
    public void setDesktopConnection(boolean connected, String ip) {
        boolean changed = (this.desktopConnected != connected) || 
                         !Objects.equals(this.desktopIP, ip);
        
        if (changed) {
            this.desktopConnected = connected;
            this.desktopIP = ip;
            
            if (connected) {
                this.lastConnectionTime = System.currentTimeMillis();
                Log.i(TAG, "Desktop connected from: " + ip);
            } else {
                Log.i(TAG, "Desktop disconnected");
            }
            
            notifyStatusChange();
        }
    }
    
    public String getStatusSummary() {
        if (desktopConnected) {
            return "Connected to desktop (" + desktopIP + ")";
        } else if (bridgeRunning && networkAvailable) {
            return "Ready - Waiting for desktop connection";
        } else if (bridgeRunning && !networkAvailable) {
            return "Bridge ready - Waiting for WiFi";
        } else if (!bridgeRunning) {
            return "Starting bridge service...";
        } else {
            return "Service initializing...";
        }
    }
    
    public boolean isReadyForConnection() {
        return bridgeRunning && networkAvailable;
    }
    
    private void notifyStatusChange() {
        for (ConnectionStatusListener listener : listeners) {
            listener.onStatusChanged(getStatusSummary(), isReadyForConnection());
        }
    }
    
    public interface ConnectionStatusListener {
        void onStatusChanged(String status, boolean ready);
    }
    
    public void addStatusListener(ConnectionStatusListener listener) {
        listeners.add(listener);
    }
    
    public void removeStatusListener(ConnectionStatusListener listener) {
        listeners.remove(listener);
    }
}
```

#### 5. Auto-Start System

```java
public class BootCompleteReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompleteReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            
            Log.i(TAG, "Boot completed - starting ClubSMS companion service");
            
            // Check if desktop mode was previously enabled
            SharedPreferences prefs = context.getSharedPreferences("clubsms", Context.MODE_PRIVATE);
            boolean desktopModeEnabled = prefs.getBoolean("desktop_mode_enabled", false);
            
            if (desktopModeEnabled) {
                Intent companionIntent = new Intent(context, CompanionService.class);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(companionIntent);
                } else {
                    context.startService(companionIntent);
                }
                
                Log.i(TAG, "Companion service started after boot");
            } else {
                Log.d(TAG, "Desktop mode disabled - not starting service");
            }
        }
    }
}

// Crash recovery receiver
public class ServiceRestartReceiver extends BroadcastReceiver {
    private static final String TAG = "ServiceRestartReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.w(TAG, "Service restart requested");
        
        Intent companionIntent = new Intent(context, CompanionService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(companionIntent);
        } else {
            context.startService(companionIntent);
        }
    }
}
```

#### 6. Battery Optimization Management

```java
private void requestBatteryOptimizations() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        String packageName = getPackageName();
        
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            Log.w(TAG, "App not whitelisted for battery optimization");
            
            // Schedule notification to prompt user
            new Handler().postDelayed(this::showBatteryOptimizationNotification, 5000);
        }
    }
}

private void showBatteryOptimizationNotification() {
    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
    intent.setData(Uri.parse("package:" + getPackageName()));
    
    PendingIntent pendingIntent = PendingIntent.getActivity(
        this, 100, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    
    Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_battery_optimization)
        .setContentTitle("ClubSMS Desktop - Battery Optimization")
        .setContentText("Tap to disable battery optimization for reliable desktop connection")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build();
    
    notificationManager.notify(NOTIFICATION_ID + 1, notification);
}

// Monitor for Doze mode entry/exit
private void registerDozeStateReceiver() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        IntentFilter dozeFilter = new IntentFilter();
        dozeFilter.addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED);
        
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                boolean isDozing = pm.isDeviceIdleMode();
                
                Log.d(TAG, "Doze mode changed: " + isDozing);
                
                if (isDozing) {
                    updateNotification("Doze mode - Limited connectivity");
                } else {
                    // Exiting Doze - verify bridge service
                    new Handler().postDelayed(() -> {
                        ensureBridgeServiceRunning();
                        updateNotification(statusManager.getStatusSummary());
                    }, 2000);
                }
            }
        }, dozeFilter);
    }
}
```

#### 7. User Interface Integration

```java
// Add to MainActivity.java
public class DesktopModeManager {
    private static final String PREFS_NAME = "clubsms";
    private static final String KEY_DESKTOP_MODE = "desktop_mode_enabled";
    
    private Context context;
    private SharedPreferences prefs;
    
    public DesktopModeManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public void enableDesktopMode() {
        prefs.edit().putBoolean(KEY_DESKTOP_MODE, true).apply();
        
        Intent companionIntent = new Intent(context, CompanionService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(companionIntent);
        } else {
            context.startService(companionIntent);
        }
        
        Log.i("DesktopMode", "Desktop mode enabled");
    }
    
    public void disableDesktopMode() {
        prefs.edit().putBoolean(KEY_DESKTOP_MODE, false).apply();
        
        context.stopService(new Intent(context, CompanionService.class));
        context.stopService(new Intent(context, BridgeService.class));
        
        Log.i("DesktopMode", "Desktop mode disabled");
    }
    
    public boolean isDesktopModeEnabled() {
        return prefs.getBoolean(KEY_DESKTOP_MODE, false);
    }
}

// In MainActivity onCreate()
private void setupDesktopModeToggle() {
    Switch desktopToggle = findViewById(R.id.desktop_mode_toggle);
    DesktopModeManager desktopManager = new DesktopModeManager(this);
    
    // Set current state
    desktopToggle.setChecked(desktopManager.isDesktopModeEnabled());
    
    desktopToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
        if (isChecked) {
            // Show confirmation dialog
            new AlertDialog.Builder(this)
                .setTitle("Enable Desktop Mode?")
                .setMessage("This will start a background service to connect to your desktop app. " +
                           "The service will use minimal battery and show a persistent notification.")
                .setPositiveButton("Enable", (dialog, which) -> {
                    desktopManager.enableDesktopMode();
                    showDesktopModeInstructions();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    desktopToggle.setChecked(false);
                })
                .show();
        } else {
            desktopManager.disableDesktopMode();
        }
    });
}
```

### Android Manifest Requirements

```xml
<!-- Companion Service -->
<service
    android:name=".companion.CompanionService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="dataSync"
    android:description="@string/companion_service_description" />

<!-- Boot receiver -->
<receiver
    android:name=".companion.BootCompleteReceiver"
    android:enabled="true"
    android:exported="true">
    <intent-filter android:priority="1000">
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</receiver>

<!-- Service restart receiver -->
<receiver
    android:name=".companion.ServiceRestartReceiver"
    android:enabled="true"
    android:exported="false" />

<!-- Additional permissions -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<!-- Doze mode exemption for critical functionality -->
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
```

### Integration Points & Dependencies

**You depend on:**
- **Network Bridge (Agent #3):** You manage the lifecycle of their BridgeService
- **Android v2.0 App (Agent #2):** Integrate with their MainActivity for UI controls

**Who depends on you:**
- **Desktop App (Agent #1):** Needs reliable service availability for connections
- **End Users:** Need seamless, battery-efficient background operation

### Testing Strategy

#### Phase 1: Service Lifecycle Testing
```bash
# Test service start/stop
adb shell am startservice com.clubsms/.companion.CompanionService
adb shell am stopservice com.clubsms/.companion.CompanionService

# Test boot receiver
adb shell am broadcast -a android.intent.action.BOOT_COMPLETED

# Monitor service state
adb shell dumpsys activity services | grep -A 10 CompanionService
```

#### Phase 2: Network State Testing
- Enable/disable WiFi - verify service handles transitions
- Switch WiFi networks - verify bridge service restarts properly  
- Enable airplane mode - verify graceful degradation
- Test in Doze mode - verify service survives and recovers

#### Phase 3: Battery Impact Testing
- Monitor battery usage over 2+ hours of operation
- Test with screen off for extended periods
- Verify service doesn't prevent device sleep
- Check wake lock usage and CPU consumption

### Performance Requirements

#### Battery Usage Targets:
- **Idle State:** <0.5% battery drain per hour
- **Active Connection:** <1% battery drain per hour  
- **Network Monitoring:** Minimal CPU usage for broadcast receivers
- **No Excessive Wake Locks:** Use PARTIAL_WAKE_LOCK sparingly

#### Memory Usage Targets:
- **Service Memory:** <10MB RAM consumption
- **No Memory Leaks:** Proper cleanup of receivers and callbacks
- **Efficient Monitoring:** Use system callbacks instead of polling

#### Reliability Targets:
- **Service Uptime:** 99% availability during active desktop sessions
- **Auto-Recovery:** Restart within 30 seconds after crash
- **Network Transitions:** Handle WiFi changes within 5 seconds

### Deliverables Timeline

#### Sprint 1 (8:30am - 10:30am): Foundation
- ✅ CompanionService skeleton with foreground notification
- ✅ Bridge service lifecycle management (start/stop/monitor)
- ✅ Basic network state monitoring
- ✅ Integration with MainActivity toggle

#### Sprint 2 (10:45am - 12:45pm): Core Functionality
- ✅ Connection status management system
- ✅ Auto-start on boot functionality  
- ✅ Battery optimization requests
- ✅ Service restart and recovery logic

#### Sprint 3 (1:15pm - 3:15pm): Polish & Optimization
- ✅ Doze mode handling
- ✅ Notification status updates
- ✅ Error handling and logging
- ✅ Integration testing with Bridge service

### Critical Constraints

1. **Service Type:** Must be foreground service with persistent notification
2. **Battery Impact:** Must request battery optimization exemption
3. **Network Dependency:** Only functional when WiFi is available
4. **Integration Boundary:** Manage Bridge service lifecycle, don't duplicate functionality
5. **Target Device:** Optimized for Samsung Galaxy S20 5G (Android 13)

### File Organization

#### Git Structure:
```bash
# Your dedicated branch
git checkout -b feature/companion-service

# Code organization within android-app-v2
app/src/main/java/com/clubsms/companion/
├── CompanionService.java
├── ConnectionStatusManager.java
├── BootCompleteReceiver.java
├── ServiceRestartReceiver.java
├── NetworkStateReceiver.java
└── DesktopModeManager.java
```

#### OneDrive Structure:
```
C:\Users\john\OneDrive\MyProjects\ClubSMS\
├── companion-service/                 # Your development workspace
│   ├── design-notes/
│   ├── battery-test-results/
│   └── service-logs/
└── android-app-v2/                   # Integration with main app
```

### Known Challenges & Solutions

#### 1. Android 13+ Notification Permissions
**Problem:** POST_NOTIFICATIONS permission required for foreground service
**Solution:** Request permission in MainActivity, graceful degradation if denied

```java
// In MainActivity
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) 
        != PackageManager.PERMISSION_GRANTED) {
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
    }
}
```

#### 2. Samsung-Specific Battery Optimization
**Problem:** Samsung's Device Care aggressively kills background services
**Solution:** Guide users to whitelist app in Device Care settings

#### 3. Service Discovery During Doze
**Problem:** Network discovery may fail when device is in deep sleep
**Solution:** Use high-priority notification to wake service when needed

### Success Criteria

#### Functional Requirements:
✅ CompanionService starts automatically and manages Bridge service lifecycle  
✅ Network state changes trigger appropriate service actions  
✅ Service survives device sleep, Doze mode, and memory pressure  
✅ Auto-starts on device boot when desktop mode is enabled  
✅ Battery usage remains under 1% per hour during active operation  
✅ User can easily enable/disable desktop mode via MainActivity toggle

#### Integration Requirements:
✅ Seamlessly manages Bridge service (Agent #3) without conflicts  
✅ Provides connection status updates to MainActivity UI  
✅ Handles service crashes and restarts automatically  
✅ Works reliably on Samsung Galaxy S20 5G with Android 13

#### Performance Requirements:
✅ Service consumes <10MB RAM during normal operation  
✅ Network state monitoring has negligible CPU impact  
✅ No memory leaks or resource accumulation over time  
✅ Graceful handling of low-memory conditions

---

## 🔋 CRITICAL BATTERY OPTIMIZATION NOTICE

**This service runs in the background constantly.** Every feature you add directly impacts battery life. Your primary responsibility is reliability with **minimal resource consumption**.

**Priority Order:**
1. **Service Reliability** - Must always restart Bridge when needed
2. **Battery Efficiency** - Optimize every operation for minimal power usage
3. **User Experience** - Clear status, easy controls
4. **Feature Completeness** - Only essential functionality

**Test extensively with real-world usage patterns:**
- Phone in pocket for hours with desktop app running
- Overnight testing with WiFi on/off cycles  
- Low battery scenarios (5-15% remaining)
- Background app refresh disabled

**Remember: Users will uninstall the app if it drains their battery. Battery efficiency is not optional - it's critical to success.** 🚀