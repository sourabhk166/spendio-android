package com.spendio.app.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendio.app.domain.model.Categories
import com.spendio.app.domain.repository.TransactionRepository
import com.spendio.app.parser.Categorizer
import com.spendio.app.ui.TransactionUi
import com.spendio.app.ui.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The needs-review queue: low-confidence (heuristic / AI) parses the user can confirm or
 * re-categorize. A correction also trains the [Categorizer] so future transactions from
 * that merchant land in the right category automatically.
 */
@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val categorizer: Categorizer,
) : ViewModel() {

    val items: StateFlow<List<TransactionUi>> =
        repository.observeNeedsReview()
            .map { txns -> txns.map { it.toUi() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun approve(item: TransactionUi) = viewModelScope.launch {
        repository.approveReview(item.id)
        // Approving confirms the assigned category. Remember it so the same payee — e.g. a
        // friend who repays you regularly, or your own self-transfer account — isn't queued
        // for review every time. Uncategorized is never worth remembering.
        if (item.categoryId != Categories.UNCATEGORIZED) {
            categorizer.correct(item.rawMerchant, item.categoryId)
        }
    }

    fun recategorize(item: TransactionUi, categoryId: String) = viewModelScope.launch {
        repository.recategorize(item.id, categoryId)
        categorizer.correct(item.rawMerchant, categoryId)
    }
}
