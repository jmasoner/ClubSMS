# AGENT BRIEFING #1: Desktop Application Lead
## Assignment: Build ClubSMS Desktop Application (Windows 11)

### Your Role
You are responsible for building the full-featured desktop application that club organizers will use as their primary interface. This is the main user-facing component of ClubSMS v2.0.

### Technology Stack (MANDATORY)
- **Framework:** Electron 28.x
- **Frontend:** React 18.x with TypeScript
- **State Management:** Redux Toolkit (recommended) or Zustand
- **Database:** SQLite3 (via better-sqlite3)
- **Styling:** Tailwind CSS (for rapid development) or Material-UI
- **WebSocket Client:** ws library for real-time communication
- **HTTP Client:** axios for REST API calls
- **File Handling:** electron-builder for packaging

### File Structure (Follow This Exactly)
```
desktop-app/
├── package.json
├── electron-builder.yml
├── tsconfig.json
├── webpack.config.js
├── src/
│   ├── main/                    # Electron main process
│   │   ├── index.ts            # Main entry point
│   │   ├── database.ts         # SQLite operations
│   │   ├── bridge-client.ts    # WebSocket connection manager
│   │   ├── file-manager.ts     # Handle media files
│   │   └── window-manager.ts   # App window management
│   ├── renderer/               # React UI (runs in browser context)
│   │   ├── App.tsx            # Root component
│   │   ├── index.tsx          # Renderer entry point
│   │   ├── components/
│   │   │   ├── ContactManager.tsx     # Contact CRUD operations
│   │   │   ├── MessageComposer.tsx    # SMS/MMS composition
│   │   │   ├── BroadcastPanel.tsx     # Send controls & progress
│   │   │   ├── Analytics.tsx          # Message history & stats
│   │   │   ├── ConnectionStatus.tsx   # Bridge connection indicator
│   │   │   └── SettingsPanel.tsx      # App configuration
│   │   ├── store/
│   │   │   ├── index.ts              # Redux store configuration
│   │   │   ├── contactsSlice.ts      # Contacts state management
│   │   │   ├── messagesSlice.ts      # Messages state management
│   │   │   └── connectionSlice.ts    # Connection state
│   │   ├── hooks/
│   │   │   ├── useWebSocket.ts       # WebSocket connection hook
│   │   │   └── useDatabase.ts        # Database operations hook
│   │   ├── utils/
│   │   │   ├── validation.ts         # Phone number validation
│   │   │   ├── csv-parser.ts         # Contact import/export
│   │   │   └── media-compression.ts  # Image compression
│   │   └── styles/
│   │       └── globals.css
│   └── shared/                 # Shared between main and renderer
│       ├── types.ts           # TypeScript interfaces
│       ├── constants.ts       # App constants
│       └── ipc-events.ts      # IPC event definitions
```

### Core Features You Must Build

#### 1. Contact Management UI (Priority: HIGH)
- **Contact List Display:**
  - Virtualized list supporting 1000+ contacts without lag
  - Real-time search/filter by name, phone, status
  - Bulk selection with checkboxes
  - Sort by name, date added, last contacted
  - Active/Inactive status toggle per contact
  - Opt-out status clearly marked (🚫 icon)

- **Contact Operations:**
  - Import from CSV/Excel files (drag & drop)
  - Add single contact with form validation
  - Edit contact details inline
  - Bulk delete with confirmation
  - Export selected contacts to CSV
  - Import from Android phone contacts (via bridge API)

- **Contact Details Panel:**
  - Phone number validation (international format)
  - Delivery success rate history
  - Last message sent timestamp
  - Notes field for additional info

#### 2. Message Composer (Priority: HIGH)
- **Text Composition:**
  - Rich text editor with basic formatting
  - Character counter showing SMS segments (160 chars = 1 segment)
  - Real-time cost estimation
  - Message templates dropdown (pre-written common messages)

- **SMS/MMS Toggle:**
  - Clear toggle switch with cost implications shown
  - SMS: Text only, cheaper, faster delivery
  - MMS: Text + image, more expensive, slower delivery

- **Media Handling (MMS only):**
  - Image picker (PNG, JPEG support)
  - Image preview with dimensions and file size
  - Automatic compression to stay under 1MB limit
  - Drag & drop image attachment

- **Recipient Selection:**
  - "Send to All Active" checkbox
  - "Send to Custom Group" with contact picker
  - Exclude opted-out contacts automatically
  - Show final recipient count before sending

#### 3. Broadcast Panel (Priority: HIGH)
- **Pre-Send Confirmation:**
  - Modal dialog showing message preview
  - Recipient count and estimated cost
  - SMS vs MMS clearly indicated
  - "Send Now" and "Cancel" buttons

- **Sending Progress:**
  - Real-time progress bar (0-100%)
  - Current status text ("Sending 45 of 100...")
  - Individual recipient status updates
  - "Cancel Broadcast" button (stops remaining sends)
  - ETA for completion

- **Post-Send Summary:**
  - Total sent, delivered, failed counts
  - List of failed recipients with error reasons
  - Total cost incurred
  - "Send to Failed Recipients" retry option

#### 4. Analytics Dashboard (Priority: MEDIUM)
- **Message History Table:**
  - Paginated list of all sent broadcasts
  - Columns: Date, Type (SMS/MMS), Recipients, Success Rate, Cost
  - Filter by date range, message type, success/failure
  - Click row to see detailed recipient status

- **Statistics Cards:**
  - Messages sent today/this week/this month
  - Average delivery success rate
  - Total cost this month
  - Top failure reasons

- **Charts (if time permits):**
  - Delivery success rate trend over time
  - SMS vs MMS usage ratio
  - Peak sending times

- **Export Options:**
  - Export message history to CSV
  - Export delivery reports for accounting

#### 5. Bridge Connection Manager (Priority: HIGH)
- **Auto-Discovery:**
  - Scan local network for ClubSMS Bridge service
  - Display discovered phones with device info
  - One-click connect to selected device

- **Connection Status Indicator:**
  - Green: Connected and ready
  - Yellow: Connecting or reconnecting
  - Red: Disconnected with error message
  - Real-time latency display (ping time)

- **Manual Connection:**
  - IP address input field for manual connection
  - Port number field (default 8443)
  - "Test Connection" button

- **Fallback Options:**
  - Bluetooth connection option when WiFi fails
  - USB connection detection (if phone connected via USB)

### Critical APIs You Must Implement

#### WebSocket Connection Management
```typescript
class BridgeClient {
  private ws: WebSocket | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;

  connect(ipAddress: string): Promise<void> {
    return new Promise((resolve, reject) => {
      this.ws = new WebSocket(`wss://${ipAddress}:8443/clubsms/v2`);
      
      this.ws.on('open', () => {
        // Send connection handshake
        this.ws!.send(JSON.stringify({
          type: 'CONNECT',
          version: '2.0.0',
          client_id: `desktop-${uuidv4()}`,
          timestamp: new Date().toISOString(),
          capabilities: ['sms', 'mms', 'contacts', 'history']
        }));
        resolve();
      });

      this.ws.on('message', (data: Buffer) => {
        const message = JSON.parse(data.toString());
        this.handleMessage(message);
      });

      this.ws.on('error', (error: Error) => {
        console.error('WebSocket error:', error);
        reject(error);
      });

      this.ws.on('close', () => {
        this.handleDisconnection();
      });
    });
  }

  private handleMessage(message: any): void {
    switch (message.type) {
      case 'CONNECTED':
        store.dispatch(connectionConnected(message.phone_info));
        break;
      case 'DELIVERY_STATUS':
        store.dispatch(updateDeliveryStatus(message));
        break;
      case 'PROGRESS':
        store.dispatch(updateBroadcastProgress(message));
        break;
      case 'BROADCAST_COMPLETE':
        store.dispatch(broadcastComplete(message));
        break;
    }
  }
}
```

#### Message Sending Implementation
```typescript
interface SendMessageRequest {
  type: 'SEND_SMS' | 'SEND_MMS';
  message_id: string;
  recipients: Array<{
    phone: string;
    name: string;
    contact_id: string;
  }>;
  content: string;
  media?: {
    type: string;
    filename: string;
    data: string; // base64
    size: number;
  };
  priority: 'normal' | 'high';
}

async function sendBroadcast(
  recipients: Contact[],
  content: string,
  media?: File
): Promise<string> {
  const messageId = `msg-${uuidv4()}`;
  
  let mediaData;
  if (media) {
    // Compress and encode image
    const compressedImage = await compressImage(media);
    mediaData = {
      type: media.type,
      filename: media.name,
      data: await fileToBase64(compressedImage),
      size: compressedImage.size
    };
  }

  const request: SendMessageRequest = {
    type: media ? 'SEND_MMS' : 'SEND_SMS',
    message_id: messageId,
    recipients: recipients.map(contact => ({
      phone: contact.phone,
      name: contact.name,
      contact_id: contact.id
    })),
    content,
    media: mediaData,
    priority: 'normal'
  };

  // Send via WebSocket
  bridgeClient.send(JSON.stringify(request));
  
  // Save to local database
  await database.saveMessage({
    id: messageId,
    type: media ? 'MMS' : 'SMS',
    content,
    media_path: media?.name || null,
    total_recipients: recipients.length,
    sent_at: new Date().toISOString()
  });

  return messageId;
}
```

### Database Schema (SQLite - Create These Tables)

```sql
-- Contacts table
CREATE TABLE contacts (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  phone TEXT NOT NULL UNIQUE,
  email TEXT,
  active INTEGER DEFAULT 1,
  opt_out INTEGER DEFAULT 0,
  notes TEXT,
  groups TEXT, -- JSON array of group names
  created_at TEXT DEFAULT CURRENT_TIMESTAMP,
  updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
  last_message_sent TEXT,
  delivery_success_rate REAL DEFAULT 1.0
);

-- Create index for fast phone lookups
CREATE INDEX idx_contacts_phone ON contacts(phone);
CREATE INDEX idx_contacts_active ON contacts(active);

-- Messages table
CREATE TABLE messages (
  id TEXT PRIMARY KEY,
  type TEXT CHECK(type IN ('SMS', 'MMS')) NOT NULL,
  content TEXT NOT NULL,
  media_path TEXT,
  media_type TEXT,
  sender_name TEXT DEFAULT 'Club SMS',
  created_at TEXT DEFAULT CURRENT_TIMESTAMP,
  sent_at TEXT,
  completed_at TEXT,
  total_recipients INTEGER DEFAULT 0,
  sent_count INTEGER DEFAULT 0,
  delivered_count INTEGER DEFAULT 0,
  failed_count INTEGER DEFAULT 0,
  cost_estimate REAL DEFAULT 0.0
);

-- Message recipients tracking
CREATE TABLE message_recipients (
  message_id TEXT NOT NULL,
  contact_id TEXT,
  phone TEXT NOT NULL,
  name TEXT,
  status TEXT CHECK(status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED')) DEFAULT 'PENDING',
  error_code INTEGER,
  error_message TEXT,
  sent_at TEXT,
  delivered_at TEXT,
  FOREIGN KEY(message_id) REFERENCES messages(id) ON DELETE CASCADE,
  FOREIGN KEY(contact_id) REFERENCES contacts(id) ON DELETE SET NULL
);

-- Settings table
CREATE TABLE settings (
  key TEXT PRIMARY KEY,
  value TEXT,
  updated_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- Insert default settings
INSERT INTO settings (key, value) VALUES 
  ('bridge_ip', ''),
  ('bridge_port', '8443'),
  ('auto_reconnect', 'true'),
  ('default_sender_name', 'Club SMS'),
  ('max_image_size_mb', '1'),
  ('sms_cost_per_message', '0.01'),
  ('mms_cost_per_message', '0.10');
```

### Integration Points

**You depend on:**
- **Network Bridge (Agent #3):** WebSocket server running on Android phone
- **Phone Companion Service (Agent #4):** Background service handling actual SMS/MMS sending
- **API_SPECIFICATION_v2.0.md:** Exact message formats and error codes

**Who depends on you:**
- **Nobody** (you're the top-level UI layer)
- **Integration Team:** For end-to-end testing

### Testing Strategy

#### Manual Testing Checklist:
1. **Performance Tests:**
   - Import 1000+ contacts - should complete in <5 seconds
   - Contact list scrolling - should be smooth with virtual scrolling
   - UI responsiveness during broadcast - interface should not freeze

2. **Connection Tests:**
   - Auto-discover phone on local network
   - Manual IP connection works
   - Graceful handling of connection loss
   - Automatic reconnection after network restored

3. **Message Tests:**
   - Send SMS to single recipient
   - Send MMS with image to single recipient
   - Send broadcast to 100+ recipients
   - Cancel broadcast mid-send
   - Retry failed recipients

#### Integration Tests (Mock WebSocket):
```typescript
// Mock WebSocket responses for testing
const mockBridgeResponses = {
  CONNECT: {
    type: 'CONNECTED',
    session_id: 'test-session',
    phone_info: {
      model: 'Samsung Galaxy S20 5G',
      android_version: '13',
      capabilities: ['sms', 'mms']
    }
  },
  SEND_SMS: {
    type: 'PROGRESS',
    message_id: 'test-msg-id',
    statistics: { total: 1, sent: 1, delivered: 0, failed: 0 }
  }
};
```

### Deliverables & Timeline

#### Sprint 1 (8:30am - 10:30am): Project Setup & UI Mockups
- ✅ Electron project initialized with TypeScript
- ✅ React app with basic routing set up
- ✅ SQLite database created with schema
- ✅ Basic UI layout with navigation (no functionality yet)
- ✅ WebSocket connection class stubbed out

#### Sprint 2 (10:45am - 12:45pm): Core Functionality
- ✅ Contact management CRUD operations working
- ✅ WebSocket connection to bridge established
- ✅ Basic message composition UI functional
- ✅ Database operations (save/load contacts and messages)
- ✅ Connection status indicator working

#### Sprint 3 (1:15pm - 3:15pm): Advanced Features & Polish
- ✅ SMS/MMS sending through bridge functional
- ✅ Real-time delivery status updates
- ✅ Progress bars and user feedback
- ✅ Error handling and user-friendly error messages
- ✅ Message history and analytics dashboard
- ✅ CSV import/export functionality

### Critical Constraints & Requirements

1. **Windows 11 Target:** Optimize for Windows 11, test on Windows 11
2. **Local Network Only:** No internet dependencies beyond OS updates
3. **Samsung Galaxy S20 5G:** Primary test device (Android 13, One UI 5.1)
4. **API Compliance:** Follow API_SPECIFICATION_v2.0.md exactly - zero deviations
5. **Performance:** Must handle 1000+ contacts smoothly
6. **Professional UI:** Club organizers should feel confident using this

### File Management & Source Control

#### Git Workflow:
```bash
# Create and switch to your feature branch
git checkout -b feature/desktop-app

# Use conventional commits
git commit -m "feat: implement contact management UI"
git commit -m "fix: resolve WebSocket reconnection issue"
git commit -m "style: improve broadcast panel layout"

# Push regularly for backup
git push origin feature/desktop-app
```

#### OneDrive Structure:
```
C:\Users\john\OneDrive\MyProjects\ClubSMS\
├── desktop-app/          # Your main code (sync with Git)
├── docs/                 # Copy of API specs and briefings
├── test-data/           # Sample CSV files for testing
└── builds/              # Generated executables for testing
```

### Escalation & Communication

#### Immediate Blockers - Contact John:
- Cannot connect to bridge service
- API responses don't match specification
- Performance issues with large contact lists
- Electron-specific technical issues

#### Questions About Requirements:
- UI design decisions not covered in brief
- Additional features not in scope
- Priority conflicts between features

#### Sync Point Communication:
- **10:30am:** "Desktop UI mockups complete, WebSocket class ready"
- **12:45pm:** "Contact management working, can connect to bridge"  
- **3:15pm:** "All features complete, ready for integration testing"

### Success Criteria (Must Pass Before Integration)

#### Functionality:
✅ App launches without errors on Windows 11  
✅ Can import and display 1000+ contacts smoothly  
✅ Can compose and send SMS messages via bridge  
✅ Can compose and send MMS messages with images  
✅ Real-time delivery status updates display correctly  
✅ Connection status accurately reflects bridge state  
✅ Message history saves and displays correctly  

#### User Experience:
✅ Professional UI that non-technical users can navigate  
✅ Clear error messages with actionable guidance  
✅ No UI freezing during long operations  
✅ Intuitive workflow for common tasks  

#### Technical:
✅ Follows exact API specification format  
✅ Proper error handling for all failure scenarios  
✅ Database operations are fast and reliable  
✅ Memory usage stays reasonable with large datasets  

---

## 🚨 BEFORE YOU START CODING

1. **READ** the complete API_SPECIFICATION_v2.0.md document
2. **UNDERSTAND** the exact JSON message formats
3. **SET UP** your development environment completely
4. **TEST** that you can compile and run a basic Electron app
5. **CONFIRM** you have access to the GitHub repo and OneDrive folder

**The success of the entire project depends on your desktop application working perfectly. Take no shortcuts on quality or API compliance.**

**Questions? Ask immediately. Assumptions? Don't make them. Blockers? Escalate instantly.**

**Let's build something that works flawlessly on day one.** 🚀