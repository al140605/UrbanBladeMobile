package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.ui.viewmodel.CatalogViewModel

@Composable
fun CatalogScreen(onBook: () -> Unit, vm: CatalogViewModel = viewModel()) {
    val services by vm.services.collectAsState()
    val barbers by vm.barbers.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Explorar", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text("Servicios y equipo de UrbanBlade", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        item { Text("Servicios", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(services, key = { it.id }) { s ->
            ElevatedCard(onClick = onBook) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(s.nombre, fontWeight = FontWeight.Bold)
                        Text("${s.duracionMin} min", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("\$${"%.0f".format(s.precio)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                }
            }
        }
        item { Text("Barberos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(barbers, key = { it.id }) { b ->
            Card {
                Row(Modifier.fillMaxWidth().padding(16.dp)) {
                    Icon(Icons.Default.ContentCut, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(b.user?.name ?: "Barbero", fontWeight = FontWeight.Bold)
                        b.descripcion?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2) }
                    }
                }
            }
        }
    }
}
