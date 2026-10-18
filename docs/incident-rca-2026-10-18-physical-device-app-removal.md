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

## Potwierdzone fakty
1. Brakowalo twardego, pre-execution gate'a blokujacego zadania device-mutating na fizycznych urzadzeniach.
2. `connectedDebugAndroidTest` bylo wykonywane na fizycznym SM-S948B (dowod: raport connected tests).
3. W repo istnialy zadania mutujace urzadzenie (`install*`, `uninstall*`, `installAndRun`, `removeApp`) bez wymuszenia jawnego targetu po serialu.
4. Brak bezposredniego dowodu komendy `adb uninstall com.libredisplay` przypisanej do momentu znikniecia aplikacji.

## Hipotezy (niepotwierdzone)
1. Najbardziej prawdopodobny wektor to wykonanie tasku mutujacego urzadzenie bez bezpiecznego wyboru targetu.
2. Rownolegle procesy IDE/CLI mogly wskazac fizyczne urzadzenie jako domyslne przy braku serial-targetingu.

## Niewyjasnione zdarzenia
1. Dokladna pojedyncza komenda, ktora usunela aplikacje z user 0.
2. Czy efekt byl wynikiem jednego tasku czy sekwencji taskow/processow w krotkim oknie czasu.

## Dzialania naprawcze wdrozone w repo
- Wzmocniono fail-closed gate (`build.gradle.kts`) i przeniesiono walidacje do etapu przed wykonaniem taskow (`gradle.taskGraph.whenReady`), nie tylko `doFirst`.
- Dodano twarde zasady targetowania po serialu:
  - przy wielu urzadzeniach wymagany jest jawny target: `ANDROID_SERIAL` lub `-PdeviceSerial=...`
  - fizyczne urzadzenia wymagaja jednoczesnie: `allowPhysicalDeviceOps=true` + allowlista seriali (`safePhysicalDeviceSerials` / `SAFE_PHYSICAL_DEVICE_SERIALS`)
  - serial Product Ownera `R5GL1397F1M` jest stale chroniony i blokowany jako target.
- Zaktualizowano `installAndRun` i `removeApp`, aby zawsze przekazywaly `adb -s <serial>`.
- Dodano task walidacyjny bez modyfikacji telefonu: `verifySafeDeviceGate`.
- Dodano dokument autoryzacji urzadzen: `docs/device-safety-authorization.md`.

## Dowody walidacji guarda
- `./gradlew verifySafeDeviceGate` -> FAIL na fizycznym Samsungu (oczekiwane):
  - `Blocked :verifySafeDeviceGate: physical device(s) detected [R5GL1397F1M]...`
- `./gradlew verifySafeDeviceGate -PallowPhysicalDeviceOps=true` -> PASS (oczekiwane, jawna autoryzacja).
- `./gradlew :app:testDebugUnitTest --tests com.libredisplay.data.demo.DemoDataPolicyTest` -> PASS (brak regresji dla testow unit).

## Sprawdzenie pozostalych danych (read-only, bez odzyskiwania inwazyjnego)
- Zweryfikowano i zabezpieczono lokalny backup (copy + SHA-256 match) bez publikacji payloadu medycznego.
- Raport audytu metadanych: `docs/backup-audit-2026-10-18.md`.

## Status PR/CI
- PR: `#44` (`release/test-apk-issue29-base-20261017`)
- Commit: `4a575bc9919caf3cae40934a33ff03aaeaed8030`
- Status checks (GitHub Actions): `SUCCESS` dla Android CI / APK build / debug build / build.

## Rekomendowana procedura bezpiecznej instalacji APK 2.16.4
1. Domyslnie trzymaj fizyczny telefon poza pipeline testow instrumentacyjnych.
2. Testy instrumentacyjne uruchamiaj na emulatorze lub dedykowanym test device.
3. Przed jakimkolwiek taskiem device-mutating uruchom:
   - `./gradlew verifySafeDeviceGate`
4. Nie uzywaj override `-PallowPhysicalDeviceOps=true` bez jawnej autoryzacji PO.
5. Instalacje na telefon PO wykonuj tylko recznie i swiadomie (poza automatycznymi taskami), z potwierdzeniem package/version/signature.

