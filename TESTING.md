# TESTING.md

Ten dokument opisuje testowanie aplikacji `LibreDisplay` na Windows + IntelliJ IDEA Ultimate **bez Android Studio**.

## 1) Wymagania wstepne

- IntelliJ IDEA Ultimate
- JDK 17 (widoczne w `PATH`)
- Android SDK (platform-tools + build-tools + platforma Android 35)
- Telefon Samsung z Androidem
- Kabel USB danych

## 2) Sprawdzenie konfiguracji projektu

Projekt jest skonfigurowany pod:

- Android Gradle Plugin: `8.5.2`
- Kotlin: `2.0.21`
- compileSdk: `35`
- targetSdk: `35`
- minSdk: `26`
- Java/Kotlin target: `17`

Kluczowe pliki:

- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`

## 3) Komendy build/install (Gradle Wrapper)

Uruchamiaj z katalogu glownego projektu:

```powershell
.\gradlew.bat clean
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
.\gradlew.bat bundleRelease
```

Co robi kazda komenda:

- `clean` - usuwa poprzednie artefakty builda.
- `assembleDebug` - buduje debug APK.
- `installDebug` - instaluje debug APK na podlaczonym urzadzeniu (`adb`).
- `bundleRelease` - buduje release Android App Bundle (`.aab`).

## 4) Taski diagnostyczne i urzadzeniowe

Dodane taski:

```powershell
.\gradlew.bat checkEnvironment
.\gradlew.bat showDevices
.\gradlew.bat showLogs
.\gradlew.bat installAndRun
.\gradlew.bat removeApp
```

### `checkEnvironment`
Pokazuje:

- wersje Java,
- wersje Gradle,
- lokalizacje Android SDK,
- compileSdk/targetSdk/minSdk,
- czy `adb` jest dostepne,
- czy telefon jest podlaczony (status `device`).

### `showDevices`
Wykonuje `adb devices` i pokazuje liste urzadzen.

### `showLogs`
Uruchamia logcat z filtrem:

- `LibreDisplay`
- `AndroidRuntime`
- `System.err`

### `installAndRun`
Automatycznie:

1. buduje i instaluje debug APK,
2. uruchamia aplikacje poleceniem:

```powershell
adb shell am start -n com.libredisplay/.MainActivity
```

### `removeApp`
Odinstalowuje aplikacje:

```powershell
adb uninstall com.libredisplay
```

## 5) Test na Samsung Galaxy (kroki)

### Krok 1 - wlacz Opcje programisty
Ustawienia -> Informacje o telefonie -> Informacje o oprogramowaniu -> kliknij 7x "Numer kompilacji".

### Krok 2 - wlacz Debugowanie USB
Ustawienia -> Opcje programisty -> Debugowanie USB.

### Krok 3 - podlacz telefon
Podlacz kablem USB i ustaw tryb przesylania danych.

### Krok 4 - zaakceptuj odcisk RSA
Na telefonie zaakceptuj komunikat o kluczu RSA komputera.

### Krok 5 - sprawdz urzadzenie

```powershell
adb devices
```

Urzadzenie powinno miec status:

- `device` (poprawnie)

Nie powinno miec statusu:

- `unauthorized`

## 6) Tryb testowy (mock data)

Jesli logowanie do LibreLinkUp nie dziala:

1. Otworz ekran `Ustawienia`.
2. Wlacz `Uzywaj danych testowych`.
3. Zapisz ustawienia.

W trybie testowym:

- aplikacja uruchamia sie na mock danych,
- odswiezanie dziala co 15 sekund,
- widzet aktualizuje sie mock odczytami.

## 7) Diagnostyka HTTP

W aplikacji obsluzone sa komunikaty po polsku.

- HTTP 401:
  - "Nieprawidlowe dane logowania lub problem z kontem LibreLinkUp."

- HTTP 430:
  - "Serwer odrzucil kolejne proby logowania. Odczekaj kilka minut i sprobuj ponownie."

Dodatkowo dla ograniczania prob polaczen:

- "Zbyt wiele prob polaczenia. Sprobuj pozniej lub sprawdz dane logowania."

## 8) Gdzie sa artefakty builda

Po `assembleDebug`:

- `app/build/outputs/apk/debug/app-debug.apk`

Po `bundleRelease`:

- `app/build/outputs/bundle/release/app-release.aab`

## 9) Reczna instalacja APK na Samsungu

1. Zbuduj APK:

```powershell
.\gradlew.bat assembleDebug
```

2. Zainstaluj przez `adb`:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

3. Uruchom aplikacje:

```powershell
adb shell am start -n com.libredisplay/.MainActivity
```

Alternatywnie:

- skopiuj `app-debug.apk` na telefon,
- otworz plik APK i zainstaluj recznie.

