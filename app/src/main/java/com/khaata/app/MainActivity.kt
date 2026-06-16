package com.khaata.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.mutableStateOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.khaata.app.capture.NotificationAccess
import com.khaata.app.data.prefs.SettingsStore
import com.khaata.app.ui.AppNav
import com.khaata.app.ui.lock.AppLock
import com.khaata.app.ui.lock.LockScreen
import com.khaata.app.ui.onboarding.PermissionScreen
import com.khaata.app.ui.theme.KhaataTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var settings: SettingsStore

    // Re-checked in onResume so returning from the system settings screen updates the UI.
    private val captureEnabled = mutableStateOf(false)
    private val locked = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        captureEnabled.value = NotificationAccess.isEnabled(this)
        setContent {
            KhaataTheme {
                when {
                    locked.value -> LockScreen(onUnlock = ::promptUnlock)
                    captureEnabled.value -> AppNav(onEnableCapture = { NotificationAccess.openSettings(this) })
                    else -> PermissionScreen(onGrantClick = { NotificationAccess.openSettings(this) })
                }
            }
        }
        lifecycleScope.launch {
            if (settings.appLockEnabledOnce() && AppLock.canAuthenticate(this@MainActivity)) {
                locked.value = true
                promptUnlock()
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
