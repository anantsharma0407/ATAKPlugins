package com.atakmap.android.helloworld.plugin;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.log.Log;

public class PluginTemplatePane {

    private static final String TAG = "PluginTemplatePane";
    private final MapView mapView;
    private View root;
    private Switch switchTrack;
    private Marker marker;
    private LocationWebSocketService webSocketService;
    private android.widget.TextView statusText;
    private android.widget.TextView locationText;

    // WebSocket URL - configure this to your server
    private static final String WEBSOCKET_URL = "ws://192.168.4.21:3000/getCoordinates";

    // Initial coordinate (will be updated by WebSocket)
    private final GeoPoint targetPoint = new GeoPoint(17.3850, 78.4867); // Hyderabad example

    public PluginTemplatePane(MapView mapView) {
        this.mapView = mapView;
        initializeWebSocket();
    }

    public PluginTemplatePane() {
        this(MapView.getMapView());
    }

    private void initializeWebSocket() {
        try {
            Log.d(TAG, "Initializing WebSocket service with URL: " + WEBSOCKET_URL);
            webSocketService = new LocationWebSocketService(WEBSOCKET_URL);

            webSocketService.setLocationUpdateListener(new LocationWebSocketService.LocationUpdateListener() {
                @Override
                public void onLocationUpdate(double latitude, double longitude) {
                    Log.d(TAG, "Received location update - Lat: " + latitude + ", Lon: " + longitude);
                    updateMarkerPosition(latitude, longitude);
                    updateLocationDisplay(latitude, longitude);
                }

                @Override
                public void onConnectionStatusChanged(final boolean connected) {
                    Log.d(TAG, "WebSocket connection status: " + connected);
                    final String status = connected ? "Connected" : "Disconnected";
                    // Show toast on UI thread
                    mapView.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mapView.getContext(), "WebSocket " + status, Toast.LENGTH_SHORT).show();
                        }
                    });
                    updateStatusDisplay(connected);
                }

                @Override
                public void onError(final String error) {
                    Log.e(TAG, "WebSocket error: " + error);
                    // Show toast on UI thread
                    mapView.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mapView.getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
                    updateErrorDisplay(error);
                }
            });
            Log.d(TAG, "WebSocket service initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize WebSocket service: " + e.getMessage(), e);
            Toast.makeText(mapView.getContext(),
                "Failed to initialize tracking: " + e.getMessage(),
                Toast.LENGTH_LONG).show();
            webSocketService = null;
        }
    }

    public View onCreateView(LayoutInflater inflater) {
        Log.d(TAG, "yrs called");
        Toast.makeText(mapView.getContext(), "Creating Location Tracker View", Toast.LENGTH_SHORT).show();

        root = inflater.inflate(R.layout.overlay_track, null, false);
        switchTrack = root.findViewById(R.id.switch_track);
        statusText = root.findViewById(R.id.status_text);
        locationText = root.findViewById(R.id.location_text);

        if (switchTrack == null) {
            Log.e(TAG, "ERROR: switch_track not found in layout!");
            Toast.makeText(mapView.getContext(), "ERROR: Switch not found!", Toast.LENGTH_LONG).show();
            return root;
        } else {
            Log.d(TAG, "Switch found, setting listener");
            Toast.makeText(mapView.getContext(), "Switch found!", Toast.LENGTH_SHORT).show();
        }

        // Use setOnClickListener instead to test if ANY interaction works
        switchTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mapView.getContext(), "Switch CLICKED!", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Switch clicked!");
            }
        });

        switchTrack.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "Switch toggled: " + isChecked);
                Toast.makeText(mapView.getContext(), "Tracking " + (isChecked ? "ENABLED" : "DISABLED"), Toast.LENGTH_SHORT).show();
                if (isChecked) {
                    addMarker();
                    startTracking();
                } else {
                    removeMarker();
                    stopTracking();
                }
            }
        });

        return root;
    }

    private void addMarker() {
        // If already present, do nothing
        if (marker != null) {
            Log.d(TAG, "Marker already exists, skipping");
            return;
        }

        Log.d(TAG, "Creating marker at: " + targetPoint);

        // Create a new Marker with more visible properties
        final Marker m = new Marker(0L, targetPoint, "TRACKED LOCATION");

        // Make the marker highly visible
        m.setType("a-f-G-E-S"); // Friendly ground equipment - shows as a bright icon
        m.setMetaString("callsign", "TRACKED LOCATION");
        m.setMetaBoolean("declutter", false); // Never hide this marker
        m.setMetaBoolean("editable", false);
        m.setMetaBoolean("movable", false);
        m.setShowLabel(true); // Always show label
        m.setMetaInteger("color", -256); // Yellow color (0xFFFFFF00)
        m.setMetaString("iconType", "b-m-p-s-m"); // Bullseye/reference point - very visible

        // Add to map on UI thread
        mapView.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Adding marker to map");
                mapView.getRootGroup().addItem(m);
                marker = m;

                // Pan to marker with a slight delay to ensure it's been added
                new Handler(mapView.getContext().getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "Panning map to marker location: " + targetPoint);
                        mapView.getMapController().panTo(targetPoint, true);
                    }
                }, 200); // 200ms delay
            }
        });
    }

    private void removeMarker() {
        final Marker m = marker;
        if (m == null) return;

        mapView.post(new Runnable() {
            @Override
            public void run() {
                // Remove if still attached
                mapView.getRootGroup().removeItem(m);
                marker = null;
            }
        });
    }

    private void startTracking() {
        Log.d(TAG, "Starting WebSocket tracking");
        if (webSocketService == null) {
            Log.e(TAG, "WebSocket service is null, reinitializing...");
            initializeWebSocket();
        }

        // Run connection on a background thread to avoid blocking UI
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (webSocketService != null) {
                        webSocketService.connect();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Exception during WebSocket connect: " + e.getMessage(), e);
                    e.printStackTrace();
                    // Notify on UI thread
                    mapView.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mapView.getContext(),
                                "Failed to start tracking: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void stopTracking() {
        Log.d(TAG, "Stopping WebSocket tracking");
        if (webSocketService != null) {
            webSocketService.disconnect();
        }
    }

    public void onDestroyView() {
        // Clean up
        Log.d(TAG, "Destroying view - cleaning up resources");
        stopTracking();
        removeMarker();

        if (webSocketService != null) {
            webSocketService.dispose();
            webSocketService = null;
        }

        if (switchTrack != null) {
            switchTrack.setOnCheckedChangeListener(null);
            switchTrack = null;
        }
        root = null;
    }

    /**
     * Update marker position from WebSocket
     * @param latitude new latitude
     * @param longitude new longitude
     */
    public void updateMarkerPosition(final double latitude, final double longitude) {
        mapView.post(new Runnable() {
            @Override
            public void run() {
                if (marker != null) {
                    GeoPoint newPoint = new GeoPoint(latitude, longitude);
                    marker.setPoint(newPoint);
                    Log.d(TAG, "Updated marker position to: " + latitude + ", " + longitude);
                    // Optionally center on new position
                    // mapView.getMapController().panTo(newPoint, true);
                }
            }
        });
    }

    /**
     * Update status text in UI
     */
    private void updateStatusDisplay(final boolean connected) {
        mapView.post(new Runnable() {
            @Override
            public void run() {
                if (statusText != null) {
                    if (connected) {
                        statusText.setText("Status: Connected");
                        statusText.setTextColor(0xFF00FF00); // Green
                    } else {
                        statusText.setText("Status: Disconnected");
                        statusText.setTextColor(0xFFFFA500); // Orange
                    }
                }
            }
        });
    }

    /**
     * Update location text in UI
     */
    private void updateLocationDisplay(final double latitude, final double longitude) {
        mapView.post(new Runnable() {
            @Override
            public void run() {
                if (locationText != null) {
                    String coordText = String.format("Location: %.4f, %.4f", latitude, longitude);
                    locationText.setText(coordText);
                }
            }
        });
    }

    /**
     * Update error display in UI
     */
    private void updateErrorDisplay(final String error) {
        mapView.post(new Runnable() {
            @Override
            public void run() {
                if (statusText != null) {
                    statusText.setText("Status: Error");
                    statusText.setTextColor(0xFFFF0000); // Red
                }
                if (locationText != null) {
                    // Truncate error if too long
                    String displayError = error;
                    if (displayError.length() > 50) {
                        displayError = displayError.substring(0, 47) + "...";
                    }
                    locationText.setText("Error: " + displayError);
                }
            }
        });
    }

    /**
     * Set the WebSocket URL (optional - for dynamic configuration)
     */
    public void setWebSocketUrl(String url) {
        if (webSocketService != null) {
            webSocketService.disconnect();
            webSocketService.dispose();
        }
        webSocketService = new LocationWebSocketService(url);
        initializeWebSocket();
    }
}