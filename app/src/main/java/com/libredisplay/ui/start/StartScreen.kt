package com.libredisplay.ui.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StartScreen(
    onConnectWithLibreLinkUp: () -> Unit,
    onTryDemoMode: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("LibreCare", fontSize = 28.sp)
                Text("Monitorowanie glikemii z konta LibreLinkUp")
                Text("LibreCare nie jest wyrobem medycznym i nie zastępuje porady lekarza.")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(onClick = onConnectWithLibreLinkUp, modifier = Modifier.fillMaxWidth()) {
                Text("Połącz z LibreLinkUp")
            }
            OutlinedButton(onClick = onTryDemoMode, modifier = Modifier.fillMaxWidth()) {
                Text("Uruchom tryb demo")
            }
            Text(
                "Tryb demo używa przykładowych danych i nie wymaga logowania.",
                fontSize = 12.sp
            )
        }
    }
}

