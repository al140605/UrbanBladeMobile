package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.BookingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(onBack: () -> Unit, onCreated: () -> Unit, vm: BookingViewModel = viewModel()) {
    val services by vm.services.collectAsState()
    val barbers by vm.barbers.collectAsState()
    val slots by vm.slots.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    var barberId by remember { mutableStateOf("") }
    var serviceId by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(vm.defaultDate()) }
    var time by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var barberMenu by remember { mutableStateOf(false) }
    var serviceMenu by remember { mutableStateOf(false) }
    var slotMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.loadCatalog() }
    LaunchedEffect(barberId, serviceId, date) {
        time = ""
        vm.loadSlots(barberId, serviceId, date)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { UrbanTopBar(title = "Reservar cita", onBack = onBack) }
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                UrbanPageHeader(
                    title = "Tu próximo corte",
                    subtitle = "Elige profesional, servicio y fecha. Solo mostramos horarios disponibles.",
                    eyebrow = "Reserva inteligente"
                )
            }

            item {
                UrbanInfoBanner(
                    "La disponibilidad se consulta en tiempo real antes de confirmar la cita.",
                    Icons.Default.Bolt
                )
            }

            item {
                BookingStep(number = "01", title = "Elige tu barbero", icon = Icons.Default.PersonSearch) {
                    ExposedDropdownMenuBox(expanded = barberMenu, onExpandedChange = { barberMenu = !barberMenu }) {
                        OutlinedTextField(
                            value = barbers.firstOrNull { it.id == barberId }?.user?.name.orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Selecciona un profesional") },
                            leadingIcon = { Icon(Icons.Default.ContentCut, null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(barberMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                        ExposedDropdownMenu(expanded = barberMenu, onDismissRequest = { barberMenu = false }) {
                            barbers.forEach { barber ->
                                DropdownMenuItem(
                                    text = { Text(barber.user?.name ?: "Barbero") },
                                    onClick = { barberId = barber.id; barberMenu = false }
                                )
                            }
                        }
                    }
                }
            }

            item {
                BookingStep(number = "02", title = "Selecciona el servicio", icon = Icons.Default.AutoFixHigh) {
                    ExposedDropdownMenuBox(expanded = serviceMenu, onExpandedChange = { serviceMenu = !serviceMenu }) {
                        OutlinedTextField(
                            value = services.firstOrNull { it.id == serviceId }?.let { "${it.nombre} · \$${"%.0f".format(it.precio)}" }.orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Corte, barba o servicio") },
                            leadingIcon = { Icon(Icons.Default.Spa, null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(serviceMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                        ExposedDropdownMenu(expanded = serviceMenu, onDismissRequest = { serviceMenu = false }) {
                            services.forEach { s ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(s.nombre)
                                            Text("${s.duracionMin} min · \$${"%.0f".format(s.precio)}", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                                        }
                                    },
                                    onClick = { serviceId = s.id; serviceMenu = false }
                                )
                            }
                        }
                    }
                }
            }

            item {
                BookingStep(number = "03", title = "Fecha y horario", icon = Icons.Default.EventAvailable) {
                    OutlinedTextField(
                        date,
                        { date = it },
                        label = { Text("Fecha") },
                        supportingText = { Text("Formato AAAA-MM-DD") },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(Modifier.height(12.dp))
                    ExposedDropdownMenuBox(expanded = slotMenu, onExpandedChange = { slotMenu = !slotMenu }) {
                        OutlinedTextField(
                            value = slots.firstOrNull { it.time == time }?.label.orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text(if (slots.isEmpty()) "Sin horarios cargados" else "Selecciona una hora") },
                            leadingIcon = { Icon(Icons.Default.Schedule, null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(slotMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        )
                        ExposedDropdownMenu(expanded = slotMenu, onDismissRequest = { slotMenu = false }) {
                            if (slots.isEmpty()) {
                                DropdownMenuItem(text = { Text("Sin horarios disponibles") }, onClick = { slotMenu = false })
                            }
                            slots.forEach { s ->
                                DropdownMenuItem(text = { Text("${s.label}${s.endLabel?.let { " — $it" } ?: ""}") }, onClick = { time = s.time; slotMenu = false })
                            }
                        }
                    }
                }
            }

            item {
                BookingStep(number = "04", title = "Últimos detalles", icon = Icons.Default.EditNote) {
                    OutlinedTextField(
                        notes,
                        { notes = it },
                        label = { Text("Notas (opcional)") },
                        placeholder = { Text("Ej. degradado bajo, barba corta…") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }

            error?.let { item { UrbanErrorBanner(it) } }

            item {
                UrbanPrimaryButton(
                    text = "Confirmar cita",
                    onClick = { vm.create(barberId, serviceId, date, time, notes, onCreated) },
                    enabled = barberId.isNotBlank() && serviceId.isNotBlank() && date.isNotBlank() && time.isNotBlank(),
                    loading = busy,
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun BookingStep(
    number: String,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    UrbanPremiumCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = UrbanColors.Gold,
                contentColor = Color(0xFF080808)
            ) {
                Text(number, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.width(10.dp))
            Icon(icon, null, tint = UrbanColors.Gold, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
}
