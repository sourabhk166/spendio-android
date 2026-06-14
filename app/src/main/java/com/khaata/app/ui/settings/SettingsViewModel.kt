package com.khaata.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khaata.app.data.DataMaintenance
import com.khaata.app.data.prefs.SettingsStore
import com.khaata.app.parser.KnownSources
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SourceToggle(val packageName: String, val displayName: String, val enabled: Boolean)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsStore,
    private val dataMaintenance: DataMaintenance,
) : ViewModel() {

    val appLockEnabled: StateFlow<Boolean> =
        settings.appLockEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val sources: StateFlow<List<SourceToggle>> =
        settings.disabledSources.map { disabled ->
            KnownSources.ALL.map { SourceToggle(it.packageName, it.displayName, it.packageName !in disabled) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setAppLock(enabled: Boolean) = viewModelScope.launch { settings.setAppLock(enabled) }

    fun toggleSource(packageName: String, enabled: Boolean) =
        viewModelScope.launch { settings.setSourceEnabled(packageName, enabled) }

    fun wipeAll() = viewModelScope.launch { dataMaintenance.wipeAll() }
}
