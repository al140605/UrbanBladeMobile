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
import com.urbanblade.mobile.ui.viewmodel.validateBarberProfile
import com.urbanblade.mobile.data.model.BarberMeResponse
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notes
import androidx.compose.ui.text.style.TextAlign
import com.urbanblade.mobile.ui.components.UrbanFormSection
import com.urbanblade.mobile.ui.components.UrbanFormSheet
import com.urbanblade.mobile.ui.components.UrbanOutlineButton

/**
 * «Mi perfil» del barbero: así lo ven los clientes (foto, calificación, especialidades y descripción) y
 * editar abre el formulario único, el mismo que el resto de la app.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BarberMeScreen(onBack: () -> Unit, vm: BarberMeViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val profile = state.profile

    var showEdit by remember { mutableStateOf(false) }
    // El formulario se cierra solo cuando el guardado terminó sin error; con error queda abierto para corregir.
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(state.saving) {
        if (submitted && !state.saving) {
            submitted = false
            if (state.saveError == null) showEdit = false
        }
    }

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
                        Triple("Calificación", profile.calificacionPromedio?.let { String.format("%.1f", it) + " de 5" } ?: "Sin reseñas", UrbanColors.Gold),
                        Triple("Reseñas", profile.totalResenas.toString(), UrbanColors.Ink),
                        Triple("Completadas", (profile.stats?.totalCompletadas ?: 0).toString(), UrbanColors.Ink)
                    )
                )
            }
            state.notice?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }
            item {
                UrbanCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (profile.fotoUrl != null) {
                            AsyncImage(
                                model = profile.fotoUrl,
                                contentDescription = "Tu foto de perfil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(88.dp).clip(CircleShape).background(UrbanColors.Gold.copy(alpha = 0.13f))
                            )
                        } else {
                            UrbanAvatar(profile.name, Modifier.size(88.dp))
                        }
                        Text(profile.name, style = MaterialTheme.typography.titleLarge, color = UrbanColors.Ink)
                        val tags = profile.especialidades.orEmpty().split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        if (tags.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                tags.forEach { SimpleStatusPill(it, UrbanColors.Gold) }
                            }
                        }
                        Text(
                            profile.descripcion?.takeIf { it.isNotBlank() } ?: "Aún no escribes nada sobre ti. Cuéntales a tus clientes quién eres y cómo trabajas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = UrbanColors.Muted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            item {
                Text(
                    "Así te ven los clientes al reservar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = UrbanColors.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                UrbanOutlineButton(
                    text = "Editar perfil",
                    onClick = { vm.clearMessages(); showEdit = true },
                    icon = Icons.Default.Edit,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showEdit && profile != null) {
        BarberMeFormSheet(
            profile = profile,
            saving = state.saving,
            error = state.saveError,
            onDismiss = { showEdit = false },
            onSubmit = { especialidades, descripcion, photo ->
                submitted = true
                vm.save(context, especialidades, descripcion, photo)
            }
        )
    }
}

/** Formulario de perfil del barbero: foto, especialidades y descripción. */
@Composable
private fun BarberMeFormSheet(
    profile: BarberMeResponse,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: (String, String, Uri?) -> Unit
) {
    var especialidades by remember { mutableStateOf(profile.especialidades.orEmpty()) }
    var descripcion by remember { mutableStateOf(profile.descripcion.orEmpty()) }
    var photo by remember { mutableStateOf<Uri?>(null) }
    val pickPhoto = rememberSingleImagePicker { photo = it }

    val dirty = especialidades.trim() != profile.especialidades.orEmpty().trim() ||
        descripcion.trim() != profile.descripcion.orEmpty().trim() || photo != null
    val problem = validateBarberProfile(especialidades, descripcion)

    UrbanFormSheet(
        eyebrow = "Mi perfil",
        title = "Editar perfil",
        primaryLabel = "Guardar cambios",
        onSubmit = { onSubmit(especialidades, descripcion, photo) },
        onDismiss = onDismiss,
        saving = saving,
        blocker = problem ?: if (!dirty) "Aún no hay cambios que guardar" else null,
        error = error
    ) {
        UrbanFormSection("Foto", icon = Icons.Default.PhotoCamera) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(UrbanColors.Card).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val model: Any? = photo ?: profile.fotoUrl
                if (model != null) {
                    AsyncImage(
                        model = model,
                        contentDescription = "Foto de perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp).clip(CircleShape)
                    )
                } else {
                    UrbanAvatar(profile.name, Modifier.size(64.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(if (photo != null) "Foto nueva" else "Tu foto", style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Ink)
                    Text("JPG, PNG o WebP de hasta 4 MB", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                }
                UrbanTextButton(onClick = pickPhoto, enabled = !saving) { Text("Cambiar", color = UrbanColors.Gold) }
            }
        }
        UrbanFormSection("Lo que ven tus clientes", icon = Icons.Default.ContentCut) {
            UrbanTextField(
                especialidades, { especialidades = it.take(FormLimits.SPECIALTIES_MAX) }, "Especialidades (opcional)", Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.ContentCut, placeholder = "Fade, barba a navaja, diseño",
                helper = "Sepáralas con comas.",
                capitalization = KeyboardCapitalization.Sentences, minLines = 2
            )
            UrbanTextField(
                descripcion, { descripcion = it.take(FormLimits.DESCRIPTION_MAX) }, "Sobre ti (opcional)", Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.Notes, placeholder = "Cuéntales quién eres y cómo trabajas",
                helper = if (FormLimits.nearLimit(descripcion, FormLimits.DESCRIPTION_MAX)) descripcion.length.toString() + "/" + FormLimits.DESCRIPTION_MAX else null,
                capitalization = KeyboardCapitalization.Sentences, minLines = 4
            )
        }
    }
}
