package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.LogsViewModel
import com.urbanblade.mobile.ui.viewmodel.ReviewsViewModel

@Composable
fun ReviewsScreen(onBack: () -> Unit, vm: ReviewsViewModel = viewModel()) {
    val reviews by vm.reviews.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    var ratingFilter by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(ratingFilter) { vm.load(rating = ratingFilter) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { UrbanTopBar("Reseñas", onBack) { IconButton(onClick = { vm.load(rating = ratingFilter) }) { Icon(Icons.Default.Refresh, "Actualizar") } } }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            error?.let { item { UrbanErrorBanner(it) } }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UrbanMetricCard("Total", reviews.stats.total.toString(), Icons.Default.Star, Modifier.weight(1f))
                    UrbanMetricCard("Promedio", "%.1f".format(reviews.stats.promedio), Icons.Default.StarHalf, Modifier.weight(1f))
                    UrbanMetricCard("Bajas (≤2★)", reviews.stats.bajas.toString(), Icons.Default.StarBorder, Modifier.weight(1f))
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = ratingFilter == null, onClick = { ratingFilter = null }, label = { Text("Todas") })
                    (5 downTo 1).forEach { r ->
                        FilterChip(selected = ratingFilter == r, onClick = { ratingFilter = if (ratingFilter == r) null else r }, label = { Text("$r★") })
                    }
                }
            }

            if (reviews.data.isEmpty() && !busy) {
                item { UrbanEmptyState("Sin reseñas", "No hay reseñas que coincidan con el filtro.", Icons.Default.Star) }
            }

            items(reviews.data) { review ->
                UrbanCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(review.barber?.name ?: "Barbero", style = MaterialTheme.typography.titleMedium)
                        Text("${review.rating}★", style = MaterialTheme.typography.titleMedium, color = UrbanColors.Gold)
                    }
                    Text(review.client?.name ?: "Cliente anónimo", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                    review.comment?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun LogsScreen(onBack: () -> Unit, vm: LogsViewModel = viewModel()) {
    val logs by vm.logs.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    var search by remember { mutableStateOf("") }
    var eventFilter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(eventFilter) { vm.load(search.takeIf { it.isNotBlank() }, event = eventFilter) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { UrbanTopBar("Logs de actividad", onBack) { IconButton(onClick = { vm.load(search.takeIf { it.isNotBlank() }, event = eventFilter) }) { Icon(Icons.Default.Refresh, "Actualizar") } } }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            error?.let { item { UrbanErrorBanner(it) } }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    UrbanMetricCard("Hoy", logs.stats.hoy.toString(), Icons.Default.Today, Modifier.weight(1f))
                    UrbanMetricCard("Creados", logs.stats.creates.toString(), Icons.Default.AddCircle, Modifier.weight(1f))
                    UrbanMetricCard("Eliminados", logs.stats.deletes.toString(), Icons.Default.RemoveCircle, Modifier.weight(1f))
                }
            }

            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Buscar en descripción…") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { vm.load(search.takeIf { it.isNotBlank() }, event = eventFilter) }),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search)
                )
            }

            if (logs.events.isNotEmpty()) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = eventFilter == null, onClick = { eventFilter = null }, label = { Text("Todos") })
                        logs.events.forEach { ev ->
                            FilterChip(selected = eventFilter == ev, onClick = { eventFilter = if (eventFilter == ev) null else ev }, label = { Text(ev) })
                        }
                    }
                }
            }

            if (logs.data.isEmpty() && !busy) {
                item { UrbanEmptyState("Sin registros", "No hay actividad que coincida con el filtro.", Icons.Default.History) }
            }

            items(logs.data) { log ->
                UrbanCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(log.description ?: log.logName ?: "—", style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(log.causer?.name ?: "Sistema", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                        }
                        log.event?.let { SimpleStatusPill(it) }
                    }
                    log.createdAt?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                    }
                }
            }
        }
    }
}
