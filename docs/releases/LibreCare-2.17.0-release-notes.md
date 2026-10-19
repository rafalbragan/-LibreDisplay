# LibreCare 2.17.0 — release notes / informacje o wydaniu

VersionCode: **51**. Previous master: **2.16.2 / 46**; reviewed P0 branch: **2.16.6 / 50**.

## PL

### Dodano
- Import eksportów LibreView CSV z systemowego selektora pliku lub folderu, bez szerokich uprawnień do pamięci.
- Podgląd przed zapisem: jawny wybór profilu, potwierdzenie nazwy, liczby pomiarów, pominięć, duplikatów, konfliktów i nowych rekordów; daty oraz interpretacja strefy/DST.
- Transakcyjny zapis partiami, bez nadpisywania konfliktów; pochodzenie każdego zaimportowanego rekordu i odporność na ponowny import.
- Nowa historia: gesty przybliżania i przesuwania, dzień/tydzień/miesiąc/rok/całość, dokładny wybór pomiaru i obsługa wielu lat.
- Materiały Google Play: grafiki marki, polski opis, informacje dla testerów i instrukcja przesyłania.

### Zmieniono i naprawiono
- Rutynowa retencja nie usuwa rekordów LibreViewCSV/LibreViewScan. Świadome usuwanie danych w Prywatności pozostaje dostępne.
- Room 2 → 3: dodano nullable `importProvenance`; zachowano istniejące dane i jawne migracje. Usunięto destrukcyjny fallback także z debug.
- Kopie danych zachowują nowe pole pochodzenia. Nie zintegrowano zmian PR #43.
- Analiza udostępnia lokalne/nieaktywne profile; zakres własny pobiera rzeczywisty zakres danych zamiast ograniczać obliczenia do bufora 400 dni.
- Zintegrowano sprawdzone poprawki P0 z PR #45 z zachowaniem pochodzenia. PR #38 przejrzano, ale nie kopiowano jego nakładających się gestów ani ładowania całej historii.

### Testy i artefakty
- Wyniki JVM, testów emulatora, migracji oraz dokładne ścieżki/hash APK: `docs/release-report-2.17.0.md`.
- Debug APK jest przeznaczony do testów na dedykowanym emulatorze, nie do instalacji na fizycznym Samsungu.
- **Nie ma zweryfikowanego podpisanego release APK/AAB ani pliku gotowego do Google Play.** Pakowanie pozostaje zablokowane konfiguracją podpisu i brakiem zarejestrowanego certyfikatu upload do porównania.

### Ograniczenia
- Obsługiwane nagłówki polskie/angielskie, UTF-8, mg/dL, typy 0/1; typ 6 i pozostałe zdarzenia są pomijane. Testowano wyłącznie syntetyczne pliki, nie prywatny eksport użytkownika.
- Folder: bez podfolderów, maksymalnie 2000 dokumentów; pojedynczy import do 128 MB. Nieobsługiwany wariant nagłówka jest odrzucany.
- Plik nie określa strefy czasowej. Użytkownik wybiera jedną strefę dla importu; godziny nieistniejące są pomijane, dwuznaczne wymagają wybranej polityki. Podróże między strefami wymagają osobnych plików.
- Wykres renderuje maksymalnie 801 zagregowanych przedziałów; pokazuje zakres min–max, nie wymyślone pomiary pomiędzy próbkami. Dokładny punkt jest odczytywany osobnym zapytaniem.
- Wymagania bieżącego target API oraz właścicielskie deklaracje Play/URL polityki prywatności wymagają potwierdzenia przed publikacją.

## EN

### Added
- Local LibreView CSV import through the Android document/tree picker, with explicit profile/name confirmation and a no-write preview.
- Streaming parsing, explicit timezone/DST handling, duplicate/conflict counts, atomic batched insertion, idempotency and per-reading provenance.
- Database-backed multi-year history with working pinch/pan gestures, exact point lookup, bounded rendering and separated person histories.
- Google Play branding assets, genuine synthetic-data emulator captures, Polish listing and internal-testing instructions.

### Changed / fixed
- Imported readings survive routine retention; explicit privacy deletion remains supported.
- Additive Room migration 2→3 and backup round-trip support for import provenance; no destructive fallback and no PR #43 integration.
- Imported inactive profiles are available in analysis; custom-period statistics use their actual database range.
- Reviewed P0 recovery/precision changes integrated from PR #45 without merging or altering that PR. PR #38 informed review only; overlapping gesture handlers were not copied.

### Tests / artifacts / limitations
See `docs/release-report-2.17.0.md` and `docs/quality-review-report-2.17.0.md` for exact final evidence and compliance status. Release signing and Play upload readiness remain **HOLD**, not a successful signed release. The parser was tested with synthetic schema-matching data only. Folder discovery is nonrecursive; one timezone interpretation applies to a file. Existing backup/security and background-monitoring limitations are not silently represented as fixed. No insulin dosing or treatment recommendation was added.

