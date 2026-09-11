package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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

    Scaffold(topBar = {
        TopAppBar(title = { Text("Reservar cita") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Text("Elige un horario realmente disponible", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("La app consulta /availability/slots antes de enviar la reserva.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                ExposedDropdownMenuBox(expanded = barberMenu, onExpandedChange = { barberMenu = !barberMenu }) {
                    OutlinedTextField(
                        value = barbers.firstOrNull { it.id == barberId }?.user?.name.orEmpty(), onValueChange = {}, readOnly = true,
                        label = { Text("Barbero") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(barberMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = barberMenu, onDismissRequest = { barberMenu = false }) {
                        barbers.forEach { barber -> DropdownMenuItem(text = { Text(barber.user?.name ?: "Barbero") }, onClick = { barberId = barber.id; barberMenu = false }) }
                    }
                }
            }
            item {
                ExposedDropdownMenuBox(expanded = serviceMenu, onExpandedChange = { serviceMenu = !serviceMenu }) {
                    OutlinedTextField(
                        value = services.firstOrNull { it.id == serviceId }?.let { "${it.nombre} · \$${"%.0f".format(it.precio)}" }.orEmpty(), onValueChange = {}, readOnly = true,
                        label = { Text("Servicio") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(serviceMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = serviceMenu, onDismissRequest = { serviceMenu = false }) {
                        services.forEach { s -> DropdownMenuItem(text = { Text("${s.nombre} · ${s.duracionMin} min") }, onClick = { serviceId = s.id; serviceMenu = false }) }
                    }
                }
            }
            item { OutlinedTextField(date, { date = it }, label = { Text("Fecha (AAAA-MM-DD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            item {
                ExposedDropdownMenuBox(expanded = slotMenu, onExpandedChange = { slotMenu = !slotMenu }) {
                    OutlinedTextField(
                        value = slots.firstOrNull { it.time == time }?.label.orEmpty(), onValueChange = {}, readOnly = true,
                        label = { Text(if (slots.isEmpty()) "Horario" else "Horario disponible") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(slotMenu) }, modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = slotMenu, onDismissRequest = { slotMenu = false }) {
                        if (slots.isEmpty()) DropdownMenuItem(text = { Text("Sin horarios disponibles") }, onClick = { slotMenu = false })
                        slots.forEach { s -> DropdownMenuItem(text = { Text(s.label) }, onClick = { time = s.time; slotMenu = false }) }
                    }
                }
            }
            item { OutlinedTextField(notes, { notes = it }, label = { Text("Notas (opcional)") }, modifier = Modifier.fillMaxWidth(), minLines = 3) }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            item {
                Button(
                    onClick = { vm.create(barberId, serviceId, date, time, notes, onCreated) },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (busy) "Reservando…" else "Confirmar cita") }
            }
        }
    }
}
