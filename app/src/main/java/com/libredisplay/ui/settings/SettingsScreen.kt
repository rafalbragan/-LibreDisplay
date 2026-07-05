package com.libredisplay.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.fragment.app.FragmentActivity
import com.libredisplay.R
import com.libredisplay.auth.BiometricAuthManager
import com.libredisplay.auth.BiometricResult

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
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    val saveSuccessMessage = stringResource(R.string.save_success)

    // Show a toast-style snackbar on successful save
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            snackbarHostState.showSnackbar(saveSuccessMessage)
            viewModel.clearSaveSuccess()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontSize = 22.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
            SectionHeader(stringResource(R.string.section_account))

            OutlinedTextField(
                value = settings.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text(stringResource(R.string.email)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            OutlinedTextField(
                value = settings.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text(stringResource(R.string.password)) },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None
                                       else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff
                                          else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) stringResource(R.string.hide_password) else stringResource(R.string.show_password),
                            tint = Color.LightGray
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors()
            )

            // ── Section: Region ────────────────────────────────────────────
            SectionHeader(stringResource(R.string.section_region))

            RegionSelector(
                selectedRegion = settings.region,
                onRegionSelected = viewModel::onRegionChange
            )

            // ── Section: Refresh ───────────────────────────────────────────
            SectionHeader(stringResource(R.string.section_refresh_interval, settings.refreshInterval))

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
            SectionHeader(stringResource(R.string.section_options))

            SettingsToggle(
                label = stringResource(R.string.kiosk_mode),
                description = stringResource(R.string.kiosk_mode_description),
                checked = settings.kioskMode,
                onCheckedChange = viewModel::onKioskModeChange
            )

            SettingsToggle(
                label = stringResource(R.string.use_mock_data),
                description = stringResource(R.string.use_mock_data_description),
                checked = settings.useMock,
                onCheckedChange = viewModel::onUseMockChange
            )

            Text(
                text = stringResource(R.string.observer_account_hint),
                color = Color.LightGray,
                fontSize = 13.sp
            )

            Text(
                text = stringResource(R.string.librelink_checklist),
                color = Color(0xFFB0BEC5),
                fontSize = 12.sp
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
                Text(stringResource(R.string.button_save), fontSize = 20.sp)
            }

            OutlinedButton(
                onClick = viewModel::loadCredentialsFromManager,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.load_credentials_from_manager))
            }

            OutlinedButton(
                onClick = viewModel::saveCredentialsInManager,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save_credentials_to_manager))
            }

            OutlinedButton(
                onClick = {
                    val activity = context as? FragmentActivity
                    if (activity == null) {
                        viewModel.clearSaveSuccess()
                        return@OutlinedButton
                    }
                    val manager = BiometricAuthManager(activity)
                    if (!manager.canAuthenticate()) {
                        activity.lifecycleScope.launchWhenStarted {
                            snackbarHostState.showSnackbar(activity.getString(R.string.biometric_not_available))
                        }
                        return@OutlinedButton
                    }
                    activity.lifecycleScope.launchWhenStarted {
                        when (
                            manager.authenticate(
                                title = activity.getString(R.string.unlock_biometric),
                                subtitle = activity.getString(R.string.biometric_subtitle)
                            )
                        ) {
                            BiometricResult.Success -> viewModel.loadCredentialsFromManager()
                            BiometricResult.Cancelled -> snackbarHostState.showSnackbar(activity.getString(R.string.biometric_cancelled))
                            BiometricResult.NotAvailable -> snackbarHostState.showSnackbar(activity.getString(R.string.biometric_not_available))
                            BiometricResult.LockedOut -> snackbarHostState.showSnackbar(activity.getString(R.string.biometric_locked_out))
                            is BiometricResult.Error -> snackbarHostState.showSnackbar(activity.getString(R.string.biometric_failed))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.unlock_biometric))
            }

            OutlinedButton(
                onClick = viewModel::logoutAndClearLocalData,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80))
            ) {
                Text(stringResource(R.string.logout_and_clear_data))
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

