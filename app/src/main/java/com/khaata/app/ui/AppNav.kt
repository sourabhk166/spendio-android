package com.khaata.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.khaata.app.ui.home.HomeScreen
import com.khaata.app.ui.review.ReviewScreen
import com.khaata.app.ui.settings.SettingsScreen

private enum class Screen { HOME, REVIEW, SETTINGS }

/**
 * Minimal in-app navigation (Home <-> Review/Settings). Deliberately a simple state switch
 * rather than a nav library — there are only a few destinations in this slice.
 */
@Composable
fun AppNav(onEnableCapture: () -> Unit) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    when (screen) {
        Screen.HOME -> HomeScreen(
            permissionGranted = true,
            onEnableCapture = onEnableCapture,
            onOpenReview = { screen = Screen.REVIEW },
            onOpenSettings = { screen = Screen.SETTINGS },
        )
        Screen.REVIEW -> ReviewScreen(onBack = { screen = Screen.HOME })
        Screen.SETTINGS -> SettingsScreen(onBack = { screen = Screen.HOME })
    }
}
