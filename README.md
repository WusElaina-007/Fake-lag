# NetConditionerVPN Ultra

A Kotlin/Jetpack Compose sample that demonstrates a **userspace, no-root** VPN network conditioner. It provides a cyber neon glassmorphism UI, DataStore-backed config presets, import/export tooling, an always-on floating radial menu, and a real-time packet graph overlay.

> ⚠️ **Note on VPN userspace limitations**
> This project focuses on the conditioning pipeline (delay/jitter/loss/throttle), telemetry, and VPN scaffolding. A full TCP/UDP forwarding implementation in userspace requires additional socket handling and protocol parsing. The sample intentionally keeps forwarding logic modular and safe for Android Studio builds.

## Build & Run

1. Open the repository in Android Studio (Giraffe or newer).
2. Sync Gradle.
3. Run on a device with Android 8.0+ (API 26+).
4. Grant VPN permission when prompted.
5. Grant **overlay permission** to enable the floating button and graph.

## Features

- Jetpack Compose + Material 3 UI with neon glassmorphism styling and motion.
- Circular sliders for latency/jitter/loss plus upload/download throttling.
- Saved configs via DataStore JSON with schema versioning.
- Import/export `.netcfg` configs via Storage Access Framework.
- Floating overlay button with drag, tap-to-toggle VPN, and long-press radial menu for instant config switching.
- Real-time packet graph overlay showing pps, drops, latency, jitter, and bandwidth.

## Important Notes

- This app is designed for **testing your own device network conditions**. It does not target any specific game or app.
- Userspace VPN performance depends on device CPU, OS version, and background load.
- For production-grade traffic forwarding, implement a full TCP/UDP relay pipeline.
