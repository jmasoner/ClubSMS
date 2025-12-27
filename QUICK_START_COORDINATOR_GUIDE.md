# QUICK START GUIDE - Project Coordinator
## ClubSMS v2.0 One-Day Sprint

### Your Role as Coordinator

You are the **integration engineer** managing 4 AI agents building ClubSMS v2.0. Your job is to:
- Ensure all agents understand their assignments
- Monitor progress at sync checkpoints
- Resolve blockers immediately
- Run integration tests
- Make go/no-go decisions

---

## 🚀 PHASE 0: Briefing (NOW - 30 minutes)

### Step 1: Distribute Documents

Send each agent their specific briefing:

**Agent #1 (Cursor/Deepseek - Desktop):**
- Send: `AGENT_BRIEFING_1_Desktop_Lead.md`
- Send: `API_SPECIFICATION_v2.0.md`
- Context: "You're building the Windows desktop app. Read both documents completely before starting."

**Agent #2 (Cursor/Deepseek - Android):**
- Send: `AGENT_BRIEFING_2_Android_Lead.md`
- Send: `API_SPECIFICATION_v2.0.md`
- Send: Existing v1.0 codebase from `/home/user/ClubSMSApp/`
- Context: "You're upgrading the Android app with MMS. Build on existing v1.0 code."

**Agent #3 (Cursor/Grok - Bridge):**
- Send: `AGENT_BRIEFING_3_Bridge_Engineer.md`
- Send: `API_SPECIFICATION_v2.0.md`
- Context: "You're building the critical WebSocket bridge. This is the nervous system."

**Agent #4 (Cursor/Ollama - Companion):**
- Send: `AGENT_BRIEFING_4_Companion_Service.md`
- Context: "You're building the background service. Keep it minimal and efficient."

### Step 2: Verify Understanding

Ask each agent:
1. "What is your primary deliverable?"
2. "Who do you depend on?"
3. "What's your first checkpoint goal (10:30am)?"

Confirm they understand the API specification and their role boundaries.

### Step 3: Set Up Git Branches

Ensure each agent creates their branch:
```bash
git checkout -b feature/desktop-app      # Agent #1
git checkout -b feature/android-v2-mms   # Agent #2
git checkout -b feature/network-bridge   # Agent #3
git checkout -b feature/companion-service # Agent #4
```

### Step 4: Sync OneDrive

Ensure agents save to correct OneDrive folders:
```
C:\Users\john\OneDrive\MyProjects\ClubSMS\
├── desktop-app\         # Agent #1
├── android-app-v2\      # Agent #2
├── network-bridge\      # Agent #3 (might be part of android-app-v2)
└── companion-service\   # Agent #4 (part of android-app-v2)
```

---

## ⏰ CHECKPOINT SCHEDULE

### SYNC 1 - 10:30am (15 minutes)

**What to Check:**

| Agent | Expected Progress | Verification |
|-------|------------------|--------------|
| #1 Desktop | UI mockups, project scaffolded | Can launch Electron app? Shows blank UI? |
| #2 Android | MMS permissions added, UI toggle | APK builds? Toggle switch visible? |
| #3 Bridge | WebSocket server scaffolded | Can accept test connection? |
| #4 Companion | Service scaffolded, notification | Service starts? Notification shows? |

**Your Actions:**
1. Ask each agent for screenshot/commit hash
2. Identify any blockers
3. Adjust timeline if needed
4. Green light to continue or redirect

---

### SYNC 2 - 12:45pm (30 minutes + lunch)

**What to Check:**

| Agent | Expected Progress | Verification |
|-------|------------------|--------------|
| #1 Desktop | Contact mgmt working, WebSocket connected | Can import CSV? Connects to mock bridge? |
| #2 Android | Image picker works, compression implemented | Can select image? Stays under 300KB? |
| #3 Bridge | Command parsing works, SMS sending integrated | Send test SMS via WebSocket command? |
| #4 Companion | Bridge lifecycle managed | Service keeps bridge alive? Survives screen off? |

**Your Actions:**
1. **INTEGRATION TEST:** Try connecting Agent #1 desktop to Agent #3 bridge
2. Run on actual Samsung S20 device
3. Verify WebSocket communication works
4. Check for API mismatches

---

### SYNC 3 - 3:15pm (15 minutes)

**What to Check:**

All agents should be CODE COMPLETE. Check:

| Agent | Deliverable | Pass/Fail |
|-------|-------------|-----------|
| #1 Desktop | All features work, sends commands to bridge | ☐ |
| #2 Android | MMS sends successfully on S20 | ☐ |
| #3 Bridge | Bidirectional communication stable | ☐ |
| #4 Companion | Service reliable, battery optimized | ☐ |

**Your Actions:**
1. Make go/no-go decision for integration phase
2. If any agent not ready, extend their time or reduce scope
3. Prepare for full integration testing

---

## 🔧 PHASE 5: Integration (3:30pm - 5:30pm)

### Integration Checklist

Run these tests in order:

#### Test 1: Basic Connection
```
1. Start CompanionService on Samsung S20
2. Note phone's local IP address
3. Launch Desktop app on Windows 11
4. Enter phone IP, click Connect
5. ✓ Connection established?
6. ✓ Desktop shows "Connected" status?
```

#### Test 2: SMS Broadcast
```
1. In desktop app, add 5 test contacts (use your own numbers)
2. Compose message: "ClubSMS v2.0 test - please ignore"
3. Select SMS mode (not MMS)
4. Click Send Broadcast
5. ✓ Progress bar updates?
6. ✓ All 5 SMS received on phones?
7. ✓ Delivery statuses show in desktop?
```

#### Test 3: MMS Broadcast
```
1. Add small test image (< 200KB)
2. Compose message with image
3. Select MMS mode
4. Send to 2 test numbers
5. ✓ MMS received with image?
6. ✓ Image displays correctly?
```

#### Test 4: Large Broadcast
```
1. Import 50+ contacts
2. Send broadcast to all
3. ✓ All messages sent without crash?
4. ✓ Delivery tracking accurate?
5. Monitor Samsung S20 - battery drain reasonable?
```

#### Test 5: Stress Tests
```
1. Disconnect WiFi, reconnect - does it recover?
2. Force close desktop app, restart - reconnects?
3. Put phone to sleep - does bridge survive?
4. Send 100 SMS - carrier limits handled?
```

---

## 🚨 BLOCKER RESOLUTION

### Common Issues & Fixes

**Issue: Desktop can't find phone**
- Fix: Check firewall, verify same WiFi network, try manual IP entry

**Issue: WebSocket connection drops**
- Fix: Check CompanionService running, verify port 8080 not blocked

**Issue: MMS fails to send**
- Fix: Check image size < 300KB, verify MMS permissions granted

**Issue: SMS rate limited**
- Fix: Implement delays between batches, inform user of carrier limits

**Issue: Agent stuck/confused**
- Fix: Show them exact API_SPECIFICATION section, provide code example

---

## 📊 PROGRESS TRACKING

Use this table to track status:

| Time | Agent #1 | Agent #2 | Agent #3 | Agent #4 | Integration |
|------|----------|----------|----------|----------|-------------|
| 10:30am | ⬜ | ⬜ | ⬜ | ⬜ | N/A |
| 12:45pm | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |
| 3:15pm | ⬜ | ⬜ | ⬜ | ⬜ | N/A |
| 5:30pm | N/A | N/A | N/A | N/A | ⬜ |

Legend: ⬜ Not started | 🟨 In progress | ✅ Complete | ❌ Blocked

---

## 🎯 MVP SUCCESS CRITERIA

By 6:30pm, you MUST have:

### Core Functionality
- ✅ Desktop app launches and runs on Windows 11
- ✅ Phone companion runs on Samsung S20 (Android 13)
- ✅ Desktop connects to phone over local WiFi
- ✅ Can send SMS broadcast from desktop
- ✅ Can send MMS broadcast from desktop
- ✅ Delivery statuses report back to desktop
- ✅ Contact management works (add/edit/delete)
- ✅ Message history displays correctly

### Quality Gates
- ✅ No crashes during 30-minute session
- ✅ Can handle 50+ recipients without issues
- ✅ Battery drain < 5% per hour on phone
- ✅ UI is professional and usable

### Documentation
- ✅ Updated user manual for v2.0
- ✅ Installation guide for desktop + phone
- ✅ Troubleshooting guide

---

## 📁 File Locations (Quick Reference)

**Master Documents:**
- AI Drive: `/ClubSMS_Project/`
- OneDrive: `C:\Users\john\OneDrive\MyProjects\ClubSMS\`
- GitHub: `https://github.com/jmasoner/ClubSMS`

**Agent Briefings:**
- AI Drive: `/ClubSMS_Project/agent_briefings/`

**API Spec:**
- AI Drive: `/ClubSMS_Project/API_SPECIFICATION_v2.0.md`

**User Manual:**
- AI Drive: `/ClubSMS_Project/documentation/`

---

## 💡 Pro Tips

1. **Check Git Commits:** Ask agents to commit every hour with descriptive messages
2. **Test Early:** Don't wait until 5:30pm to test integration
3. **Use Mock Data:** Test with 5 contacts before trying 1000
4. **Save Often:** Have agents save to OneDrive frequently
5. **Screen Share:** Use screen sharing to diagnose issues quickly
6. **Stay Calm:** If something fails, reduce scope - MVP first, polish later

---

## 🚀 LAUNCH COMMAND

When ready to start (after briefings complete):

**Send to all agents simultaneously:**

```
🚀 PROJECT START - ClubSMS v2.0 Sprint

Timeline: 10.5 hours (until 6:30pm)
Your briefing: [specific file]
Your branch: [branch name]
First checkpoint: 10:30am (2 hours from now)

Rules:
1. Follow API_SPECIFICATION_v2.0.md EXACTLY
2. Commit code every hour
3. Report blockers immediately
4. No rogue plays - coordinate with team

LET'S BUILD SOMETHING BULLETPROOF. 🔧

- John (Project Coordinator)
```

---

## ✅ FINAL CHECKLIST (6:00pm)

Before calling it complete:

- [ ] Run all 5 integration tests successfully
- [ ] Test on actual Samsung S20 (not emulator)
- [ ] Desktop app packaged as .exe installer
- [ ] Android APK built and tested
- [ ] User manual updated for v2.0
- [ ] All code committed to GitHub
- [ ] README updated with setup instructions
- [ ] Known issues documented

---

**YOU'VE GOT THIS, JOHN. Coordinate, don't micromanage. Trust the briefings. Focus on integration. Let's ship v2.0 today.** 🚀