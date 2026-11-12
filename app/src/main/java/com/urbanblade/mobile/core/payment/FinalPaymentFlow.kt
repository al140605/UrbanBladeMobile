package com.urbanblade.mobile.core.payment

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** La selección nunca cobra: solo Pagar seguido de verificación permite confirmar. */
internal class FinalPaymentFlow {
    enum class Step { IDLE, LOADING, SELECTING, REVIEW, AUTHORIZING, CONFIRMING }
    var step by mutableStateOf(Step.IDLE)
        private set
    private var generation = 0L
    fun begin(): Long? {
        if (step != Step.IDLE) return null
        generation++
        step = Step.LOADING
        return generation
    }
    fun isCurrent(id: Long) = generation == id && step != Step.IDLE
    fun loaded(id: Long): Boolean {
        if (!isCurrent(id) || step != Step.LOADING) return false
        step = Step.SELECTING
        return true
    }
    fun selected(): Boolean {
        if (step != Step.SELECTING) return false
        step = Step.REVIEW
        return true
    }
    fun edit(): Boolean {
        if (step != Step.REVIEW) return false
        step = Step.SELECTING
        return true
    }
    fun pay(): Boolean {
        if (step != Step.REVIEW) return false
        step = Step.AUTHORIZING
        return true
    }
    fun authorizationDenied() {
        if (step == Step.AUTHORIZING) step = Step.REVIEW
    }
    fun authorized(id: Long): Boolean {
        if (!isCurrent(id) || step != Step.AUTHORIZING) return false
        step = Step.CONFIRMING
        return true
    }
    fun finish() { generation++; step = Step.IDLE }
}
