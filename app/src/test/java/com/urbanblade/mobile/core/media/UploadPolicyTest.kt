package com.urbanblade.mobile.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * Actividad 09 (Mi cuenta, déficit 1): la foto de perfil se leía entera en memoria sin límite de
 * tamaño, el tipo y el nombre del archivo se tomaban de lo que declarara el selector, y el
 * contenido nunca se comprobaba.
 */
class UploadPolicyTest {

    private val mb = 1024 * 1024
    private val policy = UploadPolicy.AVATAR

    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0, 0x10)
    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0)
    private val webp = "RIFF".toByteArray() + byteArrayOf(0x24, 0, 0, 0) + "WEBP".toByteArray() + byteArrayOf(0)

    private fun rejects(block: () -> Unit) {
        try {
            block()
            fail("debía rechazarse con UploadRejectedException")
        } catch (_: UploadRejectedException) {
        }
    }

    // ---- Réplica del comportamiento anterior (MediaUploadHelper.uriToPart sin política) ----

    @Test
    fun `deficit original - la lectura anterior no ponia limite de tamano`() {
        val eightMb = ByteArray(8 * mb)
        // Antes: resolver.openInputStream(uri).use { it.readBytes() } -> se leía todo.
        val read = ByteArrayInputStream(eightMb).readBytes()
        assertTrue("8 MB leídos completos, el servidor pide máximo 4 MB", read.size > 4 * mb)
    }

    @Test
    fun `deficit original - la extension salia del tipo declarado sin filtrar`() {
        // Antes: mimeType.substringAfter('/', "bin")
        assertEquals("svg+xml", "image/svg+xml".substringAfter('/', "bin"))
        assertEquals("x-msdownload", "application/x-msdownload".substringAfter('/', "bin"))
    }

    // ---- Solución ----

    @Test
    fun `solo se aceptan JPG PNG y WebP`() {
        assertEquals("image/jpeg", policy.checkType("image/jpeg"))
        assertEquals("image/png", policy.checkType("IMAGE/PNG"))
        assertEquals("image/webp", policy.checkType("image/webp"))
        listOf("image/svg+xml", "image/gif", "application/pdf", "video/mp4", "application/octet-stream", "", null)
            .forEach { rejects { policy.checkType(it) } }
    }

    @Test
    fun `un tamano declarado mayor a 4 MB se rechaza antes de leer`() {
        policy.checkDeclaredSize(4L * mb)
        policy.checkDeclaredSize(null)
        rejects { policy.checkDeclaredSize(4L * mb + 1) }
    }

    @Test
    fun `una lectura sin tamano conocido se corta al pasar el limite sin leer todo el archivo`() {
        var consumed = 0L
        val endless = object : InputStream() {
            override fun read(): Int { consumed++; return 0 }
            override fun read(b: ByteArray, off: Int, len: Int): Int { consumed += len; return len }
        }
        rejects { policy.readLimited(endless) }
        assertTrue("se leyó de más: $consumed", consumed <= 4L * mb + 64 * 1024)
    }

    @Test
    fun `un archivo dentro del limite se lee completo`() {
        val data = ByteArray(1 * mb) { 7 }
        assertEquals(data.size, policy.readLimited(ByteArrayInputStream(data)).size)
    }

    @Test
    fun `el contenido debe coincidir con el tipo declarado`() {
        policy.checkSignature(jpeg, "image/jpeg")
        policy.checkSignature(png, "image/png")
        policy.checkSignature(webp, "image/webp")
        rejects { policy.checkSignature("<?php echo 1; ?>".toByteArray(), "image/png") }
        rejects { policy.checkSignature(png, "image/jpeg") }
        rejects { policy.checkSignature(ByteArray(0), "image/webp") }
    }

    @Test
    fun `la extension sale de la lista permitida y no del tipo declarado`() {
        assertEquals("jpg", policy.extensionFor("image/jpeg"))
        assertEquals("png", policy.extensionFor("image/png"))
        assertEquals("webp", policy.extensionFor("image/webp"))
    }

    @Test
    fun `el limite del avatar coincide con el del servidor`() {
        assertEquals(4L * mb, policy.maxBytes)
    }
}
