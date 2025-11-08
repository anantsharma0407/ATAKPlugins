
The WebSocket server simulates GPS location updates for testing the ATAK HelloWorld Plugin.
It must be executed before testing the plugin

## Prerequisites

- **Node.js** (v14 or higher)
- **npm** (comes with Node.js)

## Installation_

1. Navigate to the FakeLocationService directory:

2. Install dependencies:
```bash
npm install
```

This will install:
- `express` (v5.1.0) - Web server framework
- `ws` (v8.18.3) - WebSocket library
- `body-parser` (v2.2.0) - Request body parsing

---

## Running the Server

### Quick Start

Start the server with default settings (route mode):
```bash
node index.js
```

You should see:
```
FakeLocation WS server on http://localhost:3000  (WS path: /getCoordinates)
Starting route mode with 73 waypoints in a 100 mile diameter circle
```

### Server Configuration

The server runs on:
- **Host**: `localhost` (0.0.0.0)
- **Port**: `3000` (configurable via `PORT` environment variable)
- **WebSocket Path**: `/getCoordinates`
- **Full URL**: `ws://localhost:3000/getCoordinates`

To use a different port:
```bash
PORT=8080 node index.js
```

---

## Testing the Server

### Using wscat (WebSocket CLI tool)

Install wscat:
```bash
npm install -g wscat
```

Connect to the server:
```bash
wscat -c ws://localhost:3000/getCoordinates
```

You should see:
```json
{"type":"status","ok":true,"message":"Connected. Send {\"type\":\"subscribe\",\"hz\":1} to start streaming."}
```

Subscribe to location updates (1Hz):
```json
{"type":"subscribe","hz":1}
```

You'll receive location updates:
```json
{
  "type":"location",
  "payload":{
    "latitude":17.385,
    "longitude":78.4867,
    "mode":"route",
    "timestamp":"2025-01-07T12:00:00.000Z",
    "velocityMps":100.5,
    "routeIndex":12
  }
}
```

### Using curl (HTTP test)

Check if the server is running:
```bash
curl http://localhost:3000
```

---

## WebSocket Protocol

### Client → Server Messages

#### Subscribe to Updates
```json
{
  "type": "subscribe",
  "hz": 1
}
```
- `hz`: Update frequency (1-10 recommended)

#### Change Mode
```json
{
  "type": "setMode",
  "mode": "route"
}
```
- `mode`: `static`, `jitter`, `route`, or `square`

#### Set Static Coordinates
```json
{
  "type": "setCoords",
  "latitude": 17.3850,
  "longitude": 78.4867
}
```

#### Set Jitter Radius
```json
{
  "type": "setJitter",
  "meters": 25
}
```

#### Custom Route
```json
{
  "type": "setRoute",
  "route": [
    {"latitude": 17.385, "longitude": 78.486, "holdSeconds": 0},
    {"latitude": 17.390, "longitude": 78.490, "holdSeconds": 2}
  ],
  "speed": 5
}
```

#### Custom Circle Route
```json
{
  "type": "setCircleRoute",
  "centerLat": 17.385,
  "centerLon": 78.4867,
  "radiusMeters": 500,
  "points": 36,
  "speed": 5,
  "clockwise": true
}
```

#### Health Check
```json
{
  "type": "health"
}
```

### Server → Client Messages

#### Location Update
```json
{
  "type": "location",
  "payload": {
    "latitude": 17.3850,
    "longitude": 78.4867,
    "mode": "route",
    "timestamp": "2025-01-07T12:00:00.000Z",
    "velocityMps": 100.5,
    "routeIndex": 12
  }
}
```

#### Status Message
```json
{
  "type": "status",
  "ok": true,
  "message": "Subscribed at ~1 Hz"
}
```

#### Error Message
```json
{
  "type": "error",
  "message": "Invalid JSON"
}
```

#### Health Response
```json
{
  "type": "health",
  "mode": "route",
  "clients": 2
}
```

---

## Connecting from the ATAK Plugin

### Step 1: Find Your Computer's IP Address

#### macOS:
```bash
ipconfig getifaddr en0
```

#### Linux:
```bash
hostname -I | awk '{print $1}'
```

#### Windows:
```bash
ipconfig
```
Look for "IPv4 Address"

Example output: `192.168.4.21`

### Step 2: Update Plugin Configuration

Edit `PluginTemplatePane.java` (line 28):
```java
private static final String WEBSOCKET_URL = "ws://192.168.4.21:3000/getCoordinates";
```

Replace `192.168.4.21` with your computer's IP address.

### Step 3: Start the Server

```bash
cd FakeLocationService
node index.js
```

### Step 4: Run the Plugin

1. Build and install the plugin on your Android device
2. Launch ATAK
3. Open the HelloWorld plugin
4. Toggle the "Enable Tracking" switch

The plugin should connect and start displaying location updates.

---

## Network Configuration

### Firewall Settings

Ensure port 3000 is open on your firewall:

#### macOS:
```bash
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --add node
sudo /usr/libexec/ApplicationFirewall/socketfilterfw --unblockapp node
```

#### Linux (ufw):
```bash
sudo ufw allow 3000/tcp
```

#### Windows:
Add inbound rule for port 3000 in Windows Firewall.

### Same Network Requirement

**Important**: Your computer (running the server) and your Android device (running ATAK) must be on the same network (WiFi).

---

## Troubleshooting

### Server won't start

**Error**: `Error: listen EADDRINUSE :::3000`

**Solution**: Port 3000 is already in use. Kill the process or use a different port:
```bash
# Find process on port 3000
lsof -i :3000

# Kill it
kill -9 <PID>

# Or use a different port
PORT=8080 node index.js
```

### Plugin can't connect

**Check 1**: Server is running
```bash
curl http://localhost:3000
```

**Check 2**: Both devices on same network
```bash
ping 192.168.4.21  # From Android device
```

**Check 3**: Firewall allows connections
```bash
telnet 192.168.4.21 3000  # From another computer
```

**Check 4**: WebSocket URL in plugin matches server IP
- Server IP: `192.168.4.21`
- Plugin URL: `ws://192.168.4.21:3000/getCoordinates`

### No location updates

**Check**: Subscribed to updates
```bash
wscat -c ws://localhost:3000/getCoordinates
> {"type":"subscribe","hz":1}
```

You should see location messages streaming.

### Server crashes

**Check logs**: Look for error messages in console

**Restart server**:
```bash
node index.js
```

---

## Development & Testing

### Running in Different Modes

Start in **static mode**:
```javascript
// Edit index.js line 13
let mode = 'static';
```

Start in **square mode**:
```javascript
// Edit index.js line 13
let mode = 'square';
```

### Adjusting Speed

Edit `index.js` line 135:
```javascript
const ROUTE_SPEED_MPS = 50; // 50 meters per second
```

### Changing Route Center

Edit `index.js` lines 124-125:
```javascript
const CENTER_LAT = 37.7749; // San Francisco
const CENTER_LON = -122.4194;
```

### Enabling Debug Logging

Add console logs in `index.js`:
```javascript
function broadcastLocation() {
  const msg = JSON.stringify({ type: 'location', payload: snapshot() });
  console.log('Broadcasting:', msg); // Add this line
  wss.clients.forEach(ws => {
    if (ws.readyState === WebSocket.OPEN && ws.isSubscribed) {
      ws.send(msg);
    }
  });
}
```

---

## Quick Reference Card

### Start Server
```bash
cd FakeLocationService
npm install
node index.js
```

### Test Server
```bash
wscat -c ws://localhost:3000/getCoordinates
{"type":"subscribe","hz":1}
```

### Plugin Configuration
```java
// PluginTemplatePane.java:28
private static final String WEBSOCKET_URL = "ws://<YOUR_IP>:3000/getCoordinates";
```

### Common Commands
```bash
# Check server running
curl http://localhost:3000

# Find your IP
ipconfig getifaddr en0  # macOS

# Kill process on port 3000
lsof -i :3000
kill -9 <PID>
```

---
