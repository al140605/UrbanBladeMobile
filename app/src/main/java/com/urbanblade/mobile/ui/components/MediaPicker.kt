package com.urbanblade.mobile.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/**
 * Selector nativo de fotos/video de Android (Photo Picker, androidx.activity) --
 * no pide permiso de almacenamiento (a diferencia de un Intent.ACTION_GET_CONTENT
 * clásico) y funciona igual en minSdk 26+ vía el backport de Google Play
 * services. Los Uri que devuelve son "content://" temporales de solo
 * lectura -- se convierten a multipart con MediaUploadHelper.uriToPart().
 */
@Composable
fun rememberSingleImagePicker(onPicked: (Uri) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onPicked)
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

@Composable
fun rememberMultiMediaPicker(maxItems: Int, onPicked: (List<Uri>) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems)) { uris ->
        if (uris.isNotEmpty()) onPicked(uris)
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }
}
