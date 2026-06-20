package com.spendio.app.data.backup

import android.content.Context
import androidx.room.InvalidationTracker
import com.spendio.app.data.db.SpendioDatabase
import com.spendio.app.data.prefs.SettingsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps an encrypted backup blob fresh and pushes it to the active destination(s):
 *  - **Android Auto Backup** — the blob lives in a directory the OS syncs to the user's Google
 *    account (res/xml backup rules), no INTERNET permission needed.
 *  - **Google Drive** (opt-in) — the same blob is uploaded to Drive's private appDataFolder via
 *    [DriveBackup] when an account is connected.
 *
 * Either way only passphrase-encrypted ciphertext leaves the device. The blob is regenerated,
 * debounced, on any change to the transactions table. On a new device the OS restores the
 * Auto-Backup blob (or the user restores from Drive); the app offers to import after the passphrase.
 */
@Singleton
class AutoBackupController @Inject constructor(
    @ApplicationContext context: Context,
    private val database: SpendioDatabase,
    private val backupManager: BackupManager,
    private val secret: AutoBackupSecret,
    private val settings: SettingsStore,
    private val driveBackup: DriveBackup,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dir = File(context.filesDir, "autobackup")
    private val file = File(dir, "spendio-auto.kbk")
    private var refreshJob: Job? = null

    /** Call once at app startup. Watches for data changes and refreshes the blob if enabled. */
    fun start() {
        // Registering the observer touches the DB (creates triggers), so do it off the main thread.
        scope.launch {
            database.invalidationTracker.addObserver(
                object : InvalidationTracker.Observer("transactions") {
                    override fun onInvalidated(tables: Set<String>) = scheduleRefresh()
                },
            )
            if (settings.autoBackupEnabledOnce() || driveBackup.isConnected()) refresh()
        }
    }

    private fun scheduleRefresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            delay(DEBOUNCE_MS)
            refresh()
        }
    }

    private suspend fun refresh() {
        // Refresh runs if either destination is active: OS Auto Backup or Google Drive.
        val wantLocal = settings.autoBackupEnabledOnce()
        val wantDrive = driveBackup.isConnected()
        if (!wantLocal && !wantDrive) return
        val pass = secret.getPassphrase() ?: return
        try {
            val bytes = backupManager.createBackup(pass)
            if (wantLocal) {
                dir.mkdirs()
                file.writeBytes(bytes)
            }
            if (wantDrive) {
                runCatching { driveBackup.upload(bytes) } // best-effort; network may be down
            }
        } catch (_: Exception) {
            // Best-effort; the next change (or app launch) will retry.
        } finally {
            pass.fill(' ')
        }
    }

    suspend fun enable(passphrase: CharArray) {
        secret.setPassphrase(passphrase)
        settings.setAutoBackup(true)
        refresh()
    }

    suspend fun disable() {
        settings.setAutoBackup(false)
        runCatching { file.delete() }
        if (!driveBackup.isConnected()) secret.clear() // keep the passphrase if Drive still uses it
    }

    /** True when a backup blob is on disk (including one restored by the OS on a new device). */
    fun autoBackupExists(): Boolean = file.exists() && file.length() > 0

    /** Imports the on-disk auto-backup blob; on success re-caches the passphrase and keeps it on. */
    suspend fun restoreFromAutoBackup(passphrase: CharArray): Int {
        val bytes = file.readBytes()
        val added = backupManager.restoreBackup(bytes, passphrase)
        enable(passphrase)
        return added
    }

    // ---- Google Drive (opt-in) ----

    fun driveSignInClient() = driveBackup.signInClient()
    fun isDriveConnected(): Boolean = driveBackup.isConnected()
    fun driveEmail(): String? = driveBackup.accountEmail
    fun hasPassphrase(): Boolean = secret.hasPassphrase()

    /** After a successful Drive sign-in: ensure a passphrase, then upload the current data. */
    suspend fun onDriveConnected(passphrase: CharArray?) {
        if (passphrase != null && !secret.hasPassphrase()) secret.setPassphrase(passphrase)
        backupToDrive()
    }

    suspend fun backupToDrive() {
        val pass = secret.getPassphrase() ?: return
        try {
            driveBackup.upload(backupManager.createBackup(pass))
        } finally {
            pass.fill(' ')
        }
    }

    suspend fun restoreFromDrive(passphrase: CharArray): Int {
        val bytes = driveBackup.download() ?: return 0
        val added = backupManager.restoreBackup(bytes, passphrase)
        if (!secret.hasPassphrase()) secret.setPassphrase(passphrase)
        return added
    }

    suspend fun disconnectDrive() {
        runCatching { driveBackup.signInClient().signOut() }
        if (!settings.autoBackupEnabledOnce()) secret.clear()
    }

    private companion object {
        const val DEBOUNCE_MS = 4_000L
    }
}
