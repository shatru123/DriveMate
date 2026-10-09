# DriveMate — F-Droid Distribution & Readiness Report

## Status: READY WITH CONDITIONS 🟢

DriveMate has been audited and prepared for inclusion in the official **F-Droid** repository (`fdroiddata`). This document provides full technical compliance details, dependency license audits, permission justifications, and instructions for submitting DriveMate to F-Droid.

---

## 1. Compliance Checklist

| F-Droid Inclusion Policy Criterion | Status | Evidence / Notes |
| :--- | :---: | :--- |
| **100% Free & Open Source License** | ✅ Passed | Licensed under the OSI-approved **MIT License** ([`LICENSE`](../LICENSE)). |
| **All Source Code Publicly Available** | ✅ Passed | Hosted publicly at `https://github.com/shatru123/DriveMate`. |
| **No Proprietary Binaries / Prebuilts** | ✅ Passed | No `.aar`, `.jar`, `.so`, or proprietary blobs bundled in Git. |
| **Zero Google Play Services SDKs** | ✅ Passed | No `play-services-*` or GMS dependencies. |
| **Zero Firebase SDKs** | ✅ Passed | Completely offline-first; no Firebase Crashlytics, Analytics, or Auth. |
| **Zero Advertising / Tracking SDKs** | ✅ Passed | No telemetry libraries, analytics SDKs, or ad networks. |
| **Standard Build Toolchain** | ✅ Passed | Standard Android Gradle Plugin (`com.android.application`), Kotlin 2.0.21, JDK 21. |
| **Reproducible Build Recipe** | ✅ Passed | Packaged F-Droid YAML recipe in [`metadata/com.shatrughna.drivemate.yml`](../metadata/com.shatrughna.drivemate.yml). |

---

## 2. Dependency License Audit

All application dependencies are strictly Free/Libre and Open-Source Software (FLOSS):

| Dependency | Version | License | Upstream Origin |
| :--- | :---: | :---: | :--- |
| `androidx.core:core-ktx` | 1.15.0 | Apache 2.0 | Google AOSP / AndroidX |
| `androidx.lifecycle:*` | 2.8.7 | Apache 2.0 | Google AOSP / AndroidX |
| `androidx.activity:activity-compose` | 1.9.3 | Apache 2.0 | Google AOSP / AndroidX |
| `androidx.compose.*` (BOM 2024.10.00) | 2024.10.00 | Apache 2.0 | Google AOSP / Jetpack Compose |
| `androidx.car.app:app` | 1.7.0 | Apache 2.0 | Google AOSP / AndroidX |
| `androidx.car.app:app-projected` | 1.7.0 | Apache 2.0 | Google AOSP / AndroidX |
| `androidx.datastore:datastore-preferences` | 1.1.1 | Apache 2.0 | Google AOSP / AndroidX |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.8.1 | Apache 2.0 | JetBrains Kotlin |

*Notice*: `androidx.car.app` is an open-source AndroidX library distributed under the Apache 2.0 license. Like VLC Android or AntennaPod, including `androidx.car.app` enables Android Auto head unit interoperability while remaining 100% open source.

---

## 3. Network & Anti-Features Analysis

F-Droid flags specific behaviors with "Anti-Features". Here is DriveMate's anti-feature assessment:

### Anti-Feature Flags:
- **`Ads`**: **NO**. DriveMate contains zero ads.
- **`Tracking`**: **NO**. Zero analytics or user tracking.
- **`NonFreeAdd` / `NonFreeDep`**: **NO**. All dependencies are Apache 2.0.
- **`NonFreeNet`**: **CONDITIONAL FLAG**. 
  - The local weather feature fetches forecasts from `https://api.open-meteo.com`.
  - Open-Meteo is an open-source weather API providing open weather model data under the Creative Commons Attribution 4.0 International (CC BY 4.0) license. It requires no API key, no account, and does not log personal identities.
  - If F-Droid flags this as `NonFreeNet`, it can be documented with the following note in the metadata recipe:
    ```yaml
    AntiFeatures:
      - NonFreeNet: Local weather forecasts query the public Open-Meteo service.
    ```

---

## 4. Permissions Justification

| Permission | Category | Purpose & Justification |
| :--- | :---: | :--- |
| `RECORD_AUDIO` | Dangerous / Runtime | Required only during active driver voice interactions (Voice Assistant tab). Not recorded in background. |
| `ACCESS_FINE_LOCATION` | Dangerous / Runtime | Required for truthful GPS speedometer, trip distance, and parking spot coordinate capture. |
| `ACCESS_COARSE_LOCATION` | Dangerous / Runtime | Required for local weather forecasts via Open-Meteo. |
| `FOREGROUND_SERVICE` | Normal | Allows optional persistent background service during active driving sessions. |
| `POST_NOTIFICATIONS` | Runtime (API 33+) | Displays active driving session status notification. |
| `INTERNET` | Normal | Fetches weather forecasts from `api.open-meteo.com`. |
| `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` | Runtime | Allows selecting local vehicle photos, driver avatars, and vehicle documents. |

---

## 5. F-Droid Submission Recipe

The F-Droid recipe is stored in [`metadata/com.shatrughna.drivemate.yml`](../metadata/com.shatrughna.drivemate.yml):

```yaml
Categories:
  - Navigation
  - System
License: MIT
AuthorName: Shatrughna Ambhore
AuthorEmail: ambhoreshatrughna@gmail.com
SourceCode: https://github.com/shatru123/DriveMate
IssueTracker: https://github.com/shatru123/DriveMate/issues
Changelog: https://github.com/shatru123/DriveMate/releases

AutoName: DriveMate
Summary: Intelligent car companion with multilingual voice assistant & vehicle telemetry
Description: |-
  DriveMate is an intelligent, profile-aware car companion designed for drivers who value truthful vehicle telemetry, driver safety, and seamless voice assistance.

  Key Features:
  * Multilingual Voice Assistant (English, Hindi, Marathi, and natural code-switching)
  * Truthful vehicle telemetry (OBD2/Car Hardware/GPS with strict honesty contracts)
  * Multi-user account isolation with single vehicle profiles
  * Secure on-device automotive document vault (RC, Insurance, PUC, License)
  * Smart parking location recall and categorized expense tracking
  * 100% on-device data storage — zero trackers, zero ads, zero proprietary analytics
  * Adheres strictly to official Android for Cars safety guidelines

RepoType: git
Repo: https://github.com/shatru123/DriveMate.git

Builds:
  - versionName: 10.5.0
    versionCode: 10500
    commit: v10.5.0
    subdir: app
    gradle:
      - yes

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 10.5.0
CurrentVersionCode: 10500
```

---

## 6. How to Submit to F-Droid (Step-by-Step)

When ready to submit to F-Droid:

1. **Fork the F-Droid Data Repository**:
   - Visit [gitlab.com/fdroid/fdroiddata](https://gitlab.com/fdroid/fdroiddata).
   - Click **Fork** to create your own copy.

2. **Add DriveMate Metadata**:
   - In your fork, create the file:
     `metadata/com.shatrughna.drivemate.yml`
   - Copy the contents from [`metadata/com.shatrughna.drivemate.yml`](../metadata/com.shatrughna.drivemate.yml).

3. **Validate with `fdroid` CLI (optional)**:
   ```bash
   fdroid checkmetadata metadata/com.shatrughna.drivemate.yml
   fdroid build -v -l metadata/com.shatrughna.drivemate.yml:10500
   ```

4. **Submit Merge Request**:
   - Open a Merge Request to `fdroid/fdroiddata` on GitLab with title:
     `New App: DriveMate (com.shatrughna.drivemate)`
   - The F-Droid team and automated CI will test-build the app from Git tag `v10.5.0`.
   - Once merged, the F-Droid build server signs and publishes DriveMate to the F-Droid catalog.
