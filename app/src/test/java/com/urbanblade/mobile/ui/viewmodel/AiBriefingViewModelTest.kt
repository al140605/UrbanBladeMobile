package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.AiBriefing
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** El resumen del día es un extra: si falla, la tarjeta no aparece y el inicio sigue igual. */
@OptIn(ExperimentalCoroutinesApi::class)
class AiBriefingViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: UrbanRepository

    @Before
    fun setup() { Dispatchers.setMain(dispatcher); repo = mock() }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `muestra el resumen que manda barber`() = runTest(dispatcher) {
        whenever(repo.aiBriefing()).thenReturn(AiBriefing("Hoy hay 3 citas.", "reglas"))
        val vm = AiBriefingViewModel(repo)
        vm.load(); advanceUntilIdle()
        assertEquals("Hoy hay 3 citas.", vm.briefing.value?.text)
    }

    @Test
    fun `si falla no muestra nada ni error`() = runTest(dispatcher) {
        whenever(repo.aiBriefing()).thenThrow(RuntimeException("sin red"))
        val vm = AiBriefingViewModel(repo)
        vm.load(); advanceUntilIdle()
        assertNull(vm.briefing.value)
    }
}
