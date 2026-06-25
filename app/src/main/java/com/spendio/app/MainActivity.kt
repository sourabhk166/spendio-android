package com.spendio.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.spendio.app.capture.BatteryOptimization
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
                        onSkip = ::skipRestore,
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
            // Gated on restorePromptHandledOnce() so this asks at most once per install: without
            // it, enabling Auto Backup with zero transactions (or wiping data afterwards) would
            // make autoBackupExists() && count()==0 true on every single launch, forever.
            if (repository.count() == 0 && autoBackup.autoBackupExists() && !settings.restorePromptHandledOnce()) {
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
                settings.setRestorePromptHandled(true)
                offerRestore.value = false
                Toast.makeText(this@MainActivity, "Restored $count transactions", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun skipRestore() {
        offerRestore.value = false
        lifecycleScope.launch { settings.setRestorePromptHandled(true) }
    }

    private fun promptUnlock() {
        AppLock.authenticate(this) { locked.value = false }
    }

    override fun onResume() {
        super.onResume()
        val wasEnabled = captureEnabled.value
        captureEnabled.value = NotificationAccess.isEnabled(this)
        // The moment notification access is first granted is the natural spot to also ask for
        // the battery-optimization exemption — the user's already in a "grant permissions" frame
        // of mind, and it's the single biggest reason capture silently stops working later.
        // Gated to once ever (not just once per resume) so it can't loop: this flips to true
        // before the request fires, and the request's own return trip is itself an onResume.
        if (!wasEnabled && captureEnabled.value) {
            lifecycleScope.launch {
                if (!settings.batteryExemptionPromptedOnce()) {
                    settings.setBatteryExemptionPrompted(true)
                    if (!BatteryOptimization.isIgnoring(this@MainActivity)) {
                        startActivity(BatteryOptimization.requestIntent(this@MainActivity))
                    }
                }
            }
        }
    }
}
