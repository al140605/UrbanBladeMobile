package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.urbanblade.mobile.data.model.SocialWork
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.SocialFeedViewModel

@Composable
fun SocialFeedScreen(onBack: () -> Unit, vm: SocialFeedViewModel = viewModel()) {
    val feed by vm.feed.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { UrbanTopBar("Muro social", onBack) { IconButton(onClick = { vm.load() }) { Icon(Icons.Default.Refresh, "Actualizar") } } }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            urbanLoadingItem(busy, feed.data.isEmpty())
            error?.let { item { UrbanErrorBanner(it) } }

            if (feed.data.isEmpty() && !busy) {
                item { UrbanEmptyState("Sin publicaciones todavía", "Los trabajos que suban los barberos aparecerán aquí.", Icons.Default.PhotoLibrary) }
            }

            items(feed.data, key = { it.id }) { work ->
                WorkPost(
                    work = work,
                    onReact = { vm.toggleReaction(work.id) },
                    onSave = { vm.toggleSave(work.id) },
                    onComment = { text, onDone -> vm.comment(work.id, text, onDone) }
                )
            }
        }
    }
}

@Composable
private fun WorkPost(
    work: SocialWork,
    onReact: () -> Unit,
    onSave: () -> Unit,
    onComment: (String, () -> Unit) -> Unit
) {
    var commentText by remember { mutableStateOf("") }

    UrbanCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (work.barber?.foto != null) {
                AsyncImage(
                    model = work.barber.foto,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(36.dp).clip(CircleShape)
                )
            } else {
                UrbanAvatar(work.barber?.name ?: "?", Modifier.size(36.dp), imageUrl = work.barber?.foto)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(work.barber?.name ?: "Barbero", style = MaterialTheme.typography.titleSmall)
                work.workDate?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted) }
            }
        }

        Spacer(Modifier.height(10.dp))
        work.title?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
        work.description?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
        }

        if (work.media.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(work.media) { media ->
                    Box(Modifier.size(140.dp).clip(RoundedCornerShape(12.dp))) {
                        AsyncImage(
                            model = media.url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (media.type == "video") {
                            Icon(
                                Icons.Default.PlayCircle,
                                null,
                                tint = androidx.compose.ui.graphics.Color.White,
                                modifier = Modifier.align(Alignment.Center).size(32.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            IconRow(
                icon = if (work.isReacted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                count = work.reactionsCount,
                tint = if (work.isReacted) UrbanColors.Danger else UrbanColors.Muted,
                description = if (work.isReacted) "Quitar me gusta" else "Me gusta",
                onClick = onReact
            )
            // Solo informativo (el campo de comentario está debajo): antes era un botón que no hacía nada.
            IconRow(icon = Icons.Default.ChatBubbleOutline, count = work.commentsCount, tint = UrbanColors.Muted, description = "Comentarios", onClick = null)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSave) {
                Icon(
                    if (work.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    "Guardar",
                    tint = if (work.isSaved) UrbanColors.Gold else UrbanColors.Muted
                )
            }
        }

        if (work.comments.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                work.comments.forEach { c ->
                    Row {
                        Text(c.user?.name ?: "Usuario", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Gold)
                        Spacer(Modifier.width(6.dp))
                        Text(c.comment ?: "", style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it },
                placeholder = { Text("Escribe un comentario…") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.medium
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { onComment(commentText) { commentText = "" } },
                enabled = commentText.isNotBlank()
            ) { Icon(Icons.Default.Send, "Enviar") }
        }
    }
}

@Composable
private fun IconRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    tint: androidx.compose.ui.graphics.Color,
    description: String,
    onClick: (() -> Unit)?
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(8.dp))) {
        if (onClick != null) {
            // Área táctil estándar de 48 dp (antes 28 dp) y descripción para lectores de pantalla.
            IconButton(onClick = onClick) { Icon(icon, description, tint = tint) }
        } else {
            Icon(icon, description, tint = tint, modifier = Modifier.padding(horizontal = 12.dp))
        }
        Text(count.toString(), style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
    }
}
