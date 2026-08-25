package com.libredisplay.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import com.libredisplay.R
import com.libredisplay.data.model.QuickMetricId
import com.libredisplay.diagnostics.DiagnosticLogger
import com.libredisplay.ui.theme.LibreCareColors
import androidx.compose.ui.graphics.Color

enum class SettingsFocusSection {
    GENERAL,
    HBA1C
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    loginOnly: Boolean = false,
    focusSection: SettingsFocusSection = SettingsFocusSection.GENERAL,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToPrivacyData: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToRetention: () -> Unit,
    onNavigateToPolling: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val hba1cSettings by viewModel.hba1cSettings.collectAsState()
    val quickMetricsOrder by viewModel.quickMetricsOrder.collectAsState()
    val message by viewModel.message.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val scrollState = rememberScrollState()
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (message == "Ustawienia zapisane") {
            viewModel.clearMessage()
            onSaved()
        }
    }

    LaunchedEffect(loginOnly, focusSection) {
        if (loginOnly) return@LaunchedEffect
        val targetOffset = when (focusSection) {
            SettingsFocusSection.GENERAL -> 0
            SettingsFocusSection.HBA1C -> 1500
        }
        scrollState.animateScrollTo(targetOffset)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (loginOnly) "Logowanie" else "Ustawienia") },
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
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (message != null && message != "Ustawienia zapisane") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = message.orEmpty(),
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Dane logowania do LibreLinkUp", fontSize = 20.sp)
                    OutlinedTextField(
                        value = settings.email,
                        onValueChange = viewModel::onEmailChange,
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    OutlinedTextField(
                        value = settings.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = { Text("Hasło") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Ukryj hasło" else "Pokaż hasło"
                                )
                            }
                        }
                    )

                    // Password validation warnings
                    if (settings.password.isNotEmpty()) {
                        val hasLeadingWhitespace = settings.password.first().isWhitespace()
                        val hasTrailingWhitespace = settings.password.last().isWhitespace()
                        val hasNewLine = settings.password.contains('\n') || settings.password.contains('\r')

                        if (hasNewLine) {
                            Text(
                                "Hasło zawiera znak nowej linii. Usuń go przed zapisaniem.",
                                fontSize = 12.sp,
                                color = Color(0xFFDC2626)
                            )
                        } else if (hasLeadingWhitespace || hasTrailingWhitespace) {
                            Text(
                                "Hasło zawiera spację lub biały znak na początku albo końcu. Zostanie usunięty przed logowaniem.",
                                fontSize = 12.sp,
                                color = Color(0xFFFB923C)
                            )
                        }
                    }
                    Text(
                        "Użyj tego samego konta, którego używasz w aplikacji LibreLink / LibreLinkUp.",
                        fontSize = 13.sp
                    )
                    if (loginOnly) {
                        Button(onClick = viewModel::saveAndLogin, modifier = Modifier.fillMaxWidth()) {
                            Text("Zaloguj")
                        }
                    }
                }
            }

            if (loginOnly) {
                return@Column
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = settings.regionMode,
                        onValueChange = { viewModel.onRegionModeChange(it.uppercase()) },
                        label = { Text("Region logowania (EU/GLOBAL/EU2/DE/US/FR/JP/AP/CUSTOM)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (settings.regionMode.equals("CUSTOM", ignoreCase = true)) {
                        OutlinedTextField(
                            value = settings.customBaseUrl,
                            onValueChange = viewModel::onCustomBaseUrlChange,
                            label = { Text("Wlasny URL API") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Zakres docelowy", fontSize = 20.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NumericRangeField(
                            externalValue = settings.targetLow,
                            onValueCommit = viewModel::onTargetLowChange,
                            label = "Dolna granica",
                            modifier = Modifier.weight(1f)
                        )
                        NumericRangeField(
                            externalValue = settings.targetHigh,
                            onValueCommit = viewModel::onTargetHighChange,
                            label = "Górna granica",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tryb demo")
                            Text("Nie wymaga prawdziwego logowania. Używa przykładowych danych.", fontSize = 13.sp)
                        }
                        Switch(checked = settings.useMock, onCheckedChange = viewModel::onUseMockChange)
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Prywatność", fontSize = 20.sp)
                    Text("Zarządzaj danymi lokalnymi, sesją i ustawieniami prywatności konta.", fontSize = 13.sp)
                    OutlinedButton(onClick = onNavigateToPrivacyData, modifier = Modifier.fillMaxWidth()) {
                        Text("Otwórz Prywatność i dane")
                    }
                    OutlinedButton(onClick = onNavigateToStatistics, modifier = Modifier.fillMaxWidth()) {
                        Text("Informacje i statystyki")
                    }
                    OutlinedButton(onClick = onNavigateToRetention, modifier = Modifier.fillMaxWidth()) {
                        Text("Retencja danych")
                    }
                    OutlinedButton(onClick = onNavigateToPolling, modifier = Modifier.fillMaxWidth()) {
                        Text("Czestotliwosc odpytywania")
                    }
                    OutlinedButton(onClick = onNavigateToAbout, modifier = Modifier.fillMaxWidth()) {
                        Text("O aplikacji LibreCare")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.section_hba1c), fontSize = 20.sp)
                    Text(
                        text = stringResource(R.string.hba1c_scope_for_selected_person),
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
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
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Kolejność szybkich metryk", fontSize = 20.sp)
                    Text(
                        "Przytrzymaj uchwyt i przeciągnij metrykę, aby zmienić kolejność.",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                    var draggingMetric by remember { mutableStateOf<QuickMetricId?>(null) }
                    var dragDy by remember { mutableFloatStateOf(0f) }
                    quickMetricsOrder.forEach { metricId ->
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
                                },
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            Text(
                                text = metricLabel(metricId),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Diagnostyka", fontSize = 20.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onNavigateToDiagnostics, modifier = Modifier.weight(1f)) {
                            Text("Pokaż log")
                        }
                        OutlinedButton(onClick = { copyLog(context) }, modifier = Modifier.weight(1f)) {
                            Text("Kopiuj")
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { DiagnosticLogger.clear() }, modifier = Modifier.weight(1f)) {
                            Text("Wyczyść")
                        }
                        Button(onClick = { shareLog(context) }, modifier = Modifier.weight(1f)) {
                            Text("Udostępnij")
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = viewModel::resetSession, modifier = Modifier.weight(1f)) {
                    Text("Wyczyść zapisany token i zaloguj ponownie")
                }
                Button(
                    onClick = { viewModel.saveSettings() },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                ) {
                    Text(if (isSaving) "Zapisywanie..." else "Zapisz")
                }
            }
        }
    }
}

/**
 * Pole numeryczne z lokalnym buforem tekstu.
 *
 * Problem który rozwiązuje:
 * Standardowy kontrolowany TextField (`value = state.toString()`) przy każdym naciśnięciu
 * klawisza wymusza natychmiastowe zatwierdzenie wartości. Gdy użytkownik kasuje "180" do "18",
 * ViewModel coerces 18 → 60, pole pokazuje "60", a kolejne wciśnięcia klawiszy
 * dodają cyfry do "60" zamiast tworzyć nową liczbę.
 *
 * Rozwiązanie: lokalna zmienna tekstu w kompozablu. ViewModel dostaje wartość
 * dopiero gdy użytkownik opuści pole (focus lost).
 */
@Composable
private fun NumericRangeField(
    externalValue: Int,
    onValueCommit: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var localText by remember { mutableStateOf(externalValue.toString()) }
    var hasFocus by remember { mutableStateOf(false) }

    // Sync from external value only when field is NOT focused
    // (prevents overwriting what the user is currently typing)
    LaunchedEffect(externalValue) {
        if (!hasFocus) {
            localText = externalValue.toString()
        }
    }

    OutlinedTextField(
        value = localText,
        onValueChange = { newText ->
            // Allow only digits and empty string (user clearing the field)
            if (newText.isEmpty() || newText.all { it.isDigit() }) {
                localText = newText
            }
        },
        label = { Text(label) },
        modifier = modifier.onFocusChanged { focusState ->
            if (focusState.isFocused) {
                hasFocus = true
            } else if (hasFocus) {
                // Focus lost → commit current text to ViewModel
                hasFocus = false
                onValueCommit(localText)
                // ViewModel will coerce the value and update externalValue,
                // which will be synced back via LaunchedEffect above.
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
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

private fun copyLog(context: Context) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("LibreCareLog", DiagnosticLogger.readAll()))
}

private fun shareLog(context: Context) {
    DiagnosticLogger.createShareIntent(context)?.let {
        context.startActivity(Intent.createChooser(it, "Udostępnij log"))
    }
}
