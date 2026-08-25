# LibreCare - Quality Review Report 2.10.0

Data: 2026-08-25

## Zakres przeglądu
- Ekran `Analiza danych` (ViewModel, UI, logika metryk, logika wykresów).
- Eksport danych surowych do Excela i udostępnianie pliku.
- Zachowanie selektora zakresu Home po zmianie z auto-scroll na manual scroll.

## Findings
- **Brak krytycznych regresji** wykrytych w testach jednostkowych dla wdrożonego zakresu.
- Selekcja zakresu Home nie zmienia się samoczynnie podczas gestu przewijania.
- Exporter tworzy poprawny kontener `.xlsx` z wymaganymi arkuszami i danymi.

## Test evidence
- `HomeChartRangeSelectorBehaviorTest` - PASS
- `AnalysisMetricsFactoryTest` - PASS
- `AnalysisChartFactoryTest` - PASS
- `RawDataExcelExporterTest` - PASS
- `:app:compileDebugKotlin` - PASS

## UI review
- Dashboard/Home:
  - range chips przewijalne ręcznie,
  - brak automatycznego przeskoku po zmianie/scrollu,
  - lepsza obsługa wąskich szerokości i landscape.
- Analiza:
  - tabela metryk i wykresy mieszczą się w układzie przewijanym,
  - wszystkie dodane napisy user-facing pozostają po polsku.

## Data/Privacy review
- Eksport działa lokalnie, bez wysyłki danych do sieci.
- Udostępnienie przez `FileProvider` ogranicza uprawnienie do odczytu do celu share intent.

## Risks
- Brak testu instrumentacyjnego faktycznego otwarcia pliku `.xlsx` w zewnętrznych aplikacjach.
- Interaktywne tooltipy wykresów analitycznych są planowane w kolejnych etapach.

## Recommendations
- Dodać testy UI (Compose) dla przewijania i dostępności nowych sekcji Analizy w landscape.
- Dodać test integracyjny export -> share intent w androidTest.

