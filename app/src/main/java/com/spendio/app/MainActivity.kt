package com.spendio.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.spendio.app.capture.NotificationAccess
import com.spendio.app.data.backup.AutoBackupController
import com.spendio.app.data.prefs.SettingsStore
import com.spendio.app.domain.repository.TransactionRepository
import com.spendio.app.ui.AppNav
import com.spendio.app.ui.lock.AppLock
import com.spendio.app.ui.lock.LockScreen
import com.spendio.app.ui.onboarding.PermissionScreen
import com.spendio.app.ui.onboarding.RestorePromptScreen
import com.spendio.app.ui.splash.BrandedSplashScreen
import com.spendio.app.ui.theme.SpendioTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var settings: SettingsStore
    @Inject lateinit var autoBackup: AutoBackupController
    @Inject lateinit var repository: TransactionRepository

    // Re-checked in onResume so returning from the system settings screen updates the UI.
    private val captureEnabled = mutableStateOf(false)
    private val locked = mutableStateOf(false)
    private val offerRestore = mutableStateOf(false)
    private val restoreError = mutableStateOf(false)
    private val showBrandedSplash = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        captureEnabled.value = NotificationAccess.isEnabled(this)
        setContent {
            SpendioTheme {
                when {
                    showBrandedSplash.value -> BrandedSplashScreen(onFinished = { showBrandedSplash.value = false })
                    offerRestore.value -> RestorePromptScreen(
                        error = restoreError.value,
                        onRestore = ::restoreFromBackup,
                        onSkip = { offerRestore.value = false },
                    )
                    locked.value -> LockScreen(onUnlock = ::promptUnlock)
                    captureEnabled.value -> AppNav(onEnableCapture = { NotificationAccess.openSettings(this) })
                    else -> PermissionScreen(
                        onGrant = { name ->
                            if (name.isNotBlank()) lifecycleScope.launch { settings.setUserName(name) }
                            NotificationAccess.openSettings(this)
                        },
                    )
                }
            }
        }
        lifecycleScope.launch {
            // A fresh install with a restored Auto Backup blob (new phone) → offer to restore.
            if (repository.count() == 0 && autoBackup.autoBackupExists()) {
                offerRestore.value = true
            }
            if (settings.appLockEnabledOnce() && AppLock.canAuthenticate(this@MainActivity)) {
                locked.value = true
                promptUnlock()
            }
        }
    }

    private fun restoreFromBackup(passphrase: CharArray) {
        lifecycleScope.launch {
            val count = runCatching { autoBackup.restoreFromAutoBackup(passphrase) }.getOrNull()
            if (count == null) {
                restoreError.value = true
            } else {
                offerRestore.value = false
                Toast.makeText(this@MainActivity, "Restored $count transactions", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun promptUnlock() {
        AppLock.authenticate(this) { locked.value = false }
    }

    override fun onResume() {
        super.onResume()
        captureEnabled.value = NotificationAccess.isEnabled(this)
    }
}
