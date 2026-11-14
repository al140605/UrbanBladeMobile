package com.urbanblade.mobile.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.ui.components.UrbanFormChoice
import com.urbanblade.mobile.ui.components.UrbanFormSection
import com.urbanblade.mobile.ui.components.UrbanFormSheet
import com.urbanblade.mobile.ui.components.formBlockerText
import com.urbanblade.mobile.ui.components.rememberUrbanFormState
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Inventory2
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.urbanblade.mobile.ui.components.WorkMediaThumb
import com.urbanblade.mobile.ui.components.VideoPlayerDialog
import com.urbanblade.mobile.data.model.WorkRow
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.PortfolioViewModel

@Composable
fun BarberPortfolioScreen(onBack: () -> Unit, vm: PortfolioViewModel = viewModel()) {
    val portfolio by vm.portfolio.collectAsState()
    val busy by vm.busy.collectAsState()
    val hasLoaded by vm.hasLoaded.collectAsState()
    val uploading by vm.uploading.collectAsState()
    val message by vm.message.collectAsState()
    val error by vm.error.collectAsState()
    val context = LocalContext.current

    var showUpload by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<WorkRow?>(null) }

    LaunchedEffect(Unit) { vm.load() }

    UrbanModuleScreen(
        eyebrow = "BARBERO",
        title = "Portafolio",
        subtitle = "Tus mejores trabajos, a la vista de tus clientes.",
        onBack = onBack,
        onRefresh = { vm.load() },
        refreshing = busy
    ) {
        if (!hasLoaded && error == null) item { UrbanSkeletonList(1) }
        if (hasLoaded) item {
            UrbanHeroCard {
                Text(
                    UrbanFormat.count(portfolio.stats.totalWorks, "trabajo publicado", "trabajos publicados"),
                    style = MaterialTheme.typography.titleMedium,
                    color = UrbanColors.Ink
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = UrbanColors.Ink.copy(alpha = 0.22f))
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    UrbanHeroStat("Reacciones", portfolio.stats.totalReactions.toString(), Icons.Default.Favorite, Modifier.weight(1f))
                    UrbanHeroStat("Guardados", portfolio.stats.totalSaves.toString(), Icons.Default.Bookmark, Modifier.weight(1f))
                }
            }
        }
        item {
            UrbanPrimaryButton(
                text = "Subir trabajo",
                onClick = { showUpload = true },
                icon = Icons.Default.AddAPhoto,
                modifier = Modifier.fillMaxWidth()
            )
        }
        message?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }

        when {
            error != null && portfolio.works.isEmpty() -> item {
                UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar tu portafolio", error, "Reintentar") { vm.load() }
            }
            portfolio.works.isEmpty() && hasLoaded && !busy -> item {
                UrbanMascotState(
                    UrbanStateKind.EMPTY,
                    "Sin trabajos todavía",
                    "Sube fotos o videos de tus cortes para que los clientes los vean.",
                    actionLabel = "Subir el primero",
                    actionIcon = Icons.Default.AddAPhoto,
                    onAction = { showUpload = true }
                )
            }
            else -> {
                error?.let { item { UrbanErrorBanner(it) } }
                items(portfolio.works.chunked(2)) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { work ->
                            Box(Modifier.weight(1f)) { WorkTile(work, onDelete = { showDeleteConfirm = work }) }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }

    if (showUpload) {
        UploadWorkDialog(
            uploading = uploading,
            onDismiss = { showUpload = false },
            onUpload = { title, description, media ->
                vm.upload(context, title, description, media) { showUpload = false }
            }
        )
    }

    showDeleteConfirm?.let { work ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("¿Eliminar \"${work.title}\"?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(work.id); showDeleteConfirm = null }) { Text("Sí, eliminar") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun WorkTile(work: WorkRow, onDelete: () -> Unit) {
    // Un toque al trabajo reproduce su primer video (si tiene).
    val videoUrl = work.media.firstOrNull { it.type == "video" }?.url
    var playing by remember { mutableStateOf(false) }
    if (playing && videoUrl != null) VideoPlayerDialog(videoUrl, onDismiss = { playing = false })

    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(14.dp))
                .then(if (videoUrl != null) Modifier.clickable { playing = true } else Modifier)
        ) {
            val first = work.media.firstOrNull()
            if (first != null) {
                WorkMediaThumb(
                    url = first.url,
                    isVideo = first.type == "video",
                    contentDescription = null,
                    playIconSize = 36.dp,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(UrbanColors.Card), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Image, null, tint = UrbanColors.Muted)
                }
            }
            if (first?.type != "video" && videoUrl != null) {
                Icon(
                    Icons.Default.PlayCircle,
                    "Tiene video",
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.align(Alignment.Center).size(36.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
            ) {
                Icon(Icons.Default.Delete, "Eliminar", tint = UrbanColors.Danger)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(work.title ?: "—", style = MaterialTheme.typography.titleSmall, maxLines = 1)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Favorite, "Reacciones", tint = UrbanColors.Gold, modifier = Modifier.size(14.dp))
            Text("${work.reactionsCount}", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
            Icon(Icons.Default.ChatBubble, "Comentarios", tint = UrbanColors.Gold, modifier = Modifier.size(14.dp))
            Text("${work.commentsCount}", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
        }
    }
}

@Composable
private fun UploadWorkDialog(
    uploading: Boolean,
    onDismiss: () -> Unit,
    onUpload: (String, String?, List<Uri>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var media by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val pickMedia = rememberMultiMediaPicker(maxItems = 10) { media = it }

    val form = rememberUrbanFormState()
    val titleError = if (title.isBlank()) "Escribe un título para tu trabajo." else null

    UrbanFormSheet(
        eyebrow = "Portafolio",
        title = "Subir trabajo",
        primaryLabel = "Publicar trabajo",
        onSubmit = { onUpload(title.trim(), description.trim().takeIf { it.isNotEmpty() }, media) },
        onDismiss = onDismiss,
        saving = uploading,
        blocker = formBlockerText(listOfNotNull("Título".takeIf { titleError != null }))
    ) {
        UrbanFormSection("Tu trabajo", icon = Icons.Default.ContentCut) {
            UrbanTextField(
                title, { title = it }, "Título", Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.Title, placeholder = "Ej. Fade con diseño",
                error = form.error("title", titleError), onBlur = { form.touch("title") },
                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences
            )
            UrbanTextField(
                description, { description = it }, "Descripción (opcional)", Modifier.fillMaxWidth(),
                leadingIcon = Icons.Default.Notes, placeholder = "Técnica, productos o lo que quieras contar",
                capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences,
                imeAction = androidx.compose.ui.text.input.ImeAction.Default, minLines = 3
            )
        }
        UrbanFormSection("Fotos y video", icon = Icons.Default.AddPhotoAlternate) {
            UrbanOutlineButton(
                text = if (media.isEmpty()) "Elegir fotos o video (hasta 10)" else "${media.size} archivo(s) elegidos · cambiar",
                onClick = pickMedia,
                icon = Icons.Default.AddPhotoAlternate,
                modifier = Modifier.fillMaxWidth()
            )
            if (media.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(media) { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }
    }
}
