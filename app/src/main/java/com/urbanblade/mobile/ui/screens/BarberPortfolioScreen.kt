package com.urbanblade.mobile.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.urbanblade.mobile.data.model.WorkRow
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.PortfolioViewModel

@Composable
fun BarberPortfolioScreen(onBack: () -> Unit, vm: PortfolioViewModel = viewModel()) {
    val portfolio by vm.portfolio.collectAsState()
    val busy by vm.busy.collectAsState()
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
        item {
            UrbanHeroCard {
                UrbanHeroLabel("Tu vitrina")
                Spacer(Modifier.height(6.dp))
                Text(
                    UrbanFormat.count(portfolio.stats.totalWorks, "trabajo publicado", "trabajos publicados"),
                    style = MaterialTheme.typography.headlineSmall,
                    color = UrbanColors.Ink
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = UrbanColors.Ink.copy(alpha = 0.22f))
                Spacer(Modifier.height(14.dp))
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
            portfolio.works.isEmpty() && !busy -> item {
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
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(14.dp))
        ) {
            val first = work.media.firstOrNull()
            if (first != null) {
                AsyncImage(
                    model = first.url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(UrbanColors.Card), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Image, null, tint = UrbanColors.Muted)
                }
            }
            if (work.media.any { it.type == "video" }) {
                Icon(
                    Icons.Default.PlayCircle,
                    null,
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Subir trabajo") },
        text = {
            UrbanFormScroll(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(title, { title = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("Descripción (opcional)") }, modifier = Modifier.fillMaxWidth())
                UrbanOutlineButton(
                    text = if (media.isEmpty()) "Elegir fotos o video (hasta 10)" else "${media.size} archivo(s) elegidos",
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
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp))
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !uploading && title.isNotBlank(),
                onClick = { onUpload(title, description.takeIf { it.isNotBlank() }, media) }
            ) { Text(if (uploading) "Subiendo…" else "Subir") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
