package com.urbanblade.mobile.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.urbanblade.mobile.core.validation.FormLimits
import com.urbanblade.mobile.ui.components.UrbanAvatar
import com.urbanblade.mobile.ui.components.UrbanCard
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanInfoBanner
import com.urbanblade.mobile.ui.components.UrbanModuleScreen
import com.urbanblade.mobile.ui.components.UrbanPrimaryButton
import com.urbanblade.mobile.ui.components.UrbanStatStrip
import com.urbanblade.mobile.ui.components.UrbanTextButton
import com.urbanblade.mobile.ui.components.UrbanTextField
import com.urbanblade.mobile.ui.components.rememberSingleImagePicker
import com.urbanblade.mobile.ui.components.urbanListFeedback
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.BarberMeViewModel

/** «Mi perfil» del barbero: calificación y actividad a la vista, y edición de especialidades, descripción y foto. */
@Composable
fun BarberMeScreen(onBack: () -> Unit, vm: BarberMeViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val profile = state.profile

    var especialidades by remember(profile?.especialidades) { mutableStateOf(profile?.especialidades.orEmpty()) }
    var descripcion by remember(profile?.descripcion) { mutableStateOf(profile?.descripcion.orEmpty()) }
    var photo by remember { mutableStateOf<Uri?>(null) }
    val pickPhoto = rememberSingleImagePicker { photo = it; vm.clearMessages() }

    LaunchedEffect(Unit) { vm.load() }
    // Con la foto ya guardada, la vista previa local deja de hacer falta.
    LaunchedEffect(profile?.fotoUrl) { photo = null }

    val dirty = profile != null && (
        especialidades.trim() != profile.especialidades.orEmpty().trim() ||
            descripcion.trim() != profile.descripcion.orEmpty().trim() ||
            photo != null
        )

    UrbanModuleScreen(
        eyebrow = "BARBERO",
        title = "Mi perfil",
        subtitle = "Lo que ven tus clientes cuando eligen barbero.",
        onBack = onBack,
        onRefresh = { vm.load() },
        refreshing = state.loading
    ) {
        urbanListFeedback(
            state.loaded, state.loading, profile != null, state.loadError,
            "No pudimos cargar tu perfil", "Sin perfil de barbero",
            "Tu cuenta no tiene perfil de barbero. Pide a administración que lo cree."
        ) { vm.load() }

        if (profile != null) {
            item {
                UrbanStatStrip(
                    listOf(
                        Triple("Calificación", profile.calificacionPromedio?.let { "%.1f★".format(it) } ?: "—", UrbanColors.Gold),
                        Triple("Reseñas", profile.totalResenas.toString(), UrbanColors.Ink),
                        Triple("Completadas", (profile.stats?.totalCompletadas ?: 0).toString(), UrbanColors.Ink)
                    )
                )
            }
            state.notice?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }
            state.saveError?.let { item { UrbanErrorBanner(it) } }
            item {
                UrbanCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            if (photo != null) {
                                AsyncImage(
                                    model = photo,
                                    contentDescription = "Vista previa de tu nueva foto",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(72.dp).clip(CircleShape).background(UrbanColors.Gold.copy(alpha = 0.13f))
                                )
                            } else if (profile.fotoUrl != null) {
                                AsyncImage(
                                    model = profile.fotoUrl,
                                    contentDescription = "Tu foto de perfil",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(72.dp).clip(CircleShape).background(UrbanColors.Gold.copy(alpha = 0.13f))
                                )
                            } else {
                                Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) { UrbanAvatar(profile.name) }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(profile.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (photo != null) "Foto nueva: se guarda al pulsar «Guardar cambios»." else "JPG, PNG o WebP de hasta 4 MB.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = UrbanColors.Muted
                                )
                            }
                        }
                        UrbanTextButton(onClick = pickPhoto, enabled = !state.saving) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("  Cambiar foto")
                        }
                        UrbanTextField(
                            value = especialidades,
                            onValueChange = { especialidades = it.take(FormLimits.SPECIALTIES_MAX) },
                            label = "Especialidades",
                            placeholder = "Ej. degradados, barba a navaja, diseño",
                            helper = if (FormLimits.nearLimit(especialidades, FormLimits.SPECIALTIES_MAX)) {
                                "${especialidades.length}/${FormLimits.SPECIALTIES_MAX}"
                            } else {
                                null
                            },
                            capitalization = KeyboardCapitalization.Sentences,
                            minLines = 2
                        )
                        UrbanTextField(
                            value = descripcion,
                            onValueChange = { descripcion = it.take(FormLimits.DESCRIPTION_MAX) },
                            label = "Sobre ti",
                            placeholder = "Cuéntale a tus clientes quién eres y cómo trabajas",
                            helper = if (FormLimits.nearLimit(descripcion, FormLimits.DESCRIPTION_MAX)) {
                                "${descripcion.length}/${FormLimits.DESCRIPTION_MAX}"
                            } else {
                                null
                            },
                            capitalization = KeyboardCapitalization.Sentences,
                            minLines = 4
                        )
                        UrbanPrimaryButton(
                            text = if (state.saving) "Guardando…" else "Guardar cambios",
                            onClick = { vm.save(context, especialidades, descripcion, photo) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = dirty && !state.saving,
                            icon = Icons.Default.Save,
                            loading = state.saving
                        )
                    }
                }
            }
        }
    }
}
