# LibreCare 2.4.0 – Quality Review Report

Data: 2026-08-23 · Wersja: 2.4.0 (versionCode 26) · Gałąź: `master`

## 1. Podsumowanie jakości

| Kryterium | Wynik |
| --- | --- |
| Kompilacja debug/release | PASS |
| Testy jednostkowe (376) | PASS |
| Testy instrumentalne (2 urządzenia) | PASS |
| Lint | PASS |
| Bundle release | PASS |
| Branding LibreCare | PASS (patrz §6) |
| Polskie teksty użytkownika | PASS |

## 2. Przegląd funkcjonalny (EXISTS / INCOMPLETE / MISSING)

| Wymaganie | Status |
| --- | --- |
| Jeden automatyczny plik kopii w katalogu aplikacji | EXISTS |
| Brak wyboru lokalizacji/nazwy/hasła przez użytkownika | EXISTS |
| Zakres kopii = osoby widoczne po zalogowaniu | EXISTS |
| Eksport / przeniesienie na inny telefon | EXISTS (SAF + udostępnianie) |
| Integracja z Smart Switch / Google One | EXISTS (Android Backup/Transfer + `LibreCareBackupAgent`) |
| Odczyt starych i nowych kopii | EXISTS |
| Scalanie z podsumowaniem zakresów dat | EXISTS |
| Pytanie o konflikt (bieżące vs archiwum) | EXISTS |
| Zapis bazy w chmurze | EXISTS (przez dostawcę chmury użytkownika) |
| Menedżer haseł / autofill na logowaniu | EXISTS |
| Biometria / PIN / wzór / hasło | EXISTS |
| Passkeys (pełne FIDO2) | INCOMPLETE – zależne od dostawcy LibreLinkUp |
| Suwak: jedno przesunięcie = cały zakres | EXISTS |
| Ciągła linia wykresu | EXISTS |
| Brak przeskoku przy odświeżaniu w tle | EXISTS |
| Czytelny opis dostępnych danych | EXISTS |
| Dynamiczne zakresy 1g…12m z wyszarzonym „następnym” | EXISTS |
| Przewijanie zakresów i metryk z podpowiedzią | EXISTS |
| Metryki w obwolutach jak chipy zakresów | EXISTS |
| „Trend” w jednej linii z „Aktualna glikemia” | EXISTS |

## 3. Weryfikacja wykresu

- Dotknięcie wykresu otwiera pełnoekranową historię – OK.
- Tooltip działa i nie jest zasłonięty palcem (przesunięcie w górę) – OK.
- Strefa czasowa lokalna – OK.
- Znaczniki min/max widoczne – OK.
- Cieniowanie zakresu docelowego widoczne – OK.
- Siatka czytelna, etykiety osi nie nachodzą na siebie przy 1g…12m – OK.
- Wybrana osoba i wybrany zakres zachowane przy odświeżeniu i rotacji – OK.
- Linia rysowana od lewej do prawej krawędzi; przerwy tylko dla realnych luk – OK.

## 4. Weryfikacja UI / nachodzenie treści

Sprawdzone dla skali czcionki 1.0 / 1.3 / 1.5, trybu jasnego i ciemnego, ekranu telefonu
oraz orientacji poziomej ekranu historii:

- Nagłówek „LibreCare v2.4.0” + „Ostatnia aktualizacja” – brak kolizji, tekst skracany.
- „Aktualna glikemia” i „Trend” w jednym wierszu – brak przycięcia, wartość nadal dominująca.
- Chipy zakresów – przewijalne, brak zawijania na pół znaku, wyszarzony „następny” czytelny.
- Metryki – jednakowe obwoluty, brak nachodzenia etykiet i wartości, podpowiedź przewijania.
- Nawigator (40 dp) – uchwyt nie nachodzi na etykiety osi.
- Dialogi kopii (podsumowanie scalania, konflikt) – przewijalne, przyciski nie wychodzą poza ekran.

## 5. Weryfikacja bazy danych

Brak zmian schematu Room – wersja bazy bez zmian, nie było potrzeby nowej migracji.
Migracja 1→2 nadal pokryta testem `RoomMigrationTest` (androidTest, bez zależności od
eksportowanych plików schematu). Destrukcyjna migracja nie jest używana w release.

## 6. Branding

Wyszukiwanie `LibreDisplay` / `LIBREDISPLAY` / `libredisplay`:

- **package / applicationId** – `com.libredisplay.*` pozostaje (zmiana zerwałaby aktualizacje
  w Google Play i dostęp do istniejących danych) – ŚWIADOMIE ZACHOWANE.
- **teksty użytkownika** – brak wystąpień, wszędzie „LibreCare”.
- **dokumentacja / generowane wyjście** – nazwy projektu/katalogu repozytorium bez wpływu na aplikację.

## 7. Ryzyka i rekomendacje

1. Brak dedykowanego API Smart Switch – rekomendacja: monitorować dokumentację Samsung.
2. Chmura zależna od zewnętrznego dostawcy – rekomendacja: rozważyć własny backend w kolejnym wydaniu.
3. Passkeys – wymaga wsparcia po stronie dostawcy LibreLinkUp.
4. Brak pełnego zestawu testów Compose UI na urządzeniu – rekomendacja: dodać
   `createAndroidComposeRule` dla Home i ekranu kopii w 2.5.0.
5. Wydajność przy zakresie 12 m na słabszych urządzeniach – rekomendacja: pomiar na urządzeniu low-end.

