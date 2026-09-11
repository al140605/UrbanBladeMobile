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

    val token: Flow<String?> = context.dataStore.data.map { it[tokenKey] }

    suspend fun currentToken(): String? = token.first()

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[tokenKey] = token }
    }

    suspend fun clear() {
        context.dataStore.edit { it.remove(tokenKey) }
    }
}
