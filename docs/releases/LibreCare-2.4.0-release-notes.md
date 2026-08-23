# LibreCare 2.4.0 – Release Notes

- Aplikacja / Application: **LibreCare**
- Wersja / Version: **2.4.0** (versionCode **26**)
- Data / Date: **2026-08-23**
- Poprzednia wersja / Previous version: **2.3.0** (versionCode 25)

---

## PL

### Nowe funkcje

- **Jedna automatyczna kopia zapasowa.** Plik tworzy się sam w katalogu danych aplikacji
  (`files/backup/librecare-backup.json`). Użytkownik nie wybiera lokalizacji, nazwy ani hasła.
- **Zakres kopii = to, co widać po zalogowaniu.** Kopia obejmuje wyłącznie osoby LIVE widoczne
  dla zalogowanego konta oraz konfigurację aplikacji i sesję, dzięki czemu można przenieść całe
  ustawienie na inny telefon.
- **Przeniesienie na inny telefon.** Kopię można zapisać przez systemowy zapis pliku
  (SAF `CREATE_DOCUMENT`) lub udostępnić dowolnym kanałem (FileProvider + systemowy arkusz
  udostępniania) – Dysk Google, Samsung Cloud, e-mail, kabel.
- **Integracja z systemowym przenoszeniem danych.** `allowBackup`, reguły `backup_rules.xml`
  i `data_extraction_rules.xml` oraz `LibreCareBackupAgent` sprawiają, że kopia jest zabierana
  przez Samsung Smart Switch, kopię Google One i transfer przewodowy – uniwersalnie na dowolnym
  telefonie z Androidem.
- **Scalanie z podsumowaniem.** Identyczne odczyty są scalane po cichu, nowe zakresy dat są
  dopisywane, a podsumowanie mówi wprost, np. „Anna: wczytano 3 dni (288 odczytów) między
  18.08.2026 a 20.08.2026”.
- **Rozstrzyganie konfliktów.** Gdy ten sam znacznik czasu ma inną wartość, aplikacja pyta, czy
  zachować dane bieżące, czy z archiwum.
- **Menedżery haseł.** Ekran logowania obsługuje autofill (Google, Samsung Pass i inne) dla pola
  e-mail i hasła oraz proponuje zapisanie hasła po zalogowaniu.
- **Blokada aplikacji.** Odcisk palca, PIN, wzór lub hasło ekranu blokady (ekran „Prywatność i dane”).
- **Dynamiczne zakresy wykresu.** 1g, 3g, 6g, 9g, 12g, 24g, 3d, 7d, 14d, 1m, 3m, 6m, 12m –
  widoczne są zakresy możliwe do wyświetlenia plus dokładnie jeden wyszarzony „następny”.
- **Informacja o nowych danych.** Zamiast automatycznego przewinięcia wykresu pojawia się
  „Pojawiły się nowe dane · Pokaż najnowsze”.

### Poprawki

- Naprawiono wczytywanie kopii zapasowych – zarówno starych, jak i nowych. Dekoder jest odporny
  na uszkodzone wiersze, brakujące pola i błędne typy, a komunikaty błędów są po polsku.
- Odświeżanie w tle nie przenosi już użytkownika do najnowszych danych.
- Linia wykresu biegnie od lewej do prawej krawędzi (interpolacja na granicy okna) i jest
  przerywana wyłącznie tam, gdzie faktycznie brakuje danych.
- Suwak pod wykresem: jedno przesunięcie palcem przewija cały zakres; dotknięcie ścieżki
  przeskakuje do wskazanego miejsca; wysokość zwiększona do 40 dp. Usunięto przyczynę
  „skoku o kilka pikseli” (reset gestu przy każdym zdarzeniu).

### Ulepszenia

- Format kopii podniesiony do wersji 3 (czytelny JSON bez hasła, z sumą kontrolną). Odczyt
  starszych formatów (v1, v2 szyfrowany hasłem, warianty ze skróconymi kluczami) pozostaje wspierany.
- Zapis kopii jest atomowy (plik tymczasowy + weryfikacja odczytu + kopia poprzednia), więc
  przerwany zapis nie niszczy poprzedniej kopii.
- Napisy „Aktualna glikemia” i „Trend” są w jednej linii.
- Metryki mają takie same zaokrąglone obwoluty jak przyciski zakresów oraz podpowiedź
  „przesuń w bok, aby zobaczyć więcej ›”.
- Nieczytelny wiersz `Dane: 12g · 24g 16g 54m` zastąpiony opisem „Zapisana historia: …” oraz
  „Zakres 24g będzie dostępny za ok. …”.
- Wykres domowy korzysta z pełnej historii z bazy, a nie tylko z historii bieżącego odczytu.

### Testy

| Zadanie | Wynik |
| --- | --- |
| `./gradlew testDebugUnitTest` | PASS – 376 testów, 0 błędów |
| `./gradlew connectedDebugAndroidTest` | PASS – `SM-S948B - 16` oraz `LibreCare_API35(AVD) - 15` |
| `./gradlew lint` | PASS |
| `./gradlew assembleDebug` | PASS |
| `./gradlew assembleRelease` | PASS |
| `./gradlew bundleRelease` | PASS |

Nowe zestawy testów: `BackupCodecTest`, `BackupMergeEngineTest`, `LocalBackupStoreTest`,
`AppDataBackupRepositoryLegacyRestoreTest`, `GlucoseChartSeriesTest`, `HomeChartRangeOptionsTest`.

### Artefakty

| Artefakt | Ścieżka | Rozmiar |
| --- | --- | --- |
| Debug APK | `release-artifacts/LibreCare-2.4.0-debug.apk` | 23 629 423 B |
| Release APK | `release-artifacts/LibreCare-2.4.0-release.apk` | 3 420 808 B |
| Release AAB | `release-artifacts/LibreCare-2.4.0-release.aab` | 6 171 933 B |

Plik do wgrania w Google Play: `release-artifacts/LibreCare-2.4.0-release.aab`.

### Znane ograniczenia

- Brak dedykowanego API Samsung Smart Switch – transfer opiera się na standardowym mechanizmie
  Android Backup/Transfer, który Smart Switch i Google One wykorzystują.
- „Zapis w chmurze” realizowany jest przez SAF/udostępnianie do wybranego dostawcy chmury; nie ma
  własnego, dedykowanego backendu LibreCare.
- Passkeys: obsłużona jest blokada biometryczna/PIN/wzór/hasło urządzenia; pełne poświadczenia
  FIDO2 wymagają wsparcia po stronie dostawcy LibreLinkUp.
- Testy instrumentalne obejmują migrację Room; szeroki zestaw testów Compose UI nadal działa jako
  testy logiki układu (JVM), a nie testy na urządzeniu.

---

## EN

### New features

- **Single automatic backup file** created by the app itself in app-private storage
  (`files/backup/librecare-backup.json`). No location, filename or password chosen by the user.
- **Backup scope equals what you see after login** – LIVE persons visible to the signed-in
  account plus app configuration and session, so a full setup can be moved to another phone.
- **Transfer to another phone** via system file save (SAF `CREATE_DOCUMENT`) or the system share
  sheet (FileProvider) – Google Drive, Samsung Cloud, e-mail or cable.
- **System transfer integration**: `allowBackup`, `backup_rules.xml`, `data_extraction_rules.xml`
  and `LibreCareBackupAgent` make the backup travel with Samsung Smart Switch, Google One backup
  and wired transfer – universally on any Android phone.
- **Merge with summary**: identical readings merge silently, new date ranges are appended, and the
  summary states e.g. “Anna: loaded 3 days (288 readings) between 2026-08-18 and 2026-08-20”.
- **Conflict resolution**: when the same timestamp holds a different value, the app asks whether to
  keep current or archived data.
- **Password manager support** on the sign-in screen (Google, Samsung Pass and others).
- **App lock** with fingerprint, PIN, pattern or device password.
- **Dynamic chart ranges**: 1h, 3h, 6h, 9h, 12h, 24h, 3d, 7d, 14d, 1m, 3m, 6m, 12m – available
  ranges plus exactly one grayed-out “next” tier.
- **New-data notice** instead of auto-jumping the chart to the newest reading.

### Fixes

- Backup restore fixed for both old and new files; the decoder tolerates corrupt rows, missing
  fields and wrong types, with Polish error messages.
- Background refresh no longer moves the user to the newest data.
- The chart line spans the full width (edge interpolation) and breaks only on real data gaps.
- Chart navigator: one swipe pans the whole range; tapping the track jumps to that position.

### Improvements

- Backup format v3 (plain readable JSON with checksum); v1/v2 and short-key variants still readable.
- Atomic backup write (temp file + read-back verification + previous copy retained).
- “Aktualna glikemia” and “Trend” on a single line.
- Metrics use the same rounded chip wrappers as range buttons, with a scroll affordance.
- The confusing `Dane: 12g · 24g 16g 54m` row replaced with explicit stored-history text.
- Home chart now uses full database history.

### Tests

All unit tests (376), instrumented tests on two devices, lint and all build variants pass.

### Artifacts

- Debug APK: `release-artifacts/LibreCare-2.4.0-debug.apk` (23,629,423 B)
- Release APK: `release-artifacts/LibreCare-2.4.0-release.apk` (3,420,808 B)
- Release AAB: `release-artifacts/LibreCare-2.4.0-release.aab` (6,171,933 B) – Google Play upload file

### Known limitations

See the Polish section above.

