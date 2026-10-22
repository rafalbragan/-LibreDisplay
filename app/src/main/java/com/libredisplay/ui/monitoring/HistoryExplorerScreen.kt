package com.libredisplay.ui.monitoring

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libredisplay.LibreDisplayApp
import com.libredisplay.data.local.GlucoseReadingEntity
import com.libredisplay.data.local.HistoryBucket
import com.libredisplay.data.local.ObservedPersonEntity
import com.libredisplay.data.model.AppMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal val ExplorerDurationKey = SemanticsPropertyKey<Long>("HistoryWindowDuration")
internal val ExplorerStartKey = SemanticsPropertyKey<Long>("HistoryWindowStart")
internal val ExplorerBucketsKey = SemanticsPropertyKey<Int>("HistoryRenderedBuckets")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryExplorerScreen(onBack: () -> Unit, initialPatientId: String? = null,
    initialWindowMillis: Long = 24 * 60 * 60_000L) {
    val app = LocalContext.current.applicationContext as LibreDisplayApp
    val dao = app.database.glucoseReadingDao()
    val scope = rememberCoroutineScope()
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val settings = remember { app.settingsRepository.loadSettings() }
    val zone = ZoneId.systemDefault()
    val formatter = remember(zone) { DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(zone) }
    var persons by remember { mutableStateOf(emptyList<ObservedPersonEntity>()) }
    var patient by rememberSaveable { mutableStateOf(initialPatientId) }
    var chooser by remember { mutableStateOf(false) }
    var bounds by remember(patient) { mutableStateOf<ExplorerWindow?>(null) }
    var start by rememberSaveable(patient) { mutableStateOf<Long?>(null) }
    var length by rememberSaveable(patient) { mutableStateOf(initialWindowMillis) }
    var selectedRange by rememberSaveable(patient) { mutableStateOf(when (initialWindowMillis) {
        86_400_000L -> "Dzień"
        7 * 86_400_000L -> "Tydzień"
        30 * 86_400_000L -> "Miesiąc"
        365 * 86_400_000L -> "Rok"
        else -> ""
    }) }
    var loaded by remember(patient) { mutableStateOf<Pair<ExplorerWindow, List<HistoryBucket>>?>(null) }
    var point by remember(patient) { mutableStateOf<GlucoseReadingEntity?>(null) }
    var queryJob by remember { mutableStateOf<Job?>(null) }
    var error by remember(patient) { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        persons = withContext(Dispatchers.IO) {
            val known = if (settings.appMode == AppMode.DEMO) app.database.observedPersonDao().getAllDemoPersons()
                else app.database.observedPersonDao().getAllLivePersons()
            val requested = initialPatientId?.let { app.database.observedPersonDao().getByPatientId(it) }
                ?.takeIf { it.patientId.startsWith("demo-person-") == (settings.appMode == AppMode.DEMO) }
            (known + listOfNotNull(requested)).distinctBy { it.patientId }
        }
        if (persons.none { it.patientId == patient }) patient = persons.firstOrNull()?.patientId
    }
    LaunchedEffect(patient, persons) {
        val id = patient ?: return@LaunchedEffect
        if (persons.none { it.patientId == id }) return@LaunchedEffect
        val oldest = dao.oldestReadingTimestampForPatient(id)?.toEpochMilli() ?: return@LaunchedEffect
        val newest = dao.newestReadingTimestampForPatient(id)?.toEpochMilli() ?: oldest
        val available = ExplorerWindow(oldest, newest.coerceAtLeast(oldest + 60_000L))
        bounds = available
        val window = ExplorerWindow(start ?: (available.end - length), (start ?: (available.end - length)) + length).constrained(available)
        start = window.start; length = window.duration
    }
    val viewport = bounds?.let { full -> ExplorerWindow(start ?: (full.end - length), (start ?: (full.end - length)) + length).constrained(full) }
    val currentViewport by rememberUpdatedState(viewport)
    val currentPatient by rememberUpdatedState(patient)
    val buckets = loaded?.takeIf { it.first == viewport }?.second.orEmpty()
    LaunchedEffect(patient, viewport) {
        point = null
        val id = patient ?: return@LaunchedEffect
        val window = viewport ?: return@LaunchedEffect
        delay(60) // Coalesce gesture events without blocking drawing or loading entire history.
        try {
            val result = dao.chartBuckets(id, window.start, window.end, (window.duration / 800 + 1).coerceAtLeast(1))
            loaded = window to result; error = null
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { error = "Nie można odczytać historii. Wybierz zakres ponownie." }
    }
    fun move(window: ExplorerWindow) { start = window.start; length = window.duration; point = null }
    BackHandler(onBack = onBack)
    Scaffold(topBar = { TopAppBar(title = {
        Text(persons.firstOrNull { it.patientId == patient }?.displayName ?: "Historia glikemii", maxLines = 1)
    }, navigationIcon = { TextButton(onClick = onBack) { Text("Wstecz") } }, actions = {
        Box {
            TextButton(onClick = { chooser = true }) { Text("Osoba") }
            DropdownMenu(expanded = chooser, onDismissRequest = { chooser = false }) {
                persons.forEach { person -> DropdownMenuItem(text = { Text(person.displayName) }, onClick = {
                    queryJob?.cancel(); patient = person.patientId; chooser = false
                }) }
            }
        }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp).testTag("history-explorer"),
            verticalArrangement = Arrangement.spacedBy(if (landscape) 0.dp else 6.dp)) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Dzień" to 1L, "Tydzień" to 7L, "Miesiąc" to 30L, "Rok" to 365L, "Całość" to 0L).forEach { (label, days) ->
                    FilterChip(selected = selectedRange == label, onClick = {
                        selectedRange = label
                        bounds?.let { full ->
                            val duration = if (days == 0L) full.duration else days * 86_400_000L
                            val end = viewport?.end ?: full.end
                            move(ExplorerWindow(end - duration, end).constrained(full))
                        }
                    }, label = { Text(label) }, modifier = Modifier.testTag("history-range-$label"))
                }
            }
            Text("HISTORIA LOKALNA · nie jest potwierdzeniem połączenia na żywo", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)
            Text(viewport?.let { "${formatter.format(Instant.ofEpochMilli(it.start))} — ${formatter.format(Instant.ofEpochMilli(it.end))} · ${zone.id}" }
                ?: "Brak zapisanej historii wybranej osoby", style = MaterialTheme.typography.labelSmall, modifier = Modifier.testTag("history-range-label"))
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            ExplorerCanvas(buckets, viewport, point, settings.targetLow, settings.targetHigh,
                modifier = Modifier.weight(1f).fillMaxWidth().heightIn(min = 90.dp),
                onTransform = { zoom, pan, anchor ->
                    val window = currentViewport; val full = bounds
                    if (window != null && full != null) move(window.transform(zoom, pan, anchor, full))
                }, onSelect = { fraction ->
                    queryJob?.cancel()
                    val id = currentPatient; val window = currentViewport
                    if (id != null && window != null) queryJob = scope.launch {
                        val selected = dao.nearestInViewport(id, window.start, window.end, window.at(fraction))
                        if (currentPatient == id && currentViewport == window) point = selected
                    }
                })
            Text(point?.let { "${it.valueMgDl} mg/dL · ${formatter.format(it.timestamp)} · ${if (it.source == "LibreViewScan") "skan" else "pomiar"}" }
                ?: "Dotknij punktu · uszczypnij, aby przybliżyć · przeciągnij, aby przesunąć",
                style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("history-tooltip"))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { viewport?.let { w -> bounds?.let { move(ExplorerWindow(w.start - w.duration, w.start).constrained(it)) } } }, modifier = Modifier.testTag("history-previous")) { Text("← Wcześniej") }
                TextButton(onClick = { bounds?.let { move(ExplorerWindow(it.end - length, it.end).constrained(it)) } }, modifier = Modifier.testTag("history-latest")) { Text("Najnowsze") }
                TextButton(onClick = { viewport?.let { w -> bounds?.let { move(ExplorerWindow(w.end, w.end + w.duration).constrained(it)) } } }, modifier = Modifier.testTag("history-next")) { Text("Później →") }
            }
            if (!landscape) Text("${buckets.sumOf { it.count }} pomiarów · kreski: min–max przedziału · romby: przedziały ze skanami. Luki >20 min nie są łączone.",
                style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

@Composable
private fun ExplorerCanvas(buckets: List<HistoryBucket>, viewport: ExplorerWindow?, selected: GlucoseReadingEntity?,
    targetLow: Int, targetHigh: Int, modifier: Modifier, onTransform: (Float, Float, Float) -> Unit, onSelect: (Float) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val currentTransform by rememberUpdatedState(onTransform)
    val currentSelect by rememberUpdatedState(onSelect)
    val labelStyle = TextStyle(color = colors.onSurfaceVariant, fontSize = 10.sp)
    val minValue = minOf(40, buckets.minOfOrNull { it.minimum } ?: 40)
    val maxValue = maxOf(250, buckets.maxOfOrNull { it.maximum } ?: 250)
    Canvas(modifier.testTag("history-chart").semantics {
        this[ExplorerStartKey] = viewport?.start ?: 0L
        this[ExplorerDurationKey] = viewport?.duration ?: 0L
        this[ExplorerBucketsKey] = buckets.size
    }.pointerInput(Unit) {
        detectTransformGestures { centroid, pan, zoom, _ ->
            val left = 42.dp.toPx(); val width = (size.width - left - 12.dp.toPx()).coerceAtLeast(1f)
            currentTransform(zoom, pan.x / width, (centroid.x - left) / width)
        }
    }.pointerInput(Unit) {
        detectTapGestures { tap ->
            val left = 42.dp.toPx(); val width = (size.width - left - 12.dp.toPx()).coerceAtLeast(1f)
            if (tap.x >= left && tap.x <= left + width) currentSelect((tap.x - left) / width)
        }
    }) {
        val left = 42.dp.toPx(); val right = size.width - 12.dp.toPx()
        val top = 16.dp.toPx(); val bottom = size.height - 26.dp.toPx()
        if (right <= left || bottom <= top) return@Canvas
        fun y(value: Double) = bottom - ((value - minValue) / (maxValue - minValue).toDouble()).toFloat() * (bottom - top)
        val lowY = y(targetLow.toDouble()).coerceIn(top, bottom)
        val highY = y(targetHigh.toDouble()).coerceIn(top, bottom)
        drawRect(colors.primary.copy(alpha = 0.12f), Offset(left, highY), Size(right - left, (lowY - highY).coerceAtLeast(0f)))
        listOf(minValue, targetLow, targetHigh, maxValue).distinct().sorted().forEach { value ->
            val yy = y(value.toDouble())
            drawLine(colors.outlineVariant, Offset(left, yy), Offset(right, yy), 1.dp.toPx())
            drawText(measurer, value.toString(), Offset(0f, yy - 6.dp.toPx()), style = labelStyle)
        }
        val window = viewport ?: return@Canvas
        fun x(time: Long) = left + window.fraction(time) * (right - left)
        val tickFormat = DateTimeFormatter.ofPattern(if (window.duration > 2 * 86_400_000L) "dd.MM.yy" else "HH:mm").withZone(ZoneId.systemDefault())
        for (i in 0..3) {
            val at = window.start + window.duration * i / 3
            val xx = x(at)
            drawLine(colors.outlineVariant.copy(alpha = 0.6f), Offset(xx, top), Offset(xx, bottom))
            val text = measurer.measure(tickFormat.format(Instant.ofEpochMilli(at)), labelStyle)
            drawText(text, topLeft = Offset((xx - text.size.width / 2f).coerceIn(0f, (size.width - text.size.width).coerceAtLeast(0f)), bottom + 5.dp.toPx()))
        }
        clipRect(left, top, right, bottom) {
            var previous: HistoryBucket? = null
            for (bucket in buckets) {
                val xx = x(bucket.firstTime)
                val yy = y(bucket.average)
                if (!xx.isFinite() || !yy.isFinite()) continue
                val old = previous
                if (old != null && canJoinHistorySamples(old.lastTime, bucket.firstTime)) {
                    drawLine(colors.primary, Offset(x(old.lastTime), y(old.average)), Offset(xx, yy), 2.dp.toPx())
                }
                if (bucket.count > 1) {
                    drawLine(colors.primary.copy(alpha = 0.8f), Offset(xx, y(bucket.minimum.toDouble())), Offset(xx, y(bucket.maximum.toDouble())), 2.dp.toPx())
                } else drawCircle(colors.primary, 2.dp.toPx(), Offset(xx, yy))
                if (bucket.scans > 0) {
                    val radius = 3.dp.toPx()
                    val diamond = Path().apply { moveTo(xx, yy - radius); lineTo(xx + radius, yy); lineTo(xx, yy + radius); lineTo(xx - radius, yy); close() }
                    drawPath(diamond, colors.tertiary)
                }
                previous = bucket
            }
            selected?.let {
                val xx = x(it.timestamp.toEpochMilli()); val yy = y(it.valueMgDl.toDouble())
                drawLine(colors.tertiary, Offset(xx, top), Offset(xx, bottom), 1.dp.toPx())
                drawCircle(colors.tertiary, 5.dp.toPx(), Offset(xx, yy))
            }
        }
        if (buckets.isNotEmpty()) {
            val label = "MIN ${buckets.minOf { it.minimum }} · MAX ${buckets.maxOf { it.maximum }} mg/dL"
            drawText(measurer, label, Offset(left + 4.dp.toPx(), 0f), style = labelStyle)
        }
    }
}




