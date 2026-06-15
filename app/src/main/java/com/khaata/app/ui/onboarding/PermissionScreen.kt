package com.khaata.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        // Brand icon circle
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text("₹", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Khaata",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Track every payment automatically —\nno manual entry.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(28.dp))

        PrivacyBullet("Reads bank & UPI notifications to log spends as they happen.")
        Spacer(Modifier.height(10.dp))
        PrivacyBullet("Everything stays on your phone — nothing is ever uploaded.")
        Spacer(Modifier.height(10.dp))
        PrivacyBullet("No account, no internet permission, no ads. Always private.")

        Spacer(Modifier.height(36.dp))

        Button(
            onClick = onGrantClick,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text("Allow notification access", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = "You'll be taken to a system settings screen. Find \"Khaata\" and turn it on, then come back.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PrivacyBullet(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text("✓", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}
