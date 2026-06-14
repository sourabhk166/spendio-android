package com.khaata.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * First-run explainer for the Notification Access permission. Sets up the privacy
 * promise *before* sending the user to the system settings screen (prominent
 * disclosure — also required by Play review, see docs/06-prelaunch-checklist.md).
 */
@Composable
fun PermissionScreen(
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Khaata", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Track every payment automatically — no manual entry.",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(24.dp))

            Bullet("Khaata reads your bank & UPI notifications to log spends as they happen.")
            Bullet("Everything is processed and stored on your phone. Nothing is ever uploaded.")
            Bullet("No account, no internet permission, no ads. It will never become a loan app.")

            Spacer(Modifier.height(32.dp))
            Button(onClick = onGrantClick, modifier = Modifier.fillMaxWidth()) {
                Text("Allow notification access")
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "You'll be taken to a system settings screen. Find \"Khaata\" and turn it on, then come back.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun Bullet(text: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Text("•  ", style = MaterialTheme.typography.bodyLarge)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
