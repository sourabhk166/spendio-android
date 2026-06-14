package com.khaata.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khaata.app.domain.model.Direction
import com.khaata.app.domain.model.Money
import com.khaata.app.domain.model.Transaction
import com.khaata.app.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject

data class TransactionUi(
    val id: String,
    val title: String,
    val amountLabel: String,
    val direction: Direction,
    val occurredAtMillis: Long,
    val needsReview: Boolean,
)

data class HomeUiState(
    val month: YearMonth,
    val spent: Money,
    val income: Money,
    val recent: List<TransactionUi>,
) {
    companion object {
        fun empty(month: YearMonth) = HomeUiState(month, Money.ZERO, Money.ZERO, emptyList())
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: TransactionRepository,
) : ViewModel() {

    private val month: YearMonth = YearMonth.now()

    val state: StateFlow<HomeUiState> =
        combine(
            repository.observeMonthlyTotals(month),
            repository.observeRecent(limit = 50),
        ) { totals, recent ->
            HomeUiState(
                month = month,
                spent = totals.spent,
                income = totals.income,
                recent = recent.map { it.toUi() },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.empty(month),
        )
}

private fun Transaction.toUi() = TransactionUi(
    id = id,
    title = merchant ?: rawMerchant ?: "Unknown",
    amountLabel = amount.format(),
    direction = direction,
    occurredAtMillis = occurredAt.toEpochMilli(),
    needsReview = needsReview,
)
