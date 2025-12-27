# ClubSMS v2.0 - Build Instructions

## Prerequisites

1. **Android Studio** (recommended) OR **Command-line tools**
2. **Android SDK** with:
   - SDK Platform 34 (Android 14)
   - Build Tools 34.0.0
   - Platform Tools
3. **Java JDK 17** or higher

---

## Option A: Build with Android Studio

### 1. Open Project
1. Open Android Studio
2. File → Open
3. Navigate to `source_code` folder
4. Wait for Gradle sync to complete

### 2. Build Debug APK
1. Build → Build Bundle(s) / APK(s) → Build APK(s)
2. APK will be at: `app/build/outputs/apk/debug/`

### 3. Install on Phone
1. Connect Samsung S20 via USB
2. Enable "USB Debugging" on phone
3. Run → Run 'app' (or click green play button)

---

## Option B: Command Line Build (Windows)

### 1. Set Environment Variables
Open PowerShell and run:
```powershell
# Set ANDROID_HOME (adjust path to your SDK location)
$env:ANDROID_HOME = "C:\Users\john\AppData\Local\Android\Sdk"
$env:PATH += ";$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\tools\bin"
```

### 2. Navigate to Project
```powershell
cd C:\Users\john\OneDrive\MyProjects\ClubSMS\source_code
```

### 3. Build Debug APK
```powershell
# Use Gradle Wrapper (recommended)
.\gradlew assembleDebug

# APK location: app\build\outputs\apk\debug\app-debug.apk
```

### 4. Install on Phone
```powershell
# Check phone is connected
adb devices

# Install APK
adb install app\build\outputs\apk\debug\app-debug.apk
```

---

## First-Time Setup

If Gradle Wrapper files are missing, create them:

```powershell
# In source_code directory
gradle wrapper --gradle-version 8.2
```

Or download from a working Android project.

---

## Troubleshooting

### "SDK location not found"
Create `local.properties` in `source_code/`:
```properties
sdk.dir=C\:\\Users\\john\\AppData\\Local\\Android\\Sdk
```

### "Java version mismatch"
Ensure JAVA_HOME points to JDK 17+:
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
```

### Build fails on first try
```powershell
# Clean and rebuild
.\gradlew clean
.\gradlew assembleDebug
```

### Phone not detected
1. Enable Developer Options: Settings → About → Tap "Build Number" 7 times
2. Enable USB Debugging: Settings → Developer Options → USB Debugging
3. Authorize computer when prompted on phone

---

## Testing the Bridge

1. Build and install APK on phone
2. Open ClubSMS app
3. Enable "Desktop Mode" toggle
4. Note the IP address shown (e.g., `ws://192.168.1.105:8765`)
5. Connect from Desktop app using that URL

---

## Quick Test with wscat

Install wscat globally:
```powershell
npm install -g wscat
```

Test connection to phone:
```bash
wscat -c ws://192.168.1.105:8765/clubsms
```

Send test ping:
```json
{"type":"PING","timestamp":"2025-12-26T12:00:00Z"}
```

Should receive:
```json
{"type":"PONG","timestamp":"..."}
```

---

## Files Created by Claude

### Bridge Package
- `app/src/main/java/com/clubsms/bridge/BridgeService.java`
- `app/src/main/java/com/clubsms/bridge/BridgeWebSocketServer.java`
- `app/src/main/java/com/clubsms/bridge/CommandProcessor.java`
- `app/src/main/java/com/clubsms/bridge/StatusReporter.java`

### Companion Package
- `app/src/main/java/com/clubsms/companion/CompanionService.java`
- `app/src/main/java/com/clubsms/companion/NetworkStateReceiver.java`
- `app/src/main/java/com/clubsms/companion/BootCompleteReceiver.java`

### Resources
- `app/src/main/res/drawable/*.xml` (icons and backgrounds)

### Modified Files
- `app/build.gradle` (added WebSocket dependency)
- `app/src/main/AndroidManifest.xml` (added services/receivers)
- `app/src/main/res/values/strings.xml` (desktop mode strings)
- `app/src/main/res/values/colors.xml` (desktop mode colors)
- `app/src/main/res/layout/activity_main.xml` (desktop mode card)
- `app/src/main/java/com/clubsms/MainActivity.java` (desktop mode toggle)

---

Good luck! 🚀

