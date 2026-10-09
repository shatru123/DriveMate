# DriveMate Release Engineering Guide

This guide describes how to build, sign, tag, and publish official releases of **DriveMate** for GitHub Releases and F-Droid.

---

## 1. Versioning Protocol

DriveMate follows **Semantic Versioning** (`MAJOR.MINOR.PATCH`) mapped to monotonically increasing integer `versionCode` values:

| Release | `versionName` | `versionCode` | Formula / Logic |
| :--- | :---: | :---: | :--- |
| V10.5.0 | `"10.5.0"` | `10500` | `MAJOR * 1000 + MINOR * 100 + PATCH` |
| V10.5.1 | `"10.5.1"` | `10501` | Bugfix release |
| V10.6.0 | `"10.6.0"` | `10600` | Feature update |
| V11.0.0 | `"11.0.0"` | `11000` | Next major version |

Update these values in [`app/build.gradle.kts`](../app/build.gradle.kts) under `defaultConfig`:
```kotlin
versionCode = 10500
versionName = "10.5.0"
```

---

## 2. Release Steps

### Step 1: Update Fastlane Changelog
Create or update the changelog corresponding to the new `versionCode`:
```bash
echo "• Release description..." > fastlane/metadata/android/en-US/changelogs/10500.txt
```

### Step 2: Run Verification Checks
Ensure all tests and lint audits pass cleanly:
```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
```

### Step 3: Build the Release APK
```bash
./gradlew assembleRelease
```
The unsigned APK will be produced at:
`app/build/outputs/apk/release/app-release-unsigned.apk`

### Step 4: Compute SHA-256 Checksum
```bash
shasum -a 256 app/build/outputs/apk/release/app-release-unsigned.apk
```

---

## 3. Automated GitHub Release Pipeline

The GitHub Actions workflow in [`.github/workflows/release.yml`](../.github/workflows/release.yml) automatically triggers whenever a version tag matching `v*` is pushed.

To publish a release:
```bash
# Commit your changes
git add .
git commit -m "chore: release v10.5.0"
git push origin main

# Tag and push the tag
git tag -a v10.5.0 -m "DriveMate v10.5.0 release"
git push origin v10.5.0
```

The workflow will:
1. Compile the release APK.
2. Sign the APK using GitHub Repository Secrets (`RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`) if present.
3. Compute the SHA-256 checksum file (`.sha256`).
4. Publish a GitHub Release attaching the APK and checksum file.

---

## 4. Signing Setup for Production

To configure APK signing on GitHub Actions:

1. Base64-encode your Android release keystore:
   ```bash
   base64 -i my-release-key.jks | pbcopy
   ```
2. Navigate to your GitHub repository -> **Settings** -> **Secrets and variables** -> **Actions**.
3. Add the following repository secrets:
   - `RELEASE_KEYSTORE_BASE64`: The base64-encoded keystore contents.
   - `RELEASE_KEYSTORE_PASSWORD`: Keystore password.
   - `RELEASE_KEY_ALIAS`: Key alias.
   - `RELEASE_KEY_PASSWORD`: Key password.

---

## 5. F-Droid Synchronization

Once the tag `v10.5.0` is pushed:
1. Update [`metadata/com.shatrughna.drivemate.yml`](../metadata/com.shatrughna.drivemate.yml) if necessary.
2. Submit or update the merge request in `fdroiddata`.
3. F-Droid's build server picks up the tag, compiles the APK in its clean container environment, signs it with F-Droid's key, and publishes it to the repository.
