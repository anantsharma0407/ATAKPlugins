// server.js
const express = require('express');
const http = require('http');
const WebSocket = require('ws');

const app = express();
const server = http.createServer(app);
const wss = new WebSocket.Server({ server, path: '/getCoordinates' });

const PORT = process.env.PORT || 3000;

// -------------------- Simulation State --------------------
let mode = 'route'; // 'static' | 'jitter' | 'route' | 'square'

// Static/base coordinate
let current = { latitude: 17.3850, longitude: 78.4867 };

// Jitter config (meters)
let jitterMeters = 10;

// -------------------- Helpers --------------------
const metersToLat = meters => meters / 111320;
const metersToLon = (meters, lat) => meters / (111320 * Math.cos(lat * Math.PI / 180));

function applyJitter(base, radiusMeters) {
  const angle = Math.random() * 2 * Math.PI;
  const r = Math.sqrt(Math.random()) * radiusMeters;
  const dy = r * Math.cos(angle);
  const dx = r * Math.sin(angle);
  return {
    latitude: base.latitude + metersToLat(dy),
    longitude: base.longitude + metersToLon(dx, base.latitude)
  };
}

function haversineDistance(a, b) {
  const R = 6371000;
  const toRad = v => v * Math.PI / 180;
  const dLat = toRad(b.latitude - a.latitude);
  const dLon = toRad(b.longitude - a.longitude);
  const lat1 = toRad(a.latitude);
  const lat2 = toRad(b.latitude);
  const s = Math.sin(dLat/2)**2 + Math.cos(lat1)*Math.cos(lat2)*Math.sin(dLon/2)**2;
  return 2 * R * Math.atan2(Math.sqrt(s), Math.sqrt(1 - s));
}

function lerpCoord(a, b, t) {
  return {
    latitude: a.latitude + (b.latitude - a.latitude) * t,
    longitude: a.longitude + (b.longitude - a.longitude) * t
  };
}

// Great-circle circle generator (optional helper you can call via WS)
function circleRoute({ centerLat, centerLon, radiusMeters = 300, points = 36, holdSeconds = 0, clockwise = true }) {
  const R = 6371000;
  const toRad = d => d * Math.PI / 180;
  const toDeg = r => r * 180 / Math.PI;
  const φ1 = toRad(centerLat);
  const λ1 = toRad(centerLon);
  const δ = radiusMeters / R;

  const result = [];
  for (let i = 0; i < points; i++) {
    const frac = i / points;
    let θ = 2 * Math.PI * frac;
    if (!clockwise) θ = 2 * Math.PI - θ;

    const sinφ2 = Math.sin(φ1) * Math.cos(δ) + Math.cos(φ1) * Math.sin(δ) * Math.cos(θ);
    const φ2 = Math.asin(sinφ2);
    const y = Math.sin(θ) * Math.sin(δ) * Math.cos(φ1);
    const x = Math.cos(δ) - Math.sin(φ1) * sinφ2;
    const λ2 = λ1 + Math.atan2(y, x);

    result.push({ latitude: +toDeg(φ2).toFixed(6), longitude: +toDeg(λ2).toFixed(6), holdSeconds });
  }
  result.push({ ...result[0] }); // close loop (optional)
  return result;
}

// Square route generator - creates a square path with specified side length
function squareRoute({ centerLat, centerLon, sideMeters = 1000, pointsPerSide = 25, holdSeconds = 0, clockwise = true }) {
  const halfSide = sideMeters / 2;

  // Calculate the four corners of the square
  const topLat = centerLat + metersToLat(halfSide);
  const bottomLat = centerLat - metersToLat(halfSide);
  const rightLon = centerLon + metersToLon(halfSide, centerLat);
  const leftLon = centerLon - metersToLon(halfSide, centerLat);

  const corners = clockwise ? [
    { latitude: topLat, longitude: leftLon },      // Top-left
    { latitude: topLat, longitude: rightLon },     // Top-right
    { latitude: bottomLat, longitude: rightLon },  // Bottom-right
    { latitude: bottomLat, longitude: leftLon }    // Bottom-left
  ] : [
    { latitude: topLat, longitude: leftLon },      // Top-left
    { latitude: bottomLat, longitude: leftLon },   // Bottom-left
    { latitude: bottomLat, longitude: rightLon },  // Bottom-right
    { latitude: topLat, longitude: rightLon }      // Top-right
  ];

  const result = [];

  // Generate points along each side
  for (let side = 0; side < 4; side++) {
    const start = corners[side];
    const end = corners[(side + 1) % 4];

    for (let i = 0; i < pointsPerSide; i++) {
      const t = i / pointsPerSide;
      const lat = start.latitude + (end.latitude - start.latitude) * t;
      const lon = start.longitude + (end.longitude - start.longitude) * t;
      result.push({ latitude: +lat.toFixed(6), longitude: +lon.toFixed(6), holdSeconds });
    }
  }

  // Close the loop
  result.push({ ...result[0] });
  return result;
}

// Route config
const CENTER_LAT = 17.3850;
const CENTER_LON = 78.4867;

// Circle config: 100 mile diameter = 50 mile radius = 80467.2 meters radius
const CIRCLE_RADIUS_METERS = 80467.2; // 50 miles in meters
const CIRCLE_POINTS = 72; // one point every 5 degrees

// Square config: 100 mile sides = 160934.4 meters per side
const SQUARE_SIDE_METERS = 160934.4; // 100 miles in meters
const SQUARE_POINTS_PER_SIDE = 25; // points along each side

const ROUTE_SPEED_MPS = 100; // 100 meters per second for reasonable movement

// Initialize route based on mode
let route = mode === 'square'
  ? squareRoute({
      centerLat: CENTER_LAT,
      centerLon: CENTER_LON,
      sideMeters: SQUARE_SIDE_METERS,
      pointsPerSide: SQUARE_POINTS_PER_SIDE,
      holdSeconds: 0,
      clockwise: true
    })
  : circleRoute({
      centerLat: CENTER_LAT,
      centerLon: CENTER_LON,
      radiusMeters: CIRCLE_RADIUS_METERS,
      points: CIRCLE_POINTS,
      holdSeconds: 0,
      clockwise: true
    });

let routeIndex = 0;
let routeSpeedMetersPerSec = ROUTE_SPEED_MPS;
let isRouteMoving = false;
let lastRouteTick = Date.now();
let lastPosition = null;
let currentVelocity = 0; // meters per second

// Broadcast cadence (Hz)
let streamHz = 1; // 1 update per second
let broadcastTimer = null;

// -------------------- Route Movement Loop --------------------
function startRoute() {
  if (isRouteMoving || route.length < 2) return;
  isRouteMoving = true;
  routeIndex = 0;
  lastRouteTick = Date.now();
  current = { latitude: route[0].latitude, longitude: route[0].longitude, _f: 0 };
  tickRoute();
}

function stopRoute() {
  isRouteMoving = false;
}

function tickRoute() {
  if (!isRouteMoving || (mode !== 'route' && mode !== 'square') || route.length < 2) return;

  const now = Date.now();
  const elapsed = (now - lastRouteTick) / 1000;
  lastRouteTick = now;

  const a = route[routeIndex % route.length];
  const b = route[(routeIndex + 1) % route.length];
  const distance = haversineDistance(a, b);

  if (!current._f) current._f = 0;

  if (distance === 0) {
    routeIndex = (routeIndex + 1) % route.length;
    current = { latitude: a.latitude, longitude: a.longitude, _f: 0 };
    currentVelocity = 0;
    setTimeout(tickRoute, (a.holdSeconds || 0) * 1000);
    return;
  }

  const step = (routeSpeedMetersPerSec * elapsed) / distance;
  current._f += step;

  // Store previous position for velocity calculation
  const prevPosition = lastPosition;

  if (current._f >= 1) {
    routeIndex = (routeIndex + 1) % route.length;
    const wp = route[routeIndex];
    current = { latitude: wp.latitude, longitude: wp.longitude, _f: 0 };

    // Calculate velocity based on actual movement
    if (prevPosition && elapsed > 0) {
      const distMoved = haversineDistance(prevPosition, current);
      currentVelocity = distMoved / elapsed;
    } else {
      currentVelocity = routeSpeedMetersPerSec;
    }

    lastPosition = { latitude: current.latitude, longitude: current.longitude };
    setTimeout(tickRoute, (wp.holdSeconds || 0) * 1000);
  } else {
    const p = lerpCoord(a, b, current._f);
    current = { latitude: p.latitude, longitude: p.longitude, _f: current._f };

    // Calculate velocity based on actual movement
    if (prevPosition && elapsed > 0) {
      const distMoved = haversineDistance(prevPosition, current);
      currentVelocity = distMoved / elapsed;
    } else {
      currentVelocity = routeSpeedMetersPerSec;
    }

    lastPosition = { latitude: current.latitude, longitude: current.longitude };
    setTimeout(tickRoute, 100); // smooth movement
  }
}

// -------------------- WebSocket Protocol --------------------
/**
 * Client -> Server messages (JSON):
 * { "type": "subscribe", "hz": 1 }                 // start streaming at N Hz (default 1)
 * { "type": "setMode", "mode": "static|jitter|route" }
 * { "type": "setCoords", "latitude": 12.34, "longitude": 56.78 }
 * { "type": "setJitter", "meters": 25 }
 * { "type": "setRoute", "route": [{latitude,longitude,holdSeconds?}, ...], "speed": 5 }
 * { "type": "setCircleRoute", "centerLat": 17.385, "centerLon": 78.4867, "radiusMeters": 300, "points": 60, "speed": 5 }
 * { "type": "health" }
 *
 * Server -> Client messages:
 * { "type": "location", "payload": { latitude, longitude, mode, timestamp, velocityMps, routeIndex?, jitterMeters? } }
 * { "type": "status",   "ok": true, "message": "..." }
 * { "type": "error",    "message": "..." }
 * { "type": "health",   "mode": "...", "clients": N }
 */

function nowIso() { return new Date().toISOString(); }

function snapshot() {
  let lat, lon, extra = {};
  if (mode === 'static') {
    lat = current.latitude; lon = current.longitude;
  } else if (mode === 'jitter') {
    const j = applyJitter(current, jitterMeters);
    lat = j.latitude; lon = j.longitude;
    extra.jitterMeters = jitterMeters;
  } else if (mode === 'route' || mode === 'square') {
    lat = current.latitude; lon = current.longitude;
    extra.routeIndex = routeIndex;
  } else {
    lat = current.latitude; lon = current.longitude;
  }

  // Add velocity in meters per second
  extra.velocityMps = currentVelocity;

  return { latitude: lat, longitude: lon, mode, timestamp: nowIso(), ...extra };
}

function broadcastLocation() {
  const msg = JSON.stringify({ type: 'location', payload: snapshot() });
  wss.clients.forEach(ws => {
    if (ws.readyState === WebSocket.OPEN && ws.isSubscribed) {
      ws.send(msg);
    }
  });
}

// Global stream loop (shared timer)
function ensureBroadcastTimer() {
  if (broadcastTimer) return;
  const tick = () => {
    broadcastLocation();
    const intervalMs = Math.max(100, 1000 / streamHz);
    broadcastTimer = setTimeout(tick, intervalMs);
  };
  tick();
}

wss.on('connection', (ws) => {
  ws.isSubscribed = false;
  ws.hz = streamHz;

  // Keep-alive ping/pong
  ws.isAlive = true;
  ws.on('pong', () => { ws.isAlive = true; });

  // Send welcome
  ws.send(JSON.stringify({ type: 'status', ok: true, message: 'Connected. Send {"type":"subscribe","hz":1} to start streaming.' }));

  ws.on('message', (raw) => {
    let msg;
    try { msg = JSON.parse(raw.toString()); }
    catch (e) { return ws.send(JSON.stringify({ type: 'error', message: 'Invalid JSON' })); }

    const t = msg.type;

    if (t === 'subscribe') {
      ws.isSubscribed = true;
      if (typeof msg.hz === 'number' && msg.hz > 0) {
        ws.hz = msg.hz;
        // We keep a single global timer; optional per-client pacing could be added if needed.
        streamHz = Math.max(streamHz, ws.hz);
      }
      ensureBroadcastTimer();
      return ws.send(JSON.stringify({ type: 'status', ok: true, message: `Subscribed at ~${ws.hz} Hz` }));
    }

    if (t === 'setMode') {
      const m = msg.mode;
      if (!['static','jitter','route','square'].includes(m)) return ws.send(JSON.stringify({ type:'error', message:'mode must be static|jitter|route|square'}));
      mode = m;

      // Switch route if changing between square and route modes
      if (mode === 'square') {
        route = squareRoute({
          centerLat: CENTER_LAT,
          centerLon: CENTER_LON,
          sideMeters: SQUARE_SIDE_METERS,
          pointsPerSide: SQUARE_POINTS_PER_SIDE,
          holdSeconds: 0,
          clockwise: true
        });
        routeIndex = 0;
        current = { latitude: route[0].latitude, longitude: route[0].longitude, _f: 0 };
        startRoute();
      } else if (mode === 'route') {
        route = circleRoute({
          centerLat: CENTER_LAT,
          centerLon: CENTER_LON,
          radiusMeters: CIRCLE_RADIUS_METERS,
          points: CIRCLE_POINTS,
          holdSeconds: 0,
          clockwise: true
        });
        routeIndex = 0;
        current = { latitude: route[0].latitude, longitude: route[0].longitude, _f: 0 };
        startRoute();
      } else {
        stopRoute();
      }

      return ws.send(JSON.stringify({ type:'status', ok:true, message:`mode=${mode}` }));
    }

    if (t === 'setCoords') {
      const { latitude, longitude } = msg;
      if (typeof latitude !== 'number' || typeof longitude !== 'number')
        return ws.send(JSON.stringify({ type:'error', message:'latitude & longitude numbers required'}));
      current = { latitude, longitude };
      return ws.send(JSON.stringify({ type:'status', ok:true, message:`coords set to ${latitude},${longitude}` }));
    }

    if (t === 'setJitter') {
      const m = msg.meters;
      if (typeof m !== 'number' || m < 0) return ws.send(JSON.stringify({ type:'error', message:'meters must be positive number'}));
      jitterMeters = m;
      return ws.send(JSON.stringify({ type:'status', ok:true, message:`jitter=${m}m` }));
    }

    if (t === 'setRoute') {
      const r = msg.route;
      if (!Array.isArray(r) || r.length < 2)
        return ws.send(JSON.stringify({ type:'error', message:'route must be array of >= 2 waypoints'}));
      for (const p of r) {
        if (typeof p.latitude !== 'number' || typeof p.longitude !== 'number')
          return ws.send(JSON.stringify({ type:'error', message:'each waypoint needs numeric latitude & longitude'}));
      }
      route = r;
      routeIndex = 0;
      if (typeof msg.speed === 'number' && msg.speed > 0) routeSpeedMetersPerSec = msg.speed;
      current = { latitude: route[0].latitude, longitude: route[0].longitude, _f: 0 };
      if (mode === 'route') startRoute();
      return ws.send(JSON.stringify({ type:'status', ok:true, message:`route loaded: ${route.length} points at ${routeSpeedMetersPerSec} m/s` }));
    }

    if (t === 'setCircleRoute') {
      const { centerLat, centerLon, radiusMeters = 300, points = 36, speed = 3, clockwise = true, holdSeconds = 0 } = msg;
      if (typeof centerLat !== 'number' || typeof centerLon !== 'number') {
        return ws.send(JSON.stringify({ type:'error', message:'centerLat & centerLon required (numbers)'}));
      }
      route = circleRoute({ centerLat, centerLon, radiusMeters, points, holdSeconds, clockwise });
      routeIndex = 0;
      routeSpeedMetersPerSec = speed;
      current = { latitude: route[0].latitude, longitude: route[0].longitude, _f: 0 };
      if (mode === 'route') startRoute();
      return ws.send(JSON.stringify({ type:'status', ok:true, message:`circle route set (${points} pts, r=${radiusMeters}m)` }));
    }

    if (t === 'health') {
      return ws.send(JSON.stringify({ type:'health', mode, clients: wss.clients.size }));
    }

    return ws.send(JSON.stringify({ type:'error', message:`unknown type: ${t}` }));
  });

  ws.on('close', () => {
    // no-op
  });
});

// Ping dead connections every 30s
const interval = setInterval(() => {
  wss.clients.forEach(ws => {
    if (!ws.isAlive) return ws.terminate();
    ws.isAlive = false;
    ws.ping();
  });
}, 30000);

server.listen(PORT, () => {
  console.log(`FakeLocation WS server on http://localhost:${PORT}  (WS path: /getCoordinates)`);
  if (mode === 'square') {
    console.log(`Starting square mode with ${route.length} waypoints in a ${SQUARE_SIDE_METERS / 1609.34} mile per side square`);
  } else if (mode === 'route') {
    console.log(`Starting route mode with ${route.length} waypoints in a ${CIRCLE_RADIUS_METERS * 2 / 1609.34} mile diameter circle`);
  }
  if (mode === 'route' || mode === 'square') {
    startRoute();
  }
});