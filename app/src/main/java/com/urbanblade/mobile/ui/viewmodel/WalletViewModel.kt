package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.*
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


/** Mensaje si el código de referido está vacío o pasa el tope del servidor; null si se puede enviar. */
internal fun validateReferralCode(code: String): String? = when {
    code.isBlank() -> "Escribe el código de quien te invitó."
    code.trim().length > com.urbanblade.mobile.core.validation.FormLimits.REFERRAL_CODE_MAX ->
        "El código tiene como máximo ${com.urbanblade.mobile.core.validation.FormLimits.REFERRAL_CODE_MAX} caracteres."
    else -> null
}

data class WalletData(
    val loyalty: ClientLoyalty? = null,
    val membership: MyMembership? = null,
    val packages: List<MyPackage> = emptyList(),
    val giftCards: List<GiftCard> = emptyList(),
    val referrals: ReferralInfo? = null,
    /** Planes para contratar desde la app (solo se muestran si no tiene membresía). */
    val plans: List<MembershipPlan> = emptyList(),
    val savedCards: List<SavedCard> = emptyList()
) {
    val hasContent: Boolean get() = loyalty != null || membership != null || packages.isNotEmpty() ||
        giftCards.isNotEmpty() || referrals != null || plans.isNotEmpty()
}

/** Primer cobro de la membresía por confirmar con Stripe: tarjeta guardada o la que escribió. */
data class MembershipCheckout(val clientSecret: String, val savedCardId: String?)

/** Beneficios y membresía. Conserva datos previos al fallar una fuente; compras en BenefitPurchaseViewModel. */
class WalletViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _data = MutableStateFlow(WalletData())
    val data = _data.asStateFlow()
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun load() = viewModelScope.launch {
        if (_loading.value) return@launch
        _loading.value = true; _error.value = null
        val previous = _data.value
        var failures = 0
        suspend fun <T> source(fallback: T, read: suspend () -> T): T = try { read() }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) {
            failures++
            if (_error.value == null) _error.value = e.toFriendlyMessage("No pudimos actualizar todos tus beneficios. Intenta de nuevo.")
            fallback
        }
        try {
            _data.value = WalletData(
                loyalty = source(previous.loyalty) { repo.clientLoyalty() },
                membership = source(previous.membership) { repo.myMembership() },
                packages = source(previous.packages) { repo.myPackages() },
                giftCards = source(previous.giftCards) { repo.myGiftCards() },
                referrals = source(previous.referrals) { repo.myReferrals() },
                plans = source(previous.plans) { repo.membershipPlans().orEmpty() },
                savedCards = source(previous.savedCards) { repo.savedCards().orEmpty() }
            )
            if (failures > 0 && failures < 7) _error.value = "Algunos beneficios no se pudieron actualizar. Lo anterior se conserva; vuelve a intentar."
        } finally { _loading.value = false }
    }

    private val _referralBusy = MutableStateFlow(false)
    val referralBusy = _referralBusy.asStateFlow()
    private val _referralNotice = MutableStateFlow<String?>(null)
    val referralNotice = _referralNotice.asStateFlow()
    private val _referralError = MutableStateFlow<String?>(null)
    val referralError = _referralError.asStateFlow()

    /** True cuando el servidor ya tiene un código de quien invitó a este cliente: no se puede cambiar ni repetir. */
    private val _referralLinked = MutableStateFlow(false)
    val referralLinked = _referralLinked.asStateFlow()

    /** Registra el código de quien invitó al cliente. Los motivos de rechazo (inválido, propio, ya registrado) los explica el servidor. */
    fun linkReferral(code: String) {
        if (_referralBusy.value) return
        validateReferralCode(code)?.let {
            _referralError.value = it; _referralNotice.value = null
            return
        }
        _referralBusy.value = true; _referralError.value = null; _referralNotice.value = null
        viewModelScope.launch {
            try {
                val res = repo.linkReferral(code)
                _referralNotice.value = res.message ?: "¡Listo! Cuando completes tu primera cita, tu referido ganará sus puntos."
                _referralLinked.value = true
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                val text = e.toFriendlyMessage("No se pudo registrar el código. Inténtalo de nuevo.")
                _referralError.value = text
                // El servidor no permite cambiarlo: si ya había uno, se deja de ofrecer el formulario.
                if (text.startsWith("Ya tienes un código")) _referralLinked.value = true
            } finally {
                _referralBusy.value = false
            }
        }
    }

    fun clearReferralMessages() { _referralError.value = null; _referralNotice.value = null }

    private val _checkout = MutableStateFlow<MembershipCheckout?>(null)
    val checkout = _checkout.asStateFlow()

    /**
     * Contrata (o reintenta pagar) un plan: barber crea la suscripción y devuelve el client_secret
     * del primer cobro; la pantalla lo confirma con Stripe y avisa con [onCheckoutResult].
     */
    fun subscribe(planId: String, savedCardId: String?) = viewModelScope.launch {
        _busy.value = true; _error.value = null; _message.value = null
        try {
            val secret = repo.subscribeMembership(planId)?.clientSecret
            if (secret.isNullOrBlank()) {
                _error.value = "No se pudo iniciar el pago de la membresía. Intenta de nuevo."
                _busy.value = false
            } else {
                _checkout.value = MembershipCheckout(secret, savedCardId)
            }
        } catch (e: Exception) {
            _error.value = e.toFriendlyMessage("No se pudo iniciar el pago de la membresía.")
            _busy.value = false
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCheckoutResult(ok: Boolean, canceled: Boolean, detail: String?) = viewModelScope.launch {
        _checkout.value = null
        when {
            ok -> {
                _message.value = "¡Listo! Tu membresía se está activando; en unos segundos verás tu descuento."
                // Stripe confirma el cobro por webhook: se da un momento antes de recargar.
                kotlinx.coroutines.delay(2500)
                load()
            }
            canceled -> _message.value = "Cancelaste el pago. Puedes intentarlo de nuevo cuando quieras."
            else -> _error.value = "No se pudo cobrar tu tarjeta. Intenta de nuevo o utiliza otra tarjeta."
        }
        _busy.value = false
    }

    fun cancelMembership() = viewModelScope.launch {
        _busy.value = true; _error.value = null
        try {
            val res = repo.cancelMembership()
            _message.value = res.message ?: "Tu membresía se cancelará al finalizar el periodo actual."
            load()
        } catch (e: Exception) {
            _error.value = e.toFriendlyMessage("No se pudo cancelar la membresía.")
        } finally { _busy.value = false }
    }
}
