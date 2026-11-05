package com.urbanblade.mobile.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.urbanblade.mobile.core.network.UrbanBladeApi
import com.urbanblade.mobile.data.repository.UrbanRepository
import com.urbanblade.mobile.ui.viewmodel.AppointmentsViewModel
import com.urbanblade.mobile.ui.viewmodel.BookingViewModel
import java.time.LocalDate
import java.util.concurrent.CopyOnWriteArrayList
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * T091 (HT-28): prueba de integración de unidades de la pantalla de reserva, con enfoque de caja negra.
 *
 * Caso: «Solicitar la reserva solo con horario elegido y mostrarla pendiente». Integración simulada: se usan las
 * piezas reales de la app (BookingScreen y ClientAppointmentsScreen, sus ViewModels, UrbanRepository y Retrofit),
 * y solo la API es falsa: un MockWebServer que responde como barber (las citas nuevas nacen «pendiente»).
 *
 * Caja negra: la prueba solo toca la pantalla como un cliente y revisa lo que se ve y lo que llegó a la API. Las
 * particiones son: sin horario elegido (no se puede solicitar), con horario elegido (se solicita y queda
 * pendiente) y horario de otro día (al cambiar de día hay que volver a elegirlo).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h3000dp")
class ReservaPendienteIntegracionTest {

    @get:Rule
    val compose = createComposeRule()

    private val server = MockWebServer()
    private val recibidas = CopyOnWriteArrayList<RecordedRequest>()
    private val citas = CopyOnWriteArrayList<JSONObject>()
    private lateinit var repo: UrbanRepository

    @Before
    fun encenderApiFalsa() {
        server.dispatcher = apiFalsa
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UrbanBladeApi::class.java)
        repo = UrbanRepository(api)
    }

    @After
    fun apagarApiFalsa() {
        server.shutdown()
    }

    // ── Casos de prueba ─────────────────────────────────────────────────────────────────────────────────────────

    @Test
    fun `sin horario elegido no deja solicitar la reserva`() {
        abrirReserva()
        tocar("Corte clásico")
        tocar("Elegir horario")
        esperarTexto("18:30")

        boton("Elegir extras").assertIsNotEnabled()
        esperarTexto("Elige un horario disponible para continuar.")
        assertTrue("sin horario no se manda ninguna solicitud", solicitudesDeCita().isEmpty())
    }

    @Test
    fun `con el horario elegido solicita la reserva y la muestra pendiente`() {
        abrirReserva()
        tocar("Corte clásico")
        tocar("Elegir horario")
        tocar("18:30")
        boton("Elegir extras").assertIsEnabled()
        tocar("Elegir extras")
        tocar("Continuar sin extras")
        tocar("Acepto el cargo por inasistencia", parcial = true)
        esperarHabilitado("Reservar cita")
        tocar("Reservar cita")

        esperarTexto("¡Cita reservada!")
        esperarTexto("Pagas en el salón el día de tu cita.")
        val enviada = JSONObject(solicitudesDeCita().single().body.clone().readUtf8())
        assertEquals("se solicita con el horario elegido", "18:30", enviada.getString("hora_inicio"))
        assertEquals(LocalDate.now().toString(), enviada.getString("fecha"))
        assertEquals("svc-corte", enviada.getString("service_id"))

        tocar("Ver mis citas")
        esperarTexto("Pendiente")
        esperarTexto("Esperando que tu barbero apruebe la cita", parcial = true)
        esperarTexto("6:30 PM")
        esperarTexto("Corte clásico")
    }

    @Test
    fun `al cambiar de dia hay que volver a elegir el horario`() {
        abrirReserva()
        tocar("Corte clásico")
        tocar("Elegir horario")
        tocar("18:30")
        boton("Elegir extras").assertIsEnabled()

        val manana = LocalDate.now().plusDays(1).dayOfMonth.toString()
        compose.onAllNodesWithText(manana)[0].performClick()
        esperarTexto("Elige un horario disponible para continuar.")

        boton("Elegir extras").assertIsNotEnabled()
        assertTrue(solicitudesDeCita().isEmpty())
    }

    // ── Cómo se usa la pantalla (como lo haría un cliente) ──────────────────────────────────────────────────────

    /** Reserva y, al tocar «Ver mis citas», la pantalla de citas: lo mismo que hace la navegación de la app. */
    private fun abrirReserva() {
        val reserva = BookingViewModel(repo)
        val misCitas = AppointmentsViewModel(repo)
        compose.setContent {
            MaterialTheme {
                var enMisCitas by remember { mutableStateOf(false) }
                if (enMisCitas) {
                    ClientAppointmentsScreen(onBook = {}, onNavigate = {}, vm = misCitas)
                } else {
                    BookingScreen(onBack = {}, onCreated = { enMisCitas = true }, vm = reserva)
                }
            }
        }
    }

    private fun esperarTexto(texto: String, parcial: Boolean = false) {
        compose.waitUntil(TIEMPO_MS) {
            compose.onAllNodesWithText(texto, substring = parcial).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun tocar(texto: String, parcial: Boolean = false) {
        esperarTexto(texto, parcial)
        compose.onAllNodesWithText(texto, substring = parcial)[0].performClick()
        compose.waitForIdle()
    }

    private fun boton(texto: String): SemanticsNodeInteraction {
        esperarTexto(texto)
        return compose.onNodeWithText(texto)
    }

    private fun esperarHabilitado(texto: String) {
        compose.waitUntil(TIEMPO_MS) { runCatching { compose.onNodeWithText(texto).assertIsEnabled() }.isSuccess }
    }

    private fun solicitudesDeCita() = recibidas.filter { it.method == "POST" && it.ruta() == "appointments" }

    // ── API falsa (MockWebServer): responde como barber ─────────────────────────────────────────────────────────

    private val apiFalsa = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            recibidas += request
            val ruta = request.ruta()
            return when {
                request.method == "POST" && ruta == "appointments" -> registrarCita(request)
                ruta == "services" -> ok("""{"data":[{"id":"svc-corte","nombre":"Corte clásico","precio":180,"duracion_min":30}]}""")
                // Un solo barbero: la pantalla lo asigna sola.
                ruta == "barbers" -> ok("""{"data":[{"id":"barber-luis","slug":"luis","user":{"id":"u-luis","name":"Luis Barbero"},"es_favorito":false,"total_reviews":0}]}""")
                ruta == "availability/slots" -> ok(
                    """{"slots":[{"time":"12:00","label":"12:00","end_time":"12:30","end_label":"12:30"},""" +
                        """{"time":"18:30","label":"18:30","end_time":"19:00","end_label":"19:00"}]}"""
                )
                ruta == "appointments" -> ok(JSONObject().put("data", JSONArray(citas)).put("cancellation_policy_hours", 24).toString())
                ruta == "no-show-fees" -> ok("""{"data":[],"adeudo_total":0}""")
                ruta == "payments/transfer-info" -> ok("""{"data":{}}""")
                else -> ok("""{"data":[]}""")
            }
        }
    }

    /** Igual que AppointmentController::store de barber: la cita nueva queda «pendiente» hasta que el barbero la confirme. */
    private fun registrarCita(request: RecordedRequest): MockResponse {
        val cuerpo = JSONObject(request.body.clone().readUtf8())
        val codigo = "UB-%04d".format(citas.size + 1)
        citas += JSONObject()
            .put("id", "appt-$codigo").put("code", codigo)
            .put("fecha", cuerpo.getString("fecha")).put("hora_inicio", cuerpo.getString("hora_inicio"))
            .put("estado", "pendiente").put("precio_cobrado", 180)
            .put("service", JSONObject().put("id", "svc-corte").put("nombre", "Corte clásico").put("precio", 180).put("duracion_min", 30))
            .put("barber", JSONObject().put("id", "barber-luis").put("user", JSONObject().put("id", "u-luis").put("name", "Luis Barbero")))
        return json(201, """{"message":"Cita reservada correctamente.","data":{"id":"appt-$codigo","code":"$codigo"}}""")
    }

    private fun RecordedRequest.ruta(): String = requestUrl?.encodedPath?.removePrefix("/api/v1/") ?: ""

    private fun ok(cuerpo: String) = json(200, cuerpo)

    private fun json(codigo: Int, cuerpo: String) =
        MockResponse().setResponseCode(codigo).setHeader("Content-Type", "application/json").setBody(cuerpo)

    private companion object {
        const val TIEMPO_MS = 10_000L
    }
}
