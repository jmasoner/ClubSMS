# ClubSMS v2.0 - Desktop ↔ Phone Integration Guide
## Coordination Document for Grok 4.1 (Desktop App Developer)

**Prepared by:** Claude (Bridge + Companion Service Developer)  
**Date:** December 2025  
**Status:** ACTIVE - Follow This Specification

---

## Overview

This document defines the integration contract between:
- **Desktop App** (Electron/React on Windows 11) - Built by Grok 4.1
- **Phone Bridge Service** (Android on Samsung S20) - Built by Claude

The Desktop App sends commands over WebSocket → Phone executes SMS sending → Phone reports status back.

---

## Connection Details

### WebSocket Server (Running on Phone)

| Property | Value |
|----------|-------|
| Protocol | `ws://` (plain WebSocket, no TLS for v2.0) |
| Port | `8765` |
| Path | `/clubsms` |
| Full URL | `ws://{PHONE_IP}:8765/clubsms` |

**Example:** `ws://192.168.1.105:8765/clubsms`

### How Desktop Finds Phone IP

For v2.0, use **manual IP entry**:
1. User looks at phone's WiFi settings to find IP address
2. User enters IP in Desktop app
3. Desktop connects to `ws://{IP}:8765/clubsms`

**Phone displays its IP** in the notification when Bridge Service is running.

---

## Message Protocol

All messages are JSON objects with a `type` field.

### Connection Flow

```
Desktop                              Phone
   │                                   │
   │──── Connect to WebSocket ────────►│
   │                                   │
   │◄──── CONNECTED response ──────────│
   │                                   │
   │──── SEND_SMS command ────────────►│
   │                                   │
   │◄──── PROGRESS updates ────────────│
   │◄──── PROGRESS updates ────────────│
   │◄──── BROADCAST_COMPLETE ──────────│
   │                                   │
```

---

## Messages: Desktop → Phone

### 1. SEND_SMS (Broadcast SMS to Recipients)

```json
{
  "type": "SEND_SMS",
  "message_id": "msg-550e8400-e29b-41d4-a716-446655440000",
  "recipients": [
    {
      "phone": "+15551234567",
      "name": "John Doe",
      "contact_id": "contact-001"
    },
    {
      "phone": "+15559876543",
      "name": "Jane Smith",
      "contact_id": "contact-002"
    }
  ],
  "content": "Club meeting tonight at 7pm! Details: https://yourclub.com/meeting",
  "timestamp": "2025-12-26T14:30:00.000Z"
}
```

**Fields:**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | string | Yes | Always `"SEND_SMS"` |
| `message_id` | string | Yes | Unique UUID for tracking this broadcast |
| `recipients` | array | Yes | List of recipient objects |
| `recipients[].phone` | string | Yes | Phone number with country code (e.g., `+15551234567`) |
| `recipients[].name` | string | No | Display name for logging |
| `recipients[].contact_id` | string | No | Your internal contact ID |
| `content` | string | Yes | SMS message text (max 1000 chars) |
| `timestamp` | string | No | ISO 8601 timestamp |

### 2. GET_CONTACTS (Fetch Contacts from Phone)

```json
{
  "type": "GET_CONTACTS",
  "request_id": "req-12345"
}
```

### 3. GET_STATUS (Check Phone Status)

```json
{
  "type": "GET_STATUS",
  "request_id": "req-12346"
}
```

### 4. PING (Keep-Alive)

```json
{
  "type": "PING",
  "timestamp": "2025-12-26T14:30:00.000Z"
}
```

---

## Messages: Phone → Desktop

### 1. CONNECTED (Connection Established)

Sent automatically when Desktop connects.

```json
{
  "type": "CONNECTED",
  "session_id": "session-abc123",
  "phone_info": {
    "model": "Samsung Galaxy S20 5G",
    "android_version": "13",
    "app_version": "2.0.0",
    "carrier": "T-Mobile",
    "ip_address": "192.168.1.105",
    "battery_level": 85
  },
  "capabilities": ["sms", "contacts"],
  "timestamp": "2025-12-26T14:30:01.000Z"
}
```

### 2. PROGRESS (Broadcast Progress Update)

Sent periodically during SMS broadcast.

```json
{
  "type": "PROGRESS",
  "message_id": "msg-550e8400-e29b-41d4-a716-446655440000",
  "statistics": {
    "total": 100,
    "queued": 45,
    "sending": 1,
    "sent": 50,
    "failed": 4
  },
  "current_recipient": {
    "phone": "+15551234567",
    "name": "John Doe",
    "status": "SENDING"
  },
  "percent_complete": 54,
  "estimated_remaining_seconds": 45,
  "timestamp": "2025-12-26T14:31:00.000Z"
}
```

### 3. DELIVERY_STATUS (Individual Recipient Status)

Sent for each recipient as their SMS is processed.

```json
{
  "type": "DELIVERY_STATUS",
  "message_id": "msg-550e8400-e29b-41d4-a716-446655440000",
  "recipient": {
    "phone": "+15551234567",
    "name": "John Doe",
    "contact_id": "contact-001"
  },
  "status": "SENT",
  "error_code": null,
  "error_message": null,
  "timestamp": "2025-12-26T14:31:05.000Z"
}
```

**Status Values:**
| Status | Meaning |
|--------|---------|
| `PENDING` | Queued for sending |
| `SENDING` | Currently being sent |
| `SENT` | SMS sent successfully |
| `FAILED` | SMS failed to send |

### 4. BROADCAST_COMPLETE (Broadcast Finished)

Sent when all recipients have been processed.

```json
{
  "type": "BROADCAST_COMPLETE",
  "message_id": "msg-550e8400-e29b-41d4-a716-446655440000",
  "final_statistics": {
    "total": 100,
    "sent": 96,
    "failed": 4
  },
  "failed_recipients": [
    {
      "phone": "+15550000000",
      "name": "Bad Number",
      "error_code": 2001,
      "error_message": "Invalid phone number format"
    }
  ],
  "duration_seconds": 120,
  "timestamp": "2025-12-26T14:33:00.000Z"
}
```

### 5. CONTACTS_LIST (Response to GET_CONTACTS)

```json
{
  "type": "CONTACTS_LIST",
  "request_id": "req-12345",
  "contacts": [
    {
      "id": "contact-001",
      "name": "John Doe",
      "phone": "+15551234567",
      "active": true,
      "opted_out": false
    }
  ],
  "total_count": 150,
  "active_count": 145,
  "opted_out_count": 5,
  "timestamp": "2025-12-26T14:30:05.000Z"
}
```

### 6. PHONE_STATUS (Response to GET_STATUS)

```json
{
  "type": "PHONE_STATUS",
  "request_id": "req-12346",
  "bridge_running": true,
  "wifi_connected": true,
  "cellular_connected": true,
  "battery_level": 85,
  "sms_permission_granted": true,
  "contacts_count": 150,
  "pending_messages": 0,
  "timestamp": "2025-12-26T14:30:02.000Z"
}
```

### 7. PONG (Keep-Alive Response)

```json
{
  "type": "PONG",
  "timestamp": "2025-12-26T14:30:00.500Z"
}
```

### 8. ERROR (Error Response)

```json
{
  "type": "ERROR",
  "error_code": 2003,
  "error_message": "No recipients specified",
  "related_message_id": "msg-550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-12-26T14:30:00.000Z"
}
```

---

## Error Codes

| Code | Category | Message |
|------|----------|---------|
| 1001 | Connection | Connection timeout |
| 1002 | Connection | Authentication failed |
| 2001 | Message | Invalid phone number format |
| 2002 | Message | Message content too long |
| 2003 | Message | No recipients specified |
| 2004 | Message | Duplicate message ID |
| 2005 | Message | Rate limit exceeded |
| 4001 | System | SMS permission not granted |
| 4002 | System | No cellular connection |
| 4003 | System | Bridge service not running |
| 5001 | Network | Carrier rejected message |

---

## Desktop App Implementation Notes

### WebSocket Connection (JavaScript/TypeScript)

```typescript
// Example using 'ws' library in Electron main process
import WebSocket from 'ws';

class BridgeClient {
  private ws: WebSocket | null = null;
  private phoneIP: string = '';
  
  connect(phoneIP: string): Promise<void> {
    return new Promise((resolve, reject) => {
      this.phoneIP = phoneIP;
      const url = `ws://${phoneIP}:8765/clubsms`;
      
      this.ws = new WebSocket(url);
      
      this.ws.on('open', () => {
        console.log('Connected to phone bridge');
        resolve();
      });
      
      this.ws.on('message', (data: Buffer) => {
        const message = JSON.parse(data.toString());
        this.handleMessage(message);
      });
      
      this.ws.on('error', (error) => {
        console.error('WebSocket error:', error);
        reject(error);
      });
      
      this.ws.on('close', () => {
        console.log('Disconnected from phone');
        // Implement reconnection logic here
      });
    });
  }
  
  sendSMS(recipients: Recipient[], content: string): string {
    const messageId = `msg-${crypto.randomUUID()}`;
    
    const command = {
      type: 'SEND_SMS',
      message_id: messageId,
      recipients: recipients.map(r => ({
        phone: r.phone,
        name: r.name,
        contact_id: r.id
      })),
      content: content,
      timestamp: new Date().toISOString()
    };
    
    this.ws?.send(JSON.stringify(command));
    return messageId;
  }
  
  private handleMessage(message: any): void {
    switch (message.type) {
      case 'CONNECTED':
        // Store phone info, update UI
        break;
      case 'PROGRESS':
        // Update progress bar
        break;
      case 'DELIVERY_STATUS':
        // Update individual recipient status
        break;
      case 'BROADCAST_COMPLETE':
        // Show completion summary
        break;
      case 'ERROR':
        // Display error to user
        break;
    }
  }
}
```

### UI Recommendations

1. **Connection Panel:**
   - IP address input field (e.g., `192.168.1.105`)
   - "Connect" button
   - Connection status indicator (🟢 Connected / 🔴 Disconnected)
   - Phone info display (model, battery, carrier)

2. **Message Composer:**
   - Large text area for message
   - Character counter
   - "Send to All" button
   - Recipient count display

3. **Progress Display:**
   - Progress bar (0-100%)
   - Current status text ("Sending 45 of 100...")
   - Real-time success/failure counts
   - Cancel button (optional for v2.0)

4. **Results Summary:**
   - Total sent / failed counts
   - Failed recipient list with error reasons
   - "Retry Failed" button

---

## Testing the Integration

### Step 1: Phone Setup
1. Install ClubSMS v2.0 APK on Samsung S20
2. Grant SMS permission
3. Enable Desktop Mode (toggle in app)
4. Note the IP address shown in notification

### Step 2: Desktop Connection Test
1. Open Desktop app
2. Enter phone's IP address
3. Click Connect
4. Verify "CONNECTED" message received

### Step 3: Send Test SMS
1. Add a test contact (your own phone number)
2. Compose short message
3. Send
4. Verify SMS received on test phone
5. Verify progress/completion messages in Desktop app

### Test Commands (for debugging)

You can test the WebSocket using `wscat`:

```bash
npm install -g wscat

# Connect to phone
wscat -c ws://192.168.1.105:8765/clubsms

# Send ping
{"type":"PING","timestamp":"2025-12-26T12:00:00Z"}

# Get phone status
{"type":"GET_STATUS","request_id":"test-001"}

# Get contacts
{"type":"GET_CONTACTS","request_id":"test-002"}

# Send test SMS (use your own number!)
{"type":"SEND_SMS","message_id":"test-msg-001","recipients":[{"phone":"+15551234567","name":"Test"}],"content":"Test from Desktop!"}
```

---

## Timeline & Milestones

| Milestone | Owner | Target |
|-----------|-------|--------|
| Bridge Service accepts connections | Claude | Day 1 |
| SEND_SMS command works | Claude | Day 1-2 |
| Desktop can connect & send | Grok | Day 1-2 |
| Progress updates flowing | Both | Day 2 |
| Full integration test | Both | Day 2-3 |
| Polish & error handling | Both | Day 3 |

---

## Questions for Grok

1. **Contact Management:** Do you want to manage contacts entirely on Desktop (with SQLite), or sync from phone's existing contacts?

2. **Message History:** Store on Desktop only, or sync from phone's history?

3. **Reconnection:** How should Desktop handle phone disconnection? Auto-retry? User prompt?

4. **Progress Updates:** How frequently do you want them? Currently planning every 5 recipients or every 2 seconds.

---

## Files I'm Creating (Claude)

```
app/src/main/java/com/clubsms/
├── bridge/
│   ├── BridgeService.java           # Foreground service, WebSocket server
│   ├── BridgeWebSocketServer.java   # WebSocket implementation  
│   ├── CommandProcessor.java        # Handle incoming commands
│   └── StatusReporter.java          # Send status to desktop
├── companion/
│   ├── CompanionService.java        # Lifecycle manager
│   ├── NetworkStateReceiver.java    # WiFi monitoring
│   └── BootCompleteReceiver.java    # Auto-start
```

---

## Contact

**Coordinator:** John  
**Questions:** Route through John or post in shared channel

---

**Let's build this! 🚀**

