# LibreCare — Zweryfikowane Możliwości

**Wygenerowano z:** Udanych przebiegów testów (wynik PASS)

Te możliwości zostały przetestowane i potwierdzone jako działające.
Możliwość widoczna tutaj NIE tworzy automatycznie nowego wymagania funkcjonalnego.
Zweryfikowane możliwości śledzą odkrytą wartość produktu i wspierają planowanie roadmapy.

## TESTRUN-2026-001

**Możliwość:** Opiekun — ocena obecnej sytuacji glikemii

**Osoba:** opiekun (caregiver)

**Moduł:** Strona główna / Monitorowanie

**Data walidacji:** 2026-08-26

**Wersja aplikacji:** 2.11.1

### Co zostało zweryfikowane:

- Bieżący status glikemii był łatwo dostępny
- Przełączanie się między profilami obserwowanych osób zostało odkryte bez instrukcji
- Agent ocenił więcej niż jedną obserwowaną osobę
- Test nie wymagał wskazówek nawigacyjnych

### Co NIE zostało zweryfikowane:

- Brak odnotowanych ograniczeń w tym teście
- Zachowanie opiekuna w sytuacjach urgentnych (np. gdy jedna osoba ma zagrażającą glikemię) — nie testowane

**Pewność:** WYSOKA

**Uzasadnienie:** Istniejąca możliwość potwierdzona w teście „Opiekun — ocena obecnej sytuacji glikemii"

---

## TESTRUN-2026-002

**Możliwość:** Senior — zrozumieć moją obecną glikemię

**Osoba:** senior

**Moduł:** Strona główna / Monitorowanie

**Data walidacji:** 2026-08-26

**Wersja aplikacji:** 2.11.1

### Co zostało zweryfikowane:

- Agent wyraźnie zidentyfikował: glikemię 116 mg/dL
- Agent zidentyfikował trend: wzrost
- Agent zidentyfikował status: glikemia w normie
- Agent zidentyfikował świeżość danych / czas ostatniej aktualizacji
- Wszystkie wymagane informacje znaleziono bez nawigacji do innego modułu

### Co NIE zostało zweryfikowane:

- Brak odnotowanych ograniczeń w tym teście
- Zachowanie seniora podczas niskich wartości glikemii — nie testowane
- Zrozumienie seniora, gdy dane są nieświeże (>30 min) — nie testowane
- Kontekst przechwytywany po znaczących zdarzeniach — nie testowany

**Pewność:** WYSOKA

**Uzasadnienie:** Istniejąca możliwość potwierdzona w teście „Senior — zrozumieć moją obecną glikemię"

---

## Zastosowanie

Ta lista jest używana do:

1. Śledzenia potwierdzonej wartości produktu dla ciągłości roadmapy
2. Unikania tworzenia zduplikowanych wymagań funkcjonalnych dla działających możliwości
3. Identyfikacji miejsc, gdzie potrzebne jest dodatkowe testowanie (co NIE zostało zweryfikowane)
4. Wspierania dalszych badań (np. czy nieświeże dane naprawdę mylą seniorów?)

---

## Dalsze badania

Każda zweryfikowana możliwość sugeruje obszary dalszych badań:

- **Opiekun z jedną osobą wysokiego ryzyka wśród wielu:** Jak reaguje opiekun, gdy tylko JEDNA osoba ma zagrażającą glikemię?
- **Komunikacja danych nieświeżych:** Czy nieświeże dane rzeczywiście mylą seniorów? Jaki jest próg?
- **Senior podczas hipozy/szybkiego spadku:** Co senior robi, gdy glikemia szybko spada?
- **Przechwytywanie kontekstu:** Czy użytkownicy czerpią korzyści z przechwytywania kontekstu po zdarzeniach?
- **Diagnoza klinicysty przy różnorodnych wzorach glikemii:** Czy klinicy mogą ufać metrykom dla rozmaitych wzorów glikemii?

