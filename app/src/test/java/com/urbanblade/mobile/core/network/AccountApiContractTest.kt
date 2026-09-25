package com.urbanblade.mobile.core.network

import com.urbanblade.mobile.data.model.ChangePasswordRequest
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Contrato de "Mi cuenta" contra respuestas con la forma de Laravel
 * (Api/Profile/ProfileController y Api/Notification/NotificationController).
 */
class AccountApiContractTest {

    private lateinit var server: MockWebServer
    private lateinit var api: UrbanBladeApi

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UrbanBladeApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `profile deserializa antiguedad, verificacion y datos del cliente`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"user":{"id":"1","name":"Ana","email":"ana@test.com","avatar_url":null,
                "email_verified_at":"2026-09-01T10:00:00Z","created_at":"2026-08-15T09:00:00Z","roles":["cliente"],
                "client":{"telefono":"5512345678","fecha_nacimiento":"1995-05-15","sexo":"femenino","descuento_activo_pct":5}}}"""
            )
        )

        val user = api.profile().user

        assertEquals("/api/v1/profile", server.takeRequest().path)
        assertEquals("2026-08-15T09:00:00Z", user.createdAt)
        assertEquals("2026-09-01T10:00:00Z", user.emailVerifiedAt)
        assertEquals("1995-05-15", user.client?.fechaNacimiento)
        assertEquals(5.0, user.client?.descuentoActivoPct ?: 0.0, 0.0)
    }

    @Test
    fun `updatePassword manda los tres campos que valida Laravel`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"message":"Contraseña actualizada exitosamente"}"""))

        api.updatePassword(ChangePasswordRequest("vieja123", "nueva12345", "nueva12345"))

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/profile/password", request.path)
        assertEquals(
            """{"current_password":"vieja123","password":"nueva12345","password_confirmation":"nueva12345"}""",
            request.body.readUtf8()
        )
    }

    @Test
    fun `updateAvatar sube la foto como multipart en el campo avatar`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"message":"Foto de perfil actualizada.","user":{"name":"Ana","email":"ana@test.com","avatar_url":"https://api.test/storage/avatars/1/x.jpg"}}"""
            )
        )
        val part = MultipartBody.Part.createFormData("avatar", "foto.jpg", byteArrayOf(1, 2, 3).toRequestBody("image/jpeg".toMediaType()))

        val response = api.updateAvatar(part)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/profile/avatar", request.path)
        assertTrue(request.getHeader("Content-Type")!!.startsWith("multipart/form-data"))
        assertTrue(request.body.readUtf8().contains("name=\"avatar\"; filename=\"foto.jpg\""))
        assertEquals("https://api.test/storage/avatars/1/x.jpg", response.user?.avatarUrl)
    }

    @Test
    fun `preferencias manda solo el canal que cambio y lee la respuesta completa`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"message":"Preferencias de notificación actualizadas.",
                "data":{"in_app":true,"email":true,"sms":false,"whatsapp":false,"push":false,"promociones":false}}"""
            )
        )

        val prefs = api.updateNotificationPreferences(mapOf("promociones" to false)).data

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/notifications/preferences", request.path)
        assertEquals("""{"promociones":false}""", request.body.readUtf8())
        assertTrue(prefs.inApp)
        assertFalse(prefs.promociones)
    }
}
