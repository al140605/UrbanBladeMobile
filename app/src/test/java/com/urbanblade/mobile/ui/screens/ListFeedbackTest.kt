package com.urbanblade.mobile.ui.screens

import com.urbanblade.mobile.ui.components.ListFeedbackState
import com.urbanblade.mobile.ui.components.listFeedbackState
import org.junit.Assert.assertEquals
import org.junit.Test

class ListFeedbackTest {
    @Test fun `unknown first response is loading rather than empty`() {
        assertEquals(ListFeedbackState.INITIAL_LOADING, listFeedbackState(false, false, false, null))
    }
    @Test fun `failed initial response offers retry rather than an empty result`() {
        assertEquals(ListFeedbackState.INITIAL_ERROR, listFeedbackState(false, false, false, "Sin conexión"))
    }
    @Test fun `failed refresh preserves the previous result even when it was empty`() {
        for (hasItems in listOf(false, true)) {
            assertEquals(ListFeedbackState.STALE_ERROR, listFeedbackState(true, false, hasItems, "Sin conexión"))
        }
    }
    @Test fun `refresh does not replace cached content with an initial skeleton`() {
        assertEquals(ListFeedbackState.REFRESHING, listFeedbackState(true, true, true, null))
    }
    @Test fun `only successful confirmed empty responses produce the empty state`() {
        assertEquals(ListFeedbackState.EMPTY, listFeedbackState(true, false, false, null))
        assertEquals(ListFeedbackState.CONTENT, listFeedbackState(true, false, true, null))
    }
}
