package com.spendio.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendio.app.domain.model.Categories
import com.spendio.app.domain.model.CategorySlice
import com.spendio.app.domain.model.Direction
import com.spendio.app.domain.model.Money
import com.spendio.app.domain.repository.TransactionRepository
import com.spendio.app.ui.TransactionUi
import com.spendio.app.ui.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject

data class HomeUiState(
    val month: YearMonth,
    val spent: Money,
    val income: Money,
    val invested: Money,
    val selfTransfers: Money,
    val breakdown: List<CategorySlice>,
    val recent: List<TransactionUi>,
    val needsReviewCount: Int,
) {
    companion object {
        fun empty(month: YearMonth) =
            HomeUiState(month, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO, emptyList(), emptyList(), 0)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TransactionRepository,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())

    val state: StateFlow<HomeUiState> =
        month.flatMapLatest { m ->
            combine(
                repository.observeMonthlyTotals(m),
                repository.observeCategoryBreakdown(m),
                // Show the selected month's transactions, so the list stays consistent with the
                // month navigation and the totals above it (was: latest 50 across all months).
                repository.observeMonth(m),
                repository.observeNeedsReviewCount(),
            ) { totals, breakdown, monthTxns, reviewCount ->
                fun sumOf(category: String) = monthTxns
                    .filter { it.direction == Direction.DEBIT && it.categoryId == category }
                    .fold(0L) { acc, t -> acc + t.amount.minorUnits }
                // Self-transfers can land as either leg of the pair (the outgoing debit, the
                // matching incoming credit, or both if the bank sends two notifications), so —
                // unlike Investments, which is debit-only — count both directions here.
                val selfTransferTotal = monthTxns
                    .filter { it.categoryId == Categories.SELF_TRANSFER }
                    .fold(0L) { acc, t -> acc + t.amount.minorUnits }
                HomeUiState(
                    month = m,
                    spent = totals.spent,
                    income = totals.income,
                    invested = Money(sumOf(Categories.INVESTMENTS)),
                    selfTransfers = Money(selfTransferTotal),
                    breakdown = breakdown,
                    recent = monthTxns.map { it.toUi() },
                    needsReviewCount = reviewCount,
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.empty(YearMonth.now()),
        )

    fun nextMonth() { month.value = month.value.plusMonths(1) }
    fun prevMonth() { month.value = month.value.minusMonths(1) }
    fun goToCurrentMonth() { month.value = YearMonth.now() }
}
