package com.atakmap.android.helloworld.plugin;

import android.os.Handler;
import android.os.Looper;
import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Location simulator (non-service implementation)
 * Supports multiple modes: static, jitter, route (circle), and square
 */
public class LocationSimulator {

    private static final String TAG = "LocationSimulator";

    // Location listener interface
    private LocationUpdateListener locationListener;

    // Handler for periodic updates
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Simulation mode
    public enum Mode {
        STATIC,
        JITTER,
        ROUTE,  // Circle route
        SQUARE
    }

    private Mode mode = Mode.SQUARE;

    // Base/current coordinate
    private Coordinate current = new Coordinate(17.3850, 78.4867);

    // Jitter configuration (meters)
    private double jitterMeters = 10;

    // Route configuration
    private List<Waypoint> route = new ArrayList<>();
    private int routeIndex = 0;
    private double routeSpeedMetersPerSec = 100; // 100 m/s
    private boolean isRouteMoving = false;
    private long lastRouteTick = 0;
    private double routeFraction = 0; // Position between waypoints (0-1)

    // Broadcast cadence (Hz)
    private int streamHz = 1; // 1 update per second
    private boolean isStreaming = false;

    // Random generator
    private final Random random = new Random();

    // Constants
    private static final double CENTER_LAT = 17.3850;
    private static final double CENTER_LON = 78.4867;
    private static final double CIRCLE_RADIUS_METERS = 80467.2; // 50 miles
    private static final int CIRCLE_POINTS = 72;
    private static final double SQUARE_SIDE_METERS = 160934.4; // 100 miles
    private static final int SQUARE_POINTS_PER_SIDE = 25;

    /**
     * Coordinate class
     */
    public static class Coordinate {
        public double latitude;
        public double longitude;

        public Coordinate(double lat, double lon) {
            this.latitude = lat;
            this.longitude = lon;
        }
    }

    /**
     * Waypoint class (includes hold time)
     */
    public static class Waypoint extends Coordinate {
        public double holdSeconds;

        public Waypoint(double lat, double lon, double holdSeconds) {
            super(lat, lon);
            this.holdSeconds = holdSeconds;
        }
    }

    /**
     * Listener interface for location updates
     */
    public interface LocationUpdateListener {
        void onLocationUpdate(double latitude, double longitude);
    }

    public LocationSimulator() {
        Log.d(TAG, "LocationSimulator created");
        initializeRoute();
    }

    // ==================== Public API ====================

    /**
     * Set the location update listener
     */
    public void setLocationUpdateListener(LocationUpdateListener listener) {
        this.locationListener = listener;
    }

    /**
     * Set simulation mode
     */
    public void setMode(Mode mode) {
        this.mode = mode;
        stopRoute();
        initializeRoute();
        Log.d(TAG, "Mode set to: " + mode);
    }

    /**
     * Get current mode
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Start streaming location updates
     */
    public void startStreaming() {
        if (isStreaming) return;
        isStreaming = true;

        if (mode == Mode.ROUTE || mode == Mode.SQUARE) {
            startRoute();
        }

        startBroadcast();
        Log.d(TAG, "Started streaming in mode: " + mode);
    }

    /**
     * Stop streaming location updates
     */
    public void stopStreaming() {
        isStreaming = false;
        stopRoute();
        handler.removeCallbacksAndMessages(null);
        Log.d(TAG, "Stopped streaming");
    }

    /**
     * Set stream frequency (Hz)
     */
    public void setStreamHz(int hz) {
        this.streamHz = Math.max(1, Math.min(hz, 10)); // Clamp between 1-10 Hz
    }

    /**
     * Cleanup resources
     */
    public void dispose() {
        stopStreaming();
        locationListener = null;
        Log.d(TAG, "Disposed");
    }

    // ==================== Route Initialization ====================

    /**
     * Initialize route based on current mode
     */
    private void initializeRoute() {
        if (mode == Mode.SQUARE) {
            route = generateSquareRoute(CENTER_LAT, CENTER_LON, SQUARE_SIDE_METERS,
                                       SQUARE_POINTS_PER_SIDE, 0, true);
        } else if (mode == Mode.ROUTE) {
            route = generateCircleRoute(CENTER_LAT, CENTER_LON, CIRCLE_RADIUS_METERS,
                                       CIRCLE_POINTS, 0, true);
        }
    }

    // ==================== Coordinate Helpers ====================

    /**
     * Convert meters to latitude degrees
     */
    private double metersToLat(double meters) {
        return meters / 111320.0;
    }

    /**
     * Convert meters to longitude degrees
     */
    private double metersToLon(double meters, double lat) {
        return meters / (111320.0 * Math.cos(Math.toRadians(lat)));
    }

    /**
     * Apply jitter to coordinate
     */
    private Coordinate applyJitter(Coordinate base, double radiusMeters) {
        double angle = random.nextDouble() * 2 * Math.PI;
        double r = Math.sqrt(random.nextDouble()) * radiusMeters;
        double dy = r * Math.cos(angle);
        double dx = r * Math.sin(angle);

        return new Coordinate(
            base.latitude + metersToLat(dy),
            base.longitude + metersToLon(dx, base.latitude)
        );
    }

    /**
     * Calculate haversine distance between two coordinates (meters)
     */
    private double haversineDistance(Coordinate a, Coordinate b) {
        final double R = 6371000; // Earth radius in meters
        double dLat = Math.toRadians(b.latitude - a.latitude);
        double dLon = Math.toRadians(b.longitude - a.longitude);
        double lat1 = Math.toRadians(a.latitude);
        double lat2 = Math.toRadians(b.latitude);

        double s = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);

        return 2 * R * Math.atan2(Math.sqrt(s), Math.sqrt(1 - s));
    }

    /**
     * Linear interpolation between two coordinates
     */
    private Coordinate lerpCoord(Coordinate a, Coordinate b, double t) {
        return new Coordinate(
            a.latitude + (b.latitude - a.latitude) * t,
            a.longitude + (b.longitude - a.longitude) * t
        );
    }

    // ==================== Route Generators ====================

    /**
     * Generate circle route using great-circle calculations
     */
    private List<Waypoint> generateCircleRoute(double centerLat, double centerLon,
                                               double radiusMeters, int points,
                                               double holdSeconds, boolean clockwise) {
        List<Waypoint> result = new ArrayList<>();
        final double R = 6371000; // Earth radius in meters

        double φ1 = Math.toRadians(centerLat);
        double λ1 = Math.toRadians(centerLon);
        double δ = radiusMeters / R;

        for (int i = 0; i < points; i++) {
            double frac = (double) i / points;
            double θ = 2 * Math.PI * frac;
            if (!clockwise) θ = 2 * Math.PI - θ;

            double sinφ2 = Math.sin(φ1) * Math.cos(δ) +
                          Math.cos(φ1) * Math.sin(δ) * Math.cos(θ);
            double φ2 = Math.asin(sinφ2);
            double y = Math.sin(θ) * Math.sin(δ) * Math.cos(φ1);
            double x = Math.cos(δ) - Math.sin(φ1) * sinφ2;
            double λ2 = λ1 + Math.atan2(y, x);

            result.add(new Waypoint(
                Math.toDegrees(φ2),
                Math.toDegrees(λ2),
                holdSeconds
            ));
        }

        // Close the loop
        if (!result.isEmpty()) {
            Waypoint first = result.get(0);
            result.add(new Waypoint(first.latitude, first.longitude, holdSeconds));
        }

        Log.d(TAG, "Generated circle route with " + result.size() + " waypoints");
        return result;
    }

    /**
     * Generate square route
     */
    private List<Waypoint> generateSquareRoute(double centerLat, double centerLon,
                                               double sideMeters, int pointsPerSide,
                                               double holdSeconds, boolean clockwise) {
        List<Waypoint> result = new ArrayList<>();
        double halfSide = sideMeters / 2;

        // Calculate four corners
        double topLat = centerLat + metersToLat(halfSide);
        double bottomLat = centerLat - metersToLat(halfSide);
        double rightLon = centerLon + metersToLon(halfSide, centerLat);
        double leftLon = centerLon - metersToLon(halfSide, centerLat);

        Coordinate[] corners = clockwise ? new Coordinate[] {
            new Coordinate(topLat, leftLon),      // Top-left
            new Coordinate(topLat, rightLon),     // Top-right
            new Coordinate(bottomLat, rightLon),  // Bottom-right
            new Coordinate(bottomLat, leftLon)    // Bottom-left
        } : new Coordinate[] {
            new Coordinate(topLat, leftLon),      // Top-left
            new Coordinate(bottomLat, leftLon),   // Bottom-left
            new Coordinate(bottomLat, rightLon),  // Bottom-right
            new Coordinate(topLat, rightLon)      // Top-right
        };

        // Generate points along each side
        for (int side = 0; side < 4; side++) {
            Coordinate start = corners[side];
            Coordinate end = corners[(side + 1) % 4];

            for (int i = 0; i < pointsPerSide; i++) {
                double t = (double) i / pointsPerSide;
                double lat = start.latitude + (end.latitude - start.latitude) * t;
                double lon = start.longitude + (end.longitude - start.longitude) * t;
                result.add(new Waypoint(lat, lon, holdSeconds));
            }
        }

        // Close the loop
        if (!result.isEmpty()) {
            Waypoint first = result.get(0);
            result.add(new Waypoint(first.latitude, first.longitude, holdSeconds));
        }

        Log.d(TAG, "Generated square route with " + result.size() + " waypoints");
        return result;
    }

    // ==================== Route Movement ====================

    /**
     * Start route movement
     */
    private void startRoute() {
        if (isRouteMoving || route.size() < 2) return;
        isRouteMoving = true;
        routeIndex = 0;
        routeFraction = 0;
        lastRouteTick = System.currentTimeMillis();
        current = new Coordinate(route.get(0).latitude, route.get(0).longitude);
        Log.d(TAG, "Route started with " + route.size() + " waypoints");
        tickRoute();
    }

    /**
     * Stop route movement
     */
    private void stopRoute() {
        isRouteMoving = false;
    }

    /**
     * Route movement tick
     */
    private void tickRoute() {
        if (!isRouteMoving || (mode != Mode.ROUTE && mode != Mode.SQUARE) || route.size() < 2) {
            return;
        }

        long now = System.currentTimeMillis();
        double elapsed = (now - lastRouteTick) / 1000.0;
        lastRouteTick = now;

        Waypoint a = route.get(routeIndex % route.size());
        Waypoint b = route.get((routeIndex + 1) % route.size());
        double distance = haversineDistance(a, b);

        if (distance == 0) {
            // Move to next waypoint immediately
            routeIndex = (routeIndex + 1) % route.size();
            current = new Coordinate(a.latitude, a.longitude);
            handler.postDelayed(this::tickRoute, (long)(a.holdSeconds * 1000));
            return;
        }

        // Calculate movement step
        double step = (routeSpeedMetersPerSec * elapsed) / distance;
        routeFraction += step;

        if (routeFraction >= 1.0) {
            // Reached next waypoint
            routeIndex = (routeIndex + 1) % route.size();
            Waypoint wp = route.get(routeIndex);
            current = new Coordinate(wp.latitude, wp.longitude);
            routeFraction = 0;
            handler.postDelayed(this::tickRoute, (long)(wp.holdSeconds * 1000));
        } else {
            // Interpolate position
            Coordinate p = lerpCoord(a, b, routeFraction);
            current = new Coordinate(p.latitude, p.longitude);
            handler.postDelayed(this::tickRoute, 200); // Smooth movement, tick every 200ms
        }
    }

    // ==================== Broadcasting ====================

    /**
     * Start periodic broadcasts
     */
    private void startBroadcast() {
        handler.removeCallbacksAndMessages(null);
        broadcastLocation();
    }

    /**
     * Broadcast current location
     */
    private void broadcastLocation() {
        if (!isStreaming) return;

        Coordinate coord;

        switch (mode) {
            case STATIC:
                coord = current;
                break;
            case JITTER:
                coord = applyJitter(current, jitterMeters);
                break;
            case ROUTE:
            case SQUARE:
                coord = current;
                break;
            default:
                coord = current;
        }

        // Notify listener
        if (locationListener != null) {
            locationListener.onLocationUpdate(coord.latitude, coord.longitude);
        }

        // Schedule next broadcast
        long delayMs = 1000 / streamHz;
        handler.postDelayed(this::broadcastLocation, delayMs);
    }
}
