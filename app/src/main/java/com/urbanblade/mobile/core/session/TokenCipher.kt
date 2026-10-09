package com.urbanblade.mobile.core.session

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Cifra y descifra el token Bearer antes de guardarlo en DataStore (Actividad 09, déficit 1:
 * el token se guardaba en texto plano en `datastore/urbanblade_session.preferences_pb`).
 */
interface TokenCipher {
    fun encrypt(plain: String): String

    /** Devuelve null si el valor está alterado o la clave ya no existe: la app pide iniciar sesión de nuevo. */
    fun decrypt(encoded: String): String?
}

/**
 * AES-256-GCM con IV aleatorio de 12 bytes: se guarda Base64(IV || texto cifrado + etiqueta).
 * La clave llega por [keyProvider] para poder probar el algoritmo en la JVM sin Android Keystore.
 */
class AesGcmTokenCipher(private val keyProvider: () -> SecretKey) : TokenCipher {

    override fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider())
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(cipher.iv + encrypted)
    }

    override fun decrypt(encoded: String): String? = try {
        val bytes = Base64.getDecoder().decode(encoded)
        if (bytes.size <= IV_BYTES) {
            null
        } else {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, keyProvider(), GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES))
            String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES), Charsets.UTF_8)
        }
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}

/** Clave AES-256 guardada dentro de Android Keystore: no se puede extraer del teléfono. */
object KeystoreAesKey {
    private const val PROVIDER = "AndroidKeyStore"
    private const val ALIAS = "urbanblade_session_key"

    fun get(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }
}
