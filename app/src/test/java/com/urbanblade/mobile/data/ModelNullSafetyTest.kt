package com.urbanblade.mobile.data

import com.google.gson.annotations.SerializedName
import com.urbanblade.mobile.core.network.ApiGson
import com.urbanblade.mobile.core.network.UrbanBladeApi
import com.urbanblade.mobile.data.model.AuthUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import java.io.File
import java.lang.reflect.Modifier
import java.util.zip.ZipFile
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaField
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contrato con la API, lado nulos. Gson crea los modelos sin pasar por el constructor de Kotlin, así que si el
 * servidor omite un campo que la app declara como NO nulo y sin valor por defecto, queda `null` y la app revienta
 * después, lejos del origen (NPE al pintar una pantalla). Esta prueba parsea `{}` en cada modelo de respuesta de
 * `data/model` y falla si alguna propiedad no nula queda en `null`: así un backend que quite o renombre un campo
 * (o un modelo nuevo sin valores por defecto) se detecta en el CI y no en el teléfono.
 *
 * Los modelos de PETICIÓN (los que la app envía) no se revisan: los llena la propia app.
 */
class ModelNullSafetyTest {
    private val gson = ApiGson.instance

    private val requestSuffixes = listOf("Request", "Body", "Draft", "Payload", "Params", "Form")

    /**
     * Campos que el servidor SIEMPRE envía (identificadores, credenciales y datos sin los que la respuesta no sirve).
     * Si faltan, es una respuesta rota y conviene que falle a la vista, no que se rellene con un valor inventado.
     * Las colecciones nunca están aquí: `ApiGson` las deja vacías y la prueba exige que no queden en `null`.
     */
    private val requiredByContract = setOf(
        "id", "token", "user", "barber", "email", "name", "nombre", "code",
        "estado", "fecha", "hora_inicio", "start_time", "end_time", "day_of_week",
        "time", "label", "brand", "last4", "client_secret", "payment_intent_id", "comment_id"
    )

    /** Nombres de clase de `data/model`, leídos del jar o de la carpeta de clases compiladas, según cómo las entregue Gradle. */
    private fun modelClassNames(): List<String> {
        val prefix = "com/urbanblade/mobile/data/model/"
        val root = File(UrbanBladeApi::class.java.protectionDomain.codeSource.location.toURI())
        val entries = if (root.isFile) {
            ZipFile(root).use { zip -> zip.entries().asSequence().map { it.name }.toList() }
        } else {
            root.walkTopDown().filter { it.isFile }.map { it.relativeTo(root).path.replace(File.separatorChar, '/') }.toList()
        }
        return entries.filter { it.startsWith(prefix) && it.endsWith(".class") }
            .map { it.removeSuffix(".class").replace('/', '.') }
    }

    private fun modelClasses(): List<Class<*>> {
        val names = modelClassNames()
        check(names.isNotEmpty()) { "No se encontraron los modelos compilados de data/model" }
        return names
            .map { Class.forName(it) }
            .filter { cls ->
                !cls.isInterface && !cls.isEnum && !Modifier.isAbstract(cls.modifiers) &&
                    !cls.simpleName.contains("Companion") &&
                    requestSuffixes.none { cls.simpleName.endsWith(it) } &&
                    cls.declaredFields.any { it.isAnnotationPresent(SerializedName::class.java) }
            }
    }

    private fun violations(cls: Class<*>): List<String> {
        val parsed = gson.fromJson("{}", cls) ?: return listOf("${cls.simpleName}: no se pudo crear")
        return cls.kotlin.declaredMemberProperties.mapNotNull { prop ->
            val field = prop.javaField ?: return@mapNotNull null
            if (field.type.isPrimitive || prop.returnType.isMarkedNullable) return@mapNotNull null
            field.isAccessible = true
            prop.isAccessible = true
            if (field.get(parsed) != null) return@mapNotNull null
            val json = field.getAnnotation(SerializedName::class.java)?.value ?: field.name
            val isCollection = Collection::class.java.isAssignableFrom(field.type) || Map::class.java.isAssignableFrom(field.type)
            if (!isCollection && json in requiredByContract) return@mapNotNull null
            "${cls.simpleName}.${prop.name} (\"$json\")"
        }
    }

    @Test
    fun `un valor por defecto de Kotlin no basta con Gson pero ApiGson deja la lista vacia`() {
        val user = ApiGson.instance.fromJson("""{"email":"a@b.c","name":"Ana"}""", AuthUser::class.java)

        assertNotNull(user.profileMissing)
        assertEquals(emptyList<String>(), user.profileMissing)
        assertEquals(emptyList<String>(), user.roles)
    }

    @Test
    fun `los modelos de respuesta se encuentran`() {
        assertTrue("Se esperaban decenas de modelos, hay ${modelClasses().size}", modelClasses().size > 50)
    }

    @Test
    fun `ninguna propiedad no nula queda en null cuando el servidor omite el campo`() {
        val problems = modelClasses().flatMap { violations(it) }.sorted()
        assertTrue(
            "Propiedades no nulas sin valor por defecto (${problems.size}); dales un valor por defecto o hazlas nulables:\n" +
                problems.joinToString("\n"),
            problems.isEmpty()
        )
    }
}
