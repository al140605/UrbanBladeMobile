package com.urbanblade.mobile.ui.viewmodel

import com.urbanblade.mobile.data.model.BlockedAuthor
import com.urbanblade.mobile.data.model.BlockedAuthorsResponse
import com.urbanblade.mobile.data.model.CommentReportsResponse
import com.urbanblade.mobile.data.model.MessageResponse
import com.urbanblade.mobile.data.model.ReportedComment
import com.urbanblade.mobile.data.model.ReportedCommentAuthor
import com.urbanblade.mobile.data.model.ResolveReportResponse
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Contenido de usuarios: deshacer bloqueos (todos los roles) y moderar comentarios reportados (administrador). */
@OptIn(ExperimentalCoroutinesApi::class)
class ModerationViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: UrbanRepository

    private val ana = BlockedAuthor("b1", "Ana R.")
    private val beto = BlockedAuthor("b2", "Beto")
    private val report = ReportedComment(
        commentId = "c1",
        comment = "Texto reportado",
        author = ReportedCommentAuthor(id = "u1", name = "Carlos"),
        reportsCount = 3,
        reasons = mapOf("spam" to 1, "ofensivo" to 2)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `los bloqueos se cargan y la lista queda confirmada`() = runTest(dispatcher) {
        whenever(repo.blockedAuthors()).thenReturn(BlockedAuthorsResponse(listOf(ana, beto)))
        val vm = BlockedAuthorsViewModel(repo)

        vm.load()
        advanceUntilIdle()

        assertEquals(listOf("b1", "b2"), vm.state.value.items.map { it.id })
        assertTrue(vm.state.value.loaded)
        assertFalse(vm.state.value.loading)
    }

    @Test
    fun `si la consulta falla no se confirma el vacio y el error va aparte`() = runTest(dispatcher) {
        whenever(repo.blockedAuthors()).thenThrow(IllegalStateException("x"))
        val vm = BlockedAuthorsViewModel(repo)

        vm.load()
        advanceUntilIdle()

        assertFalse(vm.state.value.loaded)
        assertEquals("No se pudieron cargar tus bloqueos.", vm.state.value.listError)
        assertNull(vm.state.value.error)
    }

    @Test
    fun `desbloquear quita al autor y avisa`() = runTest(dispatcher) {
        whenever(repo.blockedAuthors()).thenReturn(BlockedAuthorsResponse(listOf(ana, beto)))
        whenever(repo.unblockAuthor("b1")).thenReturn(MessageResponse("Usuario desbloqueado."))
        val vm = BlockedAuthorsViewModel(repo)
        vm.load()
        advanceUntilIdle()

        vm.unblock(ana)
        advanceUntilIdle()

        assertEquals(listOf("b2"), vm.state.value.items.map { it.id })
        assertEquals("Desbloqueaste a Ana R.: ya verás sus comentarios.", vm.state.value.notice)
        assertNull(vm.state.value.unblockingId)
        verify(repo).unblockAuthor("b1")
    }

    @Test
    fun `si desbloquear falla el autor se queda y se explica`() = runTest(dispatcher) {
        whenever(repo.blockedAuthors()).thenReturn(BlockedAuthorsResponse(listOf(ana)))
        whenever(repo.unblockAuthor("b1")).thenThrow(IllegalStateException("x"))
        val vm = BlockedAuthorsViewModel(repo)
        vm.load()
        advanceUntilIdle()

        vm.unblock(ana)
        advanceUntilIdle()

        assertEquals(listOf("b1"), vm.state.value.items.map { it.id })
        assertEquals("No se pudo desbloquear. Inténtalo de nuevo.", vm.state.value.error)
        assertNull(vm.state.value.unblockingId)
    }

    @Test
    fun `un segundo toque mientras desbloquea no repite la peticion`() = runTest(dispatcher) {
        whenever(repo.blockedAuthors()).thenReturn(BlockedAuthorsResponse(listOf(ana, beto)))
        whenever(repo.unblockAuthor("b1")).thenReturn(MessageResponse("ok"))
        val vm = BlockedAuthorsViewModel(repo)
        vm.load()
        advanceUntilIdle()

        vm.unblock(ana)
        vm.unblock(ana)
        advanceUntilIdle()

        verify(repo, times(1)).unblockAuthor("b1")
        verify(repo, never()).unblockAuthor("b2")
    }

    @Test
    fun `la moderacion lista los comentarios reportados`() = runTest(dispatcher) {
        whenever(repo.commentReports()).thenReturn(CommentReportsResponse(listOf(report)))
        val vm = ModerationViewModel(repo)

        vm.load()
        advanceUntilIdle()

        assertEquals(listOf("c1"), vm.state.value.items.map { it.commentId })
        assertTrue(vm.state.value.loaded)
    }

    @Test
    fun `descartar manda dismiss, quita el reporte y avisa`() = runTest(dispatcher) {
        whenever(repo.commentReports()).thenReturn(CommentReportsResponse(listOf(report)))
        whenever(repo.resolveCommentReport("c1", "dismiss")).thenReturn(ResolveReportResponse("ok", 3))
        val vm = ModerationViewModel(repo)
        vm.load()
        advanceUntilIdle()

        vm.resolve(report, ModerationAction.Dismiss)
        advanceUntilIdle()

        assertTrue(vm.state.value.items.isEmpty())
        assertEquals("Reportes descartados; el comentario se conserva.", vm.state.value.notice)
        verify(repo).resolveCommentReport("c1", "dismiss")
    }

    @Test
    fun `eliminar y vetar manda remove_and_ban`() = runTest(dispatcher) {
        whenever(repo.commentReports()).thenReturn(CommentReportsResponse(listOf(report)))
        whenever(repo.resolveCommentReport("c1", "remove_and_ban")).thenReturn(ResolveReportResponse("ok", 3))
        val vm = ModerationViewModel(repo)
        vm.load()
        advanceUntilIdle()

        vm.resolve(report, ModerationAction.RemoveAndBan)
        advanceUntilIdle()

        assertEquals("Comentario eliminado y autor vetado para comentar.", vm.state.value.notice)
        verify(repo).resolveCommentReport("c1", "remove_and_ban")
    }

    @Test
    fun `si resolver falla el reporte se queda para reintentar`() = runTest(dispatcher) {
        whenever(repo.commentReports()).thenReturn(CommentReportsResponse(listOf(report)))
        whenever(repo.resolveCommentReport("c1", "remove")).thenThrow(IllegalStateException("x"))
        val vm = ModerationViewModel(repo)
        vm.load()
        advanceUntilIdle()

        vm.resolve(report, ModerationAction.Remove)
        advanceUntilIdle()

        assertEquals(listOf("c1"), vm.state.value.items.map { it.commentId })
        assertEquals("No se pudo resolver el reporte. Inténtalo de nuevo.", vm.state.value.error)
        assertNull(vm.state.value.resolvingId)
    }

    @Test
    fun `los motivos se etiquetan en espanol y el mas citado va primero`() {
        assertEquals("Ofensivo o acoso (2) · Spam o publicidad (1)", reasonsSummary(report.reasons))
        assertEquals("Contenido sexual o violento", reportReasonLabel("sexual_violento"))
        assertEquals("Otro motivo", reportReasonLabel("otro"))
        assertEquals("Motivo raro", reportReasonLabel("motivo_raro"))
        assertEquals("", reasonsSummary(emptyMap()))
    }
}
