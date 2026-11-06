package com.urbanblade.mobile.ui.screens

import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import com.urbanblade.mobile.core.network.UrbanBladeApi
import com.urbanblade.mobile.data.repository.UrbanRepository
import com.urbanblade.mobile.ui.viewmodel.AppointmentsViewModel
import java.io.File
import java.time.LocalDate
import java.util.concurrent.CopyOnWriteArrayList
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * T091 (HT-28): prueba de integración de unidades de la pantalla de pago de la app, con enfoque de caja negra.
 *
 * Caso: «El pago se habilita solo con la cita confirmada». Comprueba RF-15 (Pago de la cita) y RNF-07 (Mensajes de
 * error y de confirmación). Integración simulada: Mis citas y su hoja de pago, AppointmentsViewModel, UrbanRepository
 * y Retrofit son los reales. Solo son falsas la API (un MockWebServer que responde como barber) y la galería del
 * teléfono, de donde el cliente elige la foto del comprobante de transferencia.
 *
 * Caja negra: la prueba solo toca la pantalla como un cliente y revisa lo que se ve y lo que llegó a la API. Las
 * particiones son: cita pendiente (el pago no está disponible), cita confirmada (el pago se habilita) y pago
 * rechazado por el servidor (se muestra su mensaje y se puede reintentar).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h3000dp")
class PagoCitaConfirmadaIntegracionTest {

    @get:Rule
    val compose = createComposeRule()

    private val server = MockWebServer()
    private val recibidas = CopyOnWriteArrayList<RecordedRequest>()
    private lateinit var repo: UrbanRepository

    // Estado de la cita en la API falsa; los cambia cada caso y la subida de un comprobante aceptado.
    @Volatile private var estadoCita = "pendiente"
    @Volatile private var citaConPago = false

    /** Fotos que «elegirá» el cliente en la galería, en orden. */
    private val galeria = ArrayDeque<Uri>()

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
    fun `con la cita pendiente el pago no esta disponible`() {
        estadoCita = "pendiente"
        abrirMisCitas()

        esperarTexto("Pendiente")
        esperarTexto("Esperando que tu barbero apruebe la cita", parcial = true)
        esperarTexto("Corte clásico")
        compose.waitForIdle()

        assertEquals("una cita pendiente no ofrece «Pagar cita»", 0, contar("Pagar cita"))
        assertTrue("no se intenta ningún pago", solicitudesDePago().isEmpty())
    }

    @Test
    fun `con la cita confirmada se habilita el pago`() {
        estadoCita = "confirmada"
        abrirMisCitas()

        esperarTexto("Confirmada")
        esperarTexto("Cita aprobada. Resuelve el pago antes del servicio", parcial = true)
        boton("Pagar cita").assertIsEnabled()

        tocar("Pagar cita")
        esperarTexto("Total estimado")
        esperarTexto("MÉTODO DE PAGO")
        esperarTexto("Transferencia")
        assertTrue("abrir la hoja de pago todavía no cobra nada", solicitudesDePago().isEmpty())
    }

    @Test
    fun `si el pago se rechaza muestra el mensaje y deja reintentar`() {
        estadoCita = "confirmada"
        galeria += foto("foto-del-comprobante.heic", HEIC)
        galeria += foto("captura-del-comprobante.png", PNG)
        abrirMisCitas()

        // Primer intento: barber rechaza la foto HEIC (solo acepta jpg, jpeg, png o pdf).
        tocar("Pagar cita")
        tocar("Transferencia")
        tocar("Elegir comprobante")
        esperarTexto("Comprobante seleccionado")
        tocar("Subir comprobante")

        esperarTexto(MENSAJE_FORMATO)
        esperarTexto("MÉTODO DE PAGO")
        boton("Subir comprobante").assertIsEnabled()
        assertEquals(1, solicitudesDePago().size)
        assertFalse("el rechazo no registra ningún pago", citaConPago)

        // Reintento: el cliente elige otra imagen y vuelve a subirla; esta vez barber la acepta.
        tocar("Comprobante seleccionado")
        compose.waitForIdle()
        tocar("Subir comprobante")

        compose.waitUntil(TIEMPO_MS) { contar("MÉTODO DE PAGO") == 0 && contar("Pagar cita") == 0 }
        assertEquals("el mensaje del rechazo ya no se muestra", 0, contar(MENSAJE_FORMATO))
        val intentos = solicitudesDePago()
        assertEquals("el reintento llegó a la API", 2, intentos.size)
        assertTrue("primero se mandó la foto HEIC", intentos[0].body.clone().readByteString().indexOf(HEIC_MARCA) >= 0)
        assertTrue("en el reintento se mandó el PNG", intentos[1].body.clone().readByteString().indexOf(PNG) >= 0)
        assertTrue("el pago quedó registrado", citaConPago)
    }

    // ── Cómo se usa la pantalla (como lo haría un cliente) ──────────────────────────────────────────────────────

    /** Mis citas del cliente; la galería del teléfono se cambia por una falsa que devuelve las fotos de [galeria]. */
    private fun abrirMisCitas() {
        val misCitas = AppointmentsViewModel(repo)
        compose.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides galeriaFalsa) {
                MaterialTheme {
                    ClientAppointmentsScreen(onBook = {}, onNavigate = {}, vm = misCitas)
                }
            }
        }
    }

    private fun esperarTexto(texto: String, parcial: Boolean = false) {
        compose.waitUntil(TIEMPO_MS) { contar(texto, parcial) > 0 }
    }

    private fun contar(texto: String, parcial: Boolean = false) =
        compose.onAllNodesWithText(texto, substring = parcial).fetchSemanticsNodes().size

    private fun tocar(texto: String) {
        esperarTexto(texto)
        compose.onAllNodesWithText(texto)[0].performClick()
        compose.waitForIdle()
    }

    private fun boton(texto: String): SemanticsNodeInteraction {
        esperarTexto(texto)
        return compose.onAllNodesWithText(texto)[0]
    }

    private fun solicitudesDePago() = recibidas.filter { it.method == "POST" && it.ruta().contains("payment") }

    /** Escribe la foto en el teléfono (Robolectric) y devuelve su dirección, como la daría la galería. */
    private fun foto(nombre: String, contenido: ByteString): Uri {
        val archivo = File(RuntimeEnvironment.getApplication().cacheDir, nombre)
        archivo.writeBytes(contenido.toByteArray())
        return Uri.fromFile(archivo)
    }

    // ── Galería falsa: responde al selector de fotos con la siguiente foto de la lista ──────────────────────────

    private val galeriaFalsa = object : ActivityResultRegistryOwner {
        override val activityResultRegistry: ActivityResultRegistry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                val elegida = galeria.removeFirstOrNull()
                @Suppress("UNCHECKED_CAST")
                Handler(Looper.getMainLooper()).post { dispatchResult(requestCode, elegida as O) }
            }
        }
    }

    // ── API falsa (MockWebServer): responde como barber ─────────────────────────────────────────────────────────

    private val apiFalsa = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            recibidas += request
            val ruta = request.ruta()
            return when {
                request.method == "POST" && ruta == "appointments/$CODIGO/payment/receipt" -> recibirComprobante(request)
                ruta == "appointments" -> ok(JSONObject().put("data", JSONArray().put(cita())).put("cancellation_policy_hours", 24).toString())
                ruta == "no-show-fees" -> ok("""{"data":[],"adeudo_total":0}""")
                else -> ok("""{"data":[]}""")
            }
        }
    }

    /** Igual que AppointmentResource de barber: solo una cita confirmada, en proceso o completada se puede cobrar. */
    private fun cita(): JSONObject {
        val confirmada = estadoCita == "confirmada"
        return JSONObject()
            .put("id", "appt-1").put("code", CODIGO)
            .put("fecha", LocalDate.now().plusDays(1).toString()).put("hora_inicio", "18:30")
            .put("estado", estadoCita).put("precio_cobrado", 180)
            .put("is_chargeable", estadoCita in COBRABLES)
            .put("has_payment", citaConPago)
            .put("pago_resuelto", if (confirmada) false else JSONObject.NULL)
            .put("service", JSONObject().put("id", "svc-corte").put("nombre", "Corte clásico").put("precio", 180).put("duracion_min", 30))
            .put("barber", JSONObject().put("id", "barber-luis").put("user", JSONObject().put("id", "u-luis").put("name", "Luis Barbero")))
    }

    /**
     * Igual que PaymentController::uploadReceipt y PaymentService::uploadTransferReceipt de barber: primero valida el
     * archivo (jpg, jpeg, png o pdf, revisando su contenido) y luego que la cita se pueda cobrar y no tenga ya un pago.
     */
    private fun recibirComprobante(request: RecordedRequest): MockResponse {
        val cuerpo = request.body.clone().readByteString()
        val formatoValido = listOf(PNG, JPEG, PDF).any { cuerpo.indexOf(it) >= 0 }
        return when {
            !formatoValido -> json(
                422,
                JSONObject().put("message", MENSAJE_FORMATO)
                    .put("errors", JSONObject().put("comprobante", JSONArray().put(MENSAJE_FORMATO))).toString()
            )
            estadoCita !in COBRABLES -> json(422, """{"message":"Solo se puede subir comprobante de una cita aprobada por el barbero."}""")
            citaConPago -> json(422, """{"message":"La cita ya tiene un pago registrado o en revision."}""")
            else -> {
                citaConPago = true
                json(201, """{"message":"Comprobante recibido. Te avisaremos cuando se verifique.","data":{"id":"pay-1","estado":"pendiente_verificacion"}}""")
            }
        }
    }

    private fun RecordedRequest.ruta(): String = requestUrl?.encodedPath?.removePrefix("/api/v1/") ?: ""

    private fun ok(cuerpo: String) = json(200, cuerpo)

    private fun json(codigo: Int, cuerpo: String) =
        MockResponse().setResponseCode(codigo).setHeader("Content-Type", "application/json").setBody(cuerpo)

    private companion object {
        const val TIEMPO_MS = 10_000L
        const val CODIGO = "UB-0001"

        /** AppointmentStatusService::CHARGEABLE de barber. */
        val COBRABLES = listOf("confirmada", "en_proceso", "completada")

        /** Mensaje de validación de barber (lang/es/validation.php, regla mimes) para un archivo de otro tipo. */
        const val MENSAJE_FORMATO = "El campo comprobante debe ser un archivo de tipo: jpg, jpeg, png, pdf."

        // Primeros bytes de cada formato: así reconoce barber el tipo real del archivo.
        val PNG = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A).toByteString()
        val JPEG = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()).toByteString()
        val PDF = "%PDF-".encodeUtf8()
        val HEIC_MARCA = "ftypheic".encodeUtf8()

        /** Foto HEIC (formato de cámara de algunos teléfonos): barber no la acepta como comprobante. */
        val HEIC = (byteArrayOf(0, 0, 0, 0x18) + "ftypheicmif1heic".toByteArray() + ByteArray(64)).toByteString()
    }
}
