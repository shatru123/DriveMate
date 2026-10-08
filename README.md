# DriveMate 🚗

> **Your Personal Driving Companion** with truthful, profile-aware vehicle telemetry

[![Android](https://img.shields.io/badge/Platform-Android_8.0+-3DDC84.svg?style=flat&logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Android Auto](https://img.shields.io/badge/Android%20for%20Cars-1.7.0-0F9D58.svg?style=flat&logo=androidauto)](https://developer.android.com/training/cars)
[![Tests](https://img.shields.io/badge/Tests-run%20locally-lightgrey.svg)]()

---

## 📖 Overview

**DriveMate** is a connected, profile-aware automotive companion. It recognizes Android Auto projection separately from vehicle telemetry, uses only values supplied by Car Hardware/OBD2/GPS or explicitly entered by the user, and keeps Android Auto passive until a legitimate driving-session trigger occurs.

Vehicle identity, registration, odometer, service targets, and greetings are populated from the user's profile; fresh installs show unavailable states instead of sample vehicle data.

Designed with an automotive-inspired Material 3 interface, clean architecture, and complete adherence to official Google Android for Cars safety guidelines, DriveMate lays the foundation for a future autonomous AI driving assistant.

---

## ✨ Features (V1)

1. **Personalized Welcome Greeting**
   - **Time-Aware Salutations**:
     - Uses the configured driver and vehicle profile when available; otherwise says "Driver" and "vehicle".
   - **Flexible Greeting Styles**:
     - **Short**: Quick and punchy.
     - **Normal**: Balanced, time-aware greeting.
     - **Detailed**: Adds configured vehicle details and verified reminders.
     - **Custom**: User-defined templates with dynamic placeholder support (`{name}`, `{brand}`, `{model}`, `{variant}`, `{timeOfDay}`).

2. **Verified Android Auto & Car Connection Awareness**
   - Integrates with the official `androidx.car.app.connection.CarConnection` API to detect verified Android Auto projection (`CONNECTION_TYPE_PROJECTION`) and Automotive OS (`CONNECTION_TYPE_NATIVE`).
   - Explicitly distinguishes genuine Android Auto projection sessions from Bluetooth-only pairings (`BLUETOOTH_ONLY`). Car Bluetooth alone never triggers an automatic driving welcome greeting.
   - Built-in **Simulated Connection** toggle for instant testing without needing physical vehicle access.

3. **Intelligent Session Management & Guaranteed Deduplication**
   - `DrivingSessionManager` state machine generates cryptographically unique `sessionId` values per drive and strictly prevents duplicate greetings during a drive.
   - Idempotent against screen rotations, configuration changes, activity restarts, and duplicate connection callbacks.
   - **Greeting marked played only upon verified TTS playback completion**: If audio focus is denied, or TTS encounters an error mid-speech, the greeting is NOT falsely recorded as played and retries safely.
   - Automatic session cancellation and resource cleanup upon genuine vehicle disconnection.

4. **Automotive Audio Engine (Per-Utterance Tracking & Audio Ducking)**
   - Coroutine-driven `GreetingTtsManager` with thread-safe `ConcurrentHashMap` per-utterance tracking (`utteranceId`).
   - Ignores stale or cancelled utterance callbacks.
   - Strict audio focus validation (`AUDIOFOCUS_REQUEST_GRANTED` with `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE` and `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`): ducking active music/radio smoothly, delivering the greeting, and releasing audio focus cleanly.
   - 15-second utterance timeout guards against hung system TTS engines.
   - Configurable speech rate (0.5x - 2.0x), voice pitch (0.5x - 2.0x), and voice/language selection.
   - Instant phone-side audio preview with animated visualizer and stop controls.

5. **Profile-Aware Automotive Dashboard**
   - Obsidian/cyan automotive theme (`#00E5FF` electric cyan accents with amber warnings).
   - Live connection state badge with pulse indicator.
   - Truthful drive statistics with explicit empty states (`No trips yet`, `Unavailable`).
   - Quick settings access.

6. **Persistent Configuration (DataStore)**
   - Driver name, vehicle details, greeting style, custom templates, and voice parameters persisted via Jetpack DataStore Preferences.
   - 100% on-device storage with zero tracking or network dependencies.

---

## 🛡️ Critical Android Auto Requirements & Platform Limitations

To ensure stability, maintainability, and driver safety, DriveMate is engineered strictly within official Google Android for Cars guidelines without unsupported workarounds.

### What the Platform Officially Supports
* **Connection Detection**: `androidx.car.app:app` exposes `CarConnection(context).type`, allowing phone-side apps to observe whether Android Auto projection is active.
* **Audio Routing**: When the phone is connected via Android Auto or vehicle Bluetooth, standard Android audio output routes through the vehicle speakers via the car's sound system.
* **Audio Ducking**: DriveMate requests transient focus only for user-requested voice/TTS and abandons it on completion, error, cancellation, or disconnect.

### Official Google Platform Limitations
* **Allowed App Categories**: Google strictly limits third-party Android Auto head-unit applications to approved categories: *Navigation*, *Media*, *Messaging*, *Point of Interest (POI)*, *Internet of Things (IoT)*, and *Weather*. Standalone car greeting apps cannot draw arbitrary UI screens on the car head unit without implementing an approved category template (`CarAppService`).
* **Background Audio Restrictions**: Starting in Android 14 and Android 15, background applications cannot spontaneously claim audio focus or play sound if the app process is terminated or cached.
* **Why DriveMate Avoids Hacks**: DriveMate **does not** use accessibility hacks, notification exploits, hidden private APIs, or root access. Instead, DriveMate provides:
  1. Responsive **Phone-Side Docking / Foreground Mode** when docked in the car.
  2. An optional **Foreground Driving Session Service** (`DriveMateSessionService`) with a persistent generic vehicle notification.
  3. Seamless **Connection Simulator & Preview Mode** for testing anywhere.

---

## 🏗️ Architecture

DriveMate follows Clean Architecture and MVVM with unidirectional data flow (UDF):

```text
       ┌─────────────────────────────────────────────────────────────┐
       │               Official Connection Inputs                    │
       │  • androidx.car.app.connection.CarConnection (Android Auto) │
              │  • Bluetooth ACL Receiver (supplementary audio metadata)     │
       └──────────────────────────────┬──────────────────────────────┘
                                      │
                                      ▼
                      ┌───────────────────────────────┐
                      │     CarConnectionManager      │
                      │ (StateFlow<CarConnectionState>)│
                      └───────────────┬───────────────┘
                                      │
                                      ▼
                      ┌───────────────────────────────┐
                      │     DrivingSessionManager     │
                      │  • Deduplication Engine       │
                      │  • Disconnect / Reconnect     │
                      └───────────────┬───────────────┘
                                      │
                                      ▼
                      ┌───────────────────────────────┐
                      │      GreetingController       │
                      └───────┬───────────────┬───────┘
                              │               │
                              ▼               ▼
              ┌──────────────────────┐ ┌──────────────────────┐
              │  GreetingGenerator   │ │  GreetingTtsManager  │
              │  • Time of Day       │ │  • Text-to-Speech    │
              │  • Styles & Tokens   │ │  • Audio Focus Duck  │
              └──────────────────────┘ └──────────────────────┘
                              │               │
                              └───────┬───────┘
                                      │
                                      ▼
                      ┌───────────────────────────────┐
                      │    DriveMate UI / Dashboard   │
                      │   (Jetpack Compose M3 Theme)  │
                      └───────────────────────────────┘
```

### Module Layout
```text
app/src/main/java/com/shatrughna/drivemate/
├── DriveMateApplication.kt         # Dependency graph & initialization
├── MainActivity.kt                 # Compose root activity & navigation
├── car/
│   ├── CarConnectionManager.kt     # Official CarConnection observer & simulator
│   ├── CarConnectionState.kt       # State models (Connected, Disconnected, Types)
│   └── BluetoothConnectionReceiver.kt # Vehicle Bluetooth ACL broadcast receiver
├── driving/
│   ├── DrivingSessionManager.kt    # Deduplication and session lifecycle state machine
│   └── DriveMateSessionService.kt  # Foreground companion service with notification
├── greeting/
│   ├── GreetingGenerator.kt        # Pure Kotlin time-of-day & template generator
│   ├── GreetingController.kt       # Orchestrator between session, generator, & TTS
│   └── GreetingTtsManager.kt       # TTS lifecycle, audio focus ducking, voice control
├── data/
│   ├── model/
│   │   ├── DriveMateSettings.kt    # Immutable settings model
│   │   └── GreetingStyle.kt        # Short, Normal, Detailed, Custom styles
│   └── preferences/
│       └── DriveMatePreferencesRepository.kt # Jetpack DataStore implementation
├── ui/
│   ├── home/
│   │   ├── DashboardScreen.kt      # Automotive dashboard UI
│   │   └── MainViewModel.kt        # Dashboard state & actions
│   ├── settings/
│   │   ├── SettingsScreen.kt       # Driver, vehicle, greeting & voice preferences
│   │   └── SettingsViewModel.kt    # Settings business logic & template validation
│   ├── components/
│   │   └── DashboardComponents.kt  # Automotive cards, badges, and preview controls
│   └── theme/
│       ├── Color.kt                # Obsidian/cyan automotive palette
│       ├── Theme.kt                # Automotive dark theme
│       └── Type.kt                 # Typography hierarchy
└── util/
    └── AppLogger.kt                # Sanitized, tagged logging utility
```

---

## 🛠️ Tech Stack

* **Language**: Kotlin 2.0.21
* **UI**: Jetpack Compose (BOM 2024.10.00), Material 3
* **Android for Cars**: `androidx.car.app:app:1.7.0` (Official `CarConnection`)
* **Concurrency**: Kotlin Coroutines & `StateFlow` / `SharedFlow`
* **Persistence**: Jetpack DataStore Preferences 1.1.1
* **Audio Engine**: Android `TextToSpeech` + `AudioManager` / `AudioFocusRequest`
* **Build System**: Gradle 8.10.2 + Android Gradle Plugin 8.7.0
* **Testing**: JUnit 4, Kotlinx Coroutines Test, Android Test Core

---

## 🚀 Setup & Build Instructions

### Prerequisites
* **Android Studio Ladybug (2024.2.1)** or newer (recommended)
* **JDK 21**
* **Android SDK**: API 35 (Android 15) installed

### Build & Run
```bash
# Clone the repository
git clone https://github.com/shatru123/DriveMate.git
cd DriveMate

# Run all unit tests
./gradlew test

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

The compiled APK will be located at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🧪 Testing

### 1. Automated Unit Tests
The test suite covers greeting generation, passive Android Auto sessions, truthful telemetry defaults/fallbacks, GPS quality, analytics empty states, and audio ownership:
```bash
./gradlew test
```
* **`GreetingGeneratorTest`**:
  * Validates time-of-day calculations across 24 hours (Morning, Afternoon, Evening, Night).
  * Validates Short, Normal, and Detailed styles.
  * Validates Custom templates and dynamic placeholder replacements (`{name}`, `{brand}`, `{model}`, `{variant}`, `{timeOfDay}`).
  * Validates template syntax error handling and unknown token detection.
* **`AndroidAutoReliabilityTest`**: Android Auto connection is passive; only an explicit/verified motion trigger starts a drive, and reconnect remains passive.
* **`VehicleTelemetryTest`**: Fresh-state honesty, GPS speed labeling/clearing, odometer separation, and TPMS unavailability.
* **`AudioFocusLifecycleTest`** and **`AudioInputCoordinatorTest`**: single-owner microphone/TTS transitions and release on cancellation/error.
* **`DrivingSessionManagerTest`**:
  * Validates connection lifecycle: `CONNECT -> Passive`, followed by an explicit session trigger.
  * Validates deduplication: Subsequent `CONNECT` events (activity recreate, rotation, duplicate callbacks) **never** replay greetings.
  * Validates disconnect: `DISCONNECT` cleans up active session.
  * Validates reconnection: Subsequent `CONNECT` triggers greeting for the new drive.
* **`CarConnectionStateTest`**:
  * Validates connection type models, settings defaults, and enum parsing.
* **`WeatherRepositoryTest` & `TripTrackerTest`**:
  * Validates Open-Meteo response parsing, WMO weather codes, and trip tracker accumulators.

### 2. Manual & Desktop Head Unit (DHU) Testing
* **In-App Simulator**: Use the **"Simulate Car Connection"** switch on the dashboard to test the complete connection -> greeting -> deduplication cycle on an emulator or phone without physical car hardware.
* **Desktop Head Unit (DHU)**:
  1. Enable Android Auto developer options on your phone: Settings > Android Auto > tap Version 10 times > enable "Start head unit server".
  2. Forward the port from your computer:
     ```bash
     adb forward tcp:5277 tcp:5277
     ```
  3. Start the DHU binary:
     ```bash
     desktop-head-unit
     ```
  4. Plug in your phone; DriveMate will detect `CONNECTION_TYPE_PROJECTION` without automatically opening the microphone or interrupting media.

## 🗺️ Roadmap

### Version 10.3 (Multilingual Voice Assistant & Natural Speech Engine) - Released ✅
- [x] **Multilingual Spoken Intent Matching**: Zero-latency deterministic parser supporting English, Hindi (हिंदी), Marathi (मराठी), and mixed code-switching (Hinglish/Maranglish).
- [x] **Truthful Telemetry Answers**: Accurate, zero-hallucination vehicle telemetry queries (live speed, GPS speed, odometer, fuel level, driving range, vehicle connection status) with strict honesty contracts (LIVE vs GPS vs STALE vs UNAVAILABLE).
- [x] **Driver Audio Lifecycle & Coordinated Arbitration**: Android Auto audio ducking, single-owner microphone locking during TTS playback, and seamless speech recognizer intent routing across English, Hindi, and Marathi locales.
- [x] **Expanded Natural Automotive Domain**: Instant answers for fuel expenses, today's cumulative driving distance, weather forecasts, parking recall, maintenance countdowns, music streaming, and navigation.
- [x] **Language Settings & Suggestion Chips**: In-app and Android Auto voice assistant language preference (`Auto`, `English`, `हिंदी`, `मराठी`), with dynamic multilingual suggestion chips in the Voice Assistant Sheet.

### Version 3.5 (Autonomous AI Companion)
- [ ] **Multi-Model LLM Integration**: On-device / cloud LLM integration (Google Gemini / Claude / OpenAI).
- [ ] **Proactive Driving Intelligence**: Real-time traffic anomaly predictions and intelligent conversational debriefs.

---

## 👨‍💻 Creator & Lead Developer

<div align="center">
  <img src="shatrughna.jpg" width="140" style="border-radius: 50%; border: 3px solid #00E5FF;" alt="Shatrughna Ambhore" />
  <h3>Shatrughna Ambhore</h3>
  <p><b>Creator & Lead Developer of DriveMate</b></p>
  <p>
    📧 <a href="mailto:ambhoreshatrughna@gmail.com">ambhoreshatrughna@gmail.com</a> &nbsp;|&nbsp;
    📱 <a href="tel:+919604466334">+91 9604466334</a>
  </p>
  <p><i>Crafted for truthful, driver-safe vehicle companionship</i></p>
</div>

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
