package com.spendio.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendio.app.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AppNavViewModel @Inject constructor(
    repository: TransactionRepository,
) : ViewModel() {
    val reviewCount = repository.observeNeedsReviewCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
