package com.khaata.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
        val USER_NAME = stringPreferencesKey("user_name")
        val AUTO_BACKUP = booleanPreferencesKey("auto_backup_enabled")
    }

    /** Whether automatic (Auto Backup) encrypted backups are on. Off by default. */
    val autoBackupEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_BACKUP] ?: false }

    suspend fun autoBackupEnabledOnce(): Boolean = autoBackupEnabled.first()

    suspend fun setAutoBackup(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_BACKUP] = enabled }
    }

    /** The user's own name, used to spot self-transfers (paying your own account). Blank = unset. */
    val userName: Flow<String> = context.dataStore.data.map { it[Keys.USER_NAME] ?: "" }

    suspend fun userNameOnce(): String = userName.first()

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[Keys.USER_NAME] = name.trim() }
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
