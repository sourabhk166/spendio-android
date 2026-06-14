package com.khaata.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.khaata.app.ui.home.HomeScreen
import com.khaata.app.ui.review.ReviewScreen

private enum class Screen { HOME, REVIEW }

/**
 * Minimal in-app navigation (Home <-> Review). Deliberately a simple state switch rather
 * than a nav library — there are only two destinations in this slice.
 */
@Composable
fun AppNav(onEnableCapture: () -> Unit) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    when (screen) {
        Screen.HOME -> HomeScreen(
            permissionGranted = true,
            onEnableCapture = onEnableCapture,
            onOpenReview = { screen = Screen.REVIEW },
        )
        Screen.REVIEW -> ReviewScreen(onBack = { screen = Screen.HOME })
    }
}
