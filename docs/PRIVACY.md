# DriveMate Privacy Policy

**Last Updated:** October 2026

**DriveMate** is created by **Shatrughna Ambhore** and engineered with an absolute commitment to user privacy, data sovereignty, and driver safety.

---

## 1. Core Principles

- **Zero Tracking:** DriveMate contains no analytics SDKs, advertising networks, crash reporting telemetry, or third-party tracking scripts.
- **100% On-Device Storage:** All user profiles, vehicle telemetry logs, expenses, documents, and settings are stored locally on your device.
- **No Account Harvesting:** The multi-user system operates entirely offline using Android's sandboxed local storage (`DataStore` and local SQLite/Room). No server credentials or cloud databases are required.

---

## 2. Information We Process and How It Is Handled

### A. Audio & Microphone Access (`RECORD_AUDIO`)
- **Usage:** Used solely when you actively open and speak into the Voice Assistant.
- **Processing:** Spoken audio is analyzed on-device using Android's built-in `SpeechRecognizer` service.
- **Retention:** Audio is never recorded in the background, never archived to disk, and never transmitted to our servers or any third-party AI provider.

### B. Location Data (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`)
- **Usage:** 
  1. Fine location is used on-device to compute real-time GPS speed, trip distance, and to save your vehicle's parking coordinates.
  2. Coarse location (latitude/longitude) is used to retrieve local weather forecasts.
- **Retention:** Location coordinates stay on your device. Parking coordinates remain until you clear or overwrite them.

### C. Photos & Documents (`READ_MEDIA_IMAGES`)
- **Usage:** Allows you to attach vehicle photos, profile pictures, and vehicular compliance documents (RC, Insurance, PUC, Driving License).
- **Retention:** Images are stored in your device's private application storage and are never uploaded to any remote server.

### D. Weather Data Network Request
- **Endpoint:** `https://api.open-meteo.com`
- **Purpose:** To provide accurate driving condition weather summaries.
- **Details:** The Open-Meteo service requires no API key, no account, and does not collect personally identifiable information.

---

## 3. Third-Party Services & Libraries

DriveMate uses only 100% Free and Open-Source Software (FLOSS) libraries from Google AndroidX and JetBrains Kotlin. We do not integrate:
- Google Play Services Tracking / AdMob
- Firebase Analytics / Crashlytics
- Facebook SDK or Social Login
- Any third-party telematics or fleet surveillance trackers

---

## 4. Data Deletion & Export

Since all your data resides in your device's private app storage:
- You can clear individual documents, expenses, or vehicle profiles directly within the app.
- You can delete all data at any time by clearing DriveMate's storage in Android Settings or uninstalling the application.

---

## 5. Contact & Questions

If you have any questions or feedback regarding this privacy policy or DriveMate's open-source architecture, please contact:

**Shatrughna Ambhore**  
Creator & Lead Developer  
Email: [ambhoreshatrughna@gmail.com](mailto:ambhoreshatrughna@gmail.com)  
Repository: [https://github.com/shatru123/DriveMate](https://github.com/shatru123/DriveMate)
