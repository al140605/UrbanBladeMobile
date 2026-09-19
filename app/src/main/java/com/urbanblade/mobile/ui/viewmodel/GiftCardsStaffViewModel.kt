package com.urbanblade.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.CashGiftCardRequest
import com.urbanblade.mobile.data.model.GiftCard
import com.urbanblade.mobile.data.model.GiftCardSold
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Límites de monto que valida el servidor (GiftCardService); aquí solo se anticipa el error. */
const val GIFT_CARD_MIN = 100.0
const val GIFT_CARD_MAX = 5000.0

data class GiftCardsStaffState(
    val selling: Boolean = false,
    val sold: GiftCardSold? = null,
    val sellError: String? = null,
    val lookingUp: Boolean = false,
    val found: GiftCard? = null,
    val lookupError: String? = null
)

/** Gift cards en mostrador: venta en efectivo y consulta de saldo por código. */
class GiftCardsStaffViewModel @JvmOverloads constructor(
    private val repo: UrbanRepository = AppContainer.urbanRepository
) : ViewModel() {
    private val _state = MutableStateFlow(GiftCardsStaffState())
    val state: StateFlow<GiftCardsStaffState> = _state.asStateFlow()

    /** Registra la venta en efectivo; [onDone] solo se llama si el servidor la aceptó. */
    fun sell(monto: Double, comprador: String?, destinatarioEmail: String?, onDone: () -> Unit) {
        if (monto < GIFT_CARD_MIN || monto > GIFT_CARD_MAX) {
            _state.value = _state.value.copy(sellError = "El monto debe estar entre $100 y $5,000.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(selling = true, sellError = null, sold = null)
            try {
                val sold = repo.sellGiftCardCash(CashGiftCardRequest(monto, comprador, destinatarioEmail))
                _state.value = _state.value.copy(selling = false, sold = sold)
                onDone()
            } catch (e: HttpException) {
                _state.value = _state.value.copy(
                    selling = false,
                    sellError = if (e.code() == 422) e.serverMessage() ?: "No se pudo registrar la venta." else e.toFriendlyMessage("No se pudo registrar la venta.")
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(selling = false, sellError = e.toFriendlyMessage("No se pudo registrar la venta."))
            }
        }
    }

    fun clearSold() {
        _state.value = _state.value.copy(sold = null)
    }

    /** Los códigos son de 8 caracteres en minúsculas y la búsqueda del servidor es exacta. */
    fun lookup(rawCode: String) {
        val code = rawCode.trim().lowercase()
        if (code.isEmpty()) {
            _state.value = _state.value.copy(lookupError = "Escribe el código de la tarjeta.", found = null)
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(lookingUp = true, lookupError = null, found = null)
            try {
                val card = repo.giftCardByCode(code)
                _state.value = _state.value.copy(lookingUp = false, found = card)
            } catch (e: HttpException) {
                _state.value = _state.value.copy(
                    lookingUp = false,
                    lookupError = if (e.code() == 404) e.serverMessage() ?: "Código no válido o tarjeta sin saldo." else e.toFriendlyMessage("No se pudo consultar la tarjeta.")
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(lookingUp = false, lookupError = e.toFriendlyMessage("No se pudo consultar la tarjeta."))
            }
        }
    }
}
