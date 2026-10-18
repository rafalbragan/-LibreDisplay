# RCA - znikniecie LibreCare z fizycznego Samsunga (2026-10-18)

## Zakres incydentu
- Urzadzenie: Samsung SM-S948B (Android user 0)
- Pakiet: `com.libredisplay`
- Objaw potwierdzony ADB:
  - `pm list packages --user 0 com.libredisplay` -> brak
  - `pm list packages -u --user 0 com.libredisplay` -> brak
  - `pm path --user 0 com.libredisplay` -> brak
  - `dumpsys package com.libredisplay` -> `Unable to find package`

## Co zostalo zweryfikowane
- `connectedDebugAndroidTest` bylo uruchamiane na fizycznym Samsungu:
  - `app/build/outputs/androidTest-results/connected/debug/TEST-SM-S948B - 17-_app-.xml`
  - wpis: `<property name="device" value="SM-S948B - 17" />`
- W repo istnialy zadania potencjalnie destrukcyjne dla fizycznego urzadzenia:
  - `build.gradle.kts` -> `installAndRun` (zaleznosc od `:app:installDebug`)
  - `build.gradle.kts` -> `removeApp` (wywoluje `adb uninstall com.libredisplay`)
- Brak globalnego guarda blokujacego operacje `install/uninstall/connectedAndroidTest` na fizycznych urzadzeniach.

## Root cause (RCA)
1. **Root cause procesu:** brak twardych zabezpieczen w Gradle przed uruchomieniem zadan device-mutating na fizycznym telefonie.
2. **Root cause operacyjny:** poleganie na recznej ostroznosci zamiast mechanicznego gate'a. To umozliwialo wykonanie zadan instrumentacyjnych i instalacyjnych na Samsungu.
3. **Co NIE zostalo dowiedzione:** brak twardego dowodu, ze samo `connectedDebugAndroidTest` automatycznie wykonalo `uninstall` aplikacji produkcyjnej. W logach nie ma bezposredniej komendy `adb uninstall com.libredisplay` dla incydentu.
4. **Najbardziej prawdopodobny wektor:** uruchomienie operacji mutujacych urzadzenie (`installDebug` / zadania pokrewne / zadanie `removeApp`) bez ochrony device-target.

## Dzialania naprawcze wdrozone w repo
- Dodano fail-closed gate dla zadan na urzadzeniach fizycznych:
  - plik: `build.gradle.kts`
  - nowa logika:
    - wykrywanie urzadzen przez `adb devices -l`
    - blokada dla taskow: `connected*AndroidTest`, `install*`, `uninstall*`, `installAndRun`, `removeApp`
    - domyslnie blokuje fizyczne urzadzenia, dopuszcza tylko emulator
    - override tylko jawnie: `-PallowPhysicalDeviceOps=true` lub `ALLOW_PHYSICAL_DEVICE_OPS=true`
- Dodano task walidacyjny bez modyfikacji telefonu:
  - `verifySafeDeviceGate`

## Dowody walidacji guarda
- `./gradlew verifySafeDeviceGate` -> FAIL na fizycznym Samsungu (oczekiwane):
  - `Blocked :verifySafeDeviceGate: physical device(s) detected [R5GL1397F1M]...`
- `./gradlew verifySafeDeviceGate -PallowPhysicalDeviceOps=true` -> PASS (oczekiwane, jawna autoryzacja).
- `./gradlew :app:testDebugUnitTest --tests com.libredisplay.data.demo.DemoDataPolicyTest` -> PASS (brak regresji dla testow unit).

## Sprawdzenie pozostalych danych (read-only, bez odzyskiwania inwazyjnego)
- Na urzadzeniu: brak widocznego katalogu `libredisplay` w `/sdcard/Android/data` (read-only check).
- Lokalnie w repo znaleziono kopie eksportu:
  - `C:\Users\SG0216827\IdeaProjects\LibreDisplay\docs\LibreCare-live-backup-1787331718332.json`
  - rozmiar: `179947 B`

## Status PR/CI
- PR: `#44` (`release/test-apk-issue29-base-20261017`)
- Commit: `b41dbbfb58078ff931541c2446ecf056d4a0c7e7`
- Status checks (GitHub Actions): `SUCCESS` dla Android CI / APK build / debug build (stan zweryfikowany podczas audytu).

## Rekomendowana procedura bezpiecznej instalacji APK 2.16.4
1. Domyslnie trzymaj fizyczny telefon poza pipeline testow instrumentacyjnych.
2. Testy instrumentacyjne uruchamiaj na emulatorze lub dedykowanym test device.
3. Przed jakimkolwiek taskiem device-mutating uruchom:
   - `./gradlew verifySafeDeviceGate`
4. Nie uzywaj override `-PallowPhysicalDeviceOps=true` bez jawnej autoryzacji PO.
5. Instalacje na telefon PO wykonuj tylko recznie i swiadomie (poza automatycznymi taskami), z potwierdzeniem package/version/signature.

