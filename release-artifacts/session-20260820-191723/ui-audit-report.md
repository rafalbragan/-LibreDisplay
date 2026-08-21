# LibreCare UI Audit Report

- Wersja aplikacji: 2.1.0
- Data wygenerowania: 2026-08-20 19:17:36
- Liczba krokow: 13

## Zrzuty ekranow

1. [OK] Główny
   - Sciezka ekranu: Główny
   - Plik: 01_glowny.png
   - Metadane: 01_glowny.json
   - Zachowania do weryfikacji:
     - czytelność aktualnej glikemii
     - gęstość sekcji Home
     - widoczność alertów

2. [OK] Główny -> Historia
   - Sciezka ekranu: Główny -> Historia
   - Plik: 02_glowny---historia.png
   - Metadane: 02_glowny---historia.json
   - Zachowania do weryfikacji:
     - czy wykres zaczyna się wysoko
     - spójność nagłówka
     - kompaktowość statystyk

3. [OK] Główny -> Ustawienia
   - Sciezka ekranu: Główny -> Ustawienia
   - Plik: 03_glowny---ustawienia.png
   - Metadane: 03_glowny---ustawienia.json
   - Zachowania do weryfikacji:
     - hierarchia sekcji
     - czy login/hasło nie są na górze
     - liczba wierszy na viewport

4. [OK] Ustawienia -> Monitorowanie -> Zakres
   - Sciezka ekranu: Główny -> Ustawienia -> Monitorowanie -> Zakres
   - Plik: 04_ustawienia---monitorowanie---zakres.png
   - Metadane: 04_ustawienia---monitorowanie---zakres.json
   - Zachowania do weryfikacji:
     - czytelność pól zakresu
     - zwartość layoutu
     - ergonomia edycji

5. [OK] Ustawienia -> Monitorowanie -> Metryki
   - Sciezka ekranu: Główny -> Ustawienia -> Monitorowanie -> Metryki
   - Plik: 05_ustawienia---monitorowanie---metryki.png
   - Metadane: 05_ustawienia---monitorowanie---metryki.json
   - Zachowania do weryfikacji:
     - kolejność metryk
     - zrozumiałość opisów
     - dotyk i odstępy

6. [OK] Ustawienia -> Monitorowanie -> HbA1c
   - Sciezka ekranu: Główny -> Ustawienia -> Monitorowanie -> HbA1c
   - Plik: 06_ustawienia---monitorowanie---hba1c.png
   - Metadane: 06_ustawienia---monitorowanie---hba1c.json
   - Zachowania do weryfikacji:
     - czytelność pól HbA1c
     - obsługa pustych danych
     - gęstość informacji

7. [OK] Ustawienia -> LibreLinkUp
   - Sciezka ekranu: Główny -> Ustawienia -> Konto i połączenie -> LibreLinkUp
   - Plik: 07_ustawienia---librelinkup.png
   - Metadane: 07_ustawienia---librelinkup.json
   - Zachowania do weryfikacji:
     - czy hasło nie jest stale pokazane
     - status konta
     - akcje zmiany konta

8. [OK] Ustawienia -> Prywatność i dane
   - Sciezka ekranu: Główny -> Ustawienia -> Dane i prywatność
   - Plik: 08_ustawienia---prywatnosc-i-dane.png
   - Metadane: 08_ustawienia---prywatnosc-i-dane.json
   - Zachowania do weryfikacji:
     - jasność akcji prywatności
     - ryzykowne akcje
     - nazewnictwo

9. [OK] Ustawienia -> Retencja
   - Sciezka ekranu: Główny -> Ustawienia -> Dane i prywatność -> Retencja
   - Plik: 09_ustawienia---retencja.png
   - Metadane: 09_ustawienia---retencja.json
   - Zachowania do weryfikacji:
     - konsekwencje retencji
     - czytelność opcji czasu
     - ostrzeżenia

10. [OK] Ustawienia -> Synchronizacja
   - Sciezka ekranu: Główny -> Ustawienia -> Monitorowanie -> Synchronizacja
   - Plik: 10_ustawienia---synchronizacja.png
   - Metadane: 10_ustawienia---synchronizacja.json
   - Zachowania do weryfikacji:
     - zrozumiałość wpływu na baterię
     - zakres opcji
     - opisy transferu

11. [OK] Ustawienia -> O aplikacji
   - Sciezka ekranu: Główny -> Ustawienia -> Aplikacja -> O aplikacji
   - Plik: 11_ustawienia---o-aplikacji.png
   - Metadane: 11_ustawienia---o-aplikacji.json
   - Zachowania do weryfikacji:
     - dane wersji
     - czytelność informacji
     - spójność stylu

12. [OK] Ustawienia -> Zaawansowane -> Diagnostyka
   - Sciezka ekranu: Główny -> Ustawienia -> Zaawansowane -> Diagnostyka
   - Plik: 12_ustawienia---zaawansowane---diagnostyka.png
   - Metadane: 12_ustawienia---zaawansowane---diagnostyka.json
   - Zachowania do weryfikacji:
     - narzędzia logów
     - bezpieczeństwo danych
     - czy akcje są czytelne

13. [OK] Ustawienia -> Statystyki
   - Sciezka ekranu: Główny -> Ustawienia -> Statystyki
   - Plik: 13_ustawienia---statystyki.png
   - Metadane: 13_ustawienia---statystyki.json
   - Zachowania do weryfikacji:
     - użyteczność metryk
     - braki danych
     - czytelność liczb

## Globalna checklista UX (optymalizacje)
- Gestosc informacji: czy sekcje nie marnuja pionowej przestrzeni?
- Hierarchia: czy najwazniejsze dane sa najwyzej i bez duplikacji?
- Spacing: czy nie ma nadmiarowych Spacer/padding 24dp+?
- Nawigacja: czy kazdy przycisk prowadzi do oczekiwanego celu?
- Komunikaty pustego stanu: czy sa precyzyjne i pomocne?
- Accessibility: kontrast, dotyk 48dp+, czytelna typografia.

## Uwagi
- Raport jest generowany automatycznie z aktywnej sesji aplikacji.
- Do kazdego zrzutu dopisywany jest plik JSON z metadanymi urzadzenia, rozdzielczoscia i ustawieniami aplikacji.
- captureMode=viewport_png oznacza aktualnie zrzut widocznego viewportu podczas audytu.
