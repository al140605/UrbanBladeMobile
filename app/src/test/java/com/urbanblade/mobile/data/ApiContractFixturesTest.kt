package com.urbanblade.mobile.data

import com.google.gson.reflect.TypeToken
import com.urbanblade.mobile.core.network.ApiGson
import com.urbanblade.mobile.data.model.*
import java.lang.reflect.Type
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contrato con la API, lado respuestas reales. Cada archivo de `src/test/resources/contract/` es el ejemplo de
 * respuesta que `barber` documenta en su OpenAPI (`public/docs/openapi.yaml`, generado con Scribe) para un endpoint
 * que la app consume. Aquí se leen con el mismo `ApiGson` de producción en el modelo que la app declara para ese
 * endpoint, y se exige que ninguna propiedad NO nula del modelo (ni de los modelos anidados) quede en `null`.
 *
 * Si el backend renombra o quita un campo documentado, o la app declara como obligatorio algo que el servidor no
 * manda, esta prueba falla en el CI y no en el teléfono. Para refrescar los fixtures: regenerar el OpenAPI en
 * `barber` (`php artisan scribe:generate`) y volver a copiar los ejemplos.
 *
 * Excepción: `post_auth_register.json` NO es el ejemplo del OpenAPI. El `@response` de `AuthController::register()`
 * está desactualizado (id numérico, `role` en singular, sin `email`); el servidor responde con `UserResource`, igual
 * que el login, así que el fixture se construyó con esa forma. Hallazgo para corregir la documentación en `barber`.
 */
class ApiContractFixturesTest {
    private val cases: List<Pair<String, Type>> = listOf(
        "post_auth_login.json" to LoginResponse::class.java,
        "post_auth_register.json" to LoginResponse::class.java,
        "post_auth_refresh_token.json" to LoginResponse::class.java,
        "post_auth_google_token.json" to LoginResponse::class.java,
        "post_auth_forgot_password.json" to MessageResponse::class.java,
        "get_auth_me.json" to MeResponse::class.java,
        "post_appointments.json" to CreateAppointmentResponse::class.java,
        "patch_appointments_item_status.json" to MessageResponse::class.java,
        "get_services.json" to object : TypeToken<ApiList<ServiceItem>>() {}.type,
        "get_barbers.json" to object : TypeToken<ApiList<BarberItem>>() {}.type,
        "get_products.json" to object : TypeToken<ApiList<ProductItem>>() {}.type,
        "get_social_feed.json" to SocialFeedResponse::class.java,
        "get_barbers_item.json" to BarberProfileResponse::class.java,
        "get_profile.json" to AccountProfileResponse::class.java,
        "put_profile.json" to UpdateProfileResponse::class.java,
        "put_profile_password.json" to MessageResponse::class.java,
        "get_barber_schedule.json" to BarberScheduleResponse::class.java,
        "get_ai_briefing.json" to AiBriefingResponse::class.java,
        "get_inventory_products.json" to InventoryResponse::class.java,
        "get_inventory_movements.json" to InventoryMovementsResponse::class.java,
        "get_memberships_invoices.json" to MembershipInvoicesResponse::class.java,
        "get_memberships_invoices_item_receipt_link.json" to MembershipReceiptResponse::class.java,
        "get_payments_transfer_info.json" to TransferInfoResponse::class.java,
        "get_payments_cards.json" to SavedCardsResponse::class.java,
        "get_orders_item_receipt_link.json" to OrderReceiptResponse::class.java,
        "get_barber_portfolio.json" to PortfolioResponse::class.java,
        "post_barber_works.json" to CreateWorkResponse::class.java,
        "delete_barber_works_item.json" to MessageResponse::class.java
    )

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream("contract/$name")) { "Falta el fixture contract/$name" }
            .bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun isModel(cls: Class<*>) = cls.name.startsWith("com.urbanblade.mobile.data.model.")

    /** Recorre el objeto y anota cada propiedad no nula de un modelo que quedó en `null`. */
    private fun collect(value: Any?, path: String, out: MutableList<String>) {
        when {
            value == null -> Unit
            value is Collection<*> -> value.forEachIndexed { i, item -> collect(item, "$path[$i]", out) }
            value is Map<*, *> -> value.forEach { (k, v) -> collect(v, "$path[$k]", out) }
            isModel(value.javaClass) -> value.javaClass.kotlin.declaredMemberProperties.forEach { prop ->
                val field = prop.javaField ?: return@forEach
                if (field.type.isPrimitive) return@forEach
                field.isAccessible = true
                prop.isAccessible = true
                val child = field.get(value)
                if (child == null) {
                    if (!prop.returnType.isMarkedNullable) out += "$path.${prop.name}"
                } else {
                    collect(child, "$path.${prop.name}", out)
                }
            }
        }
    }

    @Test
    fun `hay fixtures para los endpoints documentados`() {
        assertTrue("Se esperaban decenas de fixtures, hay ${cases.size}", cases.size >= 25)
        cases.forEach { (file, _) -> assertTrue("Fixture vacío: $file", fixture(file).isNotBlank()) }
    }

    @Test
    fun `cada respuesta documentada se lee y respeta los campos obligatorios de la app`() {
        val problems = cases.flatMap { (file, type) ->
            val parsed = ApiGson.instance.fromJson<Any>(fixture(file), type)
            assertNotNull("$file no se pudo leer", parsed)
            mutableListOf<String>().also { collect(parsed, file, it) }
        }
        assertTrue(
            "Propiedades no nulas que el ejemplo documentado no trae (${problems.size}): o el backend omite el campo, o la app " +
                "debe declararlo nulable o con valor por defecto.\n" + problems.joinToString("\n"),
            problems.isEmpty()
        )
    }
}
