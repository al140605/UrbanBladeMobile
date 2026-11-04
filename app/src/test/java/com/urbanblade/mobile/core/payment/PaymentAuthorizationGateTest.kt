package com.urbanblade.mobile.core.payment

import org.junit.Assert.*
import org.junit.Test

class PaymentAuthorizationGateTest {
    @Test fun `one verification permits only one confirmation`() {
        val gate = PaymentAuthorizationGate()
        var confirmed = 0
        val id = gate.begin({ confirmed++ }, {})!!
        gate.finish(id, true); gate.finish(id, true)
        assertEquals(1, confirmed)
    }
    @Test fun `cancellation never confirms and permits retry`() {
        val gate = PaymentAuthorizationGate()
        var confirmed = 0
        var denied = 0
        val id = gate.begin({ confirmed++ }, { denied++ })!!
        gate.finish(id, false)
        assertEquals(0, confirmed); assertEquals(1, denied)
        assertNotNull(gate.begin({ confirmed++ }, {}))
    }
    @Test fun `double tap does not replace operation awaiting verification`() {
        val gate = PaymentAuthorizationGate()
        var chosen = ""
        val id = gate.begin({ chosen = "first" }, {})!!
        assertNull(gate.begin({ chosen = "second" }, {}))
        gate.finish(id, true)
        assertEquals("first", chosen)
    }
    @Test fun `late biometric result cannot authorize a later attempt`() {
        val gate = PaymentAuthorizationGate()
        var confirmed = 0
        val old = gate.begin({ confirmed++ }, {})!!
        gate.cancel()
        val current = gate.begin({ confirmed++ }, {})!!
        gate.finish(old, true)
        assertEquals(0, confirmed)
        gate.finish(current, true)
        assertEquals(1, confirmed)
    }
    @Test fun `leaving screen invalidates pending authorization`() {
        val gate = PaymentAuthorizationGate()
        var confirmed = 0
        var denied = 0
        val id = gate.begin({ confirmed++ }, { denied++ })!!
        gate.cancel(); gate.cancel(); gate.finish(id, true)
        assertEquals(0, confirmed); assertEquals(1, denied)
    }
}
