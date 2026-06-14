package com.khaata.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.khaata.app.domain.model.Direction
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun HomeScreen(
    permissionGranted: Boolean,
    onEnableCapture: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            MonthSummary(monthLabel = state.month.formatLabel(), spent = state.spent.format(), income = state.income.format())

            if (!permissionGranted) {
                CaptureDisabledBanner(onEnableCapture)
            }

            Spacer(Modifier.padding(4.dp))
            Text(
                "Recent",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            if (state.recent.isEmpty()) {
                EmptyState(permissionGranted)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.recent, key = { it.id }) { txn -> TransactionRow(txn) }
                }
            }
        }
    }
}

@Composable
private fun MonthSummary(monthLabel: String, spent: String, income: String) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(monthLabel, style = MaterialTheme.typography.labelMedium)
        Text("Spent $spent", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text("Received $income", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CaptureDisabledBanner(onEnableCapture: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clickable(onClick = onEnableCapture),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Auto-tracking is off", fontWeight = FontWeight.SemiBold)
            Text(
                "Tap to allow Khaata to read your bank & UPI notifications. Nothing leaves your phone.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun EmptyState(permissionGranted: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            if (permissionGranted) {
                "No transactions yet.\nMake a payment and it'll appear here automatically."
            } else {
                "Enable auto-tracking above to start capturing your spending."
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun TransactionRow(txn: TransactionUi) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.padding(end = 12.dp)) {
                Text(txn.title, fontWeight = FontWeight.Medium)
                Text(
                    txn.occurredAtMillis.formatTime() + if (txn.needsReview) "  · needs review" else "",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            val sign = if (txn.direction == Direction.DEBIT) "-" else "+"
            Text("$sign${txn.amountLabel}", fontWeight = FontWeight.SemiBold)
        }
    }
}

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(Locale("en", "IN"))

private fun Long.formatTime(): String =
    timeFormatter.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))

private fun java.time.YearMonth.formatLabel(): String =
    "${month.getDisplayName(java.time.format.TextStyle.FULL, Locale("en", "IN"))} $year"
