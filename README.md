# Wear OS Thermometer (`wearos-termometer`)

A native Wear OS application designed specifically for smartwatches with integrated thermal sensors (Samsung Galaxy Watch 5, 6, 7, Ultra, and compatible devices).

---

## 🎯 What We Built & Core Features

The app features **two distinct body temperature measurement modes**:

### 1. On-Wrist Mode (Skin Contact)
- Reads continuously from the integrated dermal heat sensor (`Samsung Skin Temp Sensor` / `android.sensor.skin_temp`).
- Monitors direct skin heat dissipation and passive resting body temperature.
- Automatically pauses when the watch is taken off (`Off-body detector`).

### 2. Forehead Distance Mode (Non-Contact IR Scan 1–3 cm)
- Inspired by digital infrared medical thermometers.
- On-screen guide instructs user to point the rear optical thermal sensor directly at the center of the forehead at a distance of 1 to 3 cm.
- Thermal compensation algorithm calculates core body temperature based on optical sensor readings and ambient temperature.

---

## 🎨 Wear OS AMOLED Interface

- **Circular AMOLED UI**: Pure black `#000000` background for maximum battery savings and outdoor readability.
- **Color-Coded Circular Gauge**:
  - 🔵 **LOW** (< 35.5 °C)
  - 🟢 **NORMAL** (35.5 °C – 37.3 °C)
  - 🟡 **LOW FEVER** (37.4 °C – 37.8 °C)
  - 🔴 **HIGH FEVER** (> 37.8 °C)
- **Haptic Alerts**: Subtle tactile vibrations upon mode switch, during measurement scan, and on final reading completion.
- **Dedicated Watch Icon**: Custom vector mercury thermometer icon with gradient accent.

---

## 🚀 How to Build & Install

### 1. Build via Gradle
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### 2. Install via ADB Wi-Fi
```bash
adb connect <WATCH_IP>:<PORT>
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📜 Credits & Architecture

- Native Wear OS project built with Android Sensor Framework and Samsung Health Privileged Sensor SDK.
- Developed by **Gustavo Lara** ([@gusdslara](https://github.com/gusdslara)).
