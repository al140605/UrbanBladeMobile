package com.urbanblade.mobile.core.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * Gson de toda la app para leer respuestas de la API.
 *
 * Gson crea los modelos sin ejecutar el constructor de Kotlin cuando la clase tiene algún parámetro obligatorio, así
 * que un valor por defecto como `val roles: List<String> = emptyList()` NO se aplica si el servidor omite el campo:
 * la propiedad queda en `null` y la app revienta al recorrerla. Aquí, al terminar de leer un modelo de
 * `data/model`, las colecciones (listas, conjuntos y mapas) que quedaron en `null` pasan a estar vacías. No cambia
 * ninguna ruta ni cuerpo del contrato: solo hace tolerante la lectura.
 */
object ApiGson {
    val instance: Gson by lazy { GsonBuilder().registerTypeAdapterFactory(EmptyCollectionsFactory()).create() }
}

private const val MODEL_PACKAGE = "com.urbanblade.mobile.data.model."

private class EmptyCollectionsFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val raw = type.rawType
        if (!raw.name.startsWith(MODEL_PACKAGE) || raw.isEnum || raw.isInterface) return null
        val fields = collectionFields(raw)
        if (fields.isEmpty()) return null
        val delegate = gson.getDelegateAdapter(this, type)
        return object : TypeAdapter<T>() {
            override fun write(out: JsonWriter, value: T?) = delegate.write(out, value)

            override fun read(input: JsonReader): T? {
                val value = delegate.read(input) ?: return null
                fields.forEach { field -> if (field.get(value) == null) field.set(value, emptyFor(field.type)) }
                return value
            }
        }
    }

    private fun collectionFields(cls: Class<*>): List<Field> {
        val found = mutableListOf<Field>()
        var current: Class<*>? = cls
        while (current != null && current != Any::class.java) {
            current.declaredFields
                .filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic }
                .filter { Collection::class.java.isAssignableFrom(it.type) || Map::class.java.isAssignableFrom(it.type) }
                .forEach { it.isAccessible = true; found += it }
            current = current.superclass
        }
        return found
    }

    private fun emptyFor(type: Class<*>): Any = when {
        Map::class.java.isAssignableFrom(type) -> emptyMap<Any, Any>()
        Set::class.java.isAssignableFrom(type) -> emptySet<Any>()
        else -> emptyList<Any>()
    }
}
