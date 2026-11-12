package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.*
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class HomeLoadStateTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo: UrbanRepository = mock()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun `client empty agenda is confirmed only after a successful response`() = runTest(dispatcher) {
        whenever(repo.appointments(page = 1, perPage = 30)).thenReturn(AppointmentsResponse())
        val vm = AppointmentsViewModel(repo)
        assertFalse(vm.hasLoaded.value)
        vm.load(); advanceUntilIdle()
        assertTrue(vm.hasLoaded.value)
        whenever(repo.appointments(page = 1, perPage = 30)).thenThrow(IllegalStateException("sin red"))
        vm.load(); advanceUntilIdle()
        assertTrue(vm.hasLoaded.value)
        assertNotNull(vm.error.value)
    }

    @Test fun `logs failure is unknown activity rather than zero confirmed movements`() = runTest(dispatcher) {
        whenever(repo.logs()).thenThrow(IllegalStateException("sin red"))
        val vm = LogsViewModel(repo)
        vm.load(); advanceUntilIdle()
        assertFalse(vm.hasLoaded.value)
        assertNotNull(vm.error.value)
        doReturn(LogsResponse(stats = LogStats(hoy = 7))).whenever(repo).logs()
        vm.load(); advanceUntilIdle()
        assertTrue(vm.hasLoaded.value)
        assertEquals(7, vm.logs.value.stats.hoy)
        assertNull(vm.error.value)
    }

    @Test fun `logs refresh failure preserves the last confirmed statistics with an error`() = runTest(dispatcher) {
        doReturn(LogsResponse(stats = LogStats(hoy = 7))).whenever(repo).logs()
        val vm = LogsViewModel(repo)
        vm.load(); advanceUntilIdle()
        whenever(repo.logs()).thenThrow(IllegalStateException("sin red"))
        vm.load(); advanceUntilIdle()
        assertTrue(vm.hasLoaded.value)
        assertEquals(7, vm.logs.value.stats.hoy)
        assertNotNull(vm.error.value)
    }

    @Test fun `reviews failure is not an empty catalog and can be retried`() = runTest(dispatcher) {
        whenever(repo.reviews()).thenThrow(IllegalStateException("sin red"))
        val vm = ReviewsViewModel(repo)
        vm.load(); advanceUntilIdle()
        assertFalse(vm.hasLoaded.value)
        assertNotNull(vm.error.value)
        doReturn(ReviewsResponse()).whenever(repo).reviews()
        vm.load(); advanceUntilIdle()
        assertTrue(vm.hasLoaded.value)
        assertNull(vm.error.value)
    }
}
