package com.libredisplay.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libredisplay.ui.common.autofillField
import com.libredisplay.ui.common.rememberAutofillCommit

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun AccountSettingsScreen(
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val message by viewModel.message.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val scrollState = rememberScrollState()
    var passwordVisible by remember { mutableStateOf(false) }
    var showChangeAccountForm by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    val commitAutofill = rememberAutofillCommit()

    LaunchedEffect(message) {
        if (message == "Ustawienia zapisane") {
            viewModel.clearMessage()
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LibreLinkUp") },
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (message != null && message != "Ustawienia zapisane") {
                Text(
                    text = message.orEmpty(),
                    modifier = Modifier.padding(12.dp),
                    fontSize = 13.sp,
                    color = Color(0xFFFB923C)
                )
            }

            if (!showChangeAccountForm) {
                // Display current account info
                Text("Status", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF22C55E))
                Text("● Połączono", fontSize = 14.sp, color = Color.White)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text("Konto", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF22C55E))
                Text(settings.email.takeIf { it.isNotEmpty() }?.let { email ->
                    email.substring(0, 3) + "***@" + email.substringAfter("@")
                } ?: "Nie zalogowano", fontSize = 14.sp, color = Color.White)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text("Region", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF22C55E))
                Text(settings.regionMode.ifEmpty { "EU" }, fontSize = 14.sp, color = Color.White)

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                OutlinedButton(
                    onClick = { showChangeAccountForm = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Zmień konto")
                }

                OutlinedButton(
                    onClick = { showLogoutConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Wyloguj", color = Color(0xFFEF4444))
                }
            } else {
                // Show login form
                Text("Zmień konto", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)

                Text("Użyj tego samego konta, którego używasz w aplikacji LibreLink / LibreLinkUp.", fontSize = 13.sp, color = Color(0xFF94A3B8))

                OutlinedTextField(
                    value = settings.email,
                    onValueChange = viewModel::onEmailChange,
                    label = { Text("Email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .autofillField(
                            autofillTypes = listOf(
                                AutofillType.Username,
                                AutofillType.EmailAddress
                            ),
                            onFill = viewModel::onEmailChange
                        ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                OutlinedTextField(
                    value = settings.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text("Hasło") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .autofillField(
                            autofillTypes = listOf(AutofillType.Password),
                            onFill = viewModel::onPasswordChange
                        ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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

                OutlinedTextField(
                    value = settings.regionMode,
                    onValueChange = { viewModel.onRegionModeChange(it.uppercase()) },
                    label = { Text("Region logowania") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showChangeAccountForm = false },
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        Text("Anuluj")
                    }
                    Button(
                        onClick = {
                            // Let the password manager offer to save the credentials.
                            commitAutofill()
                            viewModel.saveAndLogin()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        Text(if (isSaving) "Zapisywanie..." else "Zaloguj")
                    }
                }
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Wylogować konto?") },
            text = { Text("Aplikacja usunie lokalny token sesji i poprosi o ponowne logowanie.") },
            confirmButton = {
                Button(onClick = {
                    showLogoutConfirm = false
                    viewModel.resetSession()
                }) {
                    Text("Wyloguj")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutConfirm = false }) {
                    Text("Anuluj")
                }
            }
        )
    }
}
