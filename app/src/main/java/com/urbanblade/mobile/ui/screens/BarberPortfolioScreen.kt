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

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            UrbanTopBar("Portafolio", onBack) {
                IconButton(onClick = { showUpload = true }) { Icon(Icons.Default.AddAPhoto, "Subir trabajo") }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold)
            error?.let { UrbanErrorBanner(it) }
            message?.let { UrbanInfoBanner(it, Icons.Default.CheckCircle) }

            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                UrbanMetricCard("Trabajos", portfolio.stats.totalWorks.toString(), Icons.Default.PhotoLibrary, Modifier.weight(1f))
                UrbanMetricCard("Reacciones", portfolio.stats.totalReactions.toString(), Icons.Default.Favorite, Modifier.weight(1f))
                UrbanMetricCard("Guardados", portfolio.stats.totalSaves.toString(), Icons.Default.Bookmark, Modifier.weight(1f))
            }

            if (portfolio.works.isEmpty() && !busy) {
                UrbanEmptyState(
                    "Sin trabajos todavía",
                    "Sube fotos o videos de tus cortes para que los clientes los vean.",
                    Icons.Default.PhotoLibrary,
                    actionLabel = "Subir el primero",
                    onAction = { showUpload = true }
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    gridItems(portfolio.works, key = { it.id }) { work ->
                        WorkTile(work, onDelete = { showDeleteConfirm = work })
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
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("♥ ${work.reactionsCount}", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
            Text("💬 ${work.commentsCount}", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
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
