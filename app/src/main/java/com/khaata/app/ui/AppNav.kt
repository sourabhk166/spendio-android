package com.khaata.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
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
import com.khaata.app.ui.editor.AddEditTransactionScreen
import com.khaata.app.ui.home.HomeScreen
import com.khaata.app.ui.review.ReviewScreen
import com.khaata.app.ui.search.SearchScreen
import com.khaata.app.ui.settings.SettingsScreen
import com.khaata.app.ui.stats.StatsScreen

private enum class Screen { HOME, STATS, REVIEW, SETTINGS }

/** A full-screen add/edit overlay above the tabbed scaffold. */
private sealed interface EditorTarget {
    data object New : EditorTarget
    data class Edit(val id: String) : EditorTarget
}

@Composable
fun AppNav(
    onEnableCapture: () -> Unit,
    viewModel: AppNavViewModel = hiltViewModel(),
) {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var editor by remember { mutableStateOf<EditorTarget?>(null) }
    var searching by remember { mutableStateOf(false) }
    val reviewCount by viewModel.reviewCount.collectAsStateWithLifecycle()

    val current = editor
    if (current != null) {
        AddEditTransactionScreen(
            txnId = (current as? EditorTarget.Edit)?.id,
            onClose = { editor = null },
        )
        return
    }

    if (searching) {
        SearchScreen(
            onClose = { searching = false },
            onEditTransaction = { id -> searching = false; editor = EditorTarget.Edit(id) },
        )
        return
    }

    // From a non-Home tab, system back returns to Home instead of exiting the app.
    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (screen == Screen.HOME) {
                FloatingActionButton(
                    onClick = { editor = EditorTarget.New },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add transaction")
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = screen == Screen.HOME,
                    onClick = { screen = Screen.HOME },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                )
                NavigationBarItem(
                    selected = screen == Screen.STATS,
                    onClick = { screen = Screen.STATS },
                    icon = { Icon(Icons.Outlined.PieChart, contentDescription = "Stats") },
                    label = { Text("Stats") },
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
                onEditTransaction = { id -> editor = EditorTarget.Edit(id) },
                onOpenSearch = { searching = true },
                contentPadding = innerPadding,
            )
            Screen.STATS -> StatsScreen(contentPadding = innerPadding)
            Screen.REVIEW -> ReviewScreen(
                onEditTransaction = { id -> editor = EditorTarget.Edit(id) },
                contentPadding = innerPadding,
            )
            Screen.SETTINGS -> SettingsScreen(contentPadding = innerPadding)
        }
    }
}
