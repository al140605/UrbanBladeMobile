package com.urbanblade.mobile.core.media

import java.io.ByteArrayOutputStream
import java.io.InputStream

/** El archivo elegido no cumple la política de subida; el mensaje se muestra tal cual al usuario. */
class UploadRejectedException(message: String) : Exception(message)

/**
 * Reglas que la app aplica antes de subir un archivo (Actividad 09, Mi cuenta): tipo permitido,
 * tamaño máximo, contenido que coincida con el tipo y nombre de archivo propio. Así un archivo
 * enorme o disfrazado no llega a cargarse en memoria ni se manda al servidor, que de todos modos
 * lo revalida.
 *
 * @param allowed tipo MIME permitido -> extensión con la que se nombra el archivo.
 */
class UploadPolicy(private val allowed: Map<String, String>, val maxBytes: Long) {

    /** Devuelve el tipo normalizado (minúsculas) o rechaza si no está permitido. */
    fun checkType(mime: String?): String {
        val normalized = mime?.trim()?.lowercase().orEmpty()
        if (normalized !in allowed) throw UploadRejectedException(typeMessage())
        return normalized
    }

    /** Rechaza antes de leer si el proveedor ya informa un tamaño mayor al permitido. */
    fun checkDeclaredSize(size: Long?) {
        if (size != null && size > maxBytes) throw UploadRejectedException(sizeMessage())
    }

    /** Lee el archivo sin pasar nunca de [maxBytes] + un bloque: corta en cuanto lo supera. */
    fun readLimited(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(BLOCK)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) throw UploadRejectedException(sizeMessage())
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    /** El contenido real debe empezar como el tipo declarado (no basta con el nombre o el MIME). */
    fun checkSignature(bytes: ByteArray, mime: String) {
        val ok = when (mime) {
            "image/jpeg" -> bytes.startsWith(0xFF, 0xD8, 0xFF)
            "image/png" -> bytes.startsWith(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
            "image/webp" -> bytes.size >= 12 && bytes.startsWith(0x52, 0x49, 0x46, 0x46) &&
                bytes[8].toInt() == 0x57 && bytes[9].toInt() == 0x45 && bytes[10].toInt() == 0x42 && bytes[11].toInt() == 0x50
            else -> false
        }
        if (!ok) throw UploadRejectedException("El archivo no es una imagen válida. " + typeMessage())
    }

    fun extensionFor(mime: String): String = allowed.getValue(mime)

    private fun ByteArray.startsWith(vararg head: Int): Boolean =
        size >= head.size && head.indices.all { this[it].toInt() and 0xFF == head[it] }

    private fun typeMessage() = "Usa una imagen JPG, PNG o WebP."
    private fun sizeMessage() = "La foto pesa más de ${maxBytes / (1024 * 1024)} MB. Elige una más ligera."

    companion object {
        private const val BLOCK = 8 * 1024

        /** Foto de perfil: mismo límite y tipos que valida el servidor (POST /profile/avatar). */
        val AVATAR = UploadPolicy(
            allowed = mapOf("image/jpeg" to "jpg", "image/png" to "png", "image/webp" to "webp"),
            maxBytes = 4L * 1024 * 1024
        )
    }
}
