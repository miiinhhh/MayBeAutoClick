# Auto Clicker (AutoClick2) 🤖🖱️

An advanced, lightweight Android Auto Clicker application built with Java, leveraging Android's **Accessibility Service** and **Floating Window Overlay** APIs. It allows users to automate taps across any app or game without requiring root access.

---

## ✨ Key Features

- **Root-Free Operation**: Uses Android's native Accessibility Service `dispatchGesture` API to simulate real touch inputs safely and reliably.
- **Floating Control Panel**: Sleek, draggable overlay control bar providing instant access to:
  - ➕ **Add Point**: Add new numbered click points (`1, 2, 3...`).
  - ➖ **Remove Point**: Remove the last click point.
  - ⚙ **Settings**: Configure click timing and modes directly from the overlay.
  - ▶ / ⏹ **Start / Stop**: Control clicking instantly over any app or game.
- **Multi-Point Management**:
  - Create and manage multiple click points.
  - Drag and position each point independently anywhere on the screen.
  - Circular target design with a red center dot for high-precision targeting.
- **Advanced Customization (Settings)**:
  - **Delay Between Clicks**: Configurable in milliseconds (ms) or seconds (s).
  - **Click Duration (Hold Time)**: Adjust press/hold duration.
  - **Click Modes**:
    - *Multi Point*: Sequential execution (`A → B → C → A → B → C...`).
    - *Single Point*: Continuous clicks on Point 1.
    - *Random*: Randomly selects points from the configured list.
  - **Repeat / Loop Count**: Choose between 1 time, 10 times, 100 times, or Infinite (`∞`).
- **Clean Dashboard UI**: Modern dark-themed dashboard displaying real-time Accessibility status, active click points count, delay, and repeat settings.

---

## 📱 System Requirements

- **Android Version**: Android 7.0 (API 24) or higher.
- **Permissions Required**:
  - **Accessibility Service**: Required to simulate touch gestures on the screen.
  - **Display over other apps (SYSTEM_ALERT_WINDOW)**: Required to show floating click points and the control panel overlay.

---

## 🚀 How to Use

1. **Enable Accessibility**:
   - Open the app and tap **Bật Accessibility**.
   - Find **Auto Clicker** in your device's Accessibility settings and turn it **ON**.
2. **Add Click Points**:
   - Return to the app and tap **Add Point** (or use the floating panel `+` button).
   - Drag the numbered circles (`1, 2, 3...`) to your target locations on the screen.
3. **Configure Settings**:
   - Tap **⚙ Settings** to customize delay, click duration, click mode, and loop count.
4. **Start Auto Clicking**:
   - Tap **▶ START** (either in the app or on the floating control panel) to begin automated clicking!

---

## 🛠️ Tech Stack

- **Language**: Java
- **UI Framework**: Android Views (XML & Programmatic Views), Material Design
- **Core APIs**: `AccessibilityService` (`dispatchGesture`), `WindowManager` (Floating Overlays)

---

## 📄 License

This project is open-source and available for personal and educational use.
