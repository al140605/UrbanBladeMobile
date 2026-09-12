package com.urbanblade.mobile.core.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "urbanblade_session")

class SessionManager(private val context: Context) {
    private val tokenKey = stringPreferencesKey("bearer_token")
    private val themeKey = stringPreferencesKey("theme")

    val token: Flow<String?> = context.dataStore.data.map { it[tokenKey] }
    val theme: Flow<String?> = context.dataStore.data.map { it[themeKey] }

    suspend fun currentToken(): String? = token.first()

    suspend fun currentTheme(): String? = theme.first()

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[tokenKey] = token }
    }

    // El tema es una preferencia de apariencia, no de sesión -- a
    // diferencia del token, no se limpia en clear() al cerrar sesión.
    suspend fun saveTheme(theme: String) {
        context.dataStore.edit { it[themeKey] = theme }
    }

    suspend fun clear() {
        context.dataStore.edit { it.remove(tokenKey) }
    }
}
