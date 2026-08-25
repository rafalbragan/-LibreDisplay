package com.libredisplay.ui.settings

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libredisplay.R
import com.libredisplay.data.model.HbA1cSettings
import com.libredisplay.data.model.QuickMetricId
import com.libredisplay.ui.theme.LibreCareColors

enum class MonitoringSettingsSection {
    TARGET_RANGE,
    HOME_METRICS,
    HBA1C
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringSettingsScreen(
    section: MonitoringSettingsSection,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val hba1cSettings by viewModel.hba1cSettings.collectAsState()
    val quickMetricsOrder by viewModel.quickMetricsOrder.collectAsState()
    val quickMetricsVisibility by viewModel.quickMetricsVisibility.collectAsState()
    val message by viewModel.message.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    var lowDraft by remember(section, settings.targetLow) { mutableStateOf(settings.targetLow.toString()) }
    var highDraft by remember(section, settings.targetHigh) { mutableStateOf(settings.targetHigh.toString()) }

    LaunchedEffect(message) {
        if (message == "Ustawienia zapisane") {
            viewModel.clearMessage()
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (section) {
                            MonitoringSettingsSection.TARGET_RANGE -> "Zakres docelowy"
                            MonitoringSettingsSection.HOME_METRICS -> "Metryki ekranu glównego"
                            MonitoringSettingsSection.HBA1C -> "HbA1c"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (section) {
                MonitoringSettingsSection.TARGET_RANGE -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = lowDraft,
                            onValueChange = { if (it.isEmpty() || it.all(Char::isDigit)) lowDraft = it },
                            label = { Text("Dolna granica (mg/dL)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = highDraft,
                            onValueChange = { if (it.isEmpty() || it.all(Char::isDigit)) highDraft = it },
                            label = { Text("Górna granica (mg/dL)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }

                MonitoringSettingsSection.HOME_METRICS -> {
                    Text(
                        text = "Przytrzymaj uchwyt i przeciągnij, aby zmienić kolejność metryk. Przełącznikiem ustaw widoczność.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    var draggingMetric by remember { mutableStateOf<QuickMetricId?>(null) }
                    var dragDy by remember { mutableFloatStateOf(0f) }
                    quickMetricsOrder.forEachIndexed { index, metricId ->
                        val lifted = draggingMetric == metricId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (lifted) 1f else 0f)
                                .graphicsLayer {
                                    if (lifted) {
                                        translationY = dragDy
                                        scaleX = 1.03f
                                        scaleY = 1.03f
                                        shadowElevation = 12f
                                        alpha = 0.97f
                                    }
                                }
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragIndicator,
                                contentDescription = "Przeciągnij, aby zmienić kolejność",
                                tint = if (lifted) LibreCareColors.AccentTeal else Color(0xFF94A3B8),
                                modifier = Modifier
                                    .size(24.dp)
                                    .pointerInput(quickMetricsOrder) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { draggingMetric = metricId; dragDy = 0f },
                                            onDragEnd = { draggingMetric = null; dragDy = 0f },
                                            onDragCancel = { draggingMetric = null; dragDy = 0f }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            dragDy += dragAmount.y
                                            val rowThreshold = 130f
                                            val cur = quickMetricsOrder.indexOf(metricId)
                                            if (dragDy < -rowThreshold && cur > 0) {
                                                viewModel.moveQuickMetricUp(metricId)
                                                dragDy += rowThreshold
                                            } else if (dragDy > rowThreshold && cur < quickMetricsOrder.lastIndex) {
                                                viewModel.moveQuickMetricDown(metricId)
                                                dragDy -= rowThreshold
                                            }
                                        }
                                    }
                            )
                            Text(text = metricLabel(metricId), modifier = Modifier.weight(1f), fontSize = 15.sp)
                            Switch(
                                checked = quickMetricsVisibility[metricId] ?: true,
                                onCheckedChange = { visible -> viewModel.setQuickMetricVisible(metricId, visible) }
                            )
                        }
                        if (index < quickMetricsOrder.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }

                MonitoringSettingsSection.HBA1C -> {
                    HbA1cSettingsContent(
                        hba1cSettings = hba1cSettings,
                        viewModel = viewModel
                    )
                }
            }

            message?.takeIf { it != "Ustawienia zapisane" }?.let {
                Text(text = it, fontSize = 12.sp, color = Color(0xFFFB923C))
            }

            Button(
                onClick = {
                    if (section == MonitoringSettingsSection.TARGET_RANGE) {
                        applyTargetRangeDraft(viewModel, lowDraft, highDraft)
                    }
                    viewModel.saveSettings()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Zapisywanie..." else "Zapisz")
            }
        }
    }
}

@Composable
private fun HbA1cSettingsContent(
    hba1cSettings: HbA1cSettings,
    viewModel: SettingsViewModel
) {
    var showEditor by remember(hba1cSettings.labHbA1cPercent, hba1cSettings.labHbA1cDate) {
        mutableStateOf(hba1cSettings.labHbA1cPercent != null || hba1cSettings.labHbA1cDate != null)
    }

    Text(stringResource(R.string.hba1c_scope_for_selected_person), fontSize = 12.sp, color = Color(0xFF94A3B8))

    if (!showEditor) {
        Text("Brak wyniku laboratoryjnego", fontSize = 15.sp, fontWeight = FontWeight.Medium)
        OutlinedButton(onClick = { showEditor = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Dodaj wynik")
        }
        return
    }

    val latestValue = hba1cSettings.labHbA1cPercent
    val latestDate = hba1cSettings.labHbA1cDate
    if (latestValue != null) {
        Text("Ostatni wynik", fontSize = 12.sp, color = Color(0xFF94A3B8))
        Text("${"%.1f".format(latestValue).replace('.', ',')}%", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(latestDate?.toString() ?: "Brak daty", fontSize = 12.sp, color = Color(0xFF94A3B8))
        HorizontalDivider()
    }

    OutlinedTextField(
        value = hba1cSettings.labHbA1cPercent?.toString().orEmpty(),
        onValueChange = viewModel::onLabHbA1cPercentChange,
        label = { Text(stringResource(R.string.label_hba1c_lab_value)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
    OutlinedTextField(
        value = hba1cSettings.labHbA1cDate?.toString().orEmpty(),
        onValueChange = viewModel::onLabHbA1cDateChange,
        label = { Text(stringResource(R.string.label_hba1c_lab_date)) },
        supportingText = { Text(stringResource(R.string.hba1c_lab_date_format_hint)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    OutlinedTextField(
        value = hba1cSettings.targetHbA1cPercent.toString(),
        onValueChange = viewModel::onTargetHbA1cPercentChange,
        label = { Text(stringResource(R.string.label_hba1c_target)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

private fun applyTargetRangeDraft(viewModel: SettingsViewModel, lowDraft: String, highDraft: String) {
    val low = lowDraft.toIntOrNull()
    val high = highDraft.toIntOrNull()
    if (low != null) viewModel.onTargetLowChange(low.toString())
    if (high != null) viewModel.onTargetHighChange(high.toString())
}

private fun metricLabel(metricId: QuickMetricId): String = when (metricId) {
    QuickMetricId.BELOW -> "Poniżej"
    QuickMetricId.IN_RANGE -> "W zakresie"
    QuickMetricId.ABOVE -> "Powyżej"
    QuickMetricId.AVERAGE -> "Średnia glikemia"
    QuickMetricId.MINIMUM -> "Minimum"
    QuickMetricId.MAXIMUM -> "Maksimum"
    QuickMetricId.GMI -> "GMI"
    QuickMetricId.CV -> "CV (zmienność)"
    QuickMetricId.VERY_LOW_EPISODES -> "Epizody bardzo niskie"
    QuickMetricId.VERY_HIGH_EPISODES -> "Epizody bardzo wysokie"
    QuickMetricId.HBA1C -> "HbA1c"
    QuickMetricId.SENSOR_ACTIVITY -> "Aktywność sensora"
    QuickMetricId.DATA_COVERAGE -> "Pokrycie danych"
}
