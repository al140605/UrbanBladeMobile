package com.urbanblade.mobile.core.payment

import org.junit.Assert.*
import org.junit.Test

class FinalPaymentFlowTest {
    @Test fun `selecting card cannot request verification or charge`() {
        val flow = FinalPaymentFlow()
        val id = flow.begin()!!
        assertFalse(flow.pay()); assertFalse(flow.authorized(id))
        assertTrue(flow.loaded(id))
        assertFalse(flow.pay()); assertFalse(flow.authorized(id))
        assertTrue(flow.selected())
        assertEquals(FinalPaymentFlow.Step.REVIEW, flow.step)
        assertFalse(flow.authorized(id))
    }
    @Test fun `final pay followed by verification permits confirmation once`() {
        val flow = FinalPaymentFlow(); val id = flow.begin()!!
        flow.loaded(id); flow.selected()
        assertTrue(flow.pay()); assertFalse(flow.pay())
        assertTrue(flow.authorized(id)); assertFalse(flow.authorized(id))
    }
    @Test fun `cancelled fingerprint keeps review without charging`() {
        val flow = FinalPaymentFlow(); val id = flow.begin()!!
        flow.loaded(id); flow.selected(); flow.pay(); flow.authorizationDenied()
        assertEquals(FinalPaymentFlow.Step.REVIEW, flow.step)
        assertFalse(flow.authorized(id)); assertTrue(flow.pay())
    }
    @Test fun `changing card requires returning to review before paying`() {
        val flow = FinalPaymentFlow(); val id = flow.begin()!!
        flow.loaded(id); flow.selected()
        assertTrue(flow.edit()); assertFalse(flow.pay()); assertFalse(flow.authorized(id))
        flow.selected(); assertTrue(flow.pay())
    }
    @Test fun `cancelled checkout ignores late amount lookup and authorization`() {
        val flow = FinalPaymentFlow(); val old = flow.begin()!!
        flow.finish(); val current = flow.begin()!!
        assertFalse(flow.loaded(old)); assertFalse(flow.authorized(old))
        assertTrue(flow.loaded(current))
    }
    @Test fun `repeated opening cannot replace current operation`() {
        val flow = FinalPaymentFlow(); val id = flow.begin()!!
        assertNull(flow.begin()); assertTrue(flow.isCurrent(id))
        flow.finish(); assertNotNull(flow.begin())
    }
}
