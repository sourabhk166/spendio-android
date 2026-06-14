package com.khaata.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "khaata_settings")

/** User settings (app lock, which sources are read). Local, unencrypted prefs — no
 *  sensitive financial data lives here, only toggles. */
@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val APP_LOCK = booleanPreferencesKey("app_lock_enabled")
        val DISABLED_SOURCES = stringSetPreferencesKey("disabled_sources")
    }

    val appLockEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.APP_LOCK] ?: false }

    suspend fun appLockEnabledOnce(): Boolean = appLockEnabled.first()

    suspend fun setAppLock(enabled: Boolean) {
        context.dataStore.edit { it[Keys.APP_LOCK] = enabled }
    }

    /** Packages the user has turned OFF (everything known is on by default). */
    val disabledSources: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.DISABLED_SOURCES] ?: emptySet() }

    suspend fun disabledSourcesOnce(): Set<String> = disabledSources.first()

    suspend fun setSourceEnabled(packageName: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.DISABLED_SOURCES]?.toMutableSet() ?: mutableSetOf()
            if (enabled) current.remove(packageName) else current.add(packageName)
            prefs[Keys.DISABLED_SOURCES] = current
        }
    }
}
