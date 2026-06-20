package com.spendio.app.ui.budgets

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spendio.app.domain.model.Categories
import com.spendio.app.domain.model.CategoryCatalog
import com.spendio.app.domain.model.CategoryInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    onClose: () -> Unit,
    viewModel: BudgetsViewModel = hiltViewModel(),
) {
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackHandler { onClose() }

    // Budgets only make sense for spending categories — not income, transfers or investments.
    val categories = remember {
        CategoryCatalog.selectable.filter {
            it.id != Categories.INCOME && it.id !in Categories.NON_EXPENSE
        }
    }

    // Local edit buffer (whole rupees). Nothing is persisted until the user taps Save. We merge
    // in stored values as they load without clobbering anything the user has already typed.
    val edited = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(budgets) {
        budgets.forEach { (id, paise) -> if (id !in edited) edited[id] = (paise / 100).toString() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Monthly budgets", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        categories.forEach { viewModel.setBudget(it.id, edited[it.id] ?: "") }
                        Toast.makeText(context, "Budgets saved", Toast.LENGTH_SHORT).show()
                        onClose()
                    }) {
                        Text("Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    text = "Set a monthly spending limit per category, then tap Save. Progress shows on the Stats tab; leave blank for no limit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            items(categories, key = { it.id }) { cat ->
                BudgetRow(
                    cat = cat,
                    value = edited[cat.id] ?: "",
                    onValueChange = { edited[cat.id] = it.filter { c -> c.isDigit() }.take(7) },
                )
            }
        }
    }
}

@Composable
private fun BudgetRow(cat: CategoryInfo, value: String, onValueChange: (String) -> Unit) {
    val color = remember(cat.colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(cat.colorHex)) }.getOrDefault(Color.Gray)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) { Text(cat.emoji, style = MaterialTheme.typography.bodyMedium) }
        Spacer(Modifier.width(12.dp))
        Text(cat.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            prefix = { Text("₹") },
            placeholder = { Text("—") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier.width(130.dp),
        )
    }
}
