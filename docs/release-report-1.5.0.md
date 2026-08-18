# LibreCare Release Report 1.5.0

## Wersja
- Version name: `1.5.0`
- Version code: `9`
- Data: `2026-08-18`

## Zakres wydania
To wydanie koncentruje sie na mockup-based redesignie glownego dashboardu i historii glikemii.

## Zmiany wizualne
- nowy dark medical dashboard
- bardziej kompaktowy top bar
- kompaktowy selector osob
- kompaktowy range row
- dominujaca karta aktualnej glikemii
- czytelna karta warningow
- kompaktowe KPI cards
- przebudowany preview chart
- przebudowany full-screen history chart
- kompaktowa karta NFZ z dialogiem szczegolowym
- dolna nawigacja

## Interakcje wdrozone
- klik chipa osoby zmienia osobe
- klik `Zmien` przy zakresie otwiera historie / selector zakresu
- klik wykresu dashboardu otwiera pelny ekran historii
- drag na pelnym ekranie historii pokazuje tooltip
- ikona info NFZ otwiera dialog szczegolow
- dolna nawigacja ma punkty Glowna / Historia / Dodaj / Alarmy / Wiecej
- refresh nie resetuje wybranej osoby
- Settings action prowadzi do ustawien

## Testy i build
- `./gradlew clean` - PASS
- `./gradlew testDebugUnitTest` - PASS
- `./gradlew lint` - PASS
- `./gradlew connectedDebugAndroidTest` - brak urzadzenia/emulatora
- `./gradlew assembleDebug` - PASS
- `./gradlew assembleRelease` - PASS
- `./gradlew bundleRelease` - PASS

## Artefakty
- `C:\Users\SG0216827\IdeaProjects\LibreDisplay\app\build\outputs\apk\debug\app-debug.apk`
- `C:\Users\SG0216827\IdeaProjects\LibreDisplay\app\build\outputs\apk\release\app-release.apk`
- `C:\Users\SG0216827\IdeaProjects\LibreDisplay\app\build\outputs\bundle\release\app-release.aab`
- `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-1.5.0-debug.apk`
- `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-1.5.0-release.apk`
- `C:\Users\SG0216827\IdeaProjects\LibreDisplay\release-artifacts\LibreCare-1.5.0-release.aab`

## Screenshot / manual QA
Do pelnej oceny zgodnosci z makieta potrzebna jest wizualna walidacja na emulatorze lub urzadzeniu wedlug `docs/ui-dashboard-qa-checklist.md`.

## Pozostale ryzyka
- `Dodaj` i `Alarmy` w dolnej nawigacji nie maja jeszcze pelnego flow domenowego.
- `Notatki i zdarzenia` sa placeholderem UI.
- Brak connected tests ogranicza automatyczne potwierdzenie layoutu Compose na urzadzeniu.

