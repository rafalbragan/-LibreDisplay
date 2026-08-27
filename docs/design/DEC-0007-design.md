# DEC-0007: Analysis Screen Enhancements - Design Specification

**Status**: Design Phase  
**Target Implementation**: Post-Futures Tab Prototyping  
**Component**: Analytics Screen Enhancement  
**Date**: 2026-08-27

---

## 1. Overview

Enhance the existing Analysis Screen to support flexible analysis periods (1-90 days) instead of hardcoded 14-day overlay, improve metric readability, and enhance chart visualization.

**Key Changes**:
- ✅ Increase observations font size
- ✅ Support variable analysis period (1-90 days)
- ✅ Add navigation controls (day/week/month)
- ✅ Make metrics table sticky on scroll
- ✅ Enhance average line visibility
- ✅ Bind chart to selected analysis period

---

## 2. Screen Layout Design

### Current Layout (Baseline)
```
┌─────────────────────────────────────┐
│ ← Analiza          [Person Switcher] │ (Top Bar)
├─────────────────────────────────────┤
│ ◉ Ostatnie 14 dni                   │ (Period Tabs)
│ ○ 7 dni ○ 30 dni ○ Własny           │
├─────────────────────────────────────┤
│ Profil dobowy (nakładka 14 dni)     │ (Title)
│ [CHART with overlay lines & avg]    │ (Overlay Chart)
├─────────────────────────────────────┤
│ • Obserwacja 1                       │ (Observations - currently 12.sp)
│ • Obserwacja 2                       │
├─────────────────────────────────────┤
│ Zakres własny: 2026-07-01 — —       │ (Custom Range)
│ [Ustaw zakres własny (od-do)]       │ (Date Picker Button)
├─────────────────────────────────────┤
│ TIR  │ 92%  │ 88%  │ 85%  │ 80% │   │ (Metrics Table - scrollable)
│ Pod. │ 2%   │ 4%   │ 7%   │ 10% │   │
│ Pow. │ 6%   │ 8%   │ 8%   │ 10% │   │
├─────────────────────────────────────┤
│ [Eksportuj surowe dane do Excela]   │
└─────────────────────────────────────┘
```

### Enhanced Layout (DEC-0007)
```
┌─────────────────────────────────────┐
│ ← Analiza          [Person Switcher] │ (Top Bar - unchanged)
├─────────────────────────────────────┤
│ ◉ Ostatnie 14 dni ○ 7 dni ○ 30 dni  │ (Period Tabs - unchanged)
│   ○ Własny                          │
├─────────────────────────────────────┤
│ 📊 WIZUALIZACJA ZAKRESU             │ (NEW: Period Control Section)
│ Liczba dni: [30 ▼]                 │
│ ◀◀ Miesiąc | ◀ Tydzień | Dzisiaj   │ (NEW: Navigation buttons)
│ | Tydzień ▶ | Miesiąc ▶▶            │
│ Zakres: 2026-08-01 — 2026-08-31     │
├─────────────────────────────────────┤
│ Profil dobowy (nakładka 30 dni)     │ (UPDATED: Dynamic title)
│ [CHART with overlay lines & avg]    │ (Enhanced: Thicker avg line)
├─────────────────────────────────────┤
│ ⭐ WNIOSKI & OBSERWACJE             │ (NEW: Section header)
│ • Obserwacja 1                      │ (ENHANCED: 14-15.sp, bold header)
│ • Obserwacja 2                      │
│ • Obserwacja 3                      │
├─────────────────────────────────────┤
│ Zakres własny: 2026-08-01 — 31      │ (Preserved)
│ [Ustaw zakres własny (od-do)]       │
├─────────────────────────────────────┤
│ METRYKI (Sticky Header)             │ (NEW: Sticky section)
│ ┌─────────────────────────────────┐ │
│ │ Metryka │ 1g │ 3g │ 6g │ 24g │  │ (NEW: Fixed left column)
│ ├─────────────────────────────────┤ │ (Header sticky on scroll)
│ │ TIR     │92% │88% │85% │80%  │◀─┼─ (Scrollable right)
│ │ Poniżej │ 2% │ 4% │ 7% │10%  │  │
│ │ Powyżej │ 6% │ 8% │ 8% │10%  │  │
│ │ Średnia │125 │132 │135│140  │  │
│ └─────────────────────────────────┘ │
├─────────────────────────────────────┤
│ [Eksportuj surowe dane do Excela]   │
└─────────────────────────────────────┘
```

---

## 3. Component Specifications

### 3.1 Period Control Section (NEW)

**Location**: Above "Profil dobowy" chart  
**Height**: ~140dp  
**Background**: `LibreCareColors.Surface.copy(alpha = 0.15f)`  
**Padding**: 12.dp all sides

**Components**:

#### 3.1.1 Section Header
- Text: "📊 WIZUALIZACJA ZAKRESU" or "📊 ANALIZA OKRESU"
- Font size: 12.sp (bold)
- Color: `LibreCareColors.TextSecondary`
- Margin bottom: 8.dp

#### 3.1.2 Period Input Field
```
┌──────────────────────────────────┐
│ Liczba dni: [30 ▼]               │
└──────────────────────────────────┘
```
- **Layout**: Row with label and input
- **Label**: "Liczba dni:" (Font: 11.sp, color: TextSecondary)
- **Input Field**: 
  - Type: `OutlinedTextField` or custom number input
  - Width: 80.dp
  - Height: 40.dp
  - Input type: Numbers only (1-90)
  - Border: `LibreCareColors.Surface`
  - Value displayed: Current selected days (e.g., "30")
  - Validation:
    - Min: 1
    - Max: 90
    - If invalid: Show error toast "Wpisz liczbę od 1 do 90"
    - If > available data: Show "Dostępnych danych: XX dni"
- **Keyboard**: Numeric, auto-dismiss on confirm
- **Interaction**: 
  - On change: Debounce 500ms, then recalculate overlay
  - On focus: Show helper text "Wybierz liczbę dni (1-90)"

**ViewModel Integration**:
```kotlin
state.analysisPeriodDays: Int  // Store current selection
fun onAnalysisPeriodChanged(days: Int)  // Trigger recalculation
```

#### 3.1.3 Navigation Buttons (Row)
```
┌──────────────────────────────────────────────┐
│ ◀◀ Miesiąc │ ◀ Tydzień │ Dzisiaj │ Tydzień ▶ │
│                                   │ Miesiąc ▶▶ │
└──────────────────────────────────────────────┘
```

**Layout**: Wrap/Flow (if >4 items, split to 2 rows)

**Buttons**:
| Button | Action | Keyboard | Icon |
|--------|--------|----------|------|
| ◀◀ Miesiąc wstecz | Shift start by -30 days | Ctrl+Left | `⏮️` |
| ◀ Tydzień wstecz | Shift start by -7 days | Alt+Left | `◀` |
| Dzisiaj | End = now, start adjusted for period | Home | `⏱️` |
| Tydzień naprzód ▶ | Shift start by +7 days | Alt+Right | `▶` |
| Miesiąc naprzód ▶▶ | Shift start by +30 days | Ctrl+Right | `⏭️` |

**Button Style**:
- Type: `OutlinedButton` (not filled)
- Height: 36.dp
- Width: auto (wrap text)
- Font size: 11.sp
- Spacing between: 6.dp
- Color scheme:
  - Enabled: `LibreCareColors.AccentTeal` text, `Surface` border
  - Disabled: `TextSecondary.copy(alpha=0.3f)` text, border
- Ripple: Standard material ripple

**Disabled State Logic**:
```kotlin
canScrollBackward = (analysisStartInstant > oldestReadingInstant)
canScrollForward = (analysisEndInstant < now)
```

**ViewModel Integration**:
```kotlin
fun onNavigateAnalysisPeriod(offset: Duration)  // +7d, -30d, etc.
```

#### 3.1.4 Current Range Display
```
Zakres: 2026-08-01 — 2026-08-31 (31 dni)
```
- Font size: 10.sp
- Color: `LibreCareColors.TextSecondary`
- Format: `yyyy-MM-dd — yyyy-MM-dd (N dni)`
- Margin top: 6.dp

**ViewModel Integration**:
```kotlin
state.analysisStartDate: LocalDate
state.analysisEndDate: LocalDate
```

---

### 3.2 Chart Title (UPDATED)

**Current**: "Profil dobowy (nakładka 14 dni)"  
**New**: "Profil dobowy (nakładka {N} dni)"

**Implementation**:
```kotlin
Text(
    "Profil dobowy (nakładka ${state.analysisPeriodDays} dni)",
    color = LibreCareColors.TextPrimary,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp
)
```

---

### 3.3 Observations Section (ENHANCED)

**Current**:
```kotlin
Text("Obserwacje", ..., fontSize = 13.sp)
state.trendObservations.forEach { obs ->
    Text("• $obs", ..., fontSize = 12.sp)
}
```

**Enhanced**:
```kotlin
Column(..., padding = 12.dp) {
    Text(
        "⭐ WNIOSKI & OBSERWACJE",  // NEW: Header with icon
        color = LibreCareColors.TextPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp                    // Increased from 13.sp
    )
    Spacer(8.dp)
    state.trendObservations.forEach { obs ->
        Text(
            "• $obs",
            color = LibreCareColors.TextSecondary,
            fontSize = 14.sp,               // INCREASED from 12.sp
            lineHeight = 18.sp              // NEW: Better spacing
        )
    }
}
```

**Background**: `LibreCareColors.Surface.copy(alpha = 0.25f)`  
**Border**: `RoundedCornerShape(10.dp)`  
**Spacing**: 8.dp between observations

**Testing**:
- [ ] Font readable on 4.5" phone (small text)
- [ ] Font readable on 6.5" phone
- [ ] No line wrapping issues in Polish
- [ ] Dark theme contrast ≥ 4.5:1 (WCAG AA)

---

### 3.4 Metrics Table (STICKY COLUMN)

**Current Layout** (problematic):
```
Row(horizontalScroll) {
    Column(label) { "TIR", "Poniżej", "Powyżej", ... }
    Column(1g) { "92%", "2%", "6%", ... }
    Column(3g) { "88%", "4%", "8%", ... }  ← When scrolling, labels disappear
    Column(6g) { "85%", "7%", "8%", ... }
}
```

**New Layout** (sticky):
```
Row {
    Column(sticky, width=80dp) {        ← Never scrolls
        "TIR"
        "Poniżej"
        "Powyżej"
        ...
    }
    Row(horizontalScroll) {             ← Scrollable
        Column(1g) { "92%", "2%", "6%", ... }
        Column(3g) { "88%", "4%", "8%", ... }
        Column(6g) { "85%", "7%", "8%", ... }
        Column(24g) { "80%", "10%", "10%", ... }
    }
}
```

**Implementation Approach**:
```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .background(LibreCareColors.Surface.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
        .padding(8.dp)
) {
    Row {
        // STICKY LEFT COLUMN
        Column(
            modifier = Modifier
                .width(80.dp)
                .align(Alignment.Top)
        ) {
            rows.forEach { (label, _) ->
                Text(
                    label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LibreCareColors.TextSecondary,
                    modifier = Modifier
                        .height(28.dp)
                        .wrapContentHeight(Alignment.CenterVertically)
                )
            }
        }
        
        // SCROLLABLE RIGHT COLUMNS
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
        ) {
            periods.forEach { period ->
                Column(modifier = Modifier.width(60.dp)) {
                    rows.forEach { (_, formatter) ->
                        val metrics = state.metricsByPeriod[period]
                        Text(
                            formatter(metrics),
                            fontSize = 11.sp,
                            color = LibreCareColors.TextPrimary,
                            modifier = Modifier
                                .height(28.dp)
                                .wrapContentHeight(Alignment.CenterVertically)
                        )
                    }
                }
            }
        }
    }
}
```

**State Preservation**:
```kotlin
// In ViewModel
state.metricsScrollOffset: Int = 0

// In Screen
val scrollState = rememberScrollState(initial = state.metricsScrollOffset)
LaunchedEffect(scrollState.value) {
    viewModel.onMetricsScrollChanged(scrollState.value)
}
```

**Styling**:
- Header row background: `Surface.copy(alpha = 0.15f)`
- Row height: 28.dp per metric
- Left column width: 80.dp (fixed)
- Right column widths: 60.dp each (period column)
- Spacing: 2.dp between columns
- Border: 1.dp `Surface` color between sticky and scrollable

---

### 3.5 Average Line Enhancement (VERIFICATION)

**Current Implementation** (Lines 466-473 in AnalyticsScreen.kt):
```kotlin
if (overlay.averageLine.size >= 2) {
    val avg = Path()
    overlay.averageLine.sortedBy { it.minuteOfDay }.forEachIndexed { i, p ->
        val x = xFor(p.minuteOfDay); val y = yFor(p.averageMgDl)
        if (i == 0) avg.moveTo(x, y) else avg.lineTo(x, y)
    }
    drawPath(avg, LibreCareColors.AccentTeal, style = Stroke(width = 3f))
}
```

**Status**: ✅ Already implemented  
**Verification Needed**:
- [ ] Test on 4.5" phone screen
- [ ] Test on 6.5" phone screen
- [ ] Test on tablet
- [ ] Test in dark theme
- [ ] Verify contrast ratio ≥ 4.5:1
- [ ] Verify line not hidden by thin day lines
- [ ] Verify color matches legend

**Optional Enhancements** (Phase 2):
1. **Dashed Pattern**: Add visual differentiation
   ```kotlin
   style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f)))
   ```

2. **Shadow Effect**: Make line stand out
   ```kotlin
   drawPath(avg, Color.Black.copy(alpha = 0.15f), style = Stroke(width = 5f))  // Shadow
   drawPath(avg, LibreCareColors.AccentTeal, style = Stroke(width = 3f))        // Main
   ```

3. **Tooltip on Tap**: Show glucose value for time
   ```kotlin
   GestureDetector {
       onTap { minute ->
           val point = overlay.averageLine.find { it.minuteOfDay == minute }
           showTooltip(point.averageMgDl)
       }
   }
   ```

---

## 4. State Management (ViewModel)

### 4.1 New State Variables

```kotlin
data class DataAnalysisUiState(
    // ...existing fields...
    
    // NEW: Analysis period control
    val analysisPeriodDays: Int = 14,                    // 1-90 days
    val analysisStartDate: LocalDate = ...,             // Computed
    val analysisEndDate: LocalDate = ...,               // Computed
    val analysisRangeLabel: String = "...",             // Display string
    val canNavigateBackward: Boolean = false,           // Can scroll older?
    val canNavigateForward: Boolean = false,            // Can scroll newer?
    
    // Sticky metrics scroll state
    val metricsScrollOffset: Int = 0,                   // Preserve scroll position
)
```

### 4.2 New ViewModel Functions

```kotlin
class DataAnalysisViewModel(...) {
    
    // Update analysis period days (1-90)
    fun onAnalysisPeriodChanged(days: Int) {
        if (days !in 1..90) return
        _uiState.update {
            it.copy(analysisPeriodDays = days)
        }
        recomputeAnalysis()
    }
    
    // Navigate analysis by offset
    fun onNavigateAnalysisPeriod(offset: Duration) {
        val newStart = state.analysisStartDate.atStartOfDay(zoneId).toInstant() + offset
        if (newStart < oldestReading) return  // Boundary check
        
        _uiState.update {
            it.copy(
                analysisStartDate = newStart.atZone(zoneId).toLocalDate(),
                analysisEndDate = newStart.plus(Duration.ofDays(it.analysisPeriodDays.toLong()))
                    .atZone(zoneId).toLocalDate()
            )
        }
        recomputeAnalysis()
    }
    
    // Save metrics scroll position
    fun onMetricsScrollChanged(offset: Int) {
        _uiState.update { it.copy(metricsScrollOffset = offset) }
    }
    
    // Recompute overlay and metrics for new period
    private fun recomputeAnalysis() {
        viewModelScope.launch(Dispatchers.Default + exceptionHandler) {
            val readings = localRepository.getReadings(...)
            val newOverlay = AnalysisChartFactory.overlayForWindow(
                readings,
                state.analysisStartDate.atStartOfDay(zoneId).toInstant(),
                state.analysisEndDate.plusDays(1).atStartOfDay(zoneId).toInstant(),
                periodDays = state.analysisPeriodDays,  // NEW PARAMETER
                zoneId
            )
            val newMetrics = AnalysisMetricsFactory.calculate(readings, state.analysisEndDate, zoneId)
            val newObservations = AnalysisTrendInterpreter.interpret(newMetrics, newOverlay)
            
            _uiState.update {
                it.copy(
                    overlay = newOverlay,
                    metricsByPeriod = newMetrics,
                    trendObservations = newObservations,
                    analysisRangeLabel = "${state.analysisStartDate} — ${state.analysisEndDate} (${state.analysisPeriodDays} dni)"
                )
            }
        }
    }
}
```

---

## 5. Data Flow Diagram

```
User Action (AnalyticsScreen)
    ↓
onAnalysisPeriodChanged(30) or onNavigateAnalysisPeriod(-7d)
    ↓
ViewModel.recomputeAnalysis()
    ↓
Fetch readings from LocalRepository
    ↓
AnalysisChartFactory.overlayForWindow(
    readings,
    startDate,
    endDate,
    periodDays = 30,  ← KEY CHANGE
    zoneId
)
    ↓
AnalysisMetricsFactory.calculate(...)
    ↓
AnalysisTrendInterpreter.interpret(...)
    ↓
Update UiState (overlay, metrics, observations, rangeLabel)
    ↓
Compose recomposes screen with new data
    ↓
User sees updated chart & metrics
```

---

## 6. Files to Modify

| File | Purpose | Changes |
|------|---------|---------|
| `AnalysisChartFactory.kt` | Chart generation | Add `periodDays` parameter to `overlayForWindow()` |
| `AnalyticsViewModel.kt` | State & logic | New state fields, new functions |
| `AnalyticsScreen.kt` | UI rendering | New period control section, enhanced observations, sticky metrics |
| `AnalysisTrendInterpreter.kt` | Observations | (Possibly) update to consider period in interpretations |
| `AnalyticsScreen_Test.kt` | Tests | Unit tests for new functions |

---

## 7. Testing Strategy

### Unit Tests
```
AnalysisChartFactoryTest.kt
├─ overlayForWindow with periodDays=7 ✓
├─ overlayForWindow with periodDays=30 ✓
├─ overlayForWindow with periodDays=60 ✓
└─ overlayForWindow validation (invalid period) ✓

DataAnalysisViewModelTest.kt
├─ onAnalysisPeriodChanged(30) ✓
├─ onNavigateAnalysisPeriod(-7d) ✓
├─ onNavigateAnalysisPeriod boundary check ✓
├─ onMetricsScrollChanged(offset) ✓
└─ recomputeAnalysis() updates state ✓
```

### UI/Compose Tests
```
AnalyticsScreenTest.kt
├─ Period input accepts 1-90 ✓
├─ Period input rejects >90 ✓
├─ Navigation buttons enable/disable correctly ✓
├─ Sticky metrics column stays visible on scroll ✓
├─ Chart title updates with period ✓
├─ Observations font size is 14.sp ✓
└─ Dark theme contrast acceptable ✓
```

### Manual Tests
```
On Device/Emulator
├─ Test 14-day overlay (default) ✓
├─ Test 30-day overlay (full month) ✓
├─ Test 7-day overlay (short period) ✓
├─ Test 60-day overlay (2 months) ✓
├─ Scroll metrics table horizontally ✓
├─ Metric names always visible ✓
├─ Average line visible and correct ✓
├─ Dark theme contrast ✓
├─ Portrait & landscape modes ✓
├─ No performance degradation ✓
└─ Back navigation works ✓
```

---

## 8. Accessibility Checklist

- [ ] Font size ≥ 12sp for body text
- [ ] Font size ≥ 14sp for observations (improved readability)
- [ ] Contrast ratio ≥ 4.5:1 for text (dark theme)
- [ ] Touch targets ≥ 48x48dp (navigation buttons)
- [ ] Screen reader announces period duration
- [ ] Screen reader announces chart title with period
- [ ] Color not sole indicator (average line also thick/distinct)
- [ ] No blinking/flashing animations
- [ ] Keyboard accessible (Tab through inputs)

---

## 9. Performance Considerations

**Potential Issues**:
- Recalculating overlay for 90 days of data might be slow on older phones
- Large dataset might cause UI lag

**Mitigation**:
1. Debounce period input (500ms) before recalculation
2. Cache overlay calculations (store by start/end date)
3. Lazy-load older data if dataset >30 days
4. Use `Dispatchers.Default` for calculations (off main thread)
5. Consider progressive rendering for large datasets

---

## 10. Implementation Phases

### Phase 1: Foundation (Days 1-2)
- [ ] Update `AnalysisChartFactory.overlayForWindow()` with `periodDays` parameter
- [ ] Update `AnalyticsViewModel` with new state fields
- [ ] Build and test basic overlay recalculation

### Phase 2: Period Control UI (Days 3-4)
- [ ] Add period input field
- [ ] Add navigation buttons
- [ ] Implement navigation logic
- [ ] Test boundary checks

### Phase 3: Metrics Table (Days 5)
- [ ] Refactor metrics table to sticky layout
- [ ] Test scroll preservation
- [ ] Test on multiple screen sizes

### Phase 4: Polish & Font (Days 6)
- [ ] Increase observations font size
- [ ] Verify average line on devices
- [ ] Accessibility review

### Phase 5: Testing & Release (Days 7-8)
- [ ] Unit tests
- [ ] UI tests
- [ ] Manual regression tests
- [ ] Build APK/AAB
- [ ] Update release notes

---

## 11. Design Decisions

| Decision | Rationale |
|----------|-----------|
| 1-90 day range | Balance: enough data for trends, not overwhelming for UI |
| Period input field | User control without complex date pickers |
| 5 navigation buttons | Quick access to common periods (week, month) |
| Sticky left column | Metric names always visible = better UX |
| 14.sp font for observations | Improve readability for doctors/guardians (50-70y) |
| Dynamic chart title | Clear indication of current analysis period |
| Preserve scroll state | Better continuity when returning to screen |

---

## 12. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|-----------|
| Slow rendering for 90 days | Medium | High | Cache, lazy-load |
| UI layout breaks on small screens | Low | Medium | Test on 4.5" phone |
| Accessibility issues (contrast) | Low | Medium | WCAG AA compliance check |
| User confusion with controls | Low | Low | Clear labels, tooltips |
| Metric names don't align on scroll | Medium | Medium | Use fixed-width columns |

---

**Document Version**: 1.0  
**Status**: Ready for Development  
**Next Step**: Create Futures Tab Design Spec

