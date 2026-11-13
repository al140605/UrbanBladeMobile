package com.urbanblade.mobile.ui.viewmodel

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** Un 422 de Laravel trae `message` y `errors`; la app nombra los campos rechazados sin depender del idioma. */
class FieldErrorsTest {
    private fun unprocessable(body: String) = HttpException(
        Response.error<Any>(422, body.toResponseBody("application/json".toMediaType()))
    )

    @Test
    fun `lee el primer mensaje de cada campo`() {
        val errors = parseFieldErrors(
            """{"message":"The email field is required. (and 1 more error)","errors":{"email":["The email field is required."],"telefono":["a","b"]}}"""
        )

        assertEquals(mapOf("email" to "The email field is required.", "telefono" to "a"), errors)
    }

    @Test
    fun `un cuerpo vacio, roto o sin errors no revienta`() {
        assertTrue(parseFieldErrors(null).isEmpty())
        assertTrue(parseFieldErrors("").isEmpty())
        assertTrue(parseFieldErrors("no es json").isEmpty())
        assertTrue(parseFieldErrors("""{"message":"x"}""").isEmpty())
        assertTrue(parseFieldErrors("""{"errors":[]}""").isEmpty())
    }

    @Test
    fun `el resumen nombra los campos en espanol y no repite`() {
        assertEquals("Revisa: Correo, Teléfono.", fieldErrorsSummary(mapOf("email" to "x", "telefono" to "y")))
        assertEquals("Revisa: Cantidad.", fieldErrorsSummary(mapOf("items.0.cantidad" to "x", "items.1.cantidad" to "y")))
        assertNull(fieldErrorsSummary(emptyMap()))
    }

    @Test
    fun `con muchos campos se resumen los demas`() {
        val many = mapOf("name" to "a", "email" to "b", "password" to "c", "telefono" to "d", "notas" to "e")

        assertEquals("Revisa: Nombre, Correo, Contraseña y 2 más.", fieldErrorsSummary(many))
    }

    @Test
    fun `un campo desconocido se muestra legible`() {
        assertEquals("Revisa: Codigo promo.", fieldErrorsSummary(mapOf("codigo_promo" to "x")))
    }

    @Test
    fun `el motivo en espanol del servidor tiene prioridad sobre el resumen`() {
        val body = """{"message":"El barbero ya tiene una cita en ese horario.","errors":{"hora_inicio":["x"]}}"""

        assertEquals("El barbero ya tiene una cita en ese horario.", validationMessage(body))
    }

    @Test
    fun `el resumen generico en ingles se sustituye por los campos`() {
        val body = """{"message":"The email field is required. (and 1 more error)","errors":{"email":["x"],"password":["y"]}}"""

        assertEquals("Revisa: Correo, Contraseña.", validationMessage(body))
    }

    @Test
    fun `sin errors ni motivo util queda el aviso generico`() {
        assertEquals("Revisa los datos capturados.", validationMessage(null))
        assertEquals("Revisa los datos capturados.", validationMessage("""{"message":"The given data was invalid."}"""))
    }

    @Test
    fun `toFriendlyMessage usa los campos en un 422 generico`() {
        val e = unprocessable("""{"message":"The email field is required.","errors":{"email":["The email field is required."]}}""")

        assertEquals("Revisa: Correo.", e.toFriendlyMessage("Error"))
    }

    @Test
    fun `toFriendlyMessage conserva el motivo en espanol de un 422`() {
        val e = unprocessable("""{"message":"Tienes un adeudo pendiente."}""")

        assertEquals("Tienes un adeudo pendiente.", e.toFriendlyMessage("Error"))
    }
}
