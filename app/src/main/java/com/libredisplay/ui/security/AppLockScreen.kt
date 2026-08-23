package com.libredisplay.ui.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.libredisplay.auth.BiometricAuthManager
import com.libredisplay.auth.BiometricResult
import com.libredisplay.ui.theme.LibreCareColors
import kotlinx.coroutines.launch

/**
 * Blocking unlock screen shown before any medical data becomes visible.
 *
 * Uses the Android keyguard, therefore it supports fingerprint, face unlock, PIN, pattern and
 * password (screen lock) with a single prompt.
 */
@Composable
fun AppLockScreen(
    onUnlocked: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context as? FragmentActivity }
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var prompting by remember { mutableStateOf(false) }

    fun prompt() {
        val hostActivity = activity ?: run {
            onUnlocked()
            return
        }
        if (prompting) return
        prompting = true
        scope.launch {
            val manager = BiometricAuthManager(hostActivity)
            if (!manager.canAuthenticate()) {
                // Never lock the user out of their own medical data.
                onUnlocked()
                return@launch
            }
            when (val result = manager.authenticate(
                title = "Odblokuj LibreCare",
                subtitle = "Użyj odcisku palca, PIN-u lub blokady ekranu"
            )) {
                BiometricResult.Success -> onUnlocked()
                BiometricResult.Cancelled -> status = "Odblokowanie anulowane."
                BiometricResult.NotAvailable -> onUnlocked()
                BiometricResult.LockedOut -> status = "Zbyt wiele prób. Spróbuj ponownie za chwilę."
                is BiometricResult.Error -> status = result.message
            }
            prompting = false
        }
    }

    LaunchedEffect(Unit) { prompt() }

    Surface(color = LibreCareColors.Background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "LibreCare jest zablokowane",
                color = LibreCareColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Aby zobaczyć dane medyczne, potwierdź swoją tożsamość.",
                color = LibreCareColors.TextSecondary,
                fontSize = 14.sp
            )
            status?.let {
                Text(it, color = LibreCareColors.AccentAmber, fontSize = 13.sp)
            }
            Button(onClick = { prompt() }) { Text("Odblokuj") }
            OutlinedButton(onClick = onExit) { Text("Zamknij aplikację") }
        }
    }
}

