package com.libredisplay.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libredisplay.BuildConfig
import com.libredisplay.ui.privacy.PRIVACY_POLICY_URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStatistics: () -> Unit
) {
    val context = LocalContext.current
    val showLicenses = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("O aplikacji") },
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("LibreCare", fontSize = 28.sp)
            Text("Wersja ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", fontSize = 14.sp)
            Text("Zrodlo danych: LibreLinkUp", fontSize = 13.sp)
            Text(
                "LibreCare nie jest wyrobem medycznym i nie zastepuje porady lekarza.",
                fontSize = 12.sp
            )

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
                    }
                }
            ) {
                Text("Polityka prywatności")
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToStatistics
            ) {
                Text("Informacje i statystyki")
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showLicenses.value = true }
            ) {
                Text("Licencje open source")
            }
        }
    }

    if (showLicenses.value) {
        AlertDialog(
            onDismissRequest = { showLicenses.value = false },
            title = { Text("Licencje open source") },
            text = {
                Text(
                    "Ta aplikacja korzysta z oprogramowania open source, w tym AndroidX, Kotlin, Retrofit, OkHttp, Room, WorkManager i Compose. " +
                        "Szczegóły zależności znajdują się w konfiguracji Gradle projektu."
                )
            },
            confirmButton = {
                Button(onClick = { showLicenses.value = false }) {
                    Text("Zamknij")
                }
            }
        )
    }
}

