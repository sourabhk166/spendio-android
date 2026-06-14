package com.khaata.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val appLock by viewModel.appLockEnabled.collectAsStateWithLifecycle()
    val sources by viewModel.sources.collectAsStateWithLifecycle()
    var confirmWipe by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("‹ Back") }
                Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }

            SettingRow(
                title = "App lock",
                subtitle = "Require fingerprint / PIN to open Khaata",
            ) {
                Switch(checked = appLock, onCheckedChange = viewModel::setAppLock)
            }

            Divider(Modifier.padding(vertical = 12.dp))
            Text("Sources read", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text(
                "Turn off any app you don't want Khaata to read notifications from.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            sources.forEach { source ->
                SettingRow(title = source.displayName) {
                    Switch(
                        checked = source.enabled,
                        onCheckedChange = { viewModel.toggleSource(source.packageName, it) },
                    )
                }
            }

            Divider(Modifier.padding(vertical = 12.dp))
            Text("Danger zone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { confirmWipe = true }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Delete all data")
            }
            Text(
                "Permanently erases every transaction, learned format and rule on this device. Cannot be undone.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
            )
        }
    }

    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text("Delete all data?") },
            text = { Text("This erases everything Khaata has stored on this device. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.wipeAll()
                    confirmWipe = false
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String? = null, trailing: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        trailing()
    }
}
