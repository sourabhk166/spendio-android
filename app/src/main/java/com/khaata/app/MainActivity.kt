package com.khaata.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.khaata.app.capture.NotificationAccess
import com.khaata.app.ui.AppNav
import com.khaata.app.ui.onboarding.PermissionScreen
import com.khaata.app.ui.theme.KhaataTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Re-checked in onResume so returning from the system settings screen updates the UI.
    private val captureEnabled = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        captureEnabled.value = NotificationAccess.isEnabled(this)
        setContent {
            KhaataTheme {
                if (captureEnabled.value) {
                    AppNav(onEnableCapture = { NotificationAccess.openSettings(this) })
                } else {
                    PermissionScreen(
                        onGrantClick = { NotificationAccess.openSettings(this) },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        captureEnabled.value = NotificationAccess.isEnabled(this)
    }
}
