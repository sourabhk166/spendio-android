package com.khaata.app.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.khaata.app.ui.home.HomeScreen
import com.khaata.app.ui.review.ReviewScreen
import com.khaata.app.ui.settings.SettingsScreen

private enum class Screen { HOME, REVIEW, SETTINGS }

@Composable
fun AppNav(
    onEnableCapture: () -> Unit,
    viewModel: AppNavViewModel = hiltViewModel(),
) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    val reviewCount by viewModel.reviewCount.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = screen == Screen.HOME,
                    onClick = { screen = Screen.HOME },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                )
                NavigationBarItem(
                    selected = screen == Screen.REVIEW,
                    onClick = { screen = Screen.REVIEW },
                    icon = {
                        BadgedBox(
                            badge = { if (reviewCount > 0) Badge { Text("$reviewCount") } },
                        ) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = "Review")
                        }
                    },
                    label = { Text("Review") },
                )
                NavigationBarItem(
                    selected = screen == Screen.SETTINGS,
                    onClick = { screen = Screen.SETTINGS },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                )
            }
        },
    ) { innerPadding ->
        when (screen) {
            Screen.HOME -> HomeScreen(
                onOpenReview = { screen = Screen.REVIEW },
                contentPadding = innerPadding,
            )
            Screen.REVIEW -> ReviewScreen(contentPadding = innerPadding)
            Screen.SETTINGS -> SettingsScreen(contentPadding = innerPadding)
        }
    }
}
