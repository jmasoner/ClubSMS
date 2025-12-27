# API SPECIFICATION - ClubSMS v2.0

## Document Information
- **Version:** 2.0.0
- **Last Updated:** December 25, 2025
- **Status:** FINAL - DO NOT MODIFY WITHOUT COORDINATION
- **Target System:** Windows 11 Desktop + Android 13 (Samsung Galaxy S20 5G)
- **Repository:** https://github.com/jmasoner/ClubSMS

## 1. Overview

The ClubSMS v2.0 API defines communication protocols between four primary components:

1. **Desktop Application** (Electron) - Primary user interface
2. **Network Bridge Service** - WebSocket/REST server running on phone
3. **Phone Companion App** - Lightweight Android service
4. **Android App v2.0** - Enhanced mobile interface with MMS support

The architecture uses a local-network-only approach with encrypted WebSocket connections as the primary communication method and Bluetooth as fallback. All data remains within the user's local network.

## 2. WebSocket Protocol (Desktop ↔ Phone Bridge)

**Connection URL:** `wss://[PHONE_IP]:8443/clubsms/v2`

### Connection Handshake

```json
// Desktop → Bridge: Connection Request
{
  "type": "CONNECT",
  "version": "2.0.0",
  "client_id": "desktop-{uuid}",
  "timestamp": "2025-12-25T08:30:00.000Z",
  "auth_token": "sha256-hash",
  "capabilities": ["sms", "mms", "contacts", "history"]
}

// Bridge → Desktop: Connection Response
{
  "type": "CONNECTED",
  "session_id": "{uuid}",
  "server_version": "2.0.0",
  "phone_info": {
    "model": "Samsung Galaxy S20 5G",
    "manufacturer": "Samsung",
    "android_version": "13",
    "one_ui_version": "5.1",
    "device_id": "SM-G981U",
    "capabilities": ["sms", "mms", "contacts"],
    "carrier_info": {
      "name": "Carrier Name",
      "network_type": "5G",
      "sms_limit_per_minute": 30,
      "mms_limit_per_minute": 10
    }
  },
  "timestamp": "2025-12-25T08:30:01.234Z"
}

// Bridge → Desktop: Connection Error
{
  "type": "CONNECTION_ERROR",
  "error_code": 1001,
  "error_message": "Authentication failed",
  "timestamp": "2025-12-25T08:30:00.500Z"
}
```

### Message Sending Commands

```json
// Desktop → Bridge: Send SMS Broadcast
{
  "type": "SEND_SMS",
  "message_id": "msg-{uuid}",
  "recipients": [
    {
      "phone": "+1234567890",
      "name": "John Doe",
      "contact_id": "contact-{uuid}"
    },
    {
      "phone": "+0987654321",
      "name": "Jane Smith",
      "contact_id": "contact-{uuid2}"
    }
  ],
  "content": "Chess Club Meeting Tonight at 7pm. Location: Community Center Room 201. Please confirm attendance by replying YES or NO.",
  "sender_name": "Chess Club",
  "priority": "normal",
  "timestamp": "2025-12-25T08:35:00.000Z"
}

// Desktop → Bridge: Send MMS Broadcast
{
  "type": "SEND_MMS",
  "message_id": "msg-{uuid}",
  "recipients": [
    {
      "phone": "+1234567890",
      "name": "John Doe",
      "contact_id": "contact-{uuid}"
    }
  ],
  "content": "Check out our tournament bracket for this weekend!",
  "media": {
    "type": "image/jpeg",
    "filename": "tournament_bracket.jpg",
    "data": "base64-encoded-image-data",
    "size": 245678,
    "width": 800,
    "height": 600,
    "compression": "medium"
  },
  "sender_name": "Chess Club",
  "priority": "normal",
  "timestamp": "2025-12-25T08:35:00.000Z"
}
```

### Status Reports

```json
// Bridge → Desktop: Delivery Status (Individual)
{
  "type": "DELIVERY_STATUS",
  "message_id": "msg-{uuid}",
  "recipient": {
    "phone": "+1234567890",
    "name": "John Doe",
    "contact_id": "contact-{uuid}"
  },
  "status": "SENT|DELIVERED|FAILED",
  "error_code": null,
  "error_message": null,
  "carrier_reference": "msg-ref-12345",
  "timestamp": "2025-12-25T08:35:15.234Z"
}

// Bridge → Desktop: Progress Update (Batch)
{
  "type": "PROGRESS",
  "message_id": "msg-{uuid}",
  "statistics": {
    "total": 100,
    "queued": 0,
    "sending": 5,
    "sent": 45,
    "delivered": 42,
    "failed": 3
  },
  "current_batch": {
    "batch_number": 3,
    "batch_size": 20,
    "batch_progress": 15
  },
  "estimated_completion": "2025-12-25T08:45:00.000Z",
  "timestamp": "2025-12-25T08:38:30.000Z"
}

// Bridge → Desktop: Broadcast Complete
{
  "type": "BROADCAST_COMPLETE",
  "message_id": "msg-{uuid}",
  "final_statistics": {
    "total": 100,
    "sent": 97,
    "delivered": 94,
    "failed": 3
  },
  "failed_recipients": [
    {
      "phone": "+5555551234",
      "name": "Invalid Number",
      "error_code": 2001,
      "error_message": "Invalid phone number format"
    }
  ],
  "duration_seconds": 125,
  "cost_estimate": {
    "sms_count": 97,
    "mms_count": 0,
    "estimated_cost_usd": 0.97
  },
  "timestamp": "2025-12-25T08:40:05.000Z"
}
```

## 3. Bluetooth Fallback Protocol

**Used when WiFi is unavailable or WebSocket connection fails**

### Connection Format
- **Service UUID:** `6ba7b810-9dad-11d1-80b4-00c04fd430c8`
- **Command Format:** JSON over Bluetooth Serial Port Profile (SPP)
- **Maximum Payload:** 512 bytes per command

```json
// Simplified Command Structure
{
  "cmd": "SEND_SMS|GET_STATUS|SYNC_CONTACTS",
  "id": "short-id",
  "data": {
    "recipients": ["+1234567890"],
    "content": "Message (max 160 chars)",
    "timestamp": "unix-timestamp"
  }
}

// Simplified Response
{
  "cmd": "ACK|STATUS|ERROR",
  "id": "short-id",
  "status": "OK|FAILED",
  "data": {
    "sent": 5,
    "failed": 0
  }
}
```

## 4. REST API Endpoints (Local Bridge Server)

**Base URL:** `https://[PHONE_IP]:8443/api/v1`

### Contact Management

```http
# Get all contacts
GET /contacts?limit=100&offset=0&active_only=true

Response:
{
  "contacts": [
    {
      "id": "contact-{uuid}",
      "name": "John Doe",
      "phone": "+1234567890",
      "active": true,
      "opt_out": false,
      "created_at": "2025-12-25T08:00:00.000Z",
      "updated_at": "2025-12-25T08:00:00.000Z",
      "last_message_sent": "2025-12-24T19:30:00.000Z",
      "delivery_success_rate": 0.98
    }
  ],
  "pagination": {
    "total": 1000,
    "limit": 100,
    "offset": 0,
    "has_more": true
  }
}

# Add new contact
POST /contacts
Content-Type: application/json

{
  "name": "New Member",
  "phone": "+1555123456",
  "active": true,
  "notes": "Joined December 2025"
}

# Update contact
PUT /contacts/{id}
Content-Type: application/json

{
  "name": "Updated Name",
  "active": false,
  "opt_out": true,
  "notes": "Opted out on request"
}

# Delete contact
DELETE /contacts/{id}
```

### Message History

```http
# Get message history
GET /messages?limit=50&offset=0&type=SMS&start_date=2025-12-01

Response:
{
  "messages": [
    {
      "id": "msg-{uuid}",
      "type": "SMS|MMS",
      "content": "Meeting tonight at 7pm",
      "media_url": null,
      "recipients_count": 100,
      "sent_at": "2025-12-25T08:35:00.000Z",
      "status": {
        "total": 100,
        "sent": 98,
        "delivered": 95,
        "failed": 2
      },
      "cost_estimate": 0.98
    }
  ],
  "pagination": {
    "total": 500,
    "limit": 50,
    "offset": 0
  }
}

# Get specific message details
GET /messages/{id}

Response:
{
  "message": {
    "id": "msg-{uuid}",
    "type": "SMS",
    "content": "Meeting tonight at 7pm",
    "recipients": [
      {
        "phone": "+1234567890",
        "name": "John Doe",
        "status": "delivered",
        "delivered_at": "2025-12-25T08:35:30.000Z"
      }
    ]
  }
}
```

### System Status

```http
# Get system status
GET /status

Response:
{
  "system": {
    "version": "2.0.0",
    "uptime_seconds": 3600,
    "memory_usage_mb": 45,
    "cpu_usage_percent": 12
  },
  "phone": {
    "battery_level": 85,
    "signal_strength": 4,
    "network_type": "5G",
    "carrier": "Verizon"
  },
  "messaging": {
    "sms_sent_today": 150,
    "mms_sent_today": 25,
    "failed_today": 3,
    "queue_length": 0
  },
  "storage": {
    "contacts_count": 1000,
    "messages_count": 500,
    "media_files_mb": 120
  }
}
```

## 5. Data Structures

### Contact Object

```json
{
  "id": "contact-550e8400-e29b-41d4-a716-446655440000",
  "name": "John Doe",
  "phone": "+12345678901",
  "email": "john.doe@email.com",
  "active": true,
  "opt_out": false,
  "groups": ["board-members", "tournament-players"],
  "notes": "Club treasurer since 2023",
  "created_at": "2025-01-15T10:30:00.000Z",
  "updated_at": "2025-12-25T08:00:00.000Z",
  "last_message_sent": "2025-12-24T19:30:00.000Z",
  "delivery_stats": {
    "messages_sent": 45,
    "messages_delivered": 44,
    "messages_failed": 1,
    "success_rate": 0.978
  }
}
```

### Message Record

```json
{
  "id": "msg-550e8400-e29b-41d4-a716-446655440001",
  "type": "SMS|MMS",
  "content": "Chess Club Tournament this Saturday! Registration starts at 9am. Entry fee: $10. Prizes for top 3 finishers!",
  "media": {
    "type": "image/jpeg",
    "filename": "tournament_flyer.jpg",
    "url": "/media/msg-{id}/tournament_flyer.jpg",
    "size": 245678,
    "width": 800,
    "height": 600
  },
  "sender": {
    "name": "Chess Club",
    "phone": "+12345678900"
  },
  "recipients": [
    {
      "phone": "+12345678901",
      "name": "John Doe",
      "contact_id": "contact-{uuid}",
      "status": "delivered",
      "sent_at": "2025-12-25T08:35:00.000Z",
      "delivered_at": "2025-12-25T08:35:15.000Z"
    }
  ],
  "created_at": "2025-12-25T08:34:00.000Z",
  "sent_at": "2025-12-25T08:35:00.000Z",
  "completed_at": "2025-12-25T08:40:00.000Z",
  "statistics": {
    "total": 100,
    "sent": 98,
    "delivered": 95,
    "failed": 2,
    "pending": 0
  },
  "cost_estimate": {
    "sms_segments": 1,
    "mms_count": 0,
    "total_cost_usd": 0.98
  }
}
```

## 6. Error Codes

### Connection Errors (1000-1099)
- **1001:** Connection timeout
- **1002:** Authentication failed
- **1003:** Unsupported version
- **1004:** Too many connections
- **1005:** Server maintenance mode

### Message Errors (2000-2099)
- **2001:** Invalid phone number format
- **2002:** Message content too long
- **2003:** Recipient list empty
- **2004:** Duplicate message ID
- **2005:** Rate limit exceeded

### Media Errors (3000-3099)
- **3001:** MMS file too large (max 1MB)
- **3002:** Unsupported media type
- **3003:** Media compression failed
- **3004:** Media upload timeout

### System Errors (4000-4099)
- **4001:** Insufficient storage space
- **4002:** Contact database locked
- **4003:** Permission denied
- **4004:** Service unavailable

### Network Errors (5000-5099)
- **5001:** Cellular network unavailable
- **5002:** SMS service disabled
- **5003:** Carrier rejection
- **5004:** Message blacklisted

## 7. Security Considerations

### Encryption
- **WebSocket:** WSS with TLS 1.3, AES-256 encryption
- **REST API:** HTTPS with certificate pinning
- **Bluetooth:** AES-128 encryption for SPP
- **Local Storage:** AES-256 encrypted SQLite database

### Authentication
```json
// JWT Token Format
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "client_id": "desktop-{uuid}",
    "session_id": "{uuid}",
    "issued_at": 1703505600,
    "expires_at": 1703509200,
    "permissions": ["send_sms", "send_mms", "read_contacts"]
  }
}
```

### Network Security
- **Local Network Only:** All connections restricted to 192.168.x.x/172.16.x.x/10.x.x.x
- **Port Binding:** Bridge service binds only to private network interfaces
- **Firewall Rules:** No external internet access required for core functionality

## 8. Rate Limiting

### SMS Limits
- **Maximum:** 30 SMS per minute (carrier dependent)
- **Burst:** Up to 10 SMS in 10 seconds
- **Cooldown:** 30 seconds between large broadcasts (>50 recipients)
- **Queue:** Automatic queuing with exponential backoff

### MMS Limits
- **Maximum:** 10 MMS per minute
- **File Size:** 1MB per MMS (carrier dependent)
- **Concurrent:** Maximum 3 MMS sending simultaneously

### API Rate Limits
```http
# Rate limit headers in responses
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 55
X-RateLimit-Reset: 1703505660

# Rate limit error response
HTTP/1.1 429 Too Many Requests
{
  "error_code": 2005,
  "error_message": "Rate limit exceeded",
  "retry_after_seconds": 30
}
```

### Automatic Retry Logic
```json
{
  "retry_policy": {
    "max_attempts": 3,
    "backoff_strategy": "exponential",
    "initial_delay_ms": 1000,
    "max_delay_ms": 30000,
    "retry_on_errors": [1001, 5001, 5002]
  }
}
```