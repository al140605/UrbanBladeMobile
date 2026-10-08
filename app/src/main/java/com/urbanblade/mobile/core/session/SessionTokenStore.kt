package com.urbanblade.mobile.core.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Guarda el token Bearer cifrado (clave `bearer_token_enc`). Los tokens que versiones anteriores
 * dejaron en texto plano (`bearer_token`) se cifran y se borran la primera vez que se leen, así
 * nadie pierde su sesión al actualizar la app.
 *
 * Recibe el DataStore y el cifrador por parámetro para poder probarse en la JVM sin Android.
 */
class SessionTokenStore(
    private val dataStore: DataStore<Preferences>,
    private val cipher: TokenCipher
) {
    private val legacyKey = stringPreferencesKey("bearer_token")
    private val encryptedKey = stringPreferencesKey("bearer_token_enc")

    val token: Flow<String?> = dataStore.data.map { decode(it) }

    suspend fun currentToken(): String? {
        val prefs = dataStore.data.first()
        val legacy = prefs[legacyKey]
        if (!legacy.isNullOrBlank()) {
            saveToken(legacy)
            return legacy
        }
        val token = decode(prefs)
        // Valor cifrado que ya no se puede abrir (clave perdida o dato alterado): se borra.
        if (token == null && prefs[encryptedKey] != null) clear()
        return token
    }

    suspend fun saveToken(token: String) {
        val encrypted = cipher.encrypt(token)
        dataStore.edit {
            it[encryptedKey] = encrypted
            it.remove(legacyKey)
        }
    }

    suspend fun clear() {
        dataStore.edit {
            it.remove(encryptedKey)
            it.remove(legacyKey)
        }
    }

    private fun decode(prefs: Preferences): String? =
        prefs[encryptedKey]?.let { cipher.decrypt(it) } ?: prefs[legacyKey]
}
