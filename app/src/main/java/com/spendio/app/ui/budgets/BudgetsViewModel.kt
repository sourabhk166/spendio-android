package com.spendio.app.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendio.app.data.prefs.BudgetStore
import com.spendio.app.domain.model.Money
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetStore: BudgetStore,
) : ViewModel() {

    val budgets: StateFlow<Map<String, Long>> =
        budgetStore.budgets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Sets (or clears, when blank/zero) the monthly limit for a category from a rupee string. */
    fun setBudget(categoryId: String, rupeeInput: String) {
        viewModelScope.launch {
            val minor = if (rupeeInput.isBlank()) null else Money.fromRupeeString(rupeeInput)?.minorUnits
            budgetStore.setBudget(categoryId, minor)
        }
    }
}
