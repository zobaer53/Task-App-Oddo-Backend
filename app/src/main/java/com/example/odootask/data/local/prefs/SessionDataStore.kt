package com.example.odootask.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionStore: DataStore<Preferences> by preferencesDataStore(name = "odoo_session")

/**
 * Persisted Odoo credentials and uid. The base URL is fixed in
 * `BuildConfig.API_BASE_URL` and never stored here. [getSession] emits `null`
 * while logged out — i.e. when no uid has been saved or it is `0`.
 */
class SessionDataStore(private val context: Context) {

    fun getSession(): Flow<SessionData?> = context.sessionStore.data.map { prefs ->
        val uid = prefs[KEY_UID] ?: 0
        if (uid == 0) return@map null
        SessionData(
            database = prefs[KEY_DATABASE].orEmpty(),
            username = prefs[KEY_USERNAME].orEmpty(),
            password = prefs[KEY_PASSWORD].orEmpty(),
            uid = uid,
        )
    }

    suspend fun saveSession(data: SessionData) {
        context.sessionStore.edit { prefs ->
            prefs[KEY_DATABASE] = data.database
            prefs[KEY_USERNAME] = data.username
            prefs[KEY_PASSWORD] = data.password
            prefs[KEY_UID] = data.uid
        }
    }

    suspend fun clearSession() {
        context.sessionStore.edit { it.clear() }
    }

    private companion object {
        val KEY_DATABASE = stringPreferencesKey("odoo_database")
        val KEY_USERNAME = stringPreferencesKey("odoo_username")
        val KEY_PASSWORD = stringPreferencesKey("odoo_password")
        val KEY_UID = intPreferencesKey("odoo_uid")
    }
}

data class SessionData(
    val database: String,
    val username: String,
    val password: String,
    val uid: Int,
)
