# FUTURES TAB: Advanced Feature Prototyping

**Status**: Design & Prototyping  
**Release**: Post-DEC-0007 Implementation  
**Purpose**: Test & iterate on advanced features before integrating into main UI  
**Target Users**: Early adopters, doctors, tech-savvy guardians

---

## 1. Overview

The **Futures Tab** is a 4th navigation tab for exploring advanced analytics features and new UI concepts. It serves as:

1. **Prototyping Environment**: Test features without affecting main UI
2. **User Research Hub**: Gather feedback on proposed features
3. **Feature Toggle Panel**: Enable/disable experimental features
4. **Product Roadmap Showcase**: Show what's coming next

**Navigation**:
```
┌─────────────────────────────────────┐
│ 🏠 Główna │ 📊 Analiza │ 🔮 Futures │ ⚙️ Ustawienia │
└─────────────────────────────────────┘
```

---

## 2. Futures Tab Main Screen

### 2.1 Layout Structure

```
┌──────────────────────────────────────────┐
│ 🔮 Futures (Eksperymentalne funkcje)    │ (Header)
├──────────────────────────────────────────┤
│ ⚡ SZYBKI START                          │ (Section 1)
│ Włącz eksperymenty, które nas interesują│
├──────────────────────────────────────────┤
│ ✅ Wyjaśnianie zmian glukozy             │
│ ⚠️  Ryzyko hipoglikemii                  │
│ 📈 Wzorce zmienności                    │
│ ⚫ Analiza posiłków (beta)               │
│ ☆ Tygodniowy raport (beta)              │
│ 👔 Czytnik aktywności (beta)            │
│ 🏆 Odznaki i osiągnięcia (beta)         │
├──────────────────────────────────────────┤
│ 🔧 KONFIGURACJA EKSPERYMENTÓW           │ (Section 2)
│ [Wskaż które funkcje chcesz testować]   │
│ ┌────────────────────────────────────┐  │
│ │ ✅ Spike Explanations               │  │
│ │    "Dlaczego wyskoczył mój cukier?" │  │
│ │    [OTWÓRZ DEMO] [Wyłącz]           │  │
│ ├────────────────────────────────────┤  │
│ │ ⚠️  Hypoglycemia Risk Prediction    │  │
│ │    "Czy powinienem się obawiać?"   │  │
│ │    [OTWÓRZ DEMO] [Wyłącz]           │  │
│ ├────────────────────────────────────┤  │
│ │ 📈 Variability Patterns             │  │
│ │    "Kiedy mam problemy ze zmiennością"  │
│ │    [OTWÓRZ DEMO] [Wyłącz]           │  │
│ ├────────────────────────────────────┤  │
│ │ 🍽️  Meal Response Analysis         │  │
│ │    "Jak reaguję na posiłki?"        │  │
│ │    [OTWÓRZ DEMO] [Wyłącz]           │  │
│ ├────────────────────────────────────┤  │
│ │ 📋 Weekly Report Card              │  │
│ │    "Jak się mam mieć w tym tygodniu?" │  │
│ │    [OTWÓRZ DEMO] [Wyłącz]           │  │
│ ├────────────────────────────────────┤  │
│ │ ⏱️  Sensor Wear Tracking           │  │
│ │    "Czy noszę czujnik konsekwentnie?" │  │
│ │    [OTWÓRZ DEMO] [Wyłącz]           │  │
│ ├────────────────────────────────────┤  │
│ │ 🏆 Achievements & Streaks          │  │
│ │    "Gamifikacja motywacji"         │  │
│ │    [OTWÓRZ DEMO] [Wyłącz]           │  │
│ ├────────────────────────────────────┤  │
│ │ 👥 Multi-Patient Dashboard         │  │
│ │    "Dla lekarzy: widok 50+ pacjentów"  │
│ │    [OTWÓRZ DEMO] [Wyłącz]           │  │
│ ├────────────────────────────────────┤  │
│ │ 📤 Share with Doctor (QR)          │  ��
│ │    "Prześlij raport lekarzowi"     │  │
│ │    [OTWÓRZ DEMO] [Wyłącz]           │  │
│ ├────────────────────────────────────┤  │
│ │ 📲 Personalized Health Insights    │  │
│ │    "Co faktycznie mi się przyda?"   │  │
│ │    [OTWÓRZ DEMO] [Wyłącz]           │  │
│ └────────────────────────────────────┘  │
├──────────────────────────────────────────┤
│ 📧 PODZIEL SIĘ OPINIĄ                   │ (Section 3)
│ Która z tych funkcji jest ci najbardziej │
│ przydatna? Daj nam znać!                │
│ [Wyślij opinię]  [Czytaj roadmapę]      │
│                                          │
│ Wersja: 2.12.0-futures-beta              │
│ Ostatnia aktualizacja: 2026-08-27       │
└──────────────────────────────────────────┘
```

### 2.2 Visual Style

**Colors**:
- Section headers: `AccentTeal` with gradient effect
- Feature cards: `Surface.copy(alpha = 0.3f)` with 1.dp border
- Beta badges: `AccentAmber` with warning icon
- Icons: Emoji or Material Icons (larger, 24.sp)

**Spacing**:
- Top padding: 16.dp
- Section padding: 12.dp
- Card spacing: 8.dp between cards
- Bottom padding: 24.dp

---

## 3. Feature Cards (Individual)

### Template

```
┌────────────────────────────────────┐
│ 🎯 Feature Name                    │ (Icon + Title)
│                                    │
│ "Short description (one line)"     │ (Subtitle)
│                                    │
│ Status: READY / BETA / COMING SOON │ (Badge)
│ Maturity: ████░░░░░░ 40%          │ (Progress bar)
│                                    │
│ [OPEN DEMO] [ENABLE/DISABLE] [🔧]  │ (Action buttons)
└────────────────────────────────────┘
```

---

## 4. Individual Feature Screens (Demos)

### 4.1 Feature 1: Spike Explanations

**File**: `FuturesSpikesScreen.kt`  
**Navigation**: Click [OPEN DEMO] on card

**Layout**:
```
┌────────────────────────────────────────────┐
│ ← Futures / Spike Explanations             │ (Header)
├────────────────────────────────────────────┤
│ 📊 Analiza Spike'ów                         │
│ Naucz się, dlaczego wyskakuje Ci glikemia  │
├────────────────────────────────────────────┤
│ ⏰ Wybierz datę: [2026-08-27 ▼]            │
├────────────────────────────────────────────┤
│ 📈 2026-08-25 15:30 SPIKE: 145 mg/dL      │
│    (↑ +65 od poziomu bazowego)             │
│                                            │
│ 🎯 Możliwe przyczyny (wg prawdopodobieństwa):
│                                            │
│ 1️⃣ POSIŁEK (83% prawdopodobieństwo)        │
│    ├─ Czas: Jadłeś/Jadłaś ~15:00          │
│    ├─ Typowy skok: +60-80 mg/dL           │
│    └─ Rozwiązanie: Spróbuj wziąć insulinę │
│       wcześniej (15 min przed posiłkiem)   │
│                                            │
│ 2️⃣ STRES (12% prawdopodobieństwo)         │
│    ├─ Wskaźnik: Wysoki HR (>100 bpm)     │
│    ├─ Czas: Zgadzało się z pomiarem       │
│    └─ Rozwiązanie: Technika oddychania,   │
│       ćwiczenia relaksacyjne              │
│                                            │
│ 3️⃣ CHOROBA/HORMONY (5% prawdopodobieństwo)│
│    ├─ Dzień cyklu: 18 (wyższa oporność)  │
│    └─ Rozwiązanie: Zaadaptuj insulinę     │
│       w tym dniu miesiąca                 │
│                                            │
│ ✅ ROZWIĄZANIE NA NASTĘPNY RAZ:           │
│    Następnym razem, gdy będziesz mieć    │
│    posiłek, spróbuj wziąć insulinę 15 min│
│    wcześniej niż zwykle.                 │
├────────────────────────────────────────────┤
│ 📚 CZYTAJ WIĘCEJ:                         │
│    - Fenomen świtu (dawn phenomenon)     │
│    - Efekt Somogyi                       │
│    - Insulinooporność menstruacyjna      │
├────────────────────────────────────────────┤
│ [Poprzedni spike] ← → [Następny spike]    │
│ 2026-08-24 18:00      2026-08-26 12:30   │
└────────────────────────────────────────────┘
```

**Interaction**:
- Date picker: Select any date to analyze spikes
- Swipe left/right: Navigate between spikes
- Tap "CZYTAJ WIĘCEJ": Open educational articles
- Enable toggle: Add "Spike Explanations" to home screen

---

### 4.2 Feature 2: Hypoglycemia Risk Prediction

**File**: `FuturesRiskScreen.kt`

**Layout**:
```
┌────────────────────────────────────────────┐
│ ← Futures / Hypoglycemia Risk              │
├────────────────────────────────────────────┤
│ ⚠️  RYZYKO HIPOGLIKEMII                    │
│ Prognoza na kolejne 7 dni                  │
├────────────────────────────────────────────┤
│ RYZYKO: 12% (UMIARKOWANE)                  │
│ 🟡🟡🟡⚪⚪⚪⚪⚪⚪⚪ (3/10)                    │
│                                            │
│ Trend: ↓ Zmniejsza się (lepiej!)          │
│ Ostatnie hipoglikemie: 1 w tym tygodniu   │
│ Średnia: 1.2 na tydzień                    │
│                                            │
│ PIKI RYZYKA:                               │
│ ├─ 03:00-06:00 (noc): Najwyższe ryzyko    │
│ │  └─ Przyczyna: Basal insulin zbyt wysoki│
│ │  └─ Rekomendacja: Zmniejsz basal o 5%   │
│ │  └─ Monitoruj przez 1 tydzień            │
│ │                                           │
│ ├─ 14:00-15:00 (po obiedzie): Średnie     │
│ │  └─ Przyczyna: Brak konsekwencji posiłków
│ │  └─ Rekomendacja: Zjedz o stałej porze  │
│ │                                           │
│ └─ 08:00 (rano): Niskie                   │
│                                            │
│ 🎯 OSTRZE­ŻENIA:                           │
│ ├─ Ostatnie 24h: Trend ↘ (spadek) - UWAŻAJ│
│    Czujnik pokazuje spadek. Monitoruj!    │
│ └─ Wczoraj: 1 hipoglikemia o 04:30         │
│                                            │
│ 📋 DZIAŁANIA:                              │
│ □ Zmniejsz basal insulin w nocy (doradź)  │
│ □ Monitoruj posiłki (zjedz o tej samej    │
│   porze co zwykle)                        │
│ □ Sprawdź czujnik (może być stary?)       │
│ □ Umów się do lekarza                     │
├────────────────────────────────────────────┤
│ 📞 SKONTAKTUJ SIĘ Z LEKARZEM:              │
│ Przed zmianą insuliny zawsze umów się     │
│ u endokrynologa!                          │
│                                            │
│ [Podziel się z lekarzem (QR)]              │
└────────────────────────────────────────────┘
```

**Data Source**:
- Historical lows (last 30 days)
- Current sensor trend
- Time-of-day patterns
- Meal timing consistency

---

### 4.3 Feature 3: Variability Patterns

**File**: `FuturesVariabilityScreen.kt`

**Layout**:
```
┌────────────────────────────────────────────┐
│ ← Futures / Variability Analysis           │
├────────────────────────────────────────────┤
│ 📊 WZORCE ZMIENNOŚCI                       │
│ Kiedy masz problemy ze stabilnością        │
├────────────────────────────────────────────┤
│ CV (Coefficient of Variation): 32%         │
│ Klasyfikacja: UMIARKOWANA ZMIENNOŚĆ        │
│ Trend: Ulepsza się (↓ -2% od zeszłego m.)  │
│                                            │
│ 🕐 ZMIENNOŚĆ WZDŁUŻ DNIA:                  │
│                                            │
│ Rano (06:00-10:00):       CV=45% 🔴 WYSOKA│
│ ├─ Problem: Fenomen świtu zmienia basal   │
│ ├─ Rozwiązanie: Zmniejsz basal o 10% od   │
│ │  04:30 do 07:00                         │
│ └─ Test: Spróbuj przez 3 dni               │
│                                            │
│ Dzień (10:00-18:00):      CV=28% 🟡 DOBRA │
│ ├─ Status: Dość stabilny okres             │
│ ├─ Wskazówka: Trzymaj bieżące schemat     │
│ └─ Zmiany: Nie robić zmian teraz           │
│                                            │
│ Wieczór (18:00-22:00):    CV=35% 🟡 DOBRA │
│ ├─ Problem: Spike'i po kolacji             │
│ ├─ Rozwiązanie: Bierz insulinę wcześniej  │
│ │  (15 min przed posiłkiem)                │
│ └─ Test: Spróbuj 2 posiłki                │
│                                            │
│ Noc (22:00-06:00):        CV=38% 🟡 DOBRA │
│ ├─ Problem: Hipoglikemie ~03:30            │
│ ├─ Rozwiązanie: Zmniejsz basal o 5-10%   │
│ └─ Monitor: Włącz alerty na <70            │
│                                            │
│ 📅 ZMIENNOŚĆ WG. DNI TYGODNIA:             │
│ Poniedziałek:   CV=25% (najlepiej)         │
│ Wtorek:         CV=32% ↗                   │
│ Środa:          CV=31% ↗                   │
│ Czwartek:       CV=35% ↗ (gorzej)          │
│ Piątek:         CV=38% ↗ (gorzej)          │
│ Sobota:         CV=42% 🔴 (najgorzej)      │
│ Niedziela:      CV=40% 🔴                  │
│                                            │
│ Problem: Weekendy + stresu = gorsza kontrola│
│ Rozwiązanie:                               │
│ - Utrzymuj regularny harmonogram posiłków │
│ - Zwiększ ćwiczenia w sobotę-niedzielę    │
│ - Rozważ wyższe czule insuliny weekendzie │
├────────────────────────────────────────────┤
│ 💡 OGÓLNE REKOMENDACJE:                    │
│ 1. Zacznij od poranka (najtrudniejszy)    │
│ 2. Zmieniaj jeden parametr na raz         │
│ 3. Testuj przez minimum 3 dni             │
│ 4. Monitoruj efekty                        │
│                                            │
│ [Podziel się z lekarzem]                   │
└────────────────────────────────────────────┘
```

---

### 4.4 Feature 4: Meal Response Analysis

**File**: `FuturesMealResponseScreen.kt`

**Layout**:
```
┌────────────────────────────────────────────┐
│ ← Futures / Meal Response                  │
├────────────────────────────────────────────┤
│ 🍽️  ANALIZA ODPOWIEDZI NA POSIŁKI          │
│ Jak Twoje ciało reaguje na jedzenie        │
├────────────────────────────────────────────┤
│ ŚREDNIA ODPOWIEDŹ:                         │
│ ├─ Czas do szczytu: 45 minut               │
│ ├─ Wysokość szczytu: +85 mg/dL             │
│ ├─ Powrót do linii bazowej: 2.5 godziny   │
│ └─ Zmienność: ±15 mg/dL (konsekwentne)    │
│                                            │
│ NAJLEPSZE ODPOWIEDZI (niskie skoki):       │
│ Pizza (2026-08-20)      +45 mg/dL ✅       │
│ Makaron pełnoziarnisty  +50 mg/dL ✅       │
│ Kurczak z ryżem (2026-08-22) +55 mg/dL ✅  │
│                                            │
│ NAJGORSZE ODPOWIEDZI (duże skoki):         │
│ Sok pomarańczowy (2026-08-18) +120 mg/dL 🔴│
│ Białe piekarstwo (2026-08-21) +110 mg/dL 🔴│
│ Deser (2026-08-19) +100 mg/dL 🟡           │
│                                            │
│ WNIOSKI:                                   │
│ • Cukry proste (soki) = duże skoki         │
│ • Pełnoziarniste = mniejsze skoki          │
│ • Białko + tłuszcz + węglowodany = lepiej │
│                                            │
│ 💡 CO ZROBIĆ NASTĘPNYM RAZEM:              │
│ 1. Unikaj soków (wyciśnij owoc bezpośrednio)
│ 2. Jedz pełnoziarniste (chleb, ryż)        │
│ 3. Dodaj białko (mięso, jajka)             │
│ 4. Dodaj warzywa (fiber spowala wchłanianie)
│ 5. Bierz insulinę 15 min wcześniej         │
│                                            │
│ 📊 POSIŁKI - SZCZEGÓŁY:                    │
│ ┌─────────────────────────────────┐        │
│ │ Breakfast (średnia):            │        │
│ │ └─ Skok: +75 mg/dL              │        │
│ │ └─ Czas do szczytu: 60 min      │        │
│ │ └─ Powrót do baseline: 2h       │        │
│ ├─────────────────────────────────┤        │
│ │ Lunch (średnia):                │        │
│ │ └─ Skok: +90 mg/dL              │        │
│ │ └─ Czas do szczytu: 45 min      │        │
│ │ └─ Powrót do baseline: 2.5h     │        │
│ ├─────────────────────────────────┤        │
│ │ Dinner (średnia):               │        │
│ │ └─ Skok: +80 mg/dL              │        │
│ │ └─ Czas do szczytu: 50 min      │        │
│ │ └─ Powrót do baseline: 3h       │        │
│ └─────────────────────────────────┘        │
│                                            │
│ [Edytuj moją bazę posiłków]                │
│ [Podziel się z dietetykiem]                │
└────────────────────────────────────────────┘
```

**Requirements**:
- Meal logging integration (if available)
- Match meal timestamps ±5 min to readings
- Calculate spike metrics
- Group by meal type

---

### 4.5 Feature 5: Weekly Report Card

**File**: `FuturesWeeklyReportScreen.kt`

**Layout**:
```
┌────────────────────────────────────────────┐
│ ← Futures / Weekly Report                  │
├────────────────────────────────────────────┤
│ 📋 TYGODNIOWA KARTA POSTĘPÓW              │
│ Poniedziałek 21 — Niedziela 27 sierpnia   │
├────────────────────────────────────────────┤
│ OCENA: A- (Postępujesz świetnie!)          │
│ ★★★★★ (4.2/5)                             │
│ Trend: ↑ Lepiej niż poprzedni tydzień      │
│                                            │
│ 📊 METRYKI:                                │
│ ├─ TIR: 87%        (Cel: 80%)      ✅ +1%  │
│ ├─ Hipoglikemie: 1 (Cel: <2)       ✅      │
│ ├─ Hiperglikemie: 2 (Cel: <3)      ✅      │
│ └─ Stabilność (CV): 8%             ✅      │
│                                            │
│ 🌟 NAJLEPSZE DNI:                          │
│ Poniedziałek TIR: 91% 🏆                    │
│ Wtorek TIR: 89%                            │
│ Czwartek TIR: 88%                          │
│                                            │
│ 💪 NAJLEPSZE DECYZJE:                      │
│ ✅ Konsekwencja posiłków (94% na czasie)   │
│ ✅ Ćwiczenia wtorek-czwartek (3x)          │
│ ✅ Brak pominiętych pomiarów (95% aktywności)
│                                            │
│ ⚠️  CO POPRAWIĆ:                           │
│ ⚠️  Piątek i sobota: Wyższe skoki po 18:00│
│ ⚠️  Jeden skok po śniadaniu (poniedziałek) │
│ ⚠️  Mniej ćwiczeń w weekend                │
│                                            │
│ 💡 REKOMENDACJE NA KOLEJNY TYDZIEŃ:       │
│ 1. Zwróć uwagę na wieczory (piątek-sobota)│
│ 2. Bierz insulinę 15 min wcześniej na obiad
│ 3. Spróbuj ćwiczeń również w weekend      │
│ 4. Utrzymaj konsekwencję posiłków!        │
│                                            │
│ "Idziesz świetnie! Utrzymuj to! ❤️"      │
│                                            │
│ 🎖️  OSIĄGNIĘCIA TEN TYDZIEŃ:               │
│ 🥇 "Konsekwentny" - Same posiłki na czasie│
│ 🥈 "Aktywny" - 3+ ćwiczenia                │
│ 🥉 "Stabilny" - CV <10%                    │
│                                            │
│ 🔥 AKTUALNA SERIA: 5 dni TIR >80%         │
│                                            │
│ [Podziel się w mediach społecznych]        │
│ [Wyślij lekarzowi]                         │
│ [Wydrukuj raport]                          │
└────────────────────────────────────────────┘
```

---

### 4.6 Feature 6: Achievements & Streaks

**File**: `FuturesAchievementsScreen.kt`

**Layout**:
```
┌────────────────────────────────────────────┐
│ ← Futures / Achievements                   │
├────────────────────────────────────────────┤
│ 🏆 OSIĄGNIĘCIA I SERIE                     │
│ Świętuj sukces w zarządzaniu cukrzycą      │
├────────────────────────────────────────────┤
│ 🔥 AKTUALNA SERIA:                         │
│ 🔥 23 dni TIR >80% TIR                     │
│ ╔════════════════════════════════════╗    │
│ ║ TYP: Ci blisko, nie poddawaj się! ║    │
│ ╚════════════════════════════════════╝    │
│ Poprzednia seria: 15 dni                  │
│ Rekord: 45 dni (2026-07)                  │
│                                            │
│ 🥇 NIEBLOKOWANE ODZNAKI:                   │
│                                            │
│ ┌──────────────┐ ┌──────────────┐         │
│ │ 🌟 Stabilny  │ │ 💪 Aktywny   │         │
│ │ TIR >75%     │ │ 3+ ćwiczenia │         │
│ │ przez 7 dni  │ │ w tygodniu   │         │
│ │ Odblokowano: │ │ Odblokowano: │         │
│ │ 2026-08-15   │ │ 2026-08-20   │         │
│ └──────────────┘ └──────────────┘         │
│                                            │
│ ┌──────────────┐ ┌──────────────┐         │
│ │ 🎯 Konsekwent│ │ 🌙 Noc bez   │         │
│ │ Posiłki      │ │ Hipoglikemii │         │
│ │ na czasie     │ │ 14 dni       │         │
│ │ Odblokowano: │ │ Odblokowano: │         │
│ │ 2026-08-22   │ │ 2026-08-26   │         │
│ └──────────────┘ └──────────────┘         │
│                                            │
│ ☆ ZABLOKOWANE ODZNAKI (wkrótce!):        │
│                                            │
│ ┌──────────────┐ Wymaga:                   │
│ │ 🌈 Perfekcja │ TIR 100% przez 1 dzień  │
│ │ TIR 100%     │ Postęp: ███░░░░░░░░ 30% │
│ │ przez 1 dzień│                          │
│ └──────────────┘                          │
│                                            │
│ ┌──────────────┐ Wymaga:                   │
│ │ ⏰ Wczesny   │ 7 dni bez hipoglikemii   │
│ │ Ptak         │ Postęp: ██████░░░░░ 57% │
│ │ Rano TIR >85%│                          │
│ └──────────────┘                          │
│                                            │
│ ┌──────────────┐ Wymaga:                   │
│ │ 🎬 Zagniazd │ Pobiegnij/Przejdź 10km   │
│ │ 10km ćwiczeń │ Postęp: ████░░░░░░░░ 40%│
│ │ w miesiącu   │                          │
│ └──────────────┘                          │
│                                            │
│ [Zarządzaj celami] [Szuka motywacji?]     │
└────────────────────────────────────────────┘
```

---

### 4.7 Feature 7: Sensor Wear Tracking

**File**: `FuturesSensorWearScreen.kt`

**Layout**:
```
┌────────────────────────────────────────────┐
│ ← Futures / Sensor Wear                    │
├────────────────────────────────────────────┤
│ ⏱️  ANALIZA NOSZENIA CZUJNIKA               │
│ Czy konsekwentnie noszę czujnik?           │
├────────────────────────────────────────────┤
│ AKTYWNOŚĆ CZUJNIKA: 87% (DOBRA)            │
│ ███████░░░ Oczekiwane: 95%+                │
│                                            │
│ Oczekiwane odczyty: 1440/tydzień (10/h)   │
│ Rzeczywiste odczyty: 1252/tydzień (87%)   │
│ Brakujące odczyty: 188 (13%)              │
│                                            │
│ 📊 LUKI W DANYCH:                          │
│                                            │
│ 2026-08-24 22:00-06:00 (8h) 🛏️ NIGHT REMOVAL
│ └─ Typ: Normalny (spodziewany)            │
│ └─ Zalecenie: Nie pobierz podczas kąpieli│
│                                            │
│ 2026-08-22 14:30-15:30 (1h) ⚠️ SENSOR FAILURE
│ └─ Typ: Możliwa awaria                    │
│ └─ Zalecenie: Wymień czujnik, jeśli utrzymuje się
│                                            │
│ 2026-08-20 18:00-21:00 (3h) ❓ UNKNOWN    │
│ └─ Typ: Nieznana przyczyna                │
│ └─ Zalecenie: Zaznacz godzinę aktywności │
│                                            │
│ 📅 LUKI WG DNIA:                           │
│ Poniedziałek:   95% (najlepiej)            │
│ Wtorek:         92%                        │
│ Środa:          88%                        │
│ Czwartek:       85%                        │
│ Piątek:         84%                        │
│ Sobota:         78% (gorzej - weekend?)    │
│ Niedziela:      82% (gorzej - weekend?)    │
│                                            │
│ 💡 REKOMENDACJE:                           │
│ 1. Noś czujnik w poniedziałki-piątki (85-95%)
│ 2. W weekend: Włącz alerty o zdjęciu      │
│ 3. Jeśli <80% aktywności: Wymień czujnik  │
│ 4. Log aktywności (sport, kąpiel)         │
│                                            │
│ [Zaznacz aktywność dzisiaj]                │
│ [Podziel się z lekarzem]                   │
└────────────────────────────────────────────┘
```

---

### 4.8 Feature 8: Share with Doctor (QR)

**File**: `FuturesShareScreen.kt`

**Layout**:
```
┌────────────────────────────────────────────┐
│ ← Futures / Share Report                   │
├────────────────────────────────────────────┤
│ 📤 PRZEŚLIJ RAPORT LEKARZOWI               │
│ Szybka i bezpieczna wymiana danych         │
├────────────────────────────────────────────┤
│ KOK SKANOWANIA:                             │
│ ┌──────────────────────────────┐           │
│ │   ┌──────────────────────┐   │           │
│ │   │                      │   │           │
│ │   │   █████████████      │   │           │
│ │   │   █░░░░░░░░░░░█      │   │           │
│ │   │   █░ QR CODE █      │   │           │
│ │   │   █░░░░░░░░░░░█      │   │           │
│ │   │   █████████████      │   │           │
│ │   │                      │   │           │
│ │   └──────────────────────┘   │           │
│ └──────────────────────────────┘           │
│                                            │
│ Ważność kodu: 30 dni                       │
│ Wygasa: 2026-09-26                         │
│                                            │
│ 📋 CO ZAWIERA RAPORT:                      │
│ ✅ Dane osobowe pacjenta                   │
│ ✅ Zakres danych (ostatnie 30 dni)         │
│ ✅ Podsumowanie metryk (TIR, episody)      │
│ ✅ Główne obserwacje                       │
│ ✅ Wykresy (profil dobowy)                 │
│ ✅ Notatka od pacjenta (opcjonalnie)       │
│ ❌ Pełne szczegóły wszystkich pomiarów    │
│    (pacjent musi kliknąć "Pełny dostęp")  │
│                                            │
│ 🔒 BEZPIECZEŃSTWO:                         │
│ • Kod QR ważny tylko 30 dni                │
│ • Podatny musi wyrazić zgodę na każdy kod │
│ • Lekarz widzi tylko to, co pacjent wybierze
│ • Zgodne z RODO/GDPR                       │
│                                            │
│ 📝 DODAJ NOTATKĘ (opcjonalnie):             │
│ ┌──────────────────────────────┐           │
│ │ Mam problemy z wieczorem      │           │
│ │ i nocą. Proszę o pomoc       │           │
│ │ w dostosowaniu insuliny.      │           │
│ └──────────────────────────────┘           │
│                                            │
│ [WYGENERUJ KOD QR]                         │
│                                            │
│ INSTRUKCJA DLA LEKARZA:                    │
│ 1. Lekarz otwiera aplikację (webowa)       │
│ 2. Klika "Skanuj kod pacjenta"             │
│ 3. Skanuje kod QR                          │
│ 4. Widzi raport pacjenta                   │
│ 5. Może zobaczyć pełne dane (jeśli zgodnie)│
│                                            │
│ [Spróbuj ScannerApp]                       │
└────────────────────────────────────────────┘
```

---

## 5. Settings/Preferences for Futures Tab

**File**: `FuturesPreferencesScreen.kt`

```
┌────────────────────────────────────────────┐
│ ← Futures / Ustawienia                     │
├────────────────────────────────────────────┤
│ 🔧 USTAWIENIA EKSPERYMENTÓW                │
├────────────────────────────────────────────┤
│ OGÓLNE:                                    │
│ □ Włącz wszystkie eksperymentalne funkcje │
│ □ Pokaż powiadomienia o nowych funkcjach  │
│ □ Bierz udział w badaniach (anonimowo)    │
│                                            │
│ KTÓRE FUNKCJE TESTOWAĆ:                    │
│ ✅ Spike Explanations                      │
│ ✅ Hypoglycemia Risk Prediction            │
│ ✅ Variability Patterns                    │
│ ⚫ Meal Response (wymaga integracji)       │
│ ✅ Weekly Report                           │
│ ✅ Achievements                            │
│ ✅ Sensor Wear Tracking                    │
│ ⚫ Share with Doctor (wymaga backenduu)    │
│ ⚫ Multi-Patient Dashboard (lekarze)       │
│ ⚫ Personalized Insights (ML)              │
│                                            │
│ DOSTĘP:                                    │
│ □ Pokazuj wszystkie funkcje w głównym ekranie
│ □ Dodaj shortcut do nowych funkcji         │
│                                            │
│ PRYWATNOŚĆ:                                │
│ ✅ Przetwarzaj wszystko lokalnie          │
│ ✅ Nie wysyłaj danych do chmury            │
│ □ Wyślij anonimowe metryki użytkowania    │
│                                            │
│ [Resetuj preferencje]                      │
└────────────────────────────────────────────┘
```

---

## 6. Feedback & Roadmap Section

**File**: `FuturesFeedbackScreen.kt`

```
┌────────────────────────────────────────────┐
│ ← Futures / Opinia                         │
├────────────────────────────────────────────┤
│ 📧 PODZIEL SIĘ OPINIĄ                      │
│ Która funkcja jest Ci najbardziej przydatna?│
├────────────────────────────────────────────┤
│ Najczęstsze pytania:                       │
│                                            │
│ Q: Czy moje dane są bezpieczne?           │
│ A: Tak! Wszystko przetwarzamy lokalnie.    │
│    Nic nie wysyłamy do chmury.            │
│                                            │
│ Q: Czy mogę wyłączyć te funkcje?          │
│ A: Tak! Przejdź do Ustawień i wyłącz.     │
│                                            │
│ Q: Kiedy pojawi się na głównym ekranie?   │
│ A: Po zebraniu opinii (4-6 tygodni).      │
│                                            │
│ Q: Czy mogę edytować notatki/metryki?    │
│ A: Wkrótce! To jest w planie na Q4 2026.  │
│                                            │
│ 📊 ROADMAPA (Co dalej?):                  │
│                                            │
│ ✅ Spike Explanations (już gotowe)        │
│ ✅ Hypoglycemia Risk (już gotowe)         │
│ ✅ Variability Patterns (już gotowe)      │
│ 🔄 Meal Response (testowanie - sep 2026)  │
│ 🔄 Weekly Report (testowanie - sep 2026)  │
│ 🔄 Achievements (testowanie - paź 2026)  │
│ ⏳ Share with Doctor (implementacja)       │
│ ⏳ Multi-Patient Dashboard (Q1 2027)       │
│ ⏳ AI Spike Explanation (Q1 2027)         │
│ ⏳ Meal Logging Integration (Q1 2027)     │
│                                            │
│ [Wyślij opinię] [Sugeruj nową funkcję]   │
│ [Czytaj blog o przyszłości LibreDisplay]  │
│                                            │
│ Dziękujemy za testowanie! ❤️               │
└────────────────────────────────────────────┘
```

---

## 7. Navigation Structure

```
AppScreen.Futures (New top-level destination)
├─ FuturesMainScreen (default)
│  ├─ → FuturesSpikesScreen
│  ├─ → FuturesRiskScreen
│  ├─ → FuturesVariabilityScreen
│  ├─ → FuturesMealResponseScreen
│  ├─ → FuturesWeeklyReportScreen
│  ├─ → FuturesSensorWearScreen
│  ├─ → FuturesAchievementsScreen
│  ├─ → FuturesShareScreen
│  ├─ → FuturesPreferencesScreen
│  └─ → FuturesFeedbackScreen
└─ (Back to Monitoring/Analytics/Settings)
```

---

## 8. Implementation Phases

### Phase 1: Setup (1-2 days)
- [ ] Add `AppScreen.Futures` to enum
- [ ] Add `DashboardNavItem.FUTURES` to navigation
- [ ] Create `FuturesMainScreen.kt` scaffold
- [ ] Add to `MainActivitynavigation` switch statement

### Phase 2: Feature Cards (1-2 days)
- [ ] Create `FutureFeatureCard.kt` composable
- [ ] Build main screen with all cards
- [ ] Implement enable/disable toggles
- [ ] Save preferences to ViewModel

### Phase 3: Individual Feature Screens (3-5 days)
- [ ] `FuturesSpikesScreen.kt`
- [ ] `FuturesRiskScreen.kt`
- [ ] `FuturesVariabilityScreen.kt`
- [ ] `FuturesMealResponseScreen.kt`
- [ ] `FuturesWeeklyReportScreen.kt`
- [ ] `FuturesSensorWearScreen.kt`
- [ ] `FuturesAchievementsScreen.kt`
- [ ] `FuturesShareScreen.kt`

### Phase 4: Settings & Feedback (1-2 days)
- [ ] `FuturesPreferencesScreen.kt`
- [ ] `FuturesFeedbackScreen.kt`
- [ ] Email feedback integration

### Phase 5: Testing & Polish (2-3 days)
- [ ] UI tests
- [ ] Manual testing
- [ ] Performance optimization
- [ ] Dark theme verification

---

## 9. Data Flow

```
FuturesViewModel:
├─ observeSelectedFeatures()  ← Enable/disable toggles
├─ loadSpikes()               ← For spike explanation screen
├─ loadRiskData()             ← For risk prediction screen
├─ loadVariabilityData()      ← For variability screen
├─ loadWeeklyReport()         ← For weekly report screen
└─ loadFeedback()             ← Track user preferences
```

---

## 10. Success Criteria

- [ ] All 8 feature screens fully functional
- [ ] Navigation smooth and intuitive
- [ ] Enable/disable toggles work correctly
- [ ] Preferences persist after app restart
- [ ] Dark theme visually appealing
- [ ] No performance issues on old devices
- [ ] Clear explanations for each feature
- [ ] Feedback collection working
- [ ] Users can understand "coming soon" vs "beta" vs "ready"

---

**Document Version**: 1.0  
**Status**: Design Complete - Ready for Implementation  
**Next Steps**: Implementation Phase 1 (Setup)

