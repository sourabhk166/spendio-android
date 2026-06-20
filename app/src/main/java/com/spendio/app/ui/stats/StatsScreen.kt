package com.spendio.app.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendio.app.domain.model.CategorySlice
import com.spendio.app.domain.model.Money
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatsScreen(
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MonthNav(
                label = state.month.formatLabel(),
                onPrev = viewModel::prevMonth,
                onNext = viewModel::nextMonth,
            )
        }

        item {
            ModeToggle(mode = state.mode, onSelect = viewModel::setMode)
        }

        if (state.mode == StatsMode.INVEST) {
            if (state.investments.isEmpty()) {
                item { EmptyHint("No investments recorded this month yet.") }
            } else {
                item { TotalCard(total = state.total, caption = "Invested this month") }
                items(state.investments, key = { it.id }) { txn -> InvestmentRow(txn) }
            }
        } else if (state.slices.isEmpty()) {
            item {
                EmptyHint(
                    if (state.mode == StatsMode.EXPENSE) "No spending this month yet."
                    else "No income recorded this month yet.",
                )
            }
        } else {
            item {
                DonutCard(
                    slices = state.slices,
                    total = state.total,
                    caption = if (state.mode == StatsMode.EXPENSE) "Total spent" else "Total received",
                )
            }
            items(state.slices, key = { it.categoryId }) { slice ->
                CategoryRow(slice, budgetMinor = state.budgets[slice.categoryId])
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun TotalCard(total: Money, caption: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(total.format(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun InvestmentRow(txn: com.spendio.app.ui.TransactionUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("📈", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(10.dp))
            Text(txn.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(txn.amountLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ModeToggle(mode: StatsMode, onSelect: (StatsMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ToggleSegment("Expense", selected = mode == StatsMode.EXPENSE, modifier = Modifier.weight(1f)) {
            onSelect(StatsMode.EXPENSE)
        }
        ToggleSegment("Income", selected = mode == StatsMode.INCOME, modifier = Modifier.weight(1f)) {
            onSelect(StatsMode.INCOME)
        }
        ToggleSegment("Invest", selected = mode == StatsMode.INVEST, modifier = Modifier.weight(1f)) {
            onSelect(StatsMode.INVEST)
        }
    }
}

@Composable
private fun ToggleSegment(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = fg)
    }
}

@Composable
private fun DonutCard(slices: List<CategorySlice>, total: Money, caption: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            val ringTrack = MaterialTheme.colorScheme.surfaceVariant
            Canvas(modifier = Modifier.size(180.dp)) {
                val stroke = 30.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(inset, inset)
                drawArc(
                    color = ringTrack,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke),
                )
                var start = -90f
                slices.forEach { slice ->
                    val sweep = slice.fraction.coerceIn(0f, 1f) * 360f
                    drawArc(
                        color = slice.colorHex.toColor(),
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke),
                    )
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = total.format(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(slice: CategorySlice, budgetMinor: Long?) {
    val color = remember(slice.colorHex) { slice.colorHex.toColor() }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Solid color swatch — the legend that maps this row to its slice in the donut.
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(10.dp))
                Text(slice.emoji, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(slice.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(
                        text = "${slice.percent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(slice.amount.format(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }

            if (budgetMinor != null && budgetMinor > 0) {
                val ratio = (slice.amount.minorUnits.toFloat() / budgetMinor).coerceIn(0f, 1f)
                val over = slice.amount.minorUnits > budgetMinor
                val barColor = if (over) MaterialTheme.colorScheme.error else color
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(
                        Modifier.fillMaxWidth(ratio).height(4.dp)
                            .clip(RoundedCornerShape(2.dp)).background(barColor),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (over) {
                        "Over budget · ${slice.amount.format()} of ${Money(budgetMinor).format()}"
                    } else {
                        "${slice.amount.format()} of ${Money(budgetMinor).format()}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MonthNav(label: String, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrev) {
            Text("‹", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        IconButton(onClick = onNext) {
            Text("›", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun YearMonth.formatLabel(): String =
    "${month.getDisplayName(TextStyle.FULL, Locale("en", "IN"))} $year"

private fun String.toColor(): Color =
    runCatching { Color(android.graphics.Color.parseColor(this)) }.getOrDefault(Color.Gray)
