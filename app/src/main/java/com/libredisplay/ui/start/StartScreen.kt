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
                Text("Monitor glucose with LibreLinkUp or try Demo Mode without login.")
                Text("LibreCare is not a medical device and does not provide medical advice, diagnosis, treatment recommendations or emergency alerts.")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(onClick = onConnectWithLibreLinkUp, modifier = Modifier.fillMaxWidth()) {
                Text("Connect with LibreLinkUp")
            }
            OutlinedButton(onClick = onTryDemoMode, modifier = Modifier.fillMaxWidth()) {
                Text("Try Demo Mode")
            }
            Text(
                "Demo Mode uses simulated glucose data. Do not use it for medical decisions.",
                fontSize = 12.sp
            )
        }
    }
}

