# DriveMate 🚗

> **Your Personal Driving Companion** for the **Tata Nexon Creative+ S**

[![Android](https://img.shields.io/badge/Platform-Android_8.0+-3DDC84.svg?style=flat&logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Android Auto](https://img.shields.io/badge/Android%20for%20Cars-1.7.0-0F9D58.svg?style=flat&logo=androidauto)](https://developer.android.com/training/cars)
[![Tests](https://img.shields.io/badge/Unit%20Tests-100%25%20Passing-brightgreen.svg)]()

---

## 📖 Overview

**DriveMate** transforms your everyday commute into a connected, personalized automotive experience. Built specifically for the **Tata Nexon Creative+ S** driven by **Shatrughna**, DriveMate recognizes when your smartphone connects to the vehicle / Android Auto and greets you with an intelligent, time-aware audio welcome:

> *"Hey Shatrughna, welcome to your Tata Nexon. Have a safe and pleasant drive."*

Designed with an automotive-inspired Material 3 interface, clean architecture, and complete adherence to official Google Android for Cars safety guidelines, DriveMate lays the foundation for a future autonomous AI driving assistant.

---

## ✨ Features (V1)

1. **Personalized Welcome Greeting**
   - **Time-Aware Salutations**:
     - **Morning (05:00 - 11:59)**: *"Good morning, Shatrughna. Welcome to your Tata Nexon. Have a safe drive."*
     - **Afternoon (12:00 - 16:59)**: *"Good afternoon, Shatrughna. Welcome back to your Tata Nexon. Have a pleasant journey."*
     - **Evening (17:00 - 21:59)**: *"Good evening, Shatrughna. Welcome back to your Nexon. Drive safely."*
     - **Night (22:00 - 04:59)**: *"Good evening, Shatrughna. Welcome to your Tata Nexon. Please drive safely."*
   - **Flexible Greeting Styles**:
     - **Short**: Quick and punchy (*"Hey Shatrughna, welcome to your Nexon."*).
     - **Normal**: Balanced, time-aware greeting.
     - **Detailed**: Comprehensive variant greeting (*"Good evening, Shatrughna. Welcome back to your Tata Nexon Creative+ S. Your journey is ready. Have a safe and pleasant drive."*).
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

5. **Tailored Tata Nexon Dashboard**
   - Obsidian and Nexon-cyan automotive theme (`#00E5FF` electric cyan accents with amber warnings).
   - Live connection state badge with pulse indicator.
   - Today's drive statistics placeholder (`Trips`, `Distance`, `Duration`).
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
* **Audio Ducking**: By requesting `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK` under `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE`, the car's infotainment radio/media ducks momentarily while the greeting plays.

### Official Google Platform Limitations
* **Allowed App Categories**: Google strictly limits third-party Android Auto head-unit applications to approved categories: *Navigation*, *Media*, *Messaging*, *Point of Interest (POI)*, *Internet of Things (IoT)*, and *Weather*. Standalone car greeting apps cannot draw arbitrary UI screens on the car head unit without implementing an approved category template (`CarAppService`).
* **Background Audio Restrictions**: Starting in Android 14 and Android 15, background applications cannot spontaneously claim audio focus or play sound if the app process is terminated or cached.
* **Why DriveMate Avoids Hacks**: DriveMate **does not** use accessibility hacks, notification exploits, hidden private APIs, or root access. Instead, DriveMate provides:
  1. Responsive **Phone-Side Docking / Foreground Mode** when docked in the car.
  2. An optional **Foreground Driving Session Service** (`DriveMateSessionService`) with a persistent notification (`DriveMate • Tata Nexon Companion`).
  3. Seamless **Connection Simulator & Preview Mode** for testing anywhere.

---

## 🏗️ Architecture

DriveMate follows Clean Architecture and MVVM with unidirectional data flow (UDF):

```text
       ┌─────────────────────────────────────────────────────────────┐
       │               Official Connection Inputs                    │
       │  • androidx.car.app.connection.CarConnection (Android Auto) │
       │  • Bluetooth ACL Receiver (Tata Nexon Infotainment)         │
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
│       ├── Color.kt                # Tata Nexon obsidian/cyan palette
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
The test suite validates greeting generation, session deduplication, and state modeling:
```bash
./gradlew test
```
* **`GreetingGeneratorTest`**:
  * Validates time-of-day calculations across 24 hours (Morning, Afternoon, Evening, Night).
  * Validates Short, Normal, and Detailed styles.
  * Validates Custom templates and dynamic placeholder replacements (`{name}`, `{brand}`, `{model}`, `{variant}`, `{timeOfDay}`).
  * Validates template syntax error handling and unknown token detection.
* **`AndroidAutoReliabilityTest`** (13 Comprehensive Reliability Scenarios):
  * **Scenario 1**: Bluetooth-only connection updates state but does NOT trigger driving sessions or speech.
  * **Scenario 2**: Android Auto projection connecting while Bluetooth is paired triggers session and welcome greeting.
  * **Scenario 3**: Rapid duplicate connection events are strictly deduplicated (plays exactly once).
  * **Scenario 4**: Vehicle disconnect during greeting preparation cancels the greeting cleanly.
  * **Scenario 5**: Vehicle disconnect mid-speech immediately stops TTS and does NOT mark the greeting played.
  * **Scenario 6**: TTS initialization failures trigger bounded retries (max 2 retries) and do NOT mark played.
  * **Scenario 7**: TTS playback errors (onError) do NOT mark greeting played.
  * **Scenario 8**: TTS playback completion (onDone) marks greeting played for the current session ID.
  * **Scenario 9**: Audio focus denial aborts speech and does NOT mark played.
  * **Scenario 10**: Stale utterance or session IDs cannot mark active greetings played.
  * **Scenario 11**: Rapid disconnect-reconnect generates distinct session IDs and triggers fresh greetings.
  * **Scenario 12**: Weather API timeouts (>2s) or network errors fallback to basic greetings without blocking speech.
  * **Scenario 13**: Single greeting per driving session guaranteed across lifecycle re-evaluations.
* **`DrivingSessionManagerTest`**:
  * Validates connection lifecycle: `CONNECT -> Greeting Triggered`.
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
  4. Plug in your phone; DriveMate will detect `CONNECTION_TYPE_PROJECTION` and deliver the greeting.

---

## 🗺️ Roadmap

### Version 2.0 (Connected Vehicle Enhancements) - Released ✅
- [x] **Live Weather & Environmental Context**: Real-time Open-Meteo weather integration: *"Good morning, Shatrughna. It's 27°C and clear in Pune. Welcome to your Tata Nexon."*
- [x] **Trip & Distance Tracking**: Active driving session timer, distance tracker, and persistent "Today's Drive" metrics.
- [x] **Maintenance & Service Reminders**: Tata Nexon service intervals (15,000 km), fuel level alerts, and care prompts.
- [x] **Smart Destination Awareness**: Time-of-day predictions (Home / Office) with one-tap Android Auto navigation launch.

### Version 3.0 (Autonomous AI Companion)
- [ ] **Multi-Model AI Integration**: On-device / cloud LLM integration (Google Gemini / Claude / OpenAI).
- [ ] **Context-Aware Dialogue**: Proactive updates on route traffic, news briefings, and intelligent conversational assistance.

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
  <p><i>Crafted with passion for the Tata Nexon Creative+ S</i></p>
</div>

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
