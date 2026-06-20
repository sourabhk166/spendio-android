package com.spendio.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendio.app.data.DataMaintenance
import com.spendio.app.data.backup.AutoBackupController
import com.spendio.app.data.backup.BackupManager
import com.spendio.app.data.export.CsvExporter
import com.spendio.app.data.export.ExportRange
import com.spendio.app.data.prefs.SettingsStore
import com.spendio.app.domain.repository.TransactionRepository
import com.spendio.app.parser.KnownSources
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SourceToggle(val packageName: String, val displayName: String, val enabled: Boolean)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsStore,
    private val dataMaintenance: DataMaintenance,
    private val repository: TransactionRepository,
    private val backupManager: BackupManager,
    private val autoBackup: AutoBackupController,
) : ViewModel() {

    val appLockEnabled: StateFlow<Boolean> =
        settings.appLockEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val autoBackupEnabled: StateFlow<Boolean> =
        settings.autoBackupEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val userName: StateFlow<String> =
        settings.userName.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun setUserName(name: String) = viewModelScope.launch { settings.setUserName(name) }

    fun enableAutoBackup(passphrase: CharArray) = viewModelScope.launch { autoBackup.enable(passphrase) }

    fun disableAutoBackup() = viewModelScope.launch { autoBackup.disable() }

    // ---- Google Drive ----

    private val _driveConnected = kotlinx.coroutines.flow.MutableStateFlow(autoBackup.isDriveConnected())
    val driveConnected: StateFlow<Boolean> = _driveConnected

    fun driveEmail(): String? = autoBackup.driveEmail()
    fun hasBackupPassphrase(): Boolean = autoBackup.hasPassphrase()
    fun driveSignInIntent(): android.content.Intent = autoBackup.driveSignInClient().signInIntent

    /** Handles the sign-in result; [passphrase] is the just-set one when first connecting. */
    fun onDriveSignIn(data: android.content.Intent?, passphrase: CharArray?, onResult: (Boolean, String?) -> Unit) =
        viewModelScope.launch {
            var errMsg: String? = null
            val ok = runCatching {
                com.google.android.gms.auth.api.signin.GoogleSignIn
                    .getSignedInAccountFromIntent(data)
                    .getResult(com.google.android.gms.common.api.ApiException::class.java)
                autoBackup.onDriveConnected(passphrase)
            }.onFailure { e ->
                errMsg = when (e) {
                    is com.google.android.gms.common.api.ApiException ->
                        "code ${e.statusCode}${if (e.statusCode == 10) " — Google Cloud setup needed" else ""}"
                    else -> e.message ?: "unknown error"
                }
            }.isSuccess
            _driveConnected.value = autoBackup.isDriveConnected()
            onResult(ok, errMsg)
        }

    fun backupToDriveNow(onResult: (Boolean) -> Unit) = viewModelScope.launch {
        onResult(runCatching { autoBackup.backupToDrive() }.isSuccess)
    }

    fun restoreFromDrive(passphrase: CharArray, onResult: (Result<Int>) -> Unit) = viewModelScope.launch {
        onResult(runCatching { autoBackup.restoreFromDrive(passphrase) })
    }

    fun disconnectDrive() = viewModelScope.launch {
        autoBackup.disconnectDrive()
        _driveConnected.value = false
    }

    val sources: StateFlow<List<SourceToggle>> =
        settings.disabledSources.map { disabled ->
            KnownSources.ALL.map { SourceToggle(it.packageName, it.displayName, it.packageName !in disabled) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setAppLock(enabled: Boolean) = viewModelScope.launch { settings.setAppLock(enabled) }

    fun toggleSource(packageName: String, enabled: Boolean) =
        viewModelScope.launch { settings.setSourceEnabled(packageName, enabled) }

    fun wipeAll() = viewModelScope.launch { dataMaintenance.wipeAll() }

    /**
     * Builds the CSV for the chosen [range] off the main thread and hands it back via [onReady]
     * (invoked on the main thread). The caller writes the file and launches the share sheet,
     * which need an Activity context. [onReady] receives null when there's nothing to export.
     */
    fun exportCsv(range: ExportRange, onReady: (String?) -> Unit) = viewModelScope.launch {
        val txns = if (range is ExportRange.AllTime) {
            repository.allTransactions()
        } else {
            val (from, to) = range.bounds()
            repository.transactionsBetween(from, to)
        }
        onReady(if (txns.isEmpty()) null else CsvExporter.toCsv(txns))
    }

    /** Builds the encrypted backup bytes; [onReady] gets null if there's nothing to back up. */
    fun createBackup(passphrase: CharArray, onReady: (ByteArray?) -> Unit) = viewModelScope.launch {
        val hasData = repository.count() > 0
        onReady(if (hasData) backupManager.createBackup(passphrase) else null)
    }

    /** Decrypts and imports [blob]; [onResult] gets the count added, or a failure on bad passphrase. */
    fun restoreBackup(blob: ByteArray, passphrase: CharArray, onResult: (Result<Int>) -> Unit) =
        viewModelScope.launch {
            onResult(runCatching { backupManager.restoreBackup(blob, passphrase) })
        }
}
