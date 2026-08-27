# Feature: Analysis Screen Enhancements (DEC-0007)

**Status**: Documentation for Future Implementation  
**Priority**: Medium  
**Component**: Analytics Screen (`DataAnalysisScreen.kt`)  
**Date Created**: 2026-08-27

---

## User Value Proposition

Physician/Guardian needs to:
- Analyze glucose trends over **flexible timeframes** (not just 14 days)
- See **clear visual indicators** of average trends
- Understand **metric descriptions** without horizontal scrolling distraction
- Make quick clinical decisions based on **month-long trends**

---

## Functional Requirements

### 1. Observations/Conclusions Font Size Enhancement

**Current State**:
- Observations rendered in `fontSize = 12.sp` (line 214)
- Section title in `fontSize = 13.sp` (line 212)
- Located in `AnalyticsScreen.kt:204-217`

**Requirement**:
- Increase font size for observations: `12.sp` → `14.sp` or `15.sp`
- Ensure readability without line wrapping
- Test on phone-sized screens (min 4.5", max 6.5")
- Verify dark theme contrast (WCAG AA minimum)

**Priority**: Improve readability for aging population (50-70 year old doctors/guardians)

---

### 2. Custom Analysis Period (1-90 Days)

**Current State**:
- Analysis hardcoded to last 14 days in 14-day overlay
- Overlay function: `AnalysisChartFactory.overlayForWindow()` (line 220-256)
- Metrics calculated for predefined periods only: `AnalysisPeriod` enum (1h, 3h, 6h, 24h, 7d, 30d, CUSTOM)
- Custom date range already supported via `DateRangePicker`

**Requirement**:

#### 2.1 Analysis Period Input UI
- Add **integer input field** after custom range selection
- Allow user to enter: **1 to 90 days**
- Label: "Liczba dni do analizy:" (Analyzer days)
- Default: 14 days
- Validation: Reject if > available data
- Store in `DataAnalysisUiState.analysisPeriodDays: Int`

#### 2.2 Dynamic Overlay Recalculation
- Modify `FourteenDayOverlay` → `DynamicPeriodOverlay` (make it generic)
  - Parameter: `periodDays: Int` instead of hardcoded 14
  - Recalculate average line for entire period
  - Adjust time aggregation (daily if ≤30 days, maybe weekly if >30 days)

- Update `AnalysisChartFactory.overlayForWindow()`:
  ```kotlin
  fun overlayForWindow(
      readings: List<GlucoseHistoryPoint>,
      startInclusive: Instant,
      endExclusive: Instant,
      periodDays: Int = 14,  // NEW PARAMETER
      zoneId: ZoneId
  ): DynamicPeriodOverlay
  ```

#### 2.3 Update ViewModel
- Store selected `periodDays` in state
- Recalculate overlay when user changes period
- Recalculate metrics for new date range
- Update chart title: "Profil dobowy (nakładka N dni)" instead of hardcoded 14

---

### 3. Period Navigation Controls

**Requirement**: Add quick navigation controls below analysis period input

#### 3.1 Navigation Buttons (5 buttons in a row)
| Button | Action | Keyboard |
|--------|--------|----------|
| ◀◀ Miesiąc wstecz | Shift start by -30 days | Ctrl+Left |
| ◀ Tydzień wstecz | Shift start by -7 days | Alt+Left |
| Dzisiaj | Set end to now, adjust start | Home |
| Tydzień naprzód | Shift start by +7 days | Alt+Right |
| Miesiąc naprzód ▶▶ | Shift start by +30 days | Ctrl+Right |

#### 3.2 Logic
- Preserve `periodDays` (e.g., if user selected 30 days)
- When navigating: `newStart = oldStart ± offset`, `newEnd = newStart + periodDays`
- Prevent navigation beyond oldest data or future
- Show disabled state when can't scroll further
- Display current range: "2026-08-01 — 2026-08-31 (31 dni)"

#### 3.3 Swipe Gesture Support
- **Left swipe** → navigate forward (newer data, towards today)
- **Right swipe** → navigate backward (older data, past)
- Velocity-based: detect swipe vs. drag
- Visual feedback: highlight active button or show animated transition

---

### 4. Metric Descriptions (Non-Scrolling Sticky Column)

**Current State**:
- Metrics displayed in horizontal scrollable Row (line 494-498)
- When scrolling, metric names disappear
- User loses context of which column is which

**Problem**: Guardian scrolling metrics horizontally loses understanding of what each value represents.

**Requirement**: Implement sticky left column

#### 4.1 Layout Change
```
┌─────────────────────────────────────┐
│ METRIC NAME │ 1g  │ 3g  │ 6g  │24g │ (scrollable)
├─────────────────────────────────────┤
│ TIR         │ 92% │ 88% │ 85% │80% │
│ Poniżej     │ 2%  │ 4%  │ 7%  │10% │  (metric name always visible)
│ Powyżej     │ 6%  │ 8%  │ 8%  │10% │
│ Średnia     │ 125 │ 132 │ 135 │140 │
└─────────────────────────────────────┘
```

#### 4.2 Implementation
- Use `LazyRow` with fixed-width first column (metric names)
- First column: `width = 80.dp`, never scrolls
- Remaining columns: scrollable horizontally
- Metrics column styling:
  - `fontSize = 12.sp` (or slightly larger for consistency)
  - Bold font for metric names
  - Consistent alignment (right-align numeric values)
  - Color: `LibreCareColors.TextSecondary` for names

#### 4.3 Scroll State Preservation
- Save horizontal scroll position in ViewModel
- Restore when user returns to analytics screen
- Reset when changing selected person or date range

---

### 5. Average Line Enhancement on Chart

**Current State**:
- Average line already drawn in `OverlayLineChart()` (line 472)
- Color: `LibreCareColors.AccentTeal`
- Stroke width: `3f`
- Legend already exists: "średnia (gruba)" (line 310)

**Requirement**: Verify and enhance visibility

#### 5.1 Visual Verification
- ✅ Existing: Stroke width 3f (gruba/thick)
- ✅ Existing: Color AccentTeal (distinct)
- ✅ Existing: Legend shows "średnia (gruba)"
- TODO: Verify on real device (all screen sizes)
- TODO: Verify on dark theme (contrast ratio ≥ 4.5:1)
- TODO: Verify line is not hidden by day lines when overlapping

#### 5.2 Enhancement Suggestions
- Consider adding **dashed pattern** to average line (differentiate from day lines further)
  - Pattern: 5px solid, 3px gap
- Add **tooltip on hover** showing average glucose value for that time
- Consider **shadow effect** under average line to make it pop

#### 5.3 Testing Checklist
- [ ] Test on 5" phone screen in both portrait/landscape
- [ ] Test on 6.5" large phone screen
- [ ] Test on 10" tablet (if supported)
- [ ] Test in dark theme at various brightness levels
- [ ] Test with 7-day, 30-day, 60-day overlays
- [ ] Verify average line calculation is correct (matches metrics "Średnia")

---

### 6. Data Binding: Chart ↔ Analysis Period

**Current State**:
- `DataAnalysisPeriod` (1g, 3g, 6g, 24g, 7d, 30d, Custom) changes metrics
- Does NOT change the 14-day overlay chart
- Overlay is always for last 14 days regardless of period selected

**Requirement**: Bind overlay timeframe to selected analysis period

#### 6.1 Logic
```
User selects:
  - Period: 30d
  - Start: 2026-08-01, End: 2026-08-31
  
Then:
  - Overlay should show: "Profil dobowy (nakładka 30 dni)"
  - Chart data: all days from 2026-08-01 to 2026-08-31
  - Average line: calculated across 30 days
  - Metrics: calculated for same period
```

#### 6.2 State Flow
1. User changes analysis period days (1-90 input) → trigger recalculation
2. User navigates (forward/backward) → recalculate overlay
3. ViewModel updates: `overlayForWindow(start, end, periodDays)`
4. UI updates chart and title automatically via state Flow

---

## Testing Requirements

### Unit Tests
- [ ] `AnalysisChartFactory` with variable `periodDays`
- [ ] Navigation offset calculations (boundary conditions)
- [ ] Period validation (1-90 range)
- [ ] Date range constraints (not beyond oldest data)

### UI/Compose Tests
- [ ] Custom period input validates correctly
- [ ] Navigation buttons enable/disable correctly
- [ ] Swipe gestures recognized
- [ ] Sticky column scrolling preserves metric names
- [ ] Chart updates when period changes
- [ ] Font size readable on small screens

### Manual/Regression Tests
- [ ] 14-day overlay (default) renders correctly
- [ ] 30-day overlay shows complete month trends
- [ ] 60-day overlay shows 2-month trends
- [ ] Average line visible and correct
- [ ] Dark theme contrast acceptable
- [ ] No performance degradation with large date ranges
- [ ] Accessibility: screen reader announces period duration

---

## Implementation Checklist

### Phase 1: Foundation
- [ ] Rename `FourteenDayOverlay` → `DynamicPeriodOverlay`
- [ ] Add `periodDays: Int` parameter to overlay functions
- [ ] Update ViewModel to store `analysisPeriodDays`
- [ ] Update AnalyticsScreen to accept period input (hardcoded to 14 for now)
- [ ] Build and test

### Phase 2: Period Input UI
- [ ] Add integer input field (1-90 days)
- [ ] Validate input, show error message if invalid
- [ ] Trigger overlay recalculation on input change
- [ ] Update chart title dynamically
- [ ] Build and test

### Phase 3: Navigation Controls
- [ ] Add 5 navigation buttons
- [ ] Implement period shifting logic
- [ ] Add swipe gesture detection
- [ ] Implement boundary checks
- [ ] Build and test

### Phase 4: Sticky Metrics Column
- [ ] Refactor MetricsTable → LazyRow with fixed first column
- [ ] Test horizontal scrolling on all screen sizes
- [ ] Preserve scroll state in ViewModel
- [ ] Build and test

### Phase 5: Font & Polish
- [ ] Increase observations font size
- [ ] Verify contrast ratios (dark theme)
- [ ] Run accessibility scan
- [ ] Test on target devices

### Phase 6: Documentation & Release
- [ ] Update release notes (Polish + English)
- [ ] Update CHANGELOG.md
- [ ] Create release report
- [ ] Update version number
- [ ] Build final APK/AAB

---

## Architecture Notes

### Files to Modify
1. **AnalysisChartFactory.kt**
   - Change `FourteenDayOverlay` data class
   - Update `overlayForWindow()` signature
   - Update overlay calculation logic

2. **AnalyticsViewModel.kt**
   - Add `analysisPeriodDays: Int` to `DataAnalysisUiState`
   - Add period navigation logic
   - Update chart recalculation trigger

3. **AnalyticsScreen.kt**
   - Add period input UI
   - Add navigation buttons
   - Refactor MetricsTable → sticky layout
   - Increase observations font size
   - Update chart title binding

4. **Test Files** (new/updated)
   - `AnalysisChartFactoryTest.kt`
   - `DataAnalysisViewModelTest.kt`
   - `AnalyticsScreenTest.kt` (Compose UI tests)

### Backward Compatibility
- Default `periodDays` = 14 (existing behavior)
- Existing custom date range picker continues to work
- No database schema changes required
- No migration needed

---

## Design Considerations

### UX Flow
1. User opens Analytics screen → sees last 14 days by default
2. User enters "30" in days field → overlay recalculates to show 30-day profile
3. User clicks "Miesiąc wstecz" → analyzes previous 30-day period
4. User scrolls metrics table horizontally → metric names always visible
5. User observes average line clearly shows trend line across period

### Accessibility
- Large font for observations (elderly users)
- High contrast for average line
- Screen reader announces: "Analysis period: 30 days, from August 1 to August 31"
- Keyboard navigation for period input and buttons

### Performance
- Avoid recalculating overlay on every keystroke (use debounce if needed)
- Lazy load overlay calculation for >90 day periods
- Cache chart data to prevent flicker on navigation

---

## Related Features / Dependencies

- **DEC-0006**: Data freshness indicator (shows "Ostatnia aktualizacja" on home screen)
- **NFZ Eligibility**: May benefit from multi-period analysis for qualification evaluation
- **Statistics Screen**: Consider reusing sticky column pattern for consistency

---

## Open Questions for Implementation

1. Should navigation buttons show keyboard shortcuts in tooltip? (Ctrl+Left, Alt+Left, etc.)
2. Should we support >90 days? (Consider performance impact)
3. Should average line auto-adjust Y-axis to best fit data?
4. Should we add "Export Period" button specific to selected timeframe?
5. Should swipe gestures work in both directions or only horizontal?

---

## Future Enhancements (Out of Scope)

- [ ] Comparative period analysis (30 days ago vs. today's 30 days)
- [ ] Trend indicators (↑ up 5%, ↓ down 3%)
- [ ] Anomaly detection (flag unusual patterns)
- [ ] AI-powered recommendations based on selected period
- [ ] Custom color coding for periods (red/yellow/green zones)
- [ ] Multi-person overlay comparison within same period

---

**Document Version**: 1.0  
**Last Updated**: 2026-08-27  
**Author**: GitHub Copilot  
**Status**: Ready for Implementation

