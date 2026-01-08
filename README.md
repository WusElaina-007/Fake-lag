# NetConditionerVPN

A Kotlin/Jetpack Compose sample that demonstrates a **userspace, no-root** VPN network conditioner. It provides a neon glassmorphism UI, DataStore-backed config presets, and an always-on floating button overlay to toggle VPN state and switch profiles on the fly.

> ⚠️ **Note on VPN userspace limitations**
> This project focuses on the conditioning pipeline (delay/jitter/loss/throttle) and VPN scaffolding. A full TCP/UDP forwarding implementation in userspace requires additional socket handling and protocol parsing. The sample intentionally keeps the forwarding logic minimal to stay readable and safe for Android Studio builds.

## Build & Run

1. Open the repository in Android Studio (Giraffe or newer).
2. Sync Gradle.
3. Run on a device with Android 8.0+ (API 26+).
4. Grant VPN permission when prompted.
5. Grant **overlay permission** to enable the floating button.

## Features

- Jetpack Compose + Material 3 UI with dark neon glassmorphism styling.
- Real-time network conditioning sliders: base latency, jitter, packet loss, upload/download throttling.
- Saved configs via DataStore JSON.
- Floating overlay button with drag, tap-to-toggle VPN, and long-press radial menu for instant config switching.

## Important Notes

- This app is designed for **testing your own device network conditions**. It does not target any specific game or app.
- Userspace VPN performance depends heavily on device CPU and Android version.
- For production-grade traffic forwarding, consider integrating a robust packet tunnel implementation.
