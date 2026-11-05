package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.*
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class ReadOnlyListRequestsTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo: UrbanRepository = mock()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun `late review results cannot overwrite the latest rating filter`() = runTest(dispatcher) {
        whenever(repo.reviews(rating = 5)).doSuspendableAnswer {
            withContext(NonCancellable) { delay(100) }
            ReviewsResponse(stats = ReviewStats(total = 5))
        }
        whenever(repo.reviews(rating = 1)).thenReturn(ReviewsResponse(stats = ReviewStats(total = 1)))
        val vm = ReviewsViewModel(repo)
        vm.load(rating = 5); runCurrent()
        vm.load(rating = 1); advanceUntilIdle()
        assertEquals(1, vm.reviews.value.stats.total)
        assertFalse(vm.busy.value)
        assertNull(vm.error.value)
    }

    @Test fun `cancelled log failure cannot stop the new loading state or publish its error`() = runTest(dispatcher) {
        whenever(repo.logs(event = "created")).doSuspendableAnswer {
            withContext(NonCancellable) { delay(100) }
            throw IllegalStateException("old response")
        }
        whenever(repo.logs(event = "deleted")).doSuspendableAnswer {
            delay(1000)
            LogsResponse(stats = LogStats(hoy = 12))
        }
        val vm = LogsViewModel(repo)
        vm.load(event = "created"); runCurrent()
        vm.load(event = "deleted"); runCurrent()
        advanceTimeBy(101); runCurrent()
        assertTrue(vm.busy.value)
        assertNull(vm.error.value)
        assertFalse(vm.hasLoaded.value)
        advanceUntilIdle()
        assertEquals(12, vm.logs.value.stats.hoy)
        assertFalse(vm.busy.value)
    }

    @Test fun `raffle failure remains unknown until a successful retry`() = runTest(dispatcher) {
        whenever(repo.raffles()).thenThrow(IllegalStateException("sin red"))
        val vm = RafflesViewModel(repo)
        vm.load(); advanceUntilIdle()
        assertFalse(vm.hasLoaded.value)
        assertNotNull(vm.error.value)
        doReturn(RaffleResponse()).whenever(repo).raffles()
        vm.load(); advanceUntilIdle()
        assertTrue(vm.hasLoaded.value)
        assertNull(vm.error.value)
    }

    @Test fun `raffle refresh failure preserves confirmed totals rather than resetting to zero`() = runTest(dispatcher) {
        whenever(repo.raffles()).thenReturn(RaffleResponse(stats = RaffleStats(total = 5)))
        val vm = RafflesViewModel(repo)
        vm.load(); advanceUntilIdle()
        whenever(repo.raffles()).thenThrow(IllegalStateException("sin red"))
        vm.load(); advanceUntilIdle()
        assertEquals(5, vm.raffles.value.stats.total)
        assertTrue(vm.hasLoaded.value)
        assertNotNull(vm.error.value)
    }
}
