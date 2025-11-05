package com.atakmap.android.helloworld.plugin;

import android.os.Handler;
import android.os.Looper;

import com.atakmap.coremap.log.Log;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.util.concurrent.TimeUnit;

/**
 * Service to connect to WebSocket and receive location coordinates
 */
public class LocationWebSocketService {

    private static final String TAG = "LocationWebSocketService";

    private WebSocket webSocket;
    private OkHttpClient client;
    private LocationUpdateListener locationUpdateListener;
    private String websocketUrl;
    private boolean isConnected = false;
    private Handler reconnectHandler;
    private static final long RECONNECT_DELAY_MS = 5000; // 5 seconds

    public interface LocationUpdateListener {
        void onLocationUpdate(double latitude, double longitude);
        void onConnectionStatusChanged(boolean connected);
        void onError(String error);
    }

    public LocationWebSocketService(String url) {
        this.websocketUrl = url;
        this.reconnectHandler = new Handler(Looper.getMainLooper());

        // Configure OkHttp client with timeouts
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS) // No timeout for WebSocket
                .writeTimeout(10, TimeUnit.SECONDS)
                .pingInterval(30, TimeUnit.SECONDS) // Keep connection alive
                .build();
    }

    public void setLocationUpdateListener(LocationUpdateListener listener) {
        this.locationUpdateListener = listener;
    }

    public void connect() {
        if (webSocket != null) {
            Log.d(TAG, "WebSocket already connected or connecting");
            return;
        }

        Log.d(TAG, "Connecting to WebSocket: " + websocketUrl);

        try {
            Request request = new Request.Builder()
                    .url(websocketUrl)
                    .build();

            webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket connected");
                isConnected = true;

                // Send subscription message to start receiving location updates
                JSONObject subscribeMsg = new JSONObject();
                try {
                    subscribeMsg.put("type", "subscribe");
                    subscribeMsg.put("hz", 1); // Request 1 update per second
                    webSocket.send(subscribeMsg.toString());
                    Log.d(TAG, "Sent subscription request");
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to create subscription message: " + e.getMessage());
                }

                if (locationUpdateListener != null) {
                    locationUpdateListener.onConnectionStatusChanged(true);
                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, "Message received: " + text);
                parseLocationData(text);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket error: " + t.getMessage());
                isConnected = false;
                if (locationUpdateListener != null) {
                    locationUpdateListener.onConnectionStatusChanged(false);
                    locationUpdateListener.onError("Connection failed: " + t.getMessage());
                }

                // Attempt to reconnect after delay
                scheduleReconnect();
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket closing: " + reason);
                webSocket.close(1000, null);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket closed: " + reason);
                isConnected = false;
                LocationWebSocketService.this.webSocket = null;
                if (locationUpdateListener != null) {
                    locationUpdateListener.onConnectionStatusChanged(false);
                }

                // Attempt to reconnect after delay
                scheduleReconnect();
            }
        });
        } catch (IllegalArgumentException e) {
            // Invalid URL
            Log.e(TAG, "Invalid WebSocket URL: " + e.getMessage());
            if (locationUpdateListener != null) {
                locationUpdateListener.onError("Invalid WebSocket URL: " + websocketUrl);
            }
            webSocket = null;
        } catch (Exception e) {
            // Any other exception
            Log.e(TAG, "Failed to create WebSocket connection: " + e.getMessage());
            if (locationUpdateListener != null) {
                locationUpdateListener.onError("Failed to connect: " + e.getMessage());
            }
            webSocket = null;
        }
    }

    private void parseLocationData(String jsonData) {
        try {
            JSONObject json = new JSONObject(jsonData);

            // Check message type from server
            String messageType = json.optString("type", "");

            if ("location".equals(messageType)) {
                // Server sends: {"type":"location", "payload":{"latitude":..., "longitude":...}}
                if (json.has("payload")) {
                    JSONObject payload = json.getJSONObject("payload");
                    double latitude = payload.getDouble("latitude");
                    double longitude = payload.getDouble("longitude");

                    Log.d(TAG, "Parsed location - Lat: " + latitude + ", Lon: " + longitude);

                    if (locationUpdateListener != null) {
                        locationUpdateListener.onLocationUpdate(latitude, longitude);
                    }
                }
            } else if ("status".equals(messageType)) {
                // Server status messages: {"type":"status", "ok":true, "message":"..."}
                String message = json.optString("message", "Status received");
                Log.d(TAG, "Server status: " + message);
            } else if ("error".equals(messageType)) {
                // Server error messages: {"type":"error", "message":"..."}
                String errorMsg = json.optString("message", "Unknown error");
                Log.e(TAG, "Server error: " + errorMsg);
                if (locationUpdateListener != null) {
                    locationUpdateListener.onError("Server error: " + errorMsg);
                }
            } else if ("health".equals(messageType)) {
                // Health check response
                String mode = json.optString("mode", "unknown");
                int clients = json.optInt("clients", 0);
                Log.d(TAG, "Health check - Mode: " + mode + ", Clients: " + clients);
            } else if (json.has("latitude") && json.has("longitude")) {
                // Direct format (fallback): {"latitude": 17.3850, "longitude": 78.4867}
                double latitude = json.getDouble("latitude");
                double longitude = json.getDouble("longitude");

                Log.d(TAG, "Parsed location (direct) - Lat: " + latitude + ", Lon: " + longitude);

                if (locationUpdateListener != null) {
                    locationUpdateListener.onLocationUpdate(latitude, longitude);
                }
            } else if (json.has("lat") && json.has("lon")) {
                // Alternative format
                double latitude = json.getDouble("lat");
                double longitude = json.getDouble("lon");

                Log.d(TAG, "Parsed location (alt) - Lat: " + latitude + ", Lon: " + longitude);

                if (locationUpdateListener != null) {
                    locationUpdateListener.onLocationUpdate(latitude, longitude);
                }
            } else {
                Log.w(TAG, "Unknown message format: " + jsonData);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing location data: " + e.getMessage());
            if (locationUpdateListener != null) {
                locationUpdateListener.onError("Failed to parse location data");
            }
        }
    }

    private void scheduleReconnect() {
        reconnectHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isConnected && webSocket == null) {
                    Log.d(TAG, "Attempting to reconnect...");
                    connect();
                }
            }
        }, RECONNECT_DELAY_MS);
    }

    public void disconnect() {
        Log.d(TAG, "Disconnecting WebSocket");

        // Cancel any pending reconnect attempts
        reconnectHandler.removeCallbacksAndMessages(null);

        if (webSocket != null) {
            webSocket.close(1000, "Client disconnect");
            webSocket = null;
        }
        isConnected = false;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void sendMessage(String message) {
        if (webSocket != null && isConnected) {
            webSocket.send(message);
            Log.d(TAG, "Sent message: " + message);
        } else {
            Log.w(TAG, "Cannot send message - WebSocket not connected");
        }
    }

    /**
     * Clean up resources
     */
    public void dispose() {
        disconnect();
        if (client != null) {
            client.dispatcher().executorService().shutdown();
        }
    }
}