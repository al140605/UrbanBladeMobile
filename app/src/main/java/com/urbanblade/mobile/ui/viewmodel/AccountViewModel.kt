package com.urbanblade.mobile.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.AccountUser
import com.urbanblade.mobile.data.model.ClientLoyalty
import com.urbanblade.mobile.data.model.NotificationPreferences
import com.urbanblade.mobile.data.model.UpdateProfileRequest
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Sección de Mi cuenta a la que pertenece un aviso, para mostrarlo junto a lo que lo causó. */
enum class AccountSection { DATA, AVATAR, PASSWORD, NOTIFICATIONS }

data class AccountNotice(val section: AccountSection, val text: String, val isError: Boolean)

data class AccountState(
    val loading: Boolean = false,
    val loadError: String? = null,
    val profile: AccountUser? = null,
    val loyalty: ClientLoyalty? = null,
    val preferences: NotificationPreferences? = null,
    val savingData: Boolean = false,
    val savingPassword: Boolean = false,
    val uploadingAvatar: Boolean = false,
    /** Cambia cada vez que la contraseña se guarda, para que la pantalla limpie los campos. */
    val passwordChanges: Int = 0,
    val notice: AccountNotice? = null
)

/**
 * Mi cuenta, compartida por todos los roles: datos personales, foto, contraseña y avisos.
 * Cada acción avisa con [AccountNotice] en su propia sección; [onUserChanged] se llama cuando
 * cambia algo que el resto de la app muestra (nombre, correo o foto).
 */
class AccountViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AccountState())
    val state: StateFlow<AccountState> = _state.asStateFlow()

    fun load(isClient: Boolean) = viewModelScope.launch {
        _state.update { it.copy(loading = true, loadError = null) }
        try {
            val profile = repo.profile()
            // Lealtad y avisos son complementos: si fallan, la cuenta se sigue mostrando.
            val loyalty = if (isClient) runCatching { repo.clientLoyalty() }.getOrNull() else null
            val preferences = runCatching { repo.notificationPreferences() }.getOrNull()
            _state.update { it.copy(profile = profile, loyalty = loyalty, preferences = preferences) }
        } catch (e: Exception) {
            _state.update { it.copy(loadError = e.toFriendlyMessage("No se pudo cargar tu cuenta.")) }
        } finally {
            _state.update { it.copy(loading = false) }
        }
    }

    fun dismissNotice() = _state.update { it.copy(notice = null) }

    fun saveData(name: String, email: String, phone: String, birth: String, sex: String, onUserChanged: () -> Unit) =
        viewModelScope.launch {
            _state.update { it.copy(savingData = true, notice = null) }
            try {
                val res = repo.updateProfile(
                    UpdateProfileRequest(
                        name = name.trim(), email = email.trim(),
                        telefono = phone.trim().ifBlank { null },
                        fechaNacimiento = birth.ifBlank { null },
                        sexo = sex.ifBlank { null }
                    )
                )
                val profile = repo.profile()
                _state.update {
                    it.copy(profile = profile, notice = AccountNotice(AccountSection.DATA, res.message ?: "Datos actualizados.", false))
                }
                onUserChanged()
            } catch (e: Exception) {
                _state.update { it.copy(notice = AccountNotice(AccountSection.DATA, e.serverOrFriendly("No se pudieron guardar tus datos."), true)) }
            } finally {
                _state.update { it.copy(savingData = false) }
            }
        }

    fun uploadAvatar(context: Context, uri: Uri, onUserChanged: () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(uploadingAvatar = true, notice = null) }
        try {
            val res = repo.updateAvatar(context, uri)
            val avatarUrl = res.user?.avatarUrl
            _state.update {
                it.copy(
                    profile = it.profile?.copy(avatarUrl = avatarUrl ?: it.profile.avatarUrl),
                    notice = AccountNotice(AccountSection.AVATAR, res.message ?: "Foto actualizada.", false)
                )
            }
            onUserChanged()
        } catch (e: Exception) {
            _state.update {
                it.copy(notice = AccountNotice(AccountSection.AVATAR, e.serverOrFriendly("No se pudo subir la foto. Usa JPG, PNG o WebP de hasta 4 MB."), true))
            }
        } finally {
            _state.update { it.copy(uploadingAvatar = false) }
        }
    }

    fun changePassword(current: String, password: String, confirmation: String) = viewModelScope.launch {
        _state.update { it.copy(savingPassword = true, notice = null) }
        try {
            val res = repo.updatePassword(current, password, confirmation)
            _state.update {
                it.copy(
                    passwordChanges = it.passwordChanges + 1,
                    notice = AccountNotice(AccountSection.PASSWORD, res.message ?: "Contraseña actualizada.", false)
                )
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(notice = AccountNotice(AccountSection.PASSWORD, e.serverOrFriendly("No se pudo cambiar la contraseña."), true))
            }
        } finally {
            _state.update { it.copy(savingPassword = false) }
        }
    }

    /** Cambia un canal al instante (sin botón Guardar) y regresa el interruptor si el servidor falla. */
    fun setPreference(key: String, enabled: Boolean) = viewModelScope.launch {
        val before = _state.value.preferences ?: return@launch
        _state.update { it.copy(preferences = before.with(key, enabled), notice = null) }
        try {
            val saved = repo.updateNotificationPreference(key, enabled)
            _state.update { it.copy(preferences = saved) }
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    preferences = before,
                    notice = AccountNotice(AccountSection.NOTIFICATIONS, e.toFriendlyMessage("No se pudo guardar el cambio."), true)
                )
            }
        }
    }

    private fun NotificationPreferences.with(key: String, enabled: Boolean) = when (key) {
        "in_app" -> copy(inApp = enabled)
        "email" -> copy(email = enabled)
        "promociones" -> copy(promociones = enabled)
        else -> this
    }

    /** En un 422 el servidor explica qué campo falló (p. ej. "La contraseña actual es incorrecta."). */
    private fun Exception.serverOrFriendly(fallback: String): String =
        (this as? HttpException)?.takeIf { it.code() == 422 }?.serverMessage() ?: toFriendlyMessage(fallback)
}
