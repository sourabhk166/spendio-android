package com.khaata.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khaata.app.data.prefs.BudgetStore
import com.khaata.app.domain.model.Categories
import com.khaata.app.domain.model.CategorySlice
import com.khaata.app.domain.model.Direction
import com.khaata.app.domain.model.Money
import com.khaata.app.domain.repository.TransactionRepository
import com.khaata.app.ui.TransactionUi
import com.khaata.app.ui.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject

enum class StatsMode { EXPENSE, INCOME, INVEST }

data class StatsUiState(
    val month: YearMonth,
    val mode: StatsMode,
    val slices: List<CategorySlice>,
    val total: Money,
    /** categoryId -> monthly limit (paise); only populated in EXPENSE mode. */
    val budgets: Map<String, Long> = emptyMap(),
    /** Individual investment transactions; only populated in INVEST mode. */
    val investments: List<TransactionUi> = emptyList(),
) {
    companion object {
        fun empty(month: YearMonth) = StatsUiState(month, StatsMode.EXPENSE, emptyList(), Money.ZERO)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val budgetStore: BudgetStore,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val mode = MutableStateFlow(StatsMode.EXPENSE)

    val state: StateFlow<StatsUiState> =
        combine(month, mode) { m, md -> m to md }
            .flatMapLatest { (m, md) ->
                when (md) {
                    StatsMode.INVEST -> repository.observeMonth(m).map { txns ->
                        val invested = txns.filter {
                            it.direction == Direction.DEBIT && it.categoryId == Categories.INVESTMENTS
                        }
                        StatsUiState(
                            month = m,
                            mode = md,
                            slices = emptyList(),
                            total = Money(invested.sumOf { it.amount.minorUnits }),
                            investments = invested.map { it.toUi() },
                        )
                    }
                    else -> {
                        val breakdown = if (md == StatsMode.EXPENSE) {
                            repository.observeCategoryBreakdown(m)
                        } else {
                            repository.observeIncomeBreakdown(m)
                        }
                        combine(breakdown, budgetStore.budgets) { slices, budgets ->
                            StatsUiState(
                                month = m,
                                mode = md,
                                slices = slices,
                                total = Money(slices.sumOf { it.amount.minorUnits }),
                                budgets = if (md == StatsMode.EXPENSE) budgets else emptyMap(),
                            )
                        }
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = StatsUiState.empty(YearMonth.now()),
            )

    fun setMode(newMode: StatsMode) { mode.value = newMode }
    fun nextMonth() { month.value = month.value.plusMonths(1) }
    fun prevMonth() { month.value = month.value.minusMonths(1) }
}
