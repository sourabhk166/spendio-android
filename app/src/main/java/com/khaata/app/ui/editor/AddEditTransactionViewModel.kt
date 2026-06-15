package com.khaata.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khaata.app.domain.model.Categories
import com.khaata.app.domain.model.Direction
import com.khaata.app.domain.model.Money
import com.khaata.app.domain.model.Transaction
import com.khaata.app.domain.repository.TransactionRepository
import com.khaata.app.parser.Categorizer
import com.khaata.app.parser.TransactionFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/** Editable form state for the add / edit screen. */
data class EditorFormState(
    val isEdit: Boolean = false,
    val amountInput: String = "",
    val direction: Direction = Direction.DEBIT,
    val merchant: String = "",
    val categoryId: String = Categories.UNCATEGORIZED,
    val occurredAt: Instant = Instant.now(),
    val note: String = "",
    val canDelete: Boolean = false,
    val error: String? = null,
)

/**
 * Backs both flows of the manual-entry feature:
 *  - New entry (cash spends, or anything the capture layer never saw).
 *  - Editing an auto-captured entry (fix a wrong amount / merchant / category / date).
 *
 * Editing acts as a confirmation: the row is taken out of the review queue and, when the
 * category changes, the [Categorizer] is trained so future captures of that merchant land
 * correctly — exactly like a Review correction.
 */
@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val categorizer: Categorizer,
) : ViewModel() {

    private val _form = MutableStateFlow(EditorFormState())
    val form: StateFlow<EditorFormState> = _form.asStateFlow()

    private var existing: Transaction? = null
    private var loadedFor: String? = null

    /**
     * Populates the form for [txnId] (edit) or resets to blank defaults (new). This VM is
     * Activity-scoped, so the *same* instance is reused across editor sessions — every call
     * must fully (re)set state, never assume a fresh instance. Re-keyed by [txnId] so the
     * caller can reopen the editor for a different target and get the right content.
     */
    fun load(txnId: String?) {
        if (loadedFor == txnId && (txnId == null || existing?.id == txnId)) return
        loadedFor = txnId
        if (txnId == null) {
            existing = null
            _form.value = EditorFormState() // fresh blank entry
            return
        }
        // Show a clean edit placeholder immediately so no stale data flashes while we fetch.
        existing = null
        _form.value = EditorFormState(isEdit = true, canDelete = true)
        viewModelScope.launch {
            val t = repository.getById(txnId) ?: return@launch
            existing = t
            _form.value = EditorFormState(
                isEdit = true,
                amountInput = t.amount.toAmountInput(),
                direction = t.direction,
                merchant = t.merchant ?: t.rawMerchant ?: "",
                categoryId = t.categoryId,
                occurredAt = t.occurredAt,
                note = t.note ?: "",
                canDelete = true,
            )
        }
    }

    fun onAmountChange(value: String) {
        // Digits with at most one decimal point and two fractional digits (paise).
        val filtered = value.filter { it.isDigit() || it == '.' }
        val dot = filtered.indexOf('.')
        val normalized = if (dot < 0) {
            filtered
        } else {
            val whole = filtered.substring(0, dot)
            val frac = filtered.substring(dot + 1).replace(".", "").take(2)
            "$whole.$frac"
        }
        _form.update { it.copy(amountInput = normalized, error = null) }
    }

    fun onDirectionChange(direction: Direction) = _form.update { it.copy(direction = direction, error = null) }
    fun onMerchantChange(value: String) = _form.update { it.copy(merchant = value, error = null) }
    fun onCategoryChange(categoryId: String) = _form.update { it.copy(categoryId = categoryId) }
    fun onDateChange(instant: Instant) = _form.update { it.copy(occurredAt = instant) }
    fun onNoteChange(value: String) = _form.update { it.copy(note = value) }

    /** Validates and persists. Invokes [onDone] on the main thread only after the write completes. */
    fun save(onDone: () -> Unit) {
        val state = _form.value
        val amount = Money.fromRupeeString(state.amountInput)
        if (amount == null || amount.minorUnits <= 0L) {
            _form.update { it.copy(error = "Enter a valid amount") }
            return
        }
        val merchant = state.merchant.trim()
        if (merchant.isEmpty()) {
            _form.update { it.copy(error = "Enter a description") }
            return
        }
        val note = state.note.trim().ifBlank { null }

        viewModelScope.launch {
            val current = existing
            if (current == null) {
                val txn = TransactionFactory.manual(
                    amount = amount,
                    direction = state.direction,
                    merchant = merchant,
                    categoryId = state.categoryId,
                    occurredAt = state.occurredAt,
                    note = note,
                )
                repository.insertIfNew(txn)
                if (state.categoryId != Categories.UNCATEGORIZED) {
                    categorizer.correct(merchant, state.categoryId)
                }
            } else {
                val updated = current.copy(
                    amount = amount,
                    direction = state.direction,
                    merchant = merchant,
                    rawMerchant = current.rawMerchant ?: merchant,
                    categoryId = state.categoryId,
                    occurredAt = state.occurredAt,
                    note = note,
                    needsReview = false, // editing it counts as reviewing it
                )
                repository.update(updated)
                if (state.categoryId != current.categoryId && state.categoryId != Categories.UNCATEGORIZED) {
                    categorizer.correct(current.rawMerchant ?: merchant, state.categoryId)
                }
            }
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val current = existing ?: return
        viewModelScope.launch {
            repository.delete(current)
            onDone()
        }
    }
}

/** Renders paise as a plain, editable rupee string: 28300 -> "283", 28350 -> "283.50". */
private fun Money.toAmountInput(): String {
    val whole = minorUnits / 100
    val frac = (minorUnits % 100).toInt()
    return if (frac == 0) whole.toString() else "%d.%02d".format(whole, frac)
}
