package com.atakmap.android.helloworld.plugin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.log.Log;

public class PluginTemplatePane {

    private static final String TAG = "PluginTemplatePane";
    private final MapView mapView;
    private final Context context;
    private View root;

    // Location simulator (not a service - just a regular class)
    private LocationSimulator locationSimulator;

    // Map marker
    private Marker marker;

    // UI elements
    private TextView statusText;
    private TextView locationText;
    private TextView velocityText;
    private Button btnStart;
    private Button btnStop;
    private Button btnModeStatic;
    private Button btnModeJitter;
    private Button btnModeCircle;
    private Button btnModeSquare;

    public PluginTemplatePane(MapView mapView, Context context) {
        this.mapView = mapView;
        this.context = context;
        initializeSimulator();
    }

    public PluginTemplatePane() {
        this(MapView.getMapView(), MapView.getMapView().getContext());
    }

    /**
     * Initialize the location simulator
     */
    private void initializeSimulator() {
        Log.d(TAG, "Initializing location simulator");
        locationSimulator = new LocationSimulator();

        // Set up listener
        locationSimulator.setLocationUpdateListener(new LocationSimulator.LocationUpdateListener() {
            @Override
            public void onLocationUpdate(double latitude, double longitude) {
                updateMarkerPosition(latitude, longitude);
                updateLocationDisplay(latitude, longitude);
                updateVelocityDisplay();
            }
        });

        updateStatusDisplay("Ready", 0xFF00FF00);
        Log.d(TAG, "Location simulator initialized");
    }

    public View onCreateView(LayoutInflater inflater) {
        Log.d(TAG, "Creating view");

        // Create main layout
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);
        layout.setBackgroundColor(0xFF2B2B2B);

        // Title
        TextView title = new TextView(context);
        title.setText("Location Simulator");
        title.setTextSize(22);
        title.setTextColor(0xFFFFFFFF);
        title.setPadding(0, 0, 0, 20);
        layout.addView(title);

        // Status text
        statusText = new TextView(context);
        statusText.setText("Status: Ready");
        statusText.setTextSize(16);
        statusText.setTextColor(0xFF00FF00);
        statusText.setPadding(0, 10, 0, 10);
        layout.addView(statusText);

        // Location text
        locationText = new TextView(context);
        locationText.setText("Location: --");
        locationText.setTextSize(14);
        locationText.setTextColor(0xFFFFFFFF);
        locationText.setPadding(0, 10, 0, 10);
        layout.addView(locationText);

        // Velocity text
        velocityText = new TextView(context);
        velocityText.setText("Velocity: 0 m/s (0 km/h)");
        velocityText.setTextSize(14);
        velocityText.setTextColor(0xFF00BFFF); // Deep sky blue
        velocityText.setPadding(0, 10, 0, 20);
        layout.addView(velocityText);

        // Start button
        btnStart = new Button(context);
        btnStart.setText("Start Tracking");
        btnStart.setOnClickListener(v -> startTracking());
        layout.addView(btnStart);

        // Stop button
        btnStop = new Button(context);
        btnStop.setText("Stop Tracking");
        btnStop.setOnClickListener(v -> stopTracking());
        layout.addView(btnStop);

        // Mode selection label
        TextView modeLabel = new TextView(context);
        modeLabel.setText("Select Mode:");
        modeLabel.setTextSize(18);
        modeLabel.setTextColor(0xFFFFFFFF);
        modeLabel.setPadding(0, 20, 0, 10);
        layout.addView(modeLabel);

        // Mode buttons
        btnModeStatic = new Button(context);
        btnModeStatic.setText("Static");
        btnModeStatic.setOnClickListener(v -> setMode(LocationSimulator.Mode.STATIC));
        layout.addView(btnModeStatic);

        btnModeJitter = new Button(context);
        btnModeJitter.setText("Jitter");
        btnModeJitter.setOnClickListener(v -> setMode(LocationSimulator.Mode.JITTER));
        layout.addView(btnModeJitter);

        btnModeCircle = new Button(context);
        btnModeCircle.setText("Circle Route (50 mi radius)");
        btnModeCircle.setOnClickListener(v -> setMode(LocationSimulator.Mode.ROUTE));
        layout.addView(btnModeCircle);

        btnModeSquare = new Button(context);
        btnModeSquare.setText("Square Route (100 mi sides)");
        btnModeSquare.setOnClickListener(v -> setMode(LocationSimulator.Mode.SQUARE));
        layout.addView(btnModeSquare);

        // Wrap layout in ScrollView for scrollability
        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(0xFF2B2B2B);
        scrollView.addView(layout);

        root = scrollView;
        return root;
    }

    /**
     * Start tracking
     */
    private void startTracking() {
        if (locationSimulator == null) {
            Toast.makeText(context, "Simulator not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Starting tracking");
        locationSimulator.startStreaming();
        updateStatusDisplay("Tracking", 0xFF00FF00);
        Toast.makeText(context, "Tracking started", Toast.LENGTH_SHORT).show();
    }

    /**
     * Stop tracking
     */
    private void stopTracking() {
        if (locationSimulator == null) {
            return;
        }

        Log.d(TAG, "Stopping tracking");
        locationSimulator.stopStreaming();
        removeMarker();
        updateStatusDisplay("Stopped", 0xFFFFA500);
        Toast.makeText(context, "Tracking stopped", Toast.LENGTH_SHORT).show();
    }

    /**
     * Set simulation mode
     */
    private void setMode(LocationSimulator.Mode mode) {
        if (locationSimulator == null) {
            Toast.makeText(context, "Simulator not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Setting mode to: " + mode);
        locationSimulator.setMode(mode);
        Toast.makeText(context, "Mode set to: " + mode, Toast.LENGTH_SHORT).show();
    }

    /**
     * Update marker position
     */
    private void updateMarkerPosition(final double latitude, final double longitude) {
        mapView.post(new Runnable() {
            @Override
            public void run() {
                if (marker == null) {
                    createMarker(latitude, longitude);
                } else {
                    GeoPoint newPoint = new GeoPoint(latitude, longitude);
                    marker.setPoint(newPoint);
                }
                Log.d(TAG, "Updated marker to: " + latitude + ", " + longitude);
            }
        });
    }

    /**
     * Create marker on map
     */
    private void createMarker(double latitude, double longitude) {
        GeoPoint point = new GeoPoint(latitude, longitude);
        final Marker m = new Marker(0L, point, "TRACKED LOCATION");

        m.setType("a-f-G-E-S");
        m.setMetaString("callsign", "TRACKED LOCATION");
        m.setMetaBoolean("declutter", false);
        m.setMetaBoolean("editable", false);
        m.setMetaBoolean("movable", false);
        m.setShowLabel(true);
        m.setMetaInteger("color", -256); // Yellow
        m.setMetaString("iconType", "b-m-p-s-m");

        mapView.getRootGroup().addItem(m);
        marker = m;

        // Pan to marker
        mapView.getMapController().panTo(point, true);
        Log.d(TAG, "Created marker at: " + latitude + ", " + longitude);
    }

    /**
     * Remove marker from map
     */
    private void removeMarker() {
        if (marker != null) {
            mapView.post(new Runnable() {
                @Override
                public void run() {
                    mapView.getRootGroup().removeItem(marker);
                    marker = null;
                    Log.d(TAG, "Removed marker");
                }
            });
        }
    }

    /**
     * Update status display
     */
    private void updateStatusDisplay(final String status, final int color) {
        mapView.post(new Runnable() {
            @Override
            public void run() {
                if (statusText != null) {
                    statusText.setText("Status: " + status);
                    statusText.setTextColor(color);
                }
            }
        });
    }

    /**
     * Update location display
     */
    private void updateLocationDisplay(final double latitude, final double longitude) {
        mapView.post(new Runnable() {
            @Override
            public void run() {
                if (locationText != null) {
                    String coordText = String.format("Location: %.6f, %.6f", latitude, longitude);
                    locationText.setText(coordText);
                }
            }
        });
    }

    /**
     * Update velocity display
     */
    private void updateVelocityDisplay() {
        mapView.post(new Runnable() {
            @Override
            public void run() {
                if (velocityText != null && locationSimulator != null) {
                    double velocityMps = locationSimulator.getCurrentVelocityMps();
                    double velocityKmh = velocityMps * 3.6; // Convert m/s to km/h
                    String velText = String.format("Velocity: %.2f m/s (%.2f km/h)", velocityMps, velocityKmh);
                    velocityText.setText(velText);
                }
            }
        });
    }

    public void onDestroyView() {
        Log.d(TAG, "Destroying view");
        stopTracking();
        removeMarker();

        if (locationSimulator != null) {
            locationSimulator.dispose();
            locationSimulator = null;
        }

        root = null;
    }
}
