# SETUP.md

Konfiguracja projektu `LibreDisplay` na **Windows 11** bez Android Studio.

## 1. Wymagania projektu

Na podstawie konfiguracji projektu:

- **Java:** 17
- **Gradle Wrapper:** 8.9
- **Android Gradle Plugin (AGP):** 8.5.2
- **compileSdk:** 35
- **targetSdk:** 35
- **minSdk:** 26

Wymagane pakiety Android SDK:

- `platform-tools`
- `platforms;android-35`
- `build-tools;35.0.0`

## 2. Sprawdzenie lokalnego środowiska

W katalogu projektu uruchom:

```powershell
java -version
.\gradlew.bat --version
```

Sprawdzenie zmiennych:

```powershell
$env:JAVA_HOME
$env:ANDROID_HOME
$env:ANDROID_SDK_ROOT
```

## 3. Instalacja Android SDK bez Android Studio

### Krok 1: Pobierz Command-line Tools

1. Wejdź na stronę Android Developers i pobierz **Command line tools for Windows**.
2. Rozpakuj do np.:
   `C:\Android\cmdline-tools\latest`

Struktura powinna zawierać:

- `C:\Android\cmdline-tools\latest\bin\sdkmanager.bat`

### Krok 2: Ustaw zmienne środowiskowe (User)

```powershell
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", "C:\Android", "User")
[Environment]::SetEnvironmentVariable("ANDROID_HOME", "C:\Android", "User")

$pathUser = [Environment]::GetEnvironmentVariable("Path", "User")
if ($pathUser -notlike "*%ANDROID_SDK_ROOT%\\platform-tools*") {
    $pathUser = "$pathUser;%ANDROID_SDK_ROOT%\platform-tools;%ANDROID_SDK_ROOT%\cmdline-tools\latest\bin"
    [Environment]::SetEnvironmentVariable("Path", $pathUser, "User")
}
```

Zamknij i otwórz nowe okno PowerShell.

### Krok 3: Zainstaluj wymagane komponenty SDK

```powershell
sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

## 4. Ustawienie JAVA_HOME (jeśli potrzeba)

Jeśli Java jest zainstalowana, ale `gradlew` jej nie widzi:

```powershell
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-17.0.10.7-hotspot", "User")

$pathUser = [Environment]::GetEnvironmentVariable("Path", "User")
if ($pathUser -notlike "*%JAVA_HOME%\\bin*") {
    [Environment]::SetEnvironmentVariable("Path", "$pathUser;%JAVA_HOME%\bin", "User")
}
```

Zamknij i otwórz terminal ponownie.

## 5. Build i instalacja aplikacji

W katalogu projektu:

```powershell
.\gradlew.bat clean
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
.\gradlew.bat bundleRelease
```

## 6. Taski diagnostyczne dodane do projektu

```powershell
.\gradlew.bat checkEnvironment
.\gradlew.bat showDevices
.\gradlew.bat showLogs
.\gradlew.bat installAndRun
.\gradlew.bat removeApp
```

### Co robią taski

- `checkEnvironment`:
  - Java version
  - Gradle version
  - Android SDK location
  - compileSdk/targetSdk/minSdk
  - czy `adb` jest dostępne
  - czy urządzenie jest widoczne
- `showDevices`: wykonuje `adb devices`
- `showLogs`: wykonuje `adb logcat` z filtrem `LibreDisplay`, `AndroidRuntime`, `System.err`
- `installAndRun`: build + install + uruchomienie `MainActivity`
- `removeApp`: `adb uninstall com.libredisplay`

## 7. Samsung Galaxy – kroki połączenia

1. Włącz **Opcje programisty**.
2. Włącz **Debugowanie USB**.
3. Podłącz telefon kablem USB.
4. Zaakceptuj odcisk RSA na telefonie.
5. Sprawdź:

```powershell
adb devices
```

Status musi być `device` (nie `unauthorized`).

## 8. Rozwiązywanie problemów

### SDK not found

- Ustaw `ANDROID_SDK_ROOT` / `ANDROID_HOME`
- Sprawdź, czy istnieje `platforms\android-35` i `build-tools\35.0.0`

### ADB not found

- Dodaj `%ANDROID_SDK_ROOT%\platform-tools` do `Path`
- Sprawdź `adb version`

### device unauthorized

- Odłącz/podłącz kabel
- Usuń autoryzacje USB w telefonie
- Zaakceptuj ponownie RSA

### JAVA_HOME not set

- Ustaw `JAVA_HOME` na JDK 17
- Dodaj `%JAVA_HOME%\bin` do `Path`

### build failed

- Uruchom:

```powershell
.\gradlew.bat clean assembleDebug --stacktrace
```

### manifest errors

- Sprawdź `app/src/main/AndroidManifest.xml`
- Sprawdź zgodność `applicationId` i `MainActivity`

### gradle sync errors

- Sprawdź internet, proxy i repozytoria w `settings.gradle.kts`
- Wyczyść cache:

```powershell
.\gradlew.bat --stop
.\gradlew.bat clean
```

## 9. Lokalizacje artefaktów

- Debug APK:
  - `app/build/outputs/apk/debug/app-debug.apk`
- Release AAB:
  - `app/build/outputs/bundle/release/app-release.aab`

## 10. Ręczna instalacja APK na Samsungu

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.libredisplay/.MainActivity
```

