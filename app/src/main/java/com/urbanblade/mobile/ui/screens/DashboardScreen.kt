package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.JsonObject
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(user: AuthUser, onAppointments: () -> Unit, onBook: () -> Unit, vm: DashboardViewModel = viewModel()) {
    val dashboard by vm.data.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Hola, ${user.name.substringBefore(' ')}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(user.roles.joinToString(" · ").ifBlank { "usuario" }, color = MaterialTheme.colorScheme.primary)
        }
        item {
            Card {
                Column(Modifier.padding(18.dp)) {
                    Text("UrbanBlade móvil", fontWeight = FontWeight.Bold)
                    Text("Tu agenda, servicios y perfil conectados al mismo backend de la plataforma web.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        error?.let { msg -> item { Text(msg, color = MaterialTheme.colorScheme.error) } }
        dashboard?.let { response ->
            item { Text("Resumen ${response.role ?: user.roles.firstOrNull().orEmpty()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            val kpis = extractKpis(response.data)
            items(kpis.size) { index ->
                val (label, value) = kpis[index]
                ElevatedCard {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label)
                        Text(value, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item {
            Text("Acciones rápidas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AssistChip(onClick = onAppointments, label = { Text("Mis citas") }, leadingIcon = { Icon(Icons.Default.CalendarMonth, null) })
                if (user.roles.any { it in listOf("cliente", "administrador", "recepcionista") }) {
                    AssistChip(onClick = onBook, label = { Text("Reservar") }, leadingIcon = { Icon(Icons.Default.ContentCut, null) })
                }
            }
        }
        item {
            Card {
                Row(Modifier.fillMaxWidth().padding(16.dp)) {
                    Icon(Icons.Default.Insights, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Arquitectura por roles", fontWeight = FontWeight.Bold)
                        Text("La API decide el recorte de datos según administrador, recepción, barbero, cliente o ingeniero.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private fun extractKpis(data: JsonObject): List<Pair<String, String>> {
    val source = data.getAsJsonObject("kpis") ?: data
    val friendly = mapOf(
        "appointments_today" to "Citas hoy",
        "appointments_week" to "Citas esta semana",
        "revenue_today" to "Ingresos hoy",
        "revenue_month" to "Ingresos del mes",
        "clients_total" to "Clientes",
        "total_clients" to "Clientes",
        "pending_appointments" to "Citas pendientes"
    )
    val result = mutableListOf<Pair<String, String>>()
    for ((key, value) in source.entrySet()) {
        if (!value.isJsonPrimitive) continue
        val primitive = value.asJsonPrimitive
        if (!primitive.isNumber && !primitive.isString) continue
        val label = friendly[key] ?: key.replace('_', ' ').replaceFirstChar { it.uppercase() }
        result += label to primitive.asString
        if (result.size == 4) break
    }
    return result
}
