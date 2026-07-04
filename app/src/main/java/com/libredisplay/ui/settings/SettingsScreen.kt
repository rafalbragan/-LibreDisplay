package com.libredisplay.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Settings screen.
 *
 * Allows the caregiver to configure their LibreLinkUp credentials, region,
 * refresh interval, and optional kiosk / mock modes.
 *
 * All values are passed to [SettingsViewModel] and persisted via encrypted storage.
 * The password field is never rendered to logs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }

    // Show a toast-style snackbar on successful save
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            snackbarHostState.showSnackbar("Settings saved ✓")
            viewModel.clearSaveSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── Section: Credentials ───────────────────────────────────────
            SectionHeader("LibreLinkUp Account")

            OutlinedTextField(
                value = settings.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            OutlinedTextField(
                value = settings.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff
                                          else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = Color.LightGray
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            // ── Section: Region ────────────────────────────────────────────
            SectionHeader("Server Region")

            RegionSelector(
                selectedRegion = settings.region,
                onRegionSelected = viewModel::onRegionChange
            )

            // ── Section: Refresh ───────────────────────────────────────────
            SectionHeader("Refresh Interval: ${settings.refreshInterval} min")

            Slider(
                value = settings.refreshInterval.toFloat(),
                onValueChange = { viewModel.onRefreshIntervalChange(it.toInt()) },
                valueRange = 1f..30f,
                steps = 28,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF80CBC4),
                    activeTrackColor = Color(0xFF80CBC4)
                )
            )

            // ── Section: Options ───────────────────────────────────────────
            SectionHeader("Options")

            SettingsToggle(
                label = "Kiosk Mode",
                description = "Prevent leaving the app (requires Device Owner setup)",
                checked = settings.kioskMode,
                onCheckedChange = viewModel::onKioskModeChange
            )

            SettingsToggle(
                label = "Use Mock Data",
                description = "Use simulated glucose values (no real account needed)",
                checked = settings.useMock,
                onCheckedChange = viewModel::onUseMockChange
            )

            Spacer(Modifier.height(8.dp))

            // ── Save button ────────────────────────────────────────────────
            Button(
                onClick = viewModel::saveSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
            ) {
                Text("Save Settings", fontSize = 20.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = Color(0xFF80CBC4),
        modifier = Modifier.padding(top = 8.dp)
    )
    HorizontalDivider(color = Color(0xFF333333))
}

@Composable
private fun RegionSelector(
    selectedRegion: String,
    onRegionSelected: (String) -> Unit
) {
    val regions = listOf("EU", "US", "DE", "FR")
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        regions.forEach { region ->
            val selected = region == selectedRegion
            FilterChip(
                selected = selected,
                onClick = { onRegionSelected(region) },
                label = { Text(region, fontSize = 18.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF00897B),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF2A2A2A),
                    labelColor = Color.LightGray
                )
            )
        }
    }
}

@Composable
private fun SettingsToggle(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = 16.sp)
            Text(description, color = Color.Gray, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF80CBC4))
        )
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Color(0xFF80CBC4),
    unfocusedBorderColor = Color(0xFF555555),
    focusedLabelColor = Color(0xFF80CBC4),
    unfocusedLabelColor = Color.Gray,
    cursorColor = Color(0xFF80CBC4)
)

