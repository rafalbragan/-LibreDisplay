# LibreCare 1.6.0 - Release Notes

Data wydania / Release date: 2026-08-19

## PL

### Najważniejsze zmiany
- Głęboka przebudowa UI Home i History w kierunku bardziej medycznego, zwartego układu.
- Szczegóły refundacji NFZ przeniesione do pełnego, przewijalnego ekranu `Refundacja NFZ`.
- Uspójnione zachowanie Android Back: potwierdzenie zamknięcia na Home, cofanie o jeden poziom na pod-ekranach.

### Poprawki
- Ujednolicono próg nieaktualnych danych glikemii do 15 minut.
- Uproszczono dolną nawigację do działających pozycji (`Główna`, `Historia`, `Więcej`).
- Ukryto niezaimplementowaną sekcję `Notatki i zdarzenia` w historii.
- Poprawiono wykres historii: subtelniejsze pasma zakresów, cieńsza linia i tooltip bez obcinania przy prawej krawędzi.

### Usprawnienia
- Dodano skrót `Zmień metryki` z Home do odpowiedniej sekcji Ustawień (HbA1c/GMI).
- Podsumowanie `MIN / ŚREDNIA / MAX` jako kompaktowy wiersz pod wykresem.
- Dodano zmianę kolejności `Quick Metrics` metodą long press + drag & drop oraz alternatywnie w Ustawieniach (góra/dół).

### Testy i walidacja
- `./gradlew clean --no-configuration-cache` - PASS
- `./gradlew compileDebugKotlin --no-configuration-cache` - PASS
- `./gradlew testDebugUnitTest --no-configuration-cache` - PASS
- `./gradlew lint --no-configuration-cache` - PASS
- `./gradlew assembleDebug --no-configuration-cache` - PASS
- `./gradlew assembleRelease --no-configuration-cache` - PASS
- `./gradlew bundleRelease --no-configuration-cache` - PASS
- `./gradlew connectedDebugAndroidTest` - NIE WYKONANO (No connected device/emulator available.)

### Artefakty
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`

### Znane ograniczenia
- Alarmy i dodatkowe akcje medyczne są poza zakresem tej iteracji UI.

## EN

### Highlights
- Deep UI redesign of Home and History toward a cleaner medical dashboard.
- NFZ reimbursement details moved to a full-screen, scrollable `Refundacja NFZ` view.
- Consistent Android Back behavior: exit confirmation on Home, one-level back on nested screens.

### Fixes
- Unified stale glucose data threshold to 15 minutes.
- Simplified bottom navigation to working destinations only (`Home`, `History`, `More`).
- Hidden non-implemented `Notes and events` section in History.
- Improved history chart with subtler range bands, thinner line, and non-clipping tooltip at the right edge.

### Improvements
- Added `Change metrics` shortcut from Home to the relevant Settings section (HbA1c/GMI).
- Replaced separate min/max cards with a compact `MIN / AVERAGE / MAX` row.
- Added `Quick Metrics` tile reorder via long-press drag & drop and an accessibility fallback in Settings (up/down).

### Tests and validation
- `./gradlew clean --no-configuration-cache` - PASS
- `./gradlew compileDebugKotlin --no-configuration-cache` - PASS
- `./gradlew testDebugUnitTest --no-configuration-cache` - PASS
- `./gradlew lint --no-configuration-cache` - PASS
- `./gradlew assembleDebug --no-configuration-cache` - PASS
- `./gradlew assembleRelease --no-configuration-cache` - PASS
- `./gradlew bundleRelease --no-configuration-cache` - PASS
- `./gradlew connectedDebugAndroidTest` - NOT EXECUTED (No connected device/emulator available.)

### Artifacts
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`

### Known limitations
- Alarms and extra medical action flows are out of scope for this UI iteration.

