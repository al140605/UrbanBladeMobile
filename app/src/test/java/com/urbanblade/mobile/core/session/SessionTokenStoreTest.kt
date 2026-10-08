package com.urbanblade.mobile.core.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Actividad 09, déficit 1: el token Bearer se guardaba en texto plano en el DataStore.
 * Se prueba sobre un archivo real `.preferences_pb`, el mismo formato que usa la app.
 */
class SessionTokenStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val token = "7|TOKEN-SECRETO-DE-PRUEBA-abc123"
    private val legacyKey = stringPreferencesKey("bearer_token")
    private val encryptedKey = stringPreferencesKey("bearer_token_enc")

    private lateinit var scope: CoroutineScope
    private lateinit var file: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var store: SessionTokenStore

    private fun newKey(): SecretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    private fun cipher(): AesGcmTokenCipher {
        val key = newKey()
        return AesGcmTokenCipher { key }
    }

    @Before
    fun setUp() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        file = File(tmp.root, "urbanblade_session.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        store = SessionTokenStore(dataStore, cipher())
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    /** Contenido crudo del archivo, tal como lo vería quien lo extraiga del teléfono. */
    private fun rawFile(): String = String(file.readBytes(), Charsets.ISO_8859_1)

    @Test
    fun `deficit original - un token guardado como antes queda legible en el archivo`() = runBlocking {
        dataStore.edit { it[legacyKey] = token }
        assertTrue("el archivo debía contener el token en claro", rawFile().contains(token))
    }

    @Test
    fun `solucion - el token guardado no aparece en texto plano en el archivo`() = runBlocking {
        store.saveToken(token)
        assertFalse("el token no debe estar en claro", rawFile().contains(token))
        assertFalse(rawFile().contains("TOKEN-SECRETO"))
    }

    @Test
    fun `el token cifrado se recupera completo`() = runBlocking {
        store.saveToken(token)
        assertEquals(token, store.currentToken())
    }

    @Test
    fun `un token en texto plano de una version anterior se cifra y se borra al leerlo`() = runBlocking {
        dataStore.edit { it[legacyKey] = token }
        assertEquals(token, store.currentToken())
        assertFalse("tras migrar no debe quedar en claro", rawFile().contains(token))
        assertEquals(token, store.currentToken())
    }

    @Test
    fun `un valor cifrado alterado se descarta y pide iniciar sesion de nuevo`() = runBlocking {
        store.saveToken(token)
        dataStore.edit { it[encryptedKey] = "valor-manipulado" }
        assertNull(store.currentToken())
        assertNull(store.currentToken())
    }

    @Test
    fun `clear borra el token`() = runBlocking {
        store.saveToken(token)
        store.clear()
        assertNull(store.currentToken())
        assertFalse(rawFile().contains(token))
    }

    @Test
    fun `AES-GCM usa un IV distinto en cada cifrado`() {
        val c = cipher()
        assertNotEquals(c.encrypt(token), c.encrypt(token))
    }

    @Test
    fun `con otra clave el token no se puede descifrar`() {
        val encrypted = cipher().encrypt(token)
        assertNull(cipher().decrypt(encrypted))
    }
}
