package com.khaata.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.khaata.app.domain.model.CategoryCatalog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.budgetDataStore by preferencesDataStore(name = "khaata_budgets")

/**
 * Per-category monthly spending limits, in paise. Budgets are user configuration (not
 * transactions), so they live in plain DataStore prefs rather than the encrypted DB. A
 * missing or non-positive value means "no budget set" for that category.
 */
@Singleton
class BudgetStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private fun key(categoryId: String) = longPreferencesKey("budget_$categoryId")

    /** All set budgets as categoryId -> limit(paise). Categories without a budget are absent. */
    val budgets: Flow<Map<String, Long>> = context.budgetDataStore.data.map { prefs ->
        CategoryCatalog.selectable
            .mapNotNull { cat -> prefs[key(cat.id)]?.takeIf { it > 0 }?.let { cat.id to it } }
            .toMap()
    }

    suspend fun setBudget(categoryId: String, minorUnits: Long?) {
        context.budgetDataStore.edit { prefs ->
            if (minorUnits == null || minorUnits <= 0L) prefs.remove(key(categoryId))
            else prefs[key(categoryId)] = minorUnits
        }
    }
}
