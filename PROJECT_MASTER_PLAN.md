# PROJECT MASTER PLAN

## 
        ClubSMS v2.0 Development Sprint

**Date:** December 25, 2025

**Project Lead:** Genspark (AI Architect)

**Target System:** Windows 11 / Android 13

**Repository:** github.com/jmasoner/ClubSMS

**Sprint Duration:** 1 Day (10.5 Hours)

**Status:** APPROVED

    <!-- 1. Executive Summary -->
    
# 1. Executive Summary

      The goal of this intensive 1-day sprint is to upgrade the ClubSMS platform from a standalone Android application
      to a **comprehensive desktop-mobile hybrid system**. The new architecture enables club organizers to
      manage contacts and compose messages on a Windows desktop environment while leveraging their Android device's
      cellular connection for message delivery.

### Strategic Objectives

- **Professional Workflow:** Shift management and composition to the Desktop (Windows 11) for
        efficiency.
- **Rich Media Support:** Implement MMS capabilities to support image broadcasting alongside SMS.
- **Privacy & Security:** Maintain a "Local-First" architecture using local WiFi/Network bridging,
        eliminating cloud dependencies.

### Success Criteria

- ✅ Functional Electron Desktop App running on Windows 11.
- ✅ Android Companion App successfully receiving commands in background.
- ✅ Successful sending of 1 SMS and 1 MMS (Image) via Desktop trigger.
- ✅ Contact synchronization between Desktop and Phone.
- ✅ Zero data leakage to external cloud servers.

**Note:** This is an aggressive timeline. All agents must adhere strictly to the API specifications
      defined in Section 4 to ensure seamless integration at 5:30 PM.

    <!-- 2. System Architecture -->
    
# 2. System Architecture

      The system utilizes a **Master-Slave** architecture where the Desktop App acts as the command center
      (Master) and the Android device acts as the cellular modem (Slave). Communication occurs over a secure local
      WebSocket connection.

+-------------------------+ +----------------------------+ | WINDOWS DESKTOP PC | | ANDROID PHONE (S20 5G) | |
      (Master Node) | | (Worker Node) | | | | | | +-------------------+ | | +----------------------+ | | | ELECTRON APP
      | | | | COMPANION SERVICE | | | | (React Frontend) | | WiFi | | (Background Service)| | | | [SQLite DB]
      |<------------->| | [SmsManager API] | | | | [WebSocket Srvr] | | WebSocket | | [MMS API] | | |
      +-------------------+ | (JSON) | +----------+-----------+ | | ^ | | | | +-----------|-------------+
      +-------------|--------------+ | | | v +------+------+ (( CELLULAR NETWORK )) | LOCAL FILE | (SMS / MMS) | SYSTEM
      | | +-------------+ v [ CLUB MEMBERS ]

### Component Overview

| Component | Tech Stack | Responsibility |
| --- | --- | --- |
| **Desktop App** | Electron, React, SQLite, Node.js | User Interface, Data Persistence, Command Generation, Network Bridge Host. |
| **Android App v2** | Java/Kotlin, Android SDK 33 | Standard Mobile UI (legacy), MMS Composition, Contact Export. |
| **Companion Service** | Android Background Service | Headless listener. Receives JSON payloads, triggers Android SMS/MMS APIs. |
| **Network Bridge** | Node.js WebSocket (ws) | Manages connection, heartbeats, and data serialization between Desktop and Phone. |

    <!-- 3. Technical Specifications -->
    
# 3. Technical Specifications

### 3.1 Component 1: Desktop Application

- **Target OS:** Windows 11 (Primary)
- **Framework:** Electron 25+, React 18, TailwindCSS (for speed).
- **Database:** `better-sqlite3` for local contact storage.
- **Key Modules:**
    - `/src/main/bridge.ts`: WebSocket server logic.
    - `/src/renderer/components/Composer.tsx`: Rich text/Image input.
    - `/src/renderer/components/Contacts.tsx`: Grid view for 1000+ contacts.

### 3.2 Component 2: Android App v2.0 (Hybrid)

- **Base:** Existing ClubSMS v1.0 Java codebase.
- **New Permissions:** `READ_MEDIA_IMAGES`, `ACCESS_WIFI_STATE`, `INTERNET` (Local LAN only).
- **MMS Implementation:**
    - Use `SmsManager.sendMultimediaMessage`.
    - Implement Bitmap compression (Max 300KB per image).
- **Target Device:** Samsung S20 5G (Android 13/One UI 5.1).

### 3.3 Component 3: Network Bridge

- **Protocol:** WebSocket (ws://).
- **Discovery:** Desktop broadcasts UDP beacon or manual IP entry on Phone.
- **Security:** Simple handshake with a 4-digit PIN generated on Desktop.
- **Fallback:** If WiFi fails, assume manual syncing via CSV export/import (Bluetooth scope reduced
        to "Nice to Have" for 1-day sprint).

### 3.4 Component 4: Phone Companion Service

- **Type:** Foreground Service (to prevent OS killing it).
- **Notification:** Persistent notification "ClubSMS Bridge Active".
- **Wake Lock:** Partial wake lock required during broadcast sessions.

    <!-- 4. API Specification -->
    
# 4. API Specification (Internal Protocol)

All agents must adhere strictly to this JSON structure for WebSocket communication.

#### A. Handshake (Phone -> Desktop)

    {
      "type": "HANDSHAKE",
      "payload": {
        "deviceName": "Samsung S20",
        "batteryLevel": 85,
        "ipAddress": "192.168.1.105"
      }
    }

#### B. Send Command (Desktop -> Phone)

    {
      "type": "SEND_BROADCAST",
      "id": "uuid-v4-string",
      "payload": {
        "mode": "SMS" | "MMS",
        "recipients": ["+15550199", "+15550200"],
        "messageBody": "Club meeting tonight!",
        "imageBase64": "..." // Null if mode is SMS
      }
    }

#### C. Delivery Report (Phone -> Desktop)

    {
      "type": "REPORT_STATUS",
      "payload": {
        "messageId": "uuid-v4-string",
        "recipient": "+15550199",
        "status": "SENT" | "DELIVERED" | "FAILED",
        "timestamp": 1672531200000
      }
    }

    <!-- 5. Agent Assignments -->
    
# 5. Agent Assignments

| Agent | Role | Primary Responsibility | Output Directory |
| --- | --- | --- | --- |
| **Genspark (Me)** | **Architect / PM** | Master Plan, Documentation updates, Integration coordination. | `/docs` |
| **Cursor/Deepseek #1** | **Desktop Lead** | Build Electron App shell, React UI, and SQLite integration. | `/desktop-app` |
| **Cursor/Deepseek #2** | **Android Lead** | Upgrade Android App to v2.0, implement MMS logic, UI updates. | `/android-app` |
| **Cursor/Grok** | **Network Engineer** | Build Node.js WebSocket Server (Bridge) and Android WebSocket Client. | `/modules/bridge` |
| **Cursor/Ollama** | **Service Engineer** | Create Android Background Service to listen to Bridge and trigger Android Lead's Send methods. | `/android-app/service` |

    <!-- 6. Development Workflow -->
    
# 6. Development Workflow

### File Structure (OneDrive/GitHub)

    /ClubSMS
      /docs                 (Specs & Manuals)
      /desktop-app          (Electron Source)
      /android-app          (Android Source)
        /app/src/main/java/com/clubsms/service  (Companion Code)
      /shared-types         (JSON schemas)

### Rules of Engagement

1. **No Rogue Commits:** Do not modify `API_SPECIFICATION.md` without PM approval.
2. **Mock First:** Desktop Agent must mock the Phone connection to build UI before the Bridge is
        ready.
3. **Hardcoded IP:** For speed, we will allow manual IP entry in the Desktop App to find the Phone.

    <!-- 7. Detailed Timeline -->
    
# 7. Detailed Timeline (10.5 Hours)

| Time | Phase | Goal |
| --- | --- | --- |
| 08:00 - 08:30 | **Briefing** | Agents ingest Master Plan. Environment setup. |
| 08:30 - 10:30 | **Sprint 1: Foundations** | - Desktop: Hello World Electron + Contact Import UI.  <br>
            - Android: MMS Permissions + Image Picker.  <br>
            - Bridge: WebSocket Server echoing messages. |
| 10:30 - 10:45 | **SYNC POINT 1** | Verify Electron runs on Windows. Verify Android builds. |
| 10:45 - 12:45 | **Sprint 2: Logic** | - Desktop: Connect SQLite, generate JSON payloads.  <br>
            - Android: Implement `sendMultimediaMessage`.  <br>
            - Companion: Service starts on boot. |
| 12:45 - 13:15 | **LUNCH / INTEGRATION** | Test WebSocket connection between Desktop and Emulator/Phone. |
| 13:15 - 15:15 | **Sprint 3: The Bridge** | - Connect Desktop Command -> Bridge -> Android Service.  <br>
            - Handle "Image Selection" on Desktop converting to Base64. |
| 15:15 - 15:30 | **SYNC POINT 2** | Code Freeze for new features. Only bug fixes. |
| 15:30 - 17:30 | **Final Integration** | Full End-to-End testing. Sending real messages to test numbers. |
| 17:30 - 18:30 | **Polish & Docs** | Update User Manual. Package Installers. |

**Risk Warning:** The most likely failure point is the Android Background Service being killed by
      Samsung's "One UI" battery optimization. The Android Lead must implement a Foreground Service with a persistent
      notification to prevent this.

    <!-- 8. Integration Checklist -->
    
# 8. Integration Checklist

- [ ] Desktop App launches on Windows 11 without errors.
- [ ] Android App installs on S20 5G.
- [ ] Phone and Desktop can ping each other over WiFi.
- [ ] Image selected on Desktop arrives on Phone memory.
- [ ] Phone successfully dispatches MMS via Carrier.
- [ ] Desktop receives "SENT" confirmation.