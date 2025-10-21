# Bluetooth Connect Android

A simple **Android Bluetooth Classic chat app** that allows devices to connect via Bluetooth and exchange messages.  
Supports both **server** and **client** roles for peer-to-peer communication.

---

## Features

- Scan nearby Bluetooth devices.  
- Pair & connect with selected devices.  
- Act as a **server** to accept incoming connections.  
- Act as a **client** to connect to a server device.  
- Send and receive text messages in real-time.  
- Shows connection status and pairing progress.  
- Compatible with Android 6.0+ (requires runtime permissions).

---

## Screenshots

*(Optional: Add screenshots here)*

1. Device scanning & selection.  
2. Connection status view.  
3. Chat interface with server/client buttons.

---

## Getting Started

### Prerequisites

- Android Studio 2020.3+  
- Android device with Bluetooth support  
- Minimum SDK: 23 (Android 6.0)  
- Target SDK: 33 (Android 13)

### Permissions

Required for Android 12+:

```xml
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
