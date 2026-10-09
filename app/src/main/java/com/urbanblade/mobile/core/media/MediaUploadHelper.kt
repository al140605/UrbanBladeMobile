package com.urbanblade.mobile.core.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Convierte un Uri elegido con el selector nativo de fotos/video de Android
 * (Photo Picker -- ver ui/components/MediaPicker.kt) a la parte multipart
 * que espera Retrofit, leyendo los bytes vía ContentResolver ya que un
 * "content://" no es un archivo real que OkHttp pueda abrir directamente.
 */
object MediaUploadHelper {
    /**
     * Con [policy] (p. ej. [UploadPolicy.AVATAR]) se valida tipo, tamaño y contenido antes de subir y
     * lanza [UploadRejectedException]; sin política se conserva el comportamiento de siempre.
     */
    suspend fun uriToPart(
        context: Context,
        uri: Uri,
        partName: String,
        policy: UploadPolicy? = null
    ): MultipartBody.Part? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val declaredMime = resolver.getType(uri)
        val mimeType = policy?.checkType(declaredMime) ?: (declaredMime ?: "application/octet-stream")
        policy?.checkDeclaredSize(declaredSize(context, uri))
        val bytes = resolver.openInputStream(uri)?.use { input ->
            policy?.readLimited(input) ?: input.readBytes()
        } ?: return@withContext null
        policy?.checkSignature(bytes, mimeType)
        val extension = policy?.extensionFor(mimeType) ?: mimeType.substringAfter('/', "bin")
        val fileName = "upload_${System.currentTimeMillis()}.$extension"
        val body: RequestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())

        MultipartBody.Part.createFormData(partName, fileName, body)
    }

    /** Tamaño que informa el proveedor del archivo (null si no lo da). */
    private fun declaredSize(context: Context, uri: Uri): Long? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
        }
    } catch (_: Exception) {
        null
    }

    fun textPart(value: String): RequestBody = value.toRequestBody("text/plain".toMediaTypeOrNull())
}
