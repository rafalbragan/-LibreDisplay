# LibreDisplay

A fullscreen Android tablet application that **permanently displays the latest glucose reading** from LibreLinkUp, designed for caregivers monitoring an elderly diabetic parent.

---

## Features

| Feature | Details |
|---|---|
| 📊 Glucose dashboard | Huge number, trend arrow, timestamp, data age |
| 🎨 Colour-coded background | 🔴 Low · 🟢 In-range · 🟠 High · ⬛ Stale |
| ⚠ Stale data warning | Banner appears when data is older than 20 minutes |
| 🔁 Auto-refresh | Configurable interval (1–30 minutes) |
| ↩ Manual refresh | One-tap button on the dashboard |
| ⚙ Settings screen | Email, password, region (EU/US/DE/FR), interval |
| 🔒 Encrypted storage | Android Keystore + EncryptedSharedPreferences |
| 📱 Keeps screen on | `FLAG_KEEP_SCREEN_ON` always active |
| 🚀 Auto-start after reboot | Optional `BootReceiver` |
| 🔇 Foreground service | Monitoring continues when activity is backgrounded |
| 🧪 Mock data provider | Works without a real account (random 60–250 mg/dL) |

---

## Project Structure

```
LibreDisplay/
├── .github/
│   └── workflows/
│       └── android-build.yml          # CI: builds debug + release APK
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/libredisplay/
│       │   ├── LibreDisplayApp.kt     # Application class
│       │   ├── MainActivity.kt        # Single activity host
│       │   ├── data/
│       │   │   ├── api/
│       │   │   │   ├── LibreLinkUpClient.kt      # Interface
│       │   │   │   ├── MockLibreLinkUpClient.kt  # Fake data (default)
│       │   │   │   └── RealLibreLinkUpClient.kt  # TODO stubs for real API
│       │   │   ├── model/
│       │   │   │   ├── AppSettings.kt
│       │   │   │   ├── GlucoseReading.kt
│       │   │   │   └── GlucoseTrend.kt
│       │   │   ├── repository/
│       │   │   │   ├── GlucoseRepository.kt
│       │   │   │   └── SettingsRepository.kt
│       │   │   └── storage/
│       │   │       └── SecureStorage.kt
│       │   ├── receiver/
│       │   │   └── BootReceiver.kt
│       │   ├── service/
│       │   │   └── MonitoringService.kt
│       │   └── ui/
│       │       ├── monitoring/
│       │       │   ├── MonitoringScreen.kt
│       │       │   ├── MonitoringUiState.kt
│       │       │   └── MonitoringViewModel.kt
│       │       ├── settings/
│       │       │   ├── SettingsScreen.kt
│       │       │   └── SettingsViewModel.kt
│       │       └── theme/
│       │           └── Theme.kt
│       └── res/values/
│           ├── strings.xml
│           └── themes.xml
├── gradle/
│   ├── libs.versions.toml             # Version catalog
│   └── wrapper/
│       └── gradle-wrapper.properties
├── app/build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Architecture

```
UI Layer          ViewModel Layer         Data Layer
─────────         ───────────────         ──────────
MonitoringScreen ←→ MonitoringViewModel ←→ GlucoseRepository ←→ LibreLinkUpClient
SettingsScreen   ←→ SettingsViewModel   ←→ SettingsRepository ←→ SecureStorage
```

- **MVVM** with `StateFlow` for reactive UI.
- **Sealed class** `MonitoringUiState` (Loading / Success / Error).
- All sensitive data (passwords, tokens) stored encrypted, never logged.

---

## Build Instructions

### Prerequisites

- IntelliJ IDEA (with Android plugin enabled)
- JDK 17
- Android SDK 35 + Platform Tools + Emulator (or a physical tablet)

### Clone & open

```bash
git clone https://github.com/YOUR_USERNAME/LibreDisplay.git
cd LibreDisplay
```

Open in IntelliJ IDEA: **File → Open → select the `LibreDisplay` folder**.

### Build from command line

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (unsigned)
./gradlew assembleRelease
```

Output files:
- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release-unsigned.apk`

---

## GitHub Actions

The workflow at `.github/workflows/android-build.yml` automatically:

1. Runs on every push to `main` / `master`, pull requests, or manual trigger.
2. Builds both **debug** and **release (unsigned)** APKs.
3. Optionally signs release APK if signing secrets are configured.
4. Uploads artifacts retained for 14 days.

### Artifacts

- `LibreDisplay-debug`
- `LibreDisplay-release-unsigned`
- `LibreDisplay-release-signed` (only when signing secrets are present)

### Optional release signing via secrets

In GitHub: **Repository → Settings → Secrets and variables → Actions → New repository secret**.

Create these secrets:

- `RELEASE_KEYSTORE_BASE64` – Base64 content of your `.jks` file
- `RELEASE_KEYSTORE_PASSWORD` – Keystore password
- `RELEASE_KEY_ALIAS` – Key alias inside keystore
- `RELEASE_KEY_PASSWORD` – Key password

Create Base64 on Windows PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\to\release-keystore.jks"))
```

To download artifacts:
1. Go to your repository on GitHub.
2. Click **Actions → latest workflow run → Artifacts**.
3. Download the needed APK artifact.

---

## Installing the APK on a Tablet

1. On the tablet: **Settings → Security → Unknown sources** (or *Install unknown apps*) → enable for your file manager.
2. Transfer the APK via USB, email, or Google Drive.
3. Open the APK file and tap **Install**.
4. Launch **LibreDisplay** from the app drawer.
5. Open **Settings** (⚙ icon) and enter your LibreLinkUp credentials.

### Recommended tablet settings

| Setting | Value |
|---|---|
| Screen timeout | Never |
| Stay awake (Developer options) | ON |
| Auto-rotate | ON |
| Display brightness | High |

---

## Replacing Mock Data with the Real LibreLinkUp Provider

By default the app uses `MockLibreLinkUpClient` which generates random values – **no real account is needed**.

To enable real data:

1. Open the **Settings** screen in the app.
2. Enter your **LibreLinkUp email** and **password**.
3. Select the correct **region** (EU / US / DE / FR).
4. Toggle **Use Mock Data → OFF**.
5. Tap **Save Settings**.

> **Note:** `RealLibreLinkUpClient` currently contains `TODO` stubs only.
> A developer must implement the three methods following the inline documentation
> in `RealLibreLinkUpClient.kt`.

### What to implement

Open `app/src/main/java/com/libredisplay/data/api/RealLibreLinkUpClient.kt`.

The class-level KDoc contains:
- All API endpoint URLs.
- Required HTTP headers.
- Request / response JSON structure.
- Step-by-step implementation guide.

No third-party LibreLinkUp SDK is used – the endpoints are standard REST calls
over HTTPS.  Use the already-present Retrofit + OkHttp dependencies.

---

## Kiosk Mode

Enable **Kiosk Mode** in Settings to request Android lock-task mode.

- On managed devices (MDM / Device Owner), the app can be pinned automatically.
- On unmanaged devices, Android may show the normal screen-pinning confirmation.
- If lock-task is unavailable on a device, the app continues to run normally (no crash).

---

## Security Notes

- Passwords are stored using **EncryptedSharedPreferences** (AES-256-GCM / AES-SIV) backed by the **Android Keystore**.
- Passwords and auth tokens are **never written to logcat** at any log level.
- OkHttp logging is limited to **HEADERS** in debug and **NONE** in release.
- The app does not use Health Connect or Google Fit.

---

## License

MIT – see `LICENSE` file.

