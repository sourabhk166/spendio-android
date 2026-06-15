package com.khaata.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.khaata.app.domain.model.CategoryCatalog
import com.khaata.app.domain.model.Categories
import com.khaata.app.domain.model.Direction
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AddEditTransactionScreen(
    txnId: String?,
    onClose: () -> Unit,
    viewModel: AddEditTransactionViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.load(txnId) }
    val form by viewModel.form.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Text("‹", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = if (form.isEdit) "Edit transaction" else "Add transaction",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (form.canDelete) {
                IconButton(onClick = { viewModel.delete(onClose) }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Expense / Income toggle ──────────────────────────────────────────────
        DirectionToggle(
            selected = form.direction,
            onSelect = viewModel::onDirectionChange,
        )

        Spacer(Modifier.height(20.dp))

        // ── Amount ───────────────────────────────────────────────────────────────
        OutlinedTextField(
            value = form.amountInput,
            onValueChange = viewModel::onAmountChange,
            label = { Text("Amount") },
            prefix = { Text("₹ ") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(14.dp))

        // ── Description / merchant ───────────────────────────────────────────────
        OutlinedTextField(
            value = form.merchant,
            onValueChange = viewModel::onMerchantChange,
            label = { Text("Description") },
            placeholder = { Text("e.g. Auto rickshaw, Chai, Rent") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(14.dp))

        // ── Category ─────────────────────────────────────────────────────────────
        CategoryField(
            categoryId = form.categoryId,
            onPick = viewModel::onCategoryChange,
        )

        Spacer(Modifier.height(14.dp))

        // ── Date ─────────────────────────────────────────────────────────────────
        FieldRow(label = "Date", value = form.occurredAt.formatDate(), onClick = { showDatePicker = true })

        Spacer(Modifier.height(14.dp))

        // ── Note (optional) ──────────────────────────────────────────────────────
        OutlinedTextField(
            value = form.note,
            onValueChange = viewModel::onNoteChange,
            label = { Text("Note (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )

        if (form.error != null) {
            Spacer(Modifier.height(10.dp))
            Text(form.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { viewModel.save(onClose) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text(
                text = if (form.isEdit) "Save changes" else "Add transaction",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showDatePicker) {
        DatePickerSheet(
            initialMillis = form.occurredAt.toEpochMilli(),
            onConfirm = { instant ->
                viewModel.onDateChange(instant)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@Composable
private fun DirectionToggle(selected: Direction, onSelect: (Direction) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ToggleHalf("Expense", selected == Direction.DEBIT, MaterialTheme.colorScheme.error) { onSelect(Direction.DEBIT) }
        ToggleHalf("Income", selected == Direction.CREDIT, MaterialTheme.colorScheme.primary) { onSelect(Direction.CREDIT) }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ToggleHalf(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CategoryField(categoryId: String, onPick: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val info = remember(categoryId) { CategoryCatalog.info(categoryId) }
    val dotColor = remember(info.colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(info.colorHex)) }.getOrDefault(Color.Gray)
    }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(24.dp).clip(CircleShape).background(dotColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) { Text(info.emoji, style = MaterialTheme.typography.bodyMedium) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(info.name, style = MaterialTheme.typography.bodyMedium)
            }
            Text("▾", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CategoryCatalog.selectable.forEach { cat ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(cat.emoji)
                            Spacer(Modifier.width(10.dp))
                            Text(cat.name)
                        }
                    },
                    onClick = {
                        onPick(cat.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun FieldRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
        Text("▾", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerSheet(
    initialMillis: Long,
    onConfirm: (Instant) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val picked = state.selectedDateMillis
                if (picked != null) {
                    // DatePicker returns UTC midnight; pin to local noon so month/day bucketing
                    // is stable regardless of timezone.
                    val date = Instant.ofEpochMilli(picked).atZone(ZoneOffset.UTC).toLocalDate()
                    onConfirm(date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant())
                } else {
                    onDismiss()
                }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state)
    }
}

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale("en", "IN"))

private fun Instant.formatDate(): String =
    dateFormatter.format(atZone(ZoneId.systemDefault()))
