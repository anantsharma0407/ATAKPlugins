package com.atakmap.android.helloworld.plugin;

import android.os.Handler;
import android.os.Looper;
import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Location simulator for ATAK plugin
 * Simulates GPS movement along circle or square routes
 */
public class LocationSimulator {

    private static final String TAG = "LocationSimulator";

    private LocationUpdateListener locationListener;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public enum Mode {
        ROUTE,
        SQUARE
    }

    private Mode mode = Mode.ROUTE;
    private Coordinate current = new Coordinate(17.3850, 78.4867);

    private List<Waypoint> route = new ArrayList<>();
    private int routeIndex = 0;
    private double routeSpeedMetersPerSec = 100;
    private double currentVelocityMps = 0;
    private boolean isRouteMoving = false;
    private long lastRouteTick = 0;
    private double routeFraction = 0;
    private static final double LAP_TIME_SECONDS = 30.0;

    private int streamHz = 1;
    private boolean isStreaming = false;
    private boolean isBroadcasting = false;

    private static final double CENTER_LAT = 17.3850;
    private static final double CENTER_LON = 78.4867;
    private static final double CIRCLE_RADIUS_METERS = 80467.2;
    private static final int CIRCLE_POINTS = 72;
    private static final double SQUARE_SIDE_METERS = 160934.4;
    private static final int SQUARE_POINTS_PER_SIDE = 25;

    public static class Coordinate {
        public double latitude;
        public double longitude;

        public Coordinate(double lat, double lon) {
            this.latitude = lat;
            this.longitude = lon;
        }
    }

    public static class Waypoint extends Coordinate {
        public double holdSeconds;

        public Waypoint(double lat, double lon, double holdSeconds) {
            super(lat, lon);
            this.holdSeconds = holdSeconds;
        }
    }

    public interface LocationUpdateListener {
        void onLocationUpdate(double latitude, double longitude);
    }

    public LocationSimulator() {
        Log.d(TAG, "LocationSimulator created");
        initializeRoute();
    }

    public void setLocationUpdateListener(LocationUpdateListener listener) {
        this.locationListener = listener;
    }

    public void setMode(Mode mode) {
        boolean wasStreaming = isStreaming;
        this.mode = mode;
        stopRoute();
        initializeRoute();

        if (wasStreaming && (mode == Mode.ROUTE || mode == Mode.SQUARE)) {
            startRoute();
        }

        Log.d(TAG, "Mode set to: " + mode + " (wasStreaming=" + wasStreaming + ")");
    }

    public Mode getMode() {
        return mode;
    }

    public void startStreaming() {
        if (isStreaming) return;
        isStreaming = true;

        Log.d(TAG, "startStreaming called - mode: " + mode + ", route size: " + route.size());

        if (mode == Mode.ROUTE || mode == Mode.SQUARE) {
            startRoute();
        }

        startBroadcast();
        Log.d(TAG, "Started streaming in mode: " + mode);
    }

    public void stopStreaming() {
        isStreaming = false;
        isBroadcasting = false;
        stopRoute();
        handler.removeCallbacksAndMessages(null);
        Log.d(TAG, "Stopped streaming");
    }

    public void setStreamHz(int hz) {
        this.streamHz = Math.max(1, Math.min(hz, 10));
    }

    public double getCurrentVelocityMps() {
        return currentVelocityMps;
    }

    public void dispose() {
        stopStreaming();
        locationListener = null;
        Log.d(TAG, "Disposed");
    }

    private void initializeRoute() {
        route.clear();

        switch (mode) {
            case ROUTE:
                route = generateCircleRoute(CENTER_LAT, CENTER_LON, CIRCLE_RADIUS_METERS,
                                           CIRCLE_POINTS, 0, true);
                if (!route.isEmpty()) {
                    current = new Coordinate(route.get(0).latitude, route.get(0).longitude);
                }
                break;

            case SQUARE:
                route = generateSquareRoute(CENTER_LAT, CENTER_LON, SQUARE_SIDE_METERS,
                                           SQUARE_POINTS_PER_SIDE, 0, true);
                if (!route.isEmpty()) {
                    current = new Coordinate(route.get(0).latitude, route.get(0).longitude);
                }
                break;
        }

        Log.d(TAG, "Initialized route for mode: " + mode);
    }

    private double metersToLat(double meters) {
        return meters / 111320.0;
    }

    private double metersToLon(double meters, double lat) {
        return meters / (111320.0 * Math.cos(Math.toRadians(lat)));
    }

    private double haversineDistance(Coordinate a, Coordinate b) {
        final double R = 6371000;
        double dLat = Math.toRadians(b.latitude - a.latitude);
        double dLon = Math.toRadians(b.longitude - a.longitude);
        double lat1 = Math.toRadians(a.latitude);
        double lat2 = Math.toRadians(b.latitude);

        double s = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dLon/2) * Math.sin(dLon/2);

        return 2 * R * Math.atan2(Math.sqrt(s), Math.sqrt(1 - s));
    }

    private Coordinate lerpCoord(Coordinate a, Coordinate b, double t) {
        return new Coordinate(
            a.latitude + (b.latitude - a.latitude) * t,
            a.longitude + (b.longitude - a.longitude) * t
        );
    }

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

    private List<Waypoint> generateSquareRoute(double centerLat, double centerLon,
                                               double sideMeters, int pointsPerSide,
                                               double holdSeconds, boolean clockwise) {
        List<Waypoint> result = new ArrayList<>();
        double halfSide = sideMeters / 2;

        double topLat = centerLat + metersToLat(halfSide);
        double bottomLat = centerLat - metersToLat(halfSide);
        double rightLon = centerLon + metersToLon(halfSide, centerLat);
        double leftLon = centerLon - metersToLon(halfSide, centerLat);

        Coordinate[] corners = clockwise ? new Coordinate[] {
            new Coordinate(topLat, leftLon),
            new Coordinate(topLat, rightLon),
            new Coordinate(bottomLat, rightLon),
            new Coordinate(bottomLat, leftLon)
        } : new Coordinate[] {
            new Coordinate(topLat, leftLon),
            new Coordinate(bottomLat, leftLon),
            new Coordinate(bottomLat, rightLon),
            new Coordinate(topLat, rightLon)
        };

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

        if (!result.isEmpty()) {
            Waypoint first = result.get(0);
            result.add(new Waypoint(first.latitude, first.longitude, holdSeconds));
        }

        Log.d(TAG, "Generated square route with " + result.size() + " waypoints");
        return result;
    }

    private double calculateTotalRouteDistance() {
        if (route.size() < 2) return 0;

        double totalDistance = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            totalDistance += haversineDistance(route.get(i), route.get(i + 1));
        }
        return totalDistance;
    }

    private void startRoute() {
        if (isRouteMoving || route.size() < 2) return;

        double totalDistance = calculateTotalRouteDistance();
        routeSpeedMetersPerSec = totalDistance / LAP_TIME_SECONDS;
        currentVelocityMps = routeSpeedMetersPerSec;

        isRouteMoving = true;
        routeIndex = 0;
        routeFraction = 0;
        lastRouteTick = System.currentTimeMillis();
        current = new Coordinate(route.get(0).latitude, route.get(0).longitude);

        Log.d(TAG, "Route started with " + route.size() + " waypoints, total distance=" +
              String.format("%.2f", totalDistance) + "m, speed=" +
              String.format("%.2f", routeSpeedMetersPerSec) + "m/s (" +
              String.format("%.2f", routeSpeedMetersPerSec * 3.6) + " km/h)");

        tickRoute();
    }

    private void stopRoute() {
        isRouteMoving = false;
    }

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
            routeIndex = (routeIndex + 1) % route.size();
            current = new Coordinate(a.latitude, a.longitude);
            handler.postDelayed(this::tickRoute, (long)(a.holdSeconds * 1000));
            return;
        }

        double step = (routeSpeedMetersPerSec * elapsed) / distance;
        routeFraction += step;

        if (routeFraction >= 1.0) {
            routeIndex = (routeIndex + 1) % route.size();
            Waypoint wp = route.get(routeIndex);
            current = new Coordinate(wp.latitude, wp.longitude);
            routeFraction = 0;
            handler.postDelayed(this::tickRoute, (long)(wp.holdSeconds * 1000));
        } else {
            Coordinate p = lerpCoord(a, b, routeFraction);
            current = new Coordinate(p.latitude, p.longitude);
            handler.postDelayed(this::tickRoute, 200);
        }
    }

    private void startBroadcast() {
        if (!isBroadcasting) {
            isBroadcasting = true;
            broadcastLocation();
        }
    }

    private void broadcastLocation() {
        if (!isStreaming) {
            isBroadcasting = false;
            return;
        }

        if (locationListener != null) {
            locationListener.onLocationUpdate(current.latitude, current.longitude);
        }

        long delayMs = 1000 / streamHz;
        handler.postDelayed(this::broadcastLocation, delayMs);
    }
}
