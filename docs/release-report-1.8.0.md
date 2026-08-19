# Release Report – LibreCare 1.8.0

**Data:** 2026-08-19  
**Wersja:** 1.8.0 (versionCode 12)  
**Poprzednia wersja:** 1.7.0 (versionCode 11)

---

## Streszczenie

Wersja 1.8.0 naprawia trzy krytyczne błędy zgłoszone po iteracji 1.7.0:
1. Linia wykresu wychodziła poza obszar przy glikemii > 420 mg/dL.
2. Statystyki pokazywały „0%" dla czasu w zakresie nawet gdy wykres wskazywał obecność danych in-range (błąd zaokrąglania).
3. Elementy czasu względnego i licznik do pełnego zakresu danych nie aktualizowały się lokalnie między synchronizacjami sieciowymi.

---

## Przegląd architektury

| Obszar | Status |
|---|---|
| ViewModel (MonitoringViewModel) | Bez zmian |
| Repository / API | Bez zmian |
| Room / migracje | Bez zmian |
| DataCoverageModel | Zmieniono: dodano parametr `now` |
| GlucoseChart | Zmieniono: usunięto cap na 420 |
| GlucoseMetricsCalculator | Zmieniono: roundToInt zamiast toInt |
| MonitoringScreen | Zmieniono: dodano ticker |
| FullScreenGlucoseChartScreen | Zmieniono: dodano ticker |
| RedesignedGlucoseCard | Zmieniono: dodano parametr `now` |

---

## Zmiany UI

- Brak zmian wizualnych; zmiany dotyczą wyłącznie logiki obliczeń i odświeżania.
- Elementy czasu względnego teraz aktualizują się co 30 sekund lokalnie.
- Wykres nie nachodzi na etykiety osi przy wysokich wartościach glikemii.

---

## Zmiany bazy danych

Brak. Wersja Room nie zmieniona.

---

## Testy

| Typ | Wynik |
|---|---|
| Testy jednostkowe (274) | PASS |
| DataCoverageModelTest (8 przypadków) | PASS |
| Testy UI (Compose) | Nie uruchamiane – brak urządzenia/emulatora |

---

## Artefakty

| Plik | Ścieżka | Rozmiar |
|---|---|---|
| Debug APK | `release-artifacts/LibreCare-1.8.0-debug.apk` | ~22,1 MB |
| Release APK | `release-artifacts/LibreCare-1.8.0-release.apk` | ~2,9 MB |
| Release AAB | `release-artifacts/LibreCare-1.8.0-release.aab` | ~5,4 MB |
| Google Play Upload | `release-artifacts/LibreCare-1.8.0-release.aab` | ~5,4 MB |

---

## Ryzyka pozostałe

1. **Wykres – możliwe rzadkie puste miejsca w danych**: Jeśli API zwraca rzadkie punkty (co > 20 min), `rangeDistributionFromHistory` pomija te odcinki. Metryk TIR może być nieodpowiedni dla danych historycznych z przerwami > 20 min.
2. **In-range "< 1%"**: Choć `roundToInt()` naprawia truncation, wartości ≈ 0,4% nadal pokazują "0%". Użytkownik może chcieć wyświetlania "< 1%", ale nie zostało to zaimplementowane, aby nie wprowadzać nowych wymagań.
3. **Brak connected tests**: Testy Compose UI i instrumentacyjne nie zostały uruchomione – brak urządzenia/emulatora.

