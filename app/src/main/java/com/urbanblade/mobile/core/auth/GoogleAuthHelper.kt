package com.urbanblade.mobile.core.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.urbanblade.mobile.BuildConfig

/**
 * Login nativo con Google vía Credential Manager: pide un ID token
 * directamente (sin abrir navegador), que luego se manda a
 * POST auth/google/token (SocialAuthController::token() en barber) para
 * verificarlo y emitir el Bearer token propio de la app -- distinto del
 * flujo web (redirect/callback), que no aplica aquí.
 */
object GoogleAuthHelper {
    sealed interface Result {
        data class Token(val value: String) : Result
        data object Cancelled : Result
        data object Unavailable : Result
    }

    /**
     * Distingue una cancelación voluntaria de una indisponibilidad real de
     * Google Play services, para que la UI no silencie un problema de acceso.
     */
    suspend fun requestGoogleIdToken(context: Context): Result {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        return try {
            val result = CredentialManager.create(context).getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                Result.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                Result.Unavailable
            }
        } catch (e: GetCredentialCancellationException) {
            Result.Cancelled
        } catch (e: GetCredentialException) {
            Result.Unavailable
        } catch (e: GoogleIdTokenParsingException) {
            Result.Unavailable
        }
    }
}
