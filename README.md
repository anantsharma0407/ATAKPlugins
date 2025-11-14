# ATAK Location Simulator Plugin

A comprehensive ATAK plugin that demonstrates location simulation, WebSocket communication, and various ATAK SDK features. This plugin serves as both a functional location simulator and a reference implementation for ATAK plugin development.

## Table of Contents
- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Building the Plugin](#building-the-plugin)
- [Installation](#installation)
- [Testing](#testing)
  - [Manual Testing](#manual-testing)
- [Development Notes](#development-notes)
- [Troubleshooting](#troubleshooting)

---

## Overview

This plugin provides:
- **Location Simulation**: Simulates GPS movement along circular and square routes
- **Route Visualization**: Map markers and overlays showing simulated paths
- **Multi-Mode Support**: Circular and square route patterns

## Prerequisites

### Required Software
- **ATAK CIV**: Version 5.5.0 or higher installed on Android device/emulator
- **Android Studio**: Arctic Fox (2020.3.1) or later
- **JDK**: Java 17
- **Gradle**: 8.8+ (wrapper included)
- **Android SDK**: API Level 34 (compileSdk)

---

## Building the Plugin
### 1. Download and unzip the SDK package

### 2. Unzip and create a "plugins" directory within the SDK folder

### 4. Clone or Download the Project into the "plugins" directory

### 5. Set Gradle Plugin Version 8.9.0

### 6. Set Gradle Version 8.13

### 7. Configure Android Studio and set the build variant to civDebug.

### 8. Build the Plugin

#### Using Gradle Command Line
```bash
# Build debug version (CIV flavor)
./gradlew assembleCivDebug
```

#### Using Android Studio
1. Open the project in Android Studio
2. Select build variant: `Build > Select Build Variant`
3. Choose desired flavor (e.g., `civDebug`)
4. Build: `Build > Make Project` or `Ctrl+F9` (Windows/Linux) / `Cmd+F9` (Mac)

---

## Installation

### Method 1: ADB Install
```bash
# Install the plugin APK
adb install -r app/build/outputs/apk/civ/debug/ATAK-Plugin-*.apk

# Verify installation
adb shell pm list packages | grep helloworld
```

### Method 2: Via ATAK Interface
1. Copy the APK to device storage
2. Open ATAK
3. Navigate to: `Settings > Tool Preferences > Manage Plugins`
4. Tap "Load Plugin from SD Card"
5. Select the APK file
6. Restart ATAK when prompted

### Method 3: Android Studio
1. Connect device via USB
2. In Android Studio: `Run > Run 'app'`
3. Select connected device
4. Plugin will install and ATAK will restart

---

## Testing

### Manual Testing

#### 1. Verify Plugin Installation

**Steps:**
1. Open ATAK on your device
2. Tap the overflow menu (⋮) in the top-right corner
3. Look for "Location Simulator" or plugin icon
4. Alternatively, go to `Settings > Tool Preferences > Manage Plugins`
5. Verify the plugin is listed and enabled

**Expected Result:**
- Plugin appears in tools menu
- Plugin status shows "Loaded" in Manage Plugins

---

#### 2. Test Basic Location Simulation

**Steps:**
1. Open ATAK and access the plugin from the overflow menu
2. Verify the Location Simulator dropdown appears
3. Check that status shows "Ready" in green
4. Tap "Start Tracking" button
5. Observe the map for a marker appearing at the starting location
6. Tap "Stop Tracking" button

**Expected Results:**
- Dropdown displays with all UI elements visible
- Status changes to "Running" when started
- Blue marker appears on the map
- Location text shows coordinates (e.g., "37.4219, -122.0840")
- Velocity displays "0 m/s (0 km/h)" initially
- Status returns to "Stopped" when stopped

---

#### 3. Test Circle Route Mode

**Steps:**
1. Open the Location Simulator
2. Tap "Circle Route (50 mi radius)" button
3. Tap "Start Tracking"
4. Wait for 10-15 seconds
5. Observe marker movement on the map
6. Watch velocity display update

**Expected Results:**
- Mode changes to "Circle Route"
- Marker begins moving in a circular pattern
- Velocity shows non-zero values (e.g., "25 m/s (90 km/h)")
- Route forms a circle with ~50-mile radius over time
- Marker smoothly animates between waypoints

**Verification Points:**
- Location coordinates update every 2-3 seconds
- Velocity is calculated correctly between updates
- Marker doesn't jump erratically
- Circle center is approximately at starting position

---

#### 4. Test Square Route Mode

**Steps:**
1. Open the Location Simulator
2. Tap "Square Route (100 mi sides)" button
3. Tap "Start Tracking"
4. Observe marker movement
5. Let it run for 1-2 minutes to trace multiple sides

**Expected Results:**
- Mode changes to "Square Route"
- Marker moves in straight lines forming a square
- Each side is approximately 100 miles
- Sharp 90-degree turns at corners
- Velocity remains consistent during straight segments


---

## Development Notes

### Adding New Features

1. Create Java/Kotlin class in appropriate package
2. Register in `HelloWorldMapComponent.java`
3. Add UI elements to layout XMLs
4. Update `plugin.xml` if adding tools
5. Test with `./gradlew installCivDebug`

### Debugging

**Enable Verbose Logging:**
```java
private static final String TAG = "YourClass";
Log.d(TAG, "Debug message");
```

**View Logs:**
```bash
adb logcat | grep "YourClass"
```

**Debug in Android Studio:**
1. Build and install debug APK
2. Attach debugger: `Run > Attach Debugger to Android Process`
3. Select ATAK process
4. Set breakpoints in plugin code

### Common Issues

**Plugin Not Loading:**
- Check ATAK version compatibility
- Verify signing configuration
- Check `plugin.xml` syntax
- Review logcat for errors: `adb logcat | grep -i error`

**UI Not Displaying:**
- Verify correct context usage
- Check theme compatibility
- Ensure layout resources are included
- Test on different screen sizes

**WebSocket Connection Fails:**
- Verify network connectivity
- Check firewall rules
- Confirm server is running
- Validate WebSocket URL format

---

## Troubleshooting

### Build Errors

**"SDK location not found"**
```bash
# Create local.properties with:
sdk.dir=/path/to/Android/sdk
```

**"ATAK libraries not found"**
- Verify TAK repository credentials in `local.properties`
- Or ensure `atak-gradle-takdev.jar` is in correct location

**Gradle sync fails**
```bash
# Clean and rebuild
./gradlew clean
./gradlew --refresh-dependencies
```

### Runtime Errors

**ClassNotFoundException**
- Check ProGuard rules in `proguard-gradle.txt`
- Add keep rules for reflection-based classes

**OutOfMemoryError**
- Increase Gradle heap size in `gradle.properties`:
  ```properties
  org.gradle.jvmargs=-Xms256m -Xmx4096m
  ```

**MarkerNotFound**
- Verify map group exists before adding markers
- Check lifecycle state before UI operations

### Testing Issues

**Tests Not Running**
- Ensure ATAK is installed: `adb shell pm list packages | grep atak`
- Install plugin before tests: `./gradlew installCivDebug`
- Check device API level compatibility

**Instrumented Tests Fail**
- Verify device is unlocked
- Disable animations: `Settings > Developer Options > Animation off`
- Grant all permissions before tests

---


## License

This plugin follows the same license as ATAK CIV SDK.

## Support

For issues and questions:
1. Check logcat output: `adb logcat | grep helloworld`
2. Review existing tests for examples
3. Consult ATAK plugin documentation
4. Check build configuration in `app/build.gradle`

---
