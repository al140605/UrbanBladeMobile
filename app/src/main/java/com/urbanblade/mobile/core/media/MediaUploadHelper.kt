package com.urbanblade.mobile.core.media

import android.content.Context
import android.net.Uri
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
    suspend fun uriToPart(context: Context, uri: Uri, partName: String): MultipartBody.Part? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@withContext null
        val extension = mimeType.substringAfter('/', "bin")
        val fileName = "upload_${System.currentTimeMillis()}.$extension"
        val body: RequestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())

        MultipartBody.Part.createFormData(partName, fileName, body)
    }

    fun textPart(value: String): RequestBody = value.toRequestBody("text/plain".toMediaTypeOrNull())
}
