package com.khaata.app.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.khaata.app.domain.model.Categories
import com.khaata.app.domain.model.CategoryCatalog
import com.khaata.app.domain.model.Direction
import com.khaata.app.ui.TransactionUi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onClose: () -> Unit,
    onEditTransaction: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current

    BackHandler { onClose() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = { Text("Search merchant or note") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.size(8.dp))

            when {
                query.isBlank() -> Hint("Type to search your transactions.")
                results.isEmpty() -> Hint("No transactions match “$query”.")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
                ) {
                    items(results, key = { it.id }) { txn ->
                        SearchResultRow(txn, onClick = {
                            focus.clearFocus()
                            onEditTransaction(txn.id)
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(24.dp),
    )
}

@Composable
private fun SearchResultRow(txn: TransactionUi, onClick: () -> Unit) {
    val catInfo = remember(txn.categoryId) { CategoryCatalog.info(txn.categoryId) }
    val catColor = remember(catInfo.colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(catInfo.colorHex)) }.getOrDefault(Color.Gray)
    }
    val amountColor = if (txn.direction == Direction.DEBIT) MaterialTheme.colorScheme.error
                      else MaterialTheme.colorScheme.primary
    val sign = if (txn.direction == Direction.DEBIT) "−" else "+"

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(catColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                if (txn.categoryId == Categories.UNCATEGORIZED) {
                    Text(
                        text = txn.title.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(txn.categoryEmoji, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = txn.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${catInfo.name} · ${txn.occurredAtMillis.formatDate()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$sign${txn.amountLabel}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = amountColor,
                maxLines = 1,
            )
        }
    }
}

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(Locale("en", "IN"))

private fun Long.formatDate(): String =
    dateFormatter.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))
