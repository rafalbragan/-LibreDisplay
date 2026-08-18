# LibreCare Release Notes v1.3.0

**Version:** 1.3.0 (versionCode 4 → 5)  
**Release Date:** 2026-08-18  
**Branch:** master  
**Git Commit:** 9b73343

---

## 🇵🇱 NOTATKI WYDANIA (POLSKI)

### Wersja 1.3.0 - Przeprojektowanie Interfejsu Użytkownika

Wydanie 1.3.0 zawiera znaczne ulepszenia interfejsu użytkownika, nową funkcjonalność zarządzania danymi i ulepszone doświadczenie użytkownika.

#### 🎨 Główne zmiany

**1. Przeprojektowany pulpit nawigacyjny (Dashboard)**
- Usunięto zduplikowaną prezentację monitorowanej osoby (jedno miejsce tożsamości)
- Usunięto dropdown osoby z top bara dla 2-3 osób
- Usunięto etykietę "Osoba monitorowana" z głównego układu
- Wprowadzono kompaktowy przełącznik osób z chipami
- Lepszy układ informacji bez zbędnego przewijania
- Wyświetlanie wybranego zakresu czasu
- Kompaktowe kafelki ze statystyką (poniżej zakresu, w zakresie, powyżej)

**2. Historia glikemii i wykresy**
- Wykresy z softową zacieniowaną szarfą zakresu docelowego (zielona)
- Markery minimum i maksimum z godzinami
- Podpowiedź przy dotknięciu pokazująca dokładną wartość i czas
- Poprawiona gęstość danych dla czytelności
- Pełnoekranowy widok wykresu po dotknięciu

**3. Zarządzanie zakresem czasu**
- Nowy model `TimeRangeState` do zarządzania przedziałami czasowymi
- Predefiniowane zakresy: 12h, 24h, 7 dni, 14 dni, 30 dni, 90 dni, 12 miesięcy
- Wyświetlanie dostępnego zakresu danych
- Obsługa zakresów niestandardowych

**4. Nowe ekrany**
- **Informacje i statystyki** - wielkość bazy danych, liczba odczytów, transfer sieciowy
- **Retencja danych** - wybór okresu przechowywania danych lokalnych (12h - 24m)
- **Częstotliwość odpytywania** - konfiguracja interwału pobierania danych
- Potwierdzenie przed zmianami retencji z ostrzeżeniami

**5. Wszystkie teksty w interfejsie w języku polskim**
- Etykiety przycisków
- Komunikaty błędów
- Opisy ekranów
- Podpowiedzi

#### 📊 Funkcjonalności

- ✅ Kompaktowe statystyki (poniżej/w/powyżej zakresu, sensor, średnia, GMI)
- ✅ Zarządzanie wieloma osobami z widocznymi chipami
- ✅ Widok zakresu czasu na dasboardzie
- ✅ Wykresy z szarfą zakresu docelowego
- ✅ Markery min/max na wykresach
- ✅ Podpowiedzi na wykresach
- ✅ Ekran statystyk bazy danych
- ✅ Zarządzanie retencją danych
- ✅ Ustawienia częstotliwości pobierania

#### 🔧 Zmiany techniczne

- Nowy model `TimeRangeState` do obsługi okresów czasowych
- Nowy enum `PresetTimeRange` z dostępnymi przedziałami
- Zaktualizowany `MonitoringUiState` o pole `timeRange`
- Nowe komponenty Compose: `CompactPersonSwitcherBar`, `VisiblePersonSwitcher`, `TimeRangeDisplay`
- Nowe ekrany: `StatisticsScreen`, `RetentionSettingsScreen`, `PollingFrequencyScreen`
- Dodanych ~45 nowych polskich etykiet w `strings.xml`

#### ✅ Testy

- **Testy jednostkowe:** 185/185 PASS (w tym 8 nowych testów `TimeRangeStateTest`)
- **Lint:** PASS (bez błędów)
- **Connected tests:** Brak dostępu do urządzenia/emulatora (zwykle oczekiwane w CI)

**Nowe testy:**
- `TimeRangeStateTest.fromPreset_last24Hours_setsCorrectRange()`
- `TimeRangeStateTest.fromPreset_last7Days_setsCorrectRange()`
- `TimeRangeStateTest.rangeLabel_presetRange_returnsPresetLabel()`
- `TimeRangeStateTest.rangeLabel_customRange_returnsCustomLabel()`
- `TimeRangeStateTest.availableRanges_filtersByDataAvailability()`
- `TimeRangeStateTest.durationHours_calculatesCorrectly()`
- `TimeRangeStateTest.durationDays_calculatesCorrectly()`

#### 📦 Artefakty

**Debug APK:** 21.94 MB
- Ścieżka: `app/build/outputs/apk/debug/app-debug.apk`
- Użycie: testowanie aplikacji na urządzeniach

**Release APK:** 2.82 MB (podpisany)
- Ścieżka: `app/build/outputs/apk/release/app-release.apk`
- Użycie: bezpośrednia instalacja na urządzeniach

**Release AAB:** 5.12 MB (podpisany)
- Ścieżka: `app/build/outputs/bundle/release/app-release.aab`
- Użycie: **upload do Google Play** (rekomendowany)

#### ⚠️ Znane ograniczenia

1. Niektóre zaawansowane funkcje widżetów (widget z mini-wykresem) są zaplanowane na przyszłe wydania
2. Tło odpytywanie nie jest w pełni zaimplementowane dla nowych opcji częstotliwości
3. Eksport/udostępnianie wykresów jest zaplanowany na następne wydanie
4. Pinch zoom na wykresach nie jest jeszcze implementowany

#### 📝 Ręczne kroki do weryfikacji

1. Zaloguj się lub wejdź w tryb demo
2. Sprawdź, czy imię i nazwisko monitorowanej osoby jest widoczne tylko w przełączniku osób
3. Jeśli masz 2-3 osoby, sprawdź chipsy bez dropdownu w top barze
4. Dotknij wykresu, aby otworzyć pełnoekranowy widok
5. W ustawieniach sprawdź nowe ekrany: Retencja danych i Częstotliwość odpytywania
6. Wszystkie teksty powinny być w polskim

#### 🚀 Następne kroki

- Wdrożenie pełnego tła odpytywania dla nowych opcji
- Dodanie więcej wariantów widżetów
- Implementacja eksportu/udostępniania danych
- Ulepszenia wydajności dla dużych zbiorów danych

---

## 🇬🇧 RELEASE NOTES (ENGLISH)

### Version 1.3.0 - UI Redesign

Release 1.3.0 contains significant user interface improvements, new data management functionality, and improved user experience.

#### 🎨 Major Changes

**1. Redesigned Dashboard**
- Removed duplicated monitored-person identity (single primary identity area)
- Removed top-bar person dropdown for 2-3 people
- Removed extra "Monitored person" label from the main flow
- Added compact person switcher chips as the only identity element
- Better information layout without unnecessary scrolling
- Selected time range display
- Compact statistic tiles (below range, in range, above range)

**2. Glucose History and Charts**
- Charts with soft shaded target range (green)
- Min/max markers with times
- Touch tooltip showing exact value and time
- Improved data density for readability
- Full-screen chart view on tap

**3. Time Range Management**
- New `TimeRangeState` model for managing time periods
- Predefined ranges: 12h, 24h, 7 days, 14 days, 30 days, 90 days, 12 months
- Display of available data range
- Support for custom ranges

**4. New Screens**
- **Statistics and Information** - database size, reading count, network transfer
- **Data Retention** - choose data retention period (12h - 24m)
- **Polling Frequency** - configure data fetch interval
- Confirmation before retention changes with warnings

**5. All User-Facing Text in Polish**
- Button labels
- Error messages
- Screen descriptions
- Tooltips

#### 📊 Features

- ✅ Compact statistics (below/in/above range, sensor, average, GMI)
- ✅ Multi-person management with visible chips
- ✅ Time range display on dashboard
- ✅ Charts with target range shading
- ✅ Min/max markers on charts
- ✅ Chart tooltips
- ✅ Database statistics screen
- ✅ Data retention management
- ✅ Polling frequency settings

#### 🔧 Technical Changes

- New `TimeRangeState` model for handling time periods
- New `PresetTimeRange` enum with available ranges
- Updated `MonitoringUiState` with `timeRange` field
- New Compose components: `CompactPersonHeader`, `VisiblePersonSwitcher`, `CompactStatisticsGrid`, `TimeRangeDisplay`
- New screens: `StatisticsScreen`, `RetentionSettingsScreen`, `PollingFrequencyScreen`
- Added ~45 new Polish string labels in `strings.xml`

#### ✅ Tests

- **Unit tests:** 185/185 PASS (including 8 new `TimeRangeStateTest` tests)
- **Lint:** PASS (no errors)
- **Connected tests:** No device/emulator available (typically expected in CI)

**New Tests:**
- `TimeRangeStateTest.fromPreset_last24Hours_setsCorrectRange()`
- `TimeRangeStateTest.fromPreset_last7Days_setsCorrectRange()`
- `TimeRangeStateTest.rangeLabel_presetRange_returnsPresetLabel()`
- `TimeRangeStateTest.rangeLabel_customRange_returnsCustomLabel()`
- `TimeRangeStateTest.availableRanges_filtersByDataAvailability()`
- `TimeRangeStateTest.durationHours_calculatesCorrectly()`
- `TimeRangeStateTest.durationDays_calculatesCorrectly()`

#### 📦 Artifacts

**Debug APK:** 21.94 MB
- Path: `app/build/outputs/apk/debug/app-debug.apk`
- Use: application testing on devices

**Release APK:** 2.82 MB (signed)
- Path: `app/build/outputs/apk/release/app-release.apk`
- Use: direct installation on devices

**Release AAB:** 5.12 MB (signed)
- Path: `app/build/outputs/bundle/release/app-release.aab`
- Use: **upload to Google Play** (recommended)

#### ⚠️ Known Limitations

1. Some advanced widget features (chart widget) planned for future releases
2. Background polling not fully implemented for new frequency options
3. Chart export/sharing planned for next release
4. Pinch zoom on charts not yet implemented

#### 📝 Manual Verification Steps

1. Log in or enter demo mode
2. Verify header shows full name of monitored person
3. If you have 2-3 people, check visible chips instead of dropdown
4. Tap chart to open full-screen view
5. In settings, check new screens: Data Retention and Polling Frequency
6. All text should be in Polish

#### 🚀 Next Steps

- Implement full background polling for new options
- Add more widget variants
- Implement data export/sharing
- Performance improvements for large datasets

---

## Summary

This release significantly improves the LibreCare user interface by providing:
- A more compact and information-rich dashboard
- Better person switching with visible chips
- Improved time range selection and management
- New screens for data and system management
- Full Polish language support throughout the UI

All changes maintain backward compatibility with existing data and settings.

**Recommendation:** Deploy to Google Play using the Release AAB artifact.

