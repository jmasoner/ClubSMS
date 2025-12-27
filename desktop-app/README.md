# ClubSMS Desktop App

A Windows desktop application for controlling SMS broadcasting from your computer.

## Features

- 🔗 Connect to your Android phone over WiFi
- 📝 Compose messages on a full keyboard
- 👥 Select recipients from phone contacts
- 📤 Monitor broadcast progress in real-time
- 🔔 Desktop notifications for events

## Quick Start

### Prerequisites

- Node.js 18+ installed
- ClubSMS v2.0 installed on your Android phone
- Both devices on the same WiFi network

### Installation

```powershell
cd desktop-app
npm install
```

### Running

```powershell
npm start
```

### Development Mode (with DevTools)

```powershell
npm start -- --dev
```

## Usage

1. **On your phone:**
   - Open ClubSMS app
   - Enable "Desktop Mode" toggle
   - Note the IP address shown (e.g., `192.168.1.105`)

2. **On your desktop:**
   - Launch ClubSMS Desktop
   - Enter the phone's IP address
   - Click "Connect to Phone"

3. **Send messages:**
   - Select recipients from the list
   - Type your message
   - Click "Send Broadcast"

## Building for Distribution

### Windows Installer

```powershell
npm run package:win
```

The installer will be in `release/` folder.

## Project Structure

```
desktop-app/
├── main.js          # Electron main process
├── preload.js       # Secure bridge to renderer
├── index.html       # UI (includes CSS + JS)
├── package.json     # Dependencies & scripts
└── assets/          # Icons (add icon.ico here)
```

## Troubleshooting

### "Connection refused"
- Ensure phone and computer are on same WiFi
- Check that Desktop Mode is enabled on phone
- Verify the IP address is correct

### "Connection timeout"
- Phone might have gone to sleep
- Open ClubSMS app on phone to wake it up

### Contacts not loading
- Wait a few seconds after connecting
- Click "Refresh" button in contacts panel

