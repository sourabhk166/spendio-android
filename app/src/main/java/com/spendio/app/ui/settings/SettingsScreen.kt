package com.spendio.app.ui.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.spendio.app.BuildConfig
import com.spendio.app.data.export.ExportRange
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendio.app.capture.BatteryOptimization
import com.spendio.app.ui.AutoSizeText

// Guards the PBKDF2-derived backup encryption key from trivially short passphrases.
private const val MIN_PASSPHRASE_LENGTH = 8

@Composable
fun SettingsScreen(
    onOpenBudgets: () -> Unit = {},
    onOpenSources: () -> Unit = {},
    onOpenBackupHelp: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val appLock by viewModel.appLockEnabled.collectAsStateWithLifecycle()
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsStateWithLifecycle()
    val driveConnected by viewModel.driveConnected.collectAsStateWithLifecycle()
    // val sources by viewModel.sources.collectAsStateWithLifecycle() — only fed the disabled "Manage sources" row below
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    var confirmWipe by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var pickCustomRange by remember { mutableStateOf(false) }
    var autoBackupPassphrase by remember { mutableStateOf(false) }
    var driveOptions by remember { mutableStateOf(false) }
    var driveSetPass by remember { mutableStateOf(false) }
    var driveRestorePass by remember { mutableStateOf(false) }
    var pendingDrivePass by remember { mutableStateOf<CharArray?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // No callback exists for this system setting, so re-check it whenever the screen resumes —
    // covers both "just came back from the battery-exemption dialog" and "changed it in
    // system Settings directly".
    var batteryExempt by remember { mutableStateOf(BatteryOptimization.isIgnoring(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) batteryExempt = BatteryOptimization.isIgnoring(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val runExport: (ExportRange) -> Unit = { range ->
        viewModel.exportCsv(range) { csv ->
            if (csv == null) {
                Toast.makeText(context, "Nothing to export in that range", Toast.LENGTH_SHORT).show()
            } else {
                shareCsv(context, csv)
            }
        }
    }

    // Backup / restore state + Storage-Access-Framework launchers (no storage permission needed).
    var backupPassphrase by remember { mutableStateOf(false) }
    var restorePassphrase by remember { mutableStateOf(false) }
    var pendingBackup by remember { mutableStateOf<ByteArray?>(null) }
    var restoreBytes by remember { mutableStateOf<ByteArray?>(null) }

    val createDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val bytes = pendingBackup
        pendingBackup = null
        if (uri != null && bytes != null) {
            runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } }
                .onSuccess { Toast.makeText(context, "Backup saved", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(context, "Couldn't save backup", Toast.LENGTH_SHORT).show() }
        }
    }
    val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            if (bytes == null) {
                Toast.makeText(context, "Couldn't read that file", Toast.LENGTH_SHORT).show()
            } else {
                restoreBytes = bytes
                restorePassphrase = true
            }
        }
    }
    val driveSignIn = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.onDriveSignIn(result.data, pendingDrivePass) { ok, errMsg ->
            pendingDrivePass = null
            Toast.makeText(
                context,
                if (ok) "Connected — backed up to Google Drive"
                else "Couldn't connect Google Drive${errMsg?.let { ": $it" } ?: ""}",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        // Security card
        SectionCard(title = "Security") {
            SettingRow(
                title = "App lock",
                subtitle = "Require fingerprint / PIN to open Spendio",
            ) {
                Switch(checked = appLock, onCheckedChange = viewModel::setAppLock)
            }
        }

        // Profile card — the name is used to tell self-transfers (paying your own account) apart.
        SectionCard(title = "Profile") {
            ClickableRow(
                title = "Your name",
                subtitle = userName.ifBlank { "Set your name to detect self-transfers" },
                onClick = { editName = true },
            )
        }

        // Sources card. The per-app "Manage sources" row is disabled: the OS notification-access
        // permission is all-or-nothing regardless of these toggles (the listener still receives
        // every app's notifications either way; this only gated what CapturePipeline acted on
        // afterwards), so it implied a privacy boundary it didn't actually provide. Left
        // commented rather than deleted — NotificationSourcesScreen, SettingsViewModel.sources/
        // toggleSource, and SettingsStore.disabledSources are all still intact underneath.
        SectionCard(title = "Notification sources") {
            // ClickableRow(
            //     title = "Manage sources",
            //     subtitle = "${sources.count { it.enabled }} of ${sources.size} apps on",
            //     onClick = onOpenSources,
            // )
            // HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline)
            ClickableRow(
                title = "Background reliability",
                subtitle = if (batteryExempt) {
                    "Unrestricted — capture should keep running in the background"
                } else {
                    "Your phone may be killing capture in the background — tap to fix"
                },
                onClick = { context.startActivity(BatteryOptimization.requestIntent(context)) },
            )
        }

        // Data card
        SectionCard(title = "Data") {
            ClickableRow(
                title = "Monthly budgets",
                subtitle = "Set spending limits per category",
                onClick = onOpenBudgets,
            )
            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline)
            ClickableRow(
                title = "Export to CSV",
                subtitle = "Pick a date range to share as a spreadsheet",
                onClick = { showExport = true },
            )
        }

        // Backup card — everything is passphrase-encrypted; only ciphertext ever leaves the device.
        SectionCard(title = "Backup") {
            SettingRow(
                title = "Automatic backup",
                subtitle = "Encrypted, kept in sync with your Google account",
            ) {
                Switch(
                    checked = autoBackupEnabled,
                    onCheckedChange = { on -> if (on) autoBackupPassphrase = true else viewModel.disableAutoBackup() },
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline)
            ClickableRow(
                title = "Google Drive backup",
                subtitle = if (driveConnected) "Connected: ${viewModel.driveEmail() ?: "Google account"}"
                           else "Encrypted backup to your Google Drive",
                onClick = {
                    when {
                        driveConnected -> driveOptions = true
                        viewModel.hasBackupPassphrase() -> { pendingDrivePass = null; driveSignIn.launch(viewModel.driveSignInIntent()) }
                        else -> driveSetPass = true
                    }
                },
            )
            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline)
            ClickableRow(
                title = "Back up to a file",
                subtitle = "Save an encrypted backup you can restore later",
                onClick = { backupPassphrase = true },
            )
            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline)
            ClickableRow(
                title = "Restore from a file",
                subtitle = "Import transactions from a backup file",
                onClick = { openDoc.launch(arrayOf("*/*")) },
            )
            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline)
            ClickableRow(
                title = "How backup works",
                subtitle = "What each option does, and why it matters",
                onClick = onOpenBackupHelp,
            )
        }

        // About card
        SectionCard(title = "About") {
            SettingRow(
                title = "Spendio",
                subtitle = "Version ${BuildConfig.VERSION_NAME}",
            ) {}
        }

        // Danger zone card
        Spacer(Modifier.height(6.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Danger zone",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                OutlinedButton(
                    onClick = { confirmWipe = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete all data")
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Permanently erases every transaction, learned format and rule on this device. Cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text("Delete all data?") },
            text = { Text("This erases everything Spendio has stored on this device. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.wipeAll {
                            Toast.makeText(context, "All data deleted", Toast.LENGTH_SHORT).show()
                        }
                        confirmWipe = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text("Cancel") } },
        )
    }

    if (autoBackupPassphrase) {
        var pass by remember { mutableStateOf("") }
        var confirm by remember { mutableStateOf("") }
        val valid = pass.length >= MIN_PASSPHRASE_LENGTH && pass == confirm
        AlertDialog(
            onDismissRequest = { autoBackupPassphrase = false },
            title = { Text("Turn on automatic backup") },
            text = {
                Column {
                    Text(
                        "Spendio keeps an encrypted backup in sync with your phone's Google account. Set a passphrase — you'll need it to restore on a new phone, and it can't be recovered if forgotten.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(pass, { pass = it }, singleLine = true, label = { Text("Passphrase") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(confirm, { confirm = it }, singleLine = true, label = { Text("Confirm passphrase") }, modifier = Modifier.fillMaxWidth())
                    if (pass.isNotEmpty() && pass.length < MIN_PASSPHRASE_LENGTH) {
                        Text("At least $MIN_PASSPHRASE_LENGTH characters.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = valid,
                    onClick = {
                        viewModel.enableAutoBackup(pass.toCharArray())
                        autoBackupPassphrase = false
                        Toast.makeText(context, "Automatic backup on", Toast.LENGTH_SHORT).show()
                    },
                ) { Text("Turn on") }
            },
            dismissButton = { TextButton(onClick = { autoBackupPassphrase = false }) { Text("Cancel") } },
        )
    }

    if (driveSetPass) {
        var pass by remember { mutableStateOf("") }
        var confirm by remember { mutableStateOf("") }
        val valid = pass.length >= MIN_PASSPHRASE_LENGTH && pass == confirm
        AlertDialog(
            onDismissRequest = { driveSetPass = false },
            title = { Text("Set a backup passphrase") },
            text = {
                Column {
                    Text(
                        "Your Drive backup is encrypted with this. You'll need it to restore on a new phone, and it can't be recovered if forgotten.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(pass, { pass = it }, singleLine = true, label = { Text("Passphrase") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(confirm, { confirm = it }, singleLine = true, label = { Text("Confirm passphrase") }, modifier = Modifier.fillMaxWidth())
                    if (pass.isNotEmpty() && pass.length < MIN_PASSPHRASE_LENGTH) {
                        Text("At least $MIN_PASSPHRASE_LENGTH characters.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = valid, onClick = {
                    pendingDrivePass = pass.toCharArray()
                    driveSetPass = false
                    driveSignIn.launch(viewModel.driveSignInIntent())
                }) { Text("Connect") }
            },
            dismissButton = { TextButton(onClick = { driveSetPass = false }) { Text("Cancel") } },
        )
    }

    if (driveOptions) {
        AlertDialog(
            onDismissRequest = { driveOptions = false },
            title = { Text("Google Drive") },
            text = {
                Column {
                    ExportOption("Back up now") {
                        driveOptions = false
                        viewModel.backupToDriveNow { ok ->
                            Toast.makeText(context, if (ok) "Backed up to Drive" else "Backup failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                    ExportOption("Restore from Drive") { driveOptions = false; driveRestorePass = true }
                    ExportOption("Disconnect") {
                        driveOptions = false
                        viewModel.disconnectDrive()
                        Toast.makeText(context, "Disconnected Google Drive", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { driveOptions = false }) { Text("Close") } },
        )
    }

    if (driveRestorePass) {
        var pass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { driveRestorePass = false },
            title = { Text("Restore from Drive") },
            text = {
                OutlinedTextField(pass, { pass = it }, singleLine = true, label = { Text("Backup passphrase") }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(enabled = pass.isNotEmpty(), onClick = {
                    driveRestorePass = false
                    viewModel.restoreFromDrive(pass.toCharArray()) { result ->
                        result.fold(
                            onSuccess = { count ->
                                val msg = if (count == 0) "No backup found in Drive" else "Restored $count transactions"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { Toast.makeText(context, "Wrong passphrase or no backup", Toast.LENGTH_SHORT).show() },
                        )
                    }
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { driveRestorePass = false }) { Text("Cancel") } },
        )
    }

    if (showExport) {
        AlertDialog(
            onDismissRequest = { showExport = false },
            title = { Text("Export to CSV") },
            text = {
                Column {
                    ExportOption("This month") { showExport = false; runExport(ExportRange.ThisMonth) }
                    ExportOption("Last 3 months") { showExport = false; runExport(ExportRange.Last3Months) }
                    ExportOption("This year") { showExport = false; runExport(ExportRange.ThisYear) }
                    ExportOption("All time") { showExport = false; runExport(ExportRange.AllTime) }
                    ExportOption("Custom range…") { showExport = false; pickCustomRange = true }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showExport = false }) { Text("Cancel") } },
        )
    }

    if (pickCustomRange) {
        CustomRangeDialog(
            onDismiss = { pickCustomRange = false },
            onPick = { start, end ->
                pickCustomRange = false
                runExport(ExportRange.Custom(start, end + 86_399_999L))
            },
        )
    }

    if (editName) {
        var draft by remember { mutableStateOf(userName) }
        AlertDialog(
            onDismissRequest = { editName = false },
            title = { Text("Your name") },
            text = {
                Column {
                    Text(
                        "Used only on-device to recognise transfers to your own accounts as transfers, not spending.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        singleLine = true,
                        placeholder = { Text("e.g. Arjun Mehta") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setUserName(draft); editName = false }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editName = false }) { Text("Cancel") } },
        )
    }

    if (backupPassphrase) {
        var pass by remember { mutableStateOf("") }
        var confirm by remember { mutableStateOf("") }
        val valid = pass.length >= MIN_PASSPHRASE_LENGTH && pass == confirm
        AlertDialog(
            onDismissRequest = { backupPassphrase = false },
            title = { Text("Set a backup passphrase") },
            text = {
                Column {
                    Text(
                        "You'll need this exact passphrase to restore on a new phone. It can't be recovered if forgotten — write it down.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(pass, { pass = it }, singleLine = true, label = { Text("Passphrase") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(confirm, { confirm = it }, singleLine = true, label = { Text("Confirm passphrase") }, modifier = Modifier.fillMaxWidth())
                    if (pass.isNotEmpty() && pass.length < MIN_PASSPHRASE_LENGTH) {
                        Text("At least $MIN_PASSPHRASE_LENGTH characters.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = valid,
                    onClick = {
                        backupPassphrase = false
                        viewModel.createBackup(pass.toCharArray()) { bytes ->
                            if (bytes == null) {
                                Toast.makeText(context, "Nothing to back up yet", Toast.LENGTH_SHORT).show()
                            } else {
                                pendingBackup = bytes
                                createDoc.launch("spendio-backup.kbk")
                            }
                        }
                    },
                ) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { backupPassphrase = false }) { Text("Cancel") } },
        )
    }

    if (restorePassphrase) {
        var pass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { restorePassphrase = false },
            title = { Text("Enter backup passphrase") },
            text = {
                OutlinedTextField(pass, { pass = it }, singleLine = true, label = { Text("Passphrase") }, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(
                    enabled = pass.isNotEmpty(),
                    onClick = {
                        val bytes = restoreBytes
                        restorePassphrase = false
                        if (bytes != null) {
                            viewModel.restoreBackup(bytes, pass.toCharArray()) { result ->
                                result.fold(
                                    onSuccess = { count ->
                                        Toast.makeText(context, "Restored $count transactions", Toast.LENGTH_SHORT).show()
                                    },
                                    onFailure = {
                                        Toast.makeText(context, "Wrong passphrase or invalid file", Toast.LENGTH_SHORT).show()
                                    },
                                )
                            }
                        }
                    },
                ) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { restorePassphrase = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            content()
        }
    }
}

@Composable
private fun ExportOption(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRangeDialog(
    onDismiss: () -> Unit,
    onPick: (start: Long, end: Long) -> Unit,
) {
    // Single dialog, pick start then end on the same calendar — the standard date-range
    // picker pattern (same one Google's own apps use), instead of two separate dialogs.
    val state = rememberDateRangePickerState(
        initialDisplayMode = androidx.compose.material3.DisplayMode.Picker,
        yearRange = 2000..java.time.Year.now().value,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null,
                onClick = {
                    val start = state.selectedStartDateMillis
                    val end = state.selectedEndDateMillis
                    if (start != null && end != null) onPick(start, end)
                },
            ) { Text("Export") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DateRangePicker(
            state = state,
            modifier = Modifier.height(500.dp),
            title = { Text("Select range", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
            headline = {
                // Default M3 headline wraps mid-phrase ("23 Jun 2026 -\n25 Jun 2026") on
                // devices with a larger system font scale (e.g. Samsung One UI). Force it to
                // one line and shrink instead, so it never looks broken.
                AutoSizeText(
                    text = rangeHeadline(state.selectedStartDateMillis, state.selectedEndDateMillis),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                )
            },
        )
    }
}

private fun rangeHeadline(startMillis: Long?, endMillis: Long?): String {
    fun fmt(millis: Long): String =
        java.time.Instant.ofEpochMilli(millis)
            .atZone(java.time.ZoneOffset.UTC)
            .toLocalDate()
            .format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
    return when {
        startMillis != null && endMillis != null -> "${fmt(startMillis)} – ${fmt(endMillis)}"
        startMillis != null -> "${fmt(startMillis)} – End date"
        else -> "Start date – End date"
    }
}

@Composable
private fun ClickableRow(title: String, subtitle: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("›", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun shareCsv(context: Context, csv: String) {
    val dir = java.io.File(context.cacheDir, "exports").apply { mkdirs() }
    val file = java.io.File(dir, "spendio-export.csv")
    file.writeText(csv)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Export transactions"))
}

@Composable
private fun SettingRow(title: String, subtitle: String? = null, trailing: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}
