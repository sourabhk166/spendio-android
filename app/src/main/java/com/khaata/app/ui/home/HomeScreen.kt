package com.khaata.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.khaata.app.domain.model.CategorySlice
import com.khaata.app.domain.model.Direction
import com.khaata.app.ui.TransactionUi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    permissionGranted: Boolean,
    onEnableCapture: () -> Unit,
    onOpenReview: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(modifier = modifier.fillMaxWidth()) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                MonthHeader(
                    label = state.month.formatLabel(),
                    onPrev = viewModel::prevMonth,
                    onNext = viewModel::nextMonth,
                )
            }
            item {
                Text("Spent ${state.spent.format()}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Text("Received ${state.income.format()}", style = MaterialTheme.typography.bodyMedium)
            }

            if (!permissionGranted) {
                item { Banner("Auto-tracking is off", "Tap to allow Khaata to read bank & UPI notifications. Nothing leaves your phone.", onEnableCapture) }
            }
            if (state.needsReviewCount > 0) {
                item { Banner("${state.needsReviewCount} to review", "We weren't fully sure about these. Tap to confirm or fix the category.", onOpenReview) }
            }

            if (state.breakdown.isNotEmpty()) {
                item { SectionTitle("Where it went") }
                items(state.breakdown, key = { it.categoryId }) { slice -> CategoryBar(slice) }
            }

            item { SectionTitle("Recent") }
            if (state.recent.isEmpty()) {
                item {
                    Text(
                        if (permissionGranted) "No transactions yet.\nMake a payment and it'll appear here automatically."
                        else "Enable auto-tracking above to start capturing your spending.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            } else {
                items(state.recent, key = { it.id }) { txn -> TransactionRow(txn) }
            }
        }
    }
}

@Composable
private fun MonthHeader(label: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = onPrev) { Text("‹", style = MaterialTheme.typography.titleLarge) }
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        TextButton(onClick = onNext) { Text("›", style = MaterialTheme.typography.titleLarge) }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun Banner(title: String, body: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun CategoryBar(slice: CategorySlice) {
    val color = remember(slice.colorHex) { slice.colorHex.toColor() }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${slice.emoji}  ${slice.name}", style = MaterialTheme.typography.bodyMedium)
            Text(slice.amount.format(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier.fillMaxWidth(slice.fraction.coerceIn(0f, 1f)).height(8.dp)
                    .clip(RoundedCornerShape(4.dp)).background(color),
            )
        }
    }
}

@Composable
private fun TransactionRow(txn: TransactionUi) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text("${txn.categoryEmoji}  ${txn.title}", fontWeight = FontWeight.Medium, maxLines = 1)
                Text(
                    txn.occurredAtMillis.formatTime() + if (txn.needsReview) "  · needs review" else "",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            val sign = if (txn.direction == Direction.DEBIT) "-" else "+"
            Text("$sign${txn.amountLabel}", fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
        }
    }
}

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(Locale("en", "IN"))

private fun Long.formatTime(): String =
    timeFormatter.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))

private fun java.time.YearMonth.formatLabel(): String =
    "${month.getDisplayName(TextStyle.FULL, Locale("en", "IN"))} $year"

private fun String.toColor(): Color =
    runCatching { Color(android.graphics.Color.parseColor(this)) }.getOrDefault(Color.Gray)
