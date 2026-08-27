# Implementation Plan: DEC-0007 + Futures Tab

**Status**: Design → Implementation Roadmap  
**Timeline**: 6-8 weeks (phased delivery)  
**Effort**: ~200-250 developer hours  
**Priority**: Medium (feature improvements, not critical bugs)

---

## 1. Overall Timeline

```
WEEK 1-2:    DEC-0007 Foundation + Futures Tab Setup
WEEK 3-4:    Futures Features Implementation (Part 1: Spikes, Risk, Variability)
WEEK 5-6:    Futures Features Implementation (Part 2: Meals, Weekly, Achievements)
WEEK 7:      Integration & Polish
WEEK 8:      Testing & Release
```

---

## 2. Phase 1: DEC-0007 Foundation (Week 1-2)

### 2.1 Modify AnalysisChartFactory.kt

**Changes**:
- Rename `FourteenDayOverlay` → Keep for backward compatibility, use internally
- Add `periodDays` parameter to `overlayForWindow()`
- Update overlay calculation for variable periods

**Key Code Changes**:

```kotlin
// BEFORE
fun overlayForWindow(
    readings: List<GlucoseHistoryPoint>,
    startInclusive: Instant,
    endExclusive: Instant,
    zoneId: ZoneId
): FourteenDayOverlay

// AFTER
fun overlayForWindow(
    readings: List<GlucoseHistoryPoint>,
    startInclusive: Instant,
    endExclusive: Instant,
    periodDays: Int = 14,  // NEW: 1-90 days
    zoneId: ZoneId
): FourteenDayOverlay  // Keep name for compatibility
```

**Implementation Details**:
- Line 220-256: Update `overlayForWindow()` function
  - Calculate daily lines for periodDays (not just 14)
  - For periods >30 days: Consider weekly aggregation
  - Adjust time bucket size for consistency
- Line 66-68: Add PL_MONTHS array (already exists)
- Add new function: `isValidAnalysisPeriod(days: Int): Boolean`

**Testing**:
```kotlin
@Test
fun testOverlayForWindow_7days() {
    val result = overlayForWindow(readingList, start, end, periodDays=7, zoneId)
    assert(result.dayLines.size <= 7)
}

@Test
fun testOverlayForWindow_30days() {
    val result = overlayForWindow(readingList, start, end, periodDays=30, zoneId)
    assert(result.dayLines.size <= 30)
}

@Test
fun testOverlayForWindow_invalidPeriod() {
    // Should clamp or reject periods >90
}
```

---

### 2.2 Update AnalyticsViewModel.kt

**Add to DataAnalysisUiState**:

```kotlin
data class DataAnalysisUiState(
    // ...existing fields...
    
    // NEW
    val analysisPeriodDays: Int = 14,
    val canNavigateBackward: Boolean = false,
    val canNavigateForward: Boolean = false,
    val metricsScrollOffset: Int = 0,
)
```

**New Functions**:

```kotlin
fun onAnalysisPeriodChanged(days: Int) {
    // Validate 1-90
    // Update state
    // Trigger recomputeAnalysis()
}

fun onNavigateAnalysisPeriod(offset: Duration) {
    // Check boundaries
    // Update start/end dates
    // Trigger recomputeAnalysis()
}

fun onMetricsScrollChanged(offset: Int) {
    // Save scroll position
}

private fun recomputeAnalysis() {
    // Recalculate overlay with new periodDays
    // Recalculate metrics
    // Regenerate observations
    // Update UI state
}
```

**Location**: `AnalyticsViewModel.kt:82-396`

**Tests**:
```kotlin
@Test
fun onAnalysisPeriodChanged_30days() {
    viewModel.onAnalysisPeriodChanged(30)
    assert(viewModel.uiState.value.analysisPeriodDays == 30)
    assert(overlay recalculated)
}

@Test
fun onNavigateAnalysisPeriod_backward() {
    viewModel.onNavigateAnalysisPeriod(Duration.ofDays(-7))
    assert(start date shifted by -7 days)
}
```

---

### 2.3 Update AnalyticsScreen.kt

**Add Period Control Section** (after line 199):

```kotlin
if (state.selectedPatientId != null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LibreCareColors.Surface.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section Header
        Text("📊 WIZUALIZACJA ZAKRESU", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        
        // Period Input (1-90 days)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Liczba dni:", fontSize = 11.sp)
            OutlinedTextField(
                value = state.analysisPeriodDays.toString(),
                onValueChange = { newValue ->
                    newValue.toIntOrNull()?.let { viewModel.onAnalysisPeriodChanged(it) }
                },
                modifier = Modifier.width(80.dp).height(40.dp),
                textStyle = TextStyle(fontSize = 12.sp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        
        // Navigation Buttons (5 buttons)
        Wrap(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
                onClick = { viewModel.onNavigateAnalysisPeriod(Duration.ofDays(-30)) },
                enabled = state.canNavigateBackward
            ) { Text("◀◀ Miesiąc") }
            
            OutlinedButton(
                onClick = { viewModel.onNavigateAnalysisPeriod(Duration.ofDays(-7)) },
                enabled = state.canNavigateBackward
            ) { Text("◀ Tydzień") }
            
            OutlinedButton(
                onClick = { /* Reset to today */ }
            ) { Text("Dzisiaj") }
            
            OutlinedButton(
                onClick = { viewModel.onNavigateAnalysisPeriod(Duration.ofDays(7)) },
                enabled = state.canNavigateForward
            ) { Text("Tydzień ▶") }
            
            OutlinedButton(
                onClick = { viewModel.onNavigateAnalysisPeriod(Duration.ofDays(30)) },
                enabled = state.canNavigateForward
            ) { Text("Miesiąc ▶▶") }
        }
        
        // Current Range Display
        Text(
            "Zakres: ${state.analysisStartDate} — ${state.analysisEndDate} (${state.analysisPeriodDays} dni)",
            fontSize = 10.sp,
            color = LibreCareColors.TextSecondary
        )
    }
}
```

**Update Chart Title** (line 200):

```kotlin
Text(
    "Profil dobowy (nakładka ${state.analysisPeriodDays} dni)",  // UPDATED
    color = LibreCareColors.TextPrimary,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp
)
```

**Enhance Observations Section** (line 204-217):

```kotlin
if (state.trendObservations.isNotEmpty()) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LibreCareColors.Surface.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "⭐ WNIOSKI & OBSERWACJE",  // UPDATED header
            color = LibreCareColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp  // INCREASED from 13.sp
        )
        state.trendObservations.forEach { obs ->
            Text(
                "• $obs",
                color = LibreCareColors.TextSecondary,
                fontSize = 14.sp,  // INCREASED from 12.sp
                lineHeight = 18.sp
            )
        }
    }
}
```

**Replace MetricsTable with Sticky Layout** (line 478-521):

```kotlin
@Composable
private fun MetricsTable(metricsByPeriod: Map<AnalysisPeriod, PeriodMetrics>) {
    val periods = AnalysisPeriod.entries
    val rows = listOf(
        "TIR" to { m: PeriodMetrics -> m.tirPercent?.let { "$it%" } ?: "—" },
        // ... other rows ...
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(LibreCareColors.Surface.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        Row {
            // STICKY LEFT COLUMN
            Column(modifier = Modifier.width(80.dp)) {
                rows.forEach { (label, _) ->
                    Text(
                        label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LibreCareColors.TextSecondary,
                        modifier = Modifier.height(28.dp).wrapContentHeight(Alignment.CenterVertically)
                    )
                }
            }
            
            // SCROLLABLE RIGHT COLUMNS
            val scrollState = rememberScrollState(initial = state.metricsScrollOffset)
            LaunchedEffect(scrollState.value) {
                viewModel.onMetricsScrollChanged(scrollState.value)
            }
            
            Row(modifier = Modifier.weight(1f).horizontalScroll(scrollState)) {
                periods.forEach { period ->
                    Column(modifier = Modifier.width(60.dp)) {
                        rows.forEach { (_, formatter) ->
                            val metrics = metricsByPeriod[period]
                            Text(
                                formatter(metrics ?: PeriodMetrics()),
                                fontSize = 11.sp,
                                color = LibreCareColors.TextPrimary,
                                modifier = Modifier.height(28.dp).wrapContentHeight(Alignment.CenterVertically)
                            )
                        }
                    }
                }
            }
        }
    }
}
```

---

## 3. Phase 2: Futures Tab Setup (Week 1-2)

### 3.1 Update AppNavigationState.kt

**Modify `isTopLevelDestination()`** (line 18-23):

```kotlin
internal fun AppScreen.isTopLevelDestination(): Boolean = when (this) {
    AppScreen.Monitoring,
    AppScreen.Analytics,
    AppScreen.Futures,        // NEW
    AppScreen.Settings -> true
    else -> false
}
```

**Update `navigationGraphEdges()`** (line 55-81):

```kotlin
internal fun navigationGraphEdges(): Map<AppScreen, List<AppScreen>> = mapOf(
    AppScreen.Monitoring to listOf(AppScreen.Analytics, AppScreen.Settings, AppScreen.Futures, ...),
    AppScreen.Analytics to listOf(AppScreen.Futures, ...),
    AppScreen.Futures to listOf(
        AppScreen.FuturesSpikes,
        AppScreen.FuturesRisk,
        AppScreen.FuturesVariability,
        AppScreen.FuturesMealResponse,
        AppScreen.FuturesWeeklyReport,
        AppScreen.FuturesSensorWear,
        AppScreen.FuturesAchievements,
        AppScreen.FuturesShare
    ),
    // ... other entries ...
)
```

---

### 3.2 Update MainActivity.kt (AppScreen Enum)

**Add to enum** (line 46-61):

```kotlin
enum class AppScreen {
    Start,
    Monitoring,
    Analytics,
    Futures,                    // NEW: Main Futures screen
    FuturesSpikes,             // NEW: Sub-screens
    FuturesRisk,
    FuturesVariability,
    FuturesMealResponse,
    FuturesWeeklyReport,
    FuturesSensorWear,
    FuturesAchievements,
    FuturesShare,
    Settings,
    // ... other settings screens ...
}
```

**Add to screen rendering** (line 308-509):

```kotlin
AppScreen.Futures -> {
    BackHandler { navigateTo(AppScreen.Monitoring) }
    FuturesMainScreen(
        onNavigateToSpikes = { navigateTo(AppScreen.FuturesSpikes) },
        onNavigateToRisk = { navigateTo(AppScreen.FuturesRisk) },
        // ... other navigations ...
        onOpenHome = { navigateTo(AppScreen.Monitoring) },
        onOpenAnalytics = { navigateTo(AppScreen.Analytics) },
        onOpenSettings = { navigateTo(AppScreen.Settings) }
    )
}

AppScreen.FuturesSpikes -> {
    BackHandler { navigateBack() }
    FuturesSpikesScreen(onNavigateBack = { navigateBack() })
}

// ... other Futures screens ...
```

---

### 3.3 Update TopLevelNavigationBar.kt

**Modify enum** (line 32-36):

```kotlin
internal enum class DashboardNavItem(val label: String, val accessibilityLabel: String) {
    GLOWNA("Główna", "Główna"),
    HISTORIA("Analiza", "Analiza"),
    FUTURES("🔮 Futures", "Futures"),     // NEW
    USTAWIENIA("Ustawienia", "Ustawienia")
}
```

**Add to navigation bar** (line 45-54):

```kotlin
@Composable
internal fun TopLevelNavigationBar(
    selected: DashboardNavItem,
    onOpenHome: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenFutures: () -> Unit,    // NEW
    onOpenSettings: () -> Unit
) {
    NavigationBar(...) {
        TopLevelNavItem(selected == DashboardNavItem.GLOWNA, ..., Icons.Default.Home, onOpenHome)
        TopLevelNavItem(selected == DashboardNavItem.HISTORIA, ..., Icons.Filled.BarChart, onOpenHistory)
        TopLevelNavItem(selected == DashboardNavItem.FUTURES, ..., Icons.Filled.AutoAwesome, onOpenFutures)  // NEW
        TopLevelNavItem(selected == DashboardNavItem.USTAWIENIA, ..., Icons.Default.Settings, onOpenSettings)
    }
}
```

---

## 4. Phase 3: Futures Feature Screens (Week 3-6)

Create composable files in `ui/futures/`:

```
ui/futures/
├─ FuturesMainScreen.kt          (Main screen with feature cards)
├─ FuturesSpikesScreen.kt        (Spike explanations)
├─ FuturesRiskScreen.kt          (Hypoglycemia risk)
├─ FuturesVariabilityScreen.kt   (Variability patterns)
├─ FuturesMealResponseScreen.kt  (Meal analysis)
├─ FuturesWeeklyReportScreen.kt  (Weekly card)
├─ FuturesSensorWearScreen.kt    (Sensor tracking)
├─ FuturesAchievementsScreen.kt  (Badges & streaks)
├─ FuturesShareScreen.kt         (Share with doctor)
├─ FuturesPreferencesScreen.kt   (Settings)
├─ FuturesFeedbackScreen.kt      (Feedback)
└─ FuturesViewModel.kt           (State management)
```

**Key Implementation Notes**:
- Use existing data from `LocalRepository`
- Mock data for demonstration (where real data not available)
- All calculations local (no cloud)
- Test each screen independently

---

## 5. Phase 4: Integration & Polish (Week 7)

- Swipe gesture support for navigation
- Keyboard shortcuts (Ctrl+Left, Alt+Left, etc.)
- Dark theme verification
- Accessibility audit
- Performance optimization

---

## 6. Phase 5: Testing & Release (Week 8)

### Unit Tests
- All ViewModels
- All data transformation functions
- Navigation logic

### UI Tests
- All Compose screens
- Gesture interactions
- State preservation

### Manual Tests
- End-to-end flows
- Device compatibility (4.5", 6.5", tablet)
- Dark theme
- Portrait/landscape
- Back navigation
- Memory leaks

### Build & Release
```bash
./gradlew clean
./gradlew testDebugUnitTest
./gradlew lint
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew bundleRelease
```

---

## 7. Potential Issues & Mitigation

| Issue | Severity | Mitigation |
|-------|----------|-----------|
| Slow overlay calculation for 90 days | High | Cache results, lazy-load, use Dispatchers.Default |
| UI lag on old devices | Medium | Profile on Pixel 3a, optimize rendering |
| Complex state management | Medium | Use proper ViewModel pattern, test state flows |
| Navigation confusion (4 tabs) | Low | Clear labeling, tooltips, user education |
| Feature toggle persistence | Low | Save to SharedPreferences, sync with ViewModel |

---

## 8. Success Metrics

**Post-Implementation**:
- [ ] All DEC-0007 features working as designed
- [ ] Futures tab launches without crash
- [ ] No new ANRs (Application Not Responding)
- [ ] Battery usage unchanged
- [ ] All manual tests passing
- [ ] Dark theme contrast ≥4.5:1
- [ ] Accessibility score ≥90%

---

## 9. Documentation Updates Required

After implementation:

1. **Release Notes**: `docs/releases/LibreCare-<version>-release-notes.md`
   - Polish & English sections
   - List all new features
   - Known limitations

2. **Changelog**: Update `CHANGELOG.md`
   - Added: DEC-0007 enhancements, Futures tab
   - Changed: UI improvements
   - Fixed: Any bugs

3. **Quality Report**: `docs/quality-review-report-<version>.md`
   - Architecture review
   - Test coverage
   - Accessibility findings

4. **User Guide** (Optional): `docs/USER_GUIDE_FUTURES.md`
   - Explain each Futures feature
   - Screenshots
   - Tips for best use

---

## 10. Git Workflow

```bash
# Create feature branch
git checkout -b feature/dec-0007-analysis-enhancements

# Regular commits
git add app/src/main/java/com/libredisplay/ui/analytics/AnalyticsScreen.kt
git commit -m "feat: Add period control section (DEC-0007)"

# Create feature branch for Futures
git checkout -b feature/futures-tab

# Final merge
git push origin feature/dec-0007-analysis-enhancements
git push origin feature/futures-tab
```

---

**Document Version**: 1.0  
**Status**: Ready for Sprint Planning  
**Questions**: Contact development team for timeline adjustments

