package com.khaata.app.ui.editor

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.khaata.app.domain.model.Categories
import com.khaata.app.domain.model.CategoryCatalog
import com.khaata.app.domain.model.Direction
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FieldShape = RoundedCornerShape(12.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    txnId: String?,
    onClose: () -> Unit,
    viewModel: AddEditTransactionViewModel = hiltViewModel(),
) {
    LaunchedEffect(txnId) { viewModel.load(txnId) }
    val form by viewModel.form.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    // System back should return to the tab we came from, not exit the app.
    BackHandler { onClose() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (form.isEdit) "Edit transaction" else "Add transaction",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (form.canDelete) {
                        IconButton(onClick = { viewModel.delete(onClose) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DirectionToggle(selected = form.direction, onSelect = viewModel::onDirectionChange)

            OutlinedTextField(
                value = form.amountInput,
                onValueChange = viewModel::onAmountChange,
                label = { Text("Amount") },
                prefix = { Text("₹ ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.headlineSmall,
                shape = FieldShape,
                colors = khaataFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = form.merchant,
                onValueChange = viewModel::onMerchantChange,
                label = { Text("Description") },
                placeholder = { Text("e.g. Auto rickshaw, Chai, Rent") },
                singleLine = true,
                shape = FieldShape,
                colors = khaataFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            CategoryField(
                categoryId = form.categoryId,
                direction = form.direction,
                onPick = viewModel::onCategoryChange,
            )

            PickerRow(label = "Date", value = form.occurredAt.formatDate(), onClick = { showDatePicker = true })

            OutlinedTextField(
                value = form.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text("Note (optional)") },
                shape = FieldShape,
                colors = khaataFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            if (form.error != null) {
                Text(form.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.save(onClose) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = FieldShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    text = if (form.isEdit) "Save changes" else "Add transaction",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        DatePickerSheet(
            initialMillis = form.occurredAt.toEpochMilli(),
            onConfirm = {
                viewModel.onDateChange(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@Composable
private fun DirectionToggle(selected: Direction, onSelect: (Direction) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TypeSegment(
            label = "Expense",
            selected = selected == Direction.DEBIT,
            accent = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        ) { onSelect(Direction.DEBIT) }
        TypeSegment(
            label = "Income",
            selected = selected == Direction.CREDIT,
            accent = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        ) { onSelect(Direction.CREDIT) }
    }
}

@Composable
private fun TypeSegment(
    label: String,
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bg = if (selected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface
    val borderColor = if (selected) accent else MaterialTheme.colorScheme.outline
    val textColor = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .clip(FieldShape)
            .background(bg)
            .border(if (selected) 1.5.dp else 1.dp, borderColor, FieldShape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor,
        )
    }
}

@Composable
private fun CategoryField(categoryId: String, direction: Direction, onPick: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val info = remember(categoryId) { CategoryCatalog.info(categoryId) }
    val dotColor = remember(info.colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(info.colorHex)) }.getOrDefault(Color.Gray)
    }
    // Show direction-appropriate options: income gets Income/Other; expenses get the
    // spending categories (Income makes no sense for a debit).
    val options = remember(direction) {
        when (direction) {
            Direction.CREDIT -> CategoryCatalog.incomeCategories
            Direction.DEBIT -> CategoryCatalog.selectable.filter { it.id !in Categories.INCOME_ALL }
        }
    }
    Box {
        FieldContainer(onClick = { expanded = true }) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(dotColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) { Text(info.emoji, style = MaterialTheme.typography.bodyMedium) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Category", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(info.name, style = MaterialTheme.typography.bodyLarge)
            }
            Text("▾", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { cat ->
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
private fun PickerRow(label: String, value: String, onClick: () -> Unit) {
    FieldContainer(onClick = onClick) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
        Text("▾", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A tappable row styled to match the filled text fields, so the whole form reads as one set. */
@Composable
private fun FieldContainer(onClick: () -> Unit, content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FieldShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, FieldShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun khaataFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    cursorColor = MaterialTheme.colorScheme.primary,
)

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
