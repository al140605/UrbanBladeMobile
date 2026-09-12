package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.UpdateSettingRequest
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = viewModel()) {
    val setting by vm.setting.collectAsState()
    val busy by vm.busy.collectAsState()
    val saving by vm.saving.collectAsState()
    val message by vm.message.collectAsState()
    val error by vm.error.collectAsState()

    var nombre by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var horarioApertura by remember { mutableStateOf("") }
    var horarioCierre by remember { mutableStateOf("") }
    var politicaCancelacion by remember { mutableStateOf("24") }
    var instagram by remember { mutableStateOf("") }
    var facebook by remember { mutableStateOf("") }
    var tiktok by remember { mutableStateOf("") }
    var clabe by remember { mutableStateOf("") }
    var banco by remember { mutableStateOf("") }
    var beneficiario by remember { mutableStateOf("") }
    var concepto by remember { mutableStateOf("") }
    var showMaintenanceConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(setting) {
        nombre = setting.nombre
        direccion = setting.direccion.orEmpty()
        telefono = setting.telefono.orEmpty()
        horarioApertura = setting.horarioApertura.orEmpty()
        horarioCierre = setting.horarioCierre.orEmpty()
        politicaCancelacion = setting.politicaCancelacion.toString()
        instagram = setting.redesSociales.instagram.orEmpty()
        facebook = setting.redesSociales.facebook.orEmpty()
        tiktok = setting.redesSociales.tiktok.orEmpty()
        clabe = setting.datosBancarios.clabe.orEmpty()
        banco = setting.datosBancarios.banco.orEmpty()
        beneficiario = setting.datosBancarios.beneficiario.orEmpty()
        concepto = setting.datosBancarios.concepto.orEmpty()
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = { UrbanTopBar("Configuración", onBack) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            error?.let { item { UrbanErrorBanner(it) } }
            message?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }

            item {
                UrbanCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Modo mantenimiento", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (setting.maintenanceMode) "El sistema está fuera de línea para todos" else "El sistema está en línea",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (setting.maintenanceMode) UrbanColors.Danger else UrbanColors.Muted
                            )
                        }
                        Switch(
                            checked = setting.maintenanceMode,
                            onCheckedChange = { showMaintenanceConfirm = true },
                            colors = SwitchDefaults.colors(checkedTrackColor = UrbanColors.Danger)
                        )
                    }
                }
            }

            item { UrbanSectionTitle("Identidad y contacto") }
            item {
                UrbanCard(Modifier.fillMaxWidth()) {
                    OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre del negocio") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(direccion, { direccion = it }, label = { Text("Dirección") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(telefono, { telefono = it }, label = { Text("Teléfono") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }

            item { UrbanSectionTitle("Horario y política") }
            item {
                UrbanCard(Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(horarioApertura, { horarioApertura = it }, label = { Text("Apertura (HH:mm)") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(horarioCierre, { horarioCierre = it }, label = { Text("Cierre (HH:mm)") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        politicaCancelacion,
                        { politicaCancelacion = it.filter(Char::isDigit) },
                        label = { Text("Política de cancelación (horas)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item { UrbanSectionTitle("Redes sociales") }
            item {
                UrbanCard(Modifier.fillMaxWidth()) {
                    OutlinedTextField(instagram, { instagram = it }, label = { Text("Instagram") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(facebook, { facebook = it }, label = { Text("Facebook") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(tiktok, { tiktok = it }, label = { Text("TikTok") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }

            item { UrbanSectionTitle("Datos bancarios", "Solo visibles para administración") }
            item {
                UrbanCard(Modifier.fillMaxWidth()) {
                    OutlinedTextField(clabe, { clabe = it }, label = { Text("CLABE") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(banco, { banco = it }, label = { Text("Banco") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(beneficiario, { beneficiario = it }, label = { Text("Beneficiario") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(concepto, { concepto = it }, label = { Text("Concepto") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }

            item {
                val politicaValid = politicaCancelacion.toIntOrNull()?.let { it in 1..168 } ?: false
                UrbanPrimaryButton(
                    text = "Guardar configuración",
                    onClick = {
                        vm.save(
                            UpdateSettingRequest(
                                nombre = nombre,
                                direccion = direccion.takeIf { it.isNotBlank() },
                                telefono = telefono.takeIf { it.isNotBlank() },
                                horarioApertura = horarioApertura.takeIf { it.isNotBlank() },
                                horarioCierre = horarioCierre.takeIf { it.isNotBlank() },
                                politicaCancelacion = politicaCancelacion.toIntOrNull() ?: 24,
                                depositoNoShowUmbral = null,
                                depositoNoShowPorcentaje = null,
                                instagram = instagram.takeIf { it.isNotBlank() },
                                facebook = facebook.takeIf { it.isNotBlank() },
                                tiktok = tiktok.takeIf { it.isNotBlank() },
                                clabe = clabe.takeIf { it.isNotBlank() },
                                banco = banco.takeIf { it.isNotBlank() },
                                beneficiario = beneficiario.takeIf { it.isNotBlank() },
                                concepto = concepto.takeIf { it.isNotBlank() }
                            )
                        )
                    },
                    enabled = nombre.isNotBlank() && politicaValid,
                    loading = saving,
                    icon = Icons.Default.Save,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showMaintenanceConfirm) {
        AlertDialog(
            onDismissRequest = { showMaintenanceConfirm = false },
            title = { Text(if (setting.maintenanceMode) "¿Reactivar el sistema?" else "¿Activar modo mantenimiento?") },
            text = {
                Text(
                    if (setting.maintenanceMode) "El sistema volverá a estar disponible para todos los usuarios."
                    else "Nadie podrá usar el sistema hasta que lo reactives. Úsalo solo para mantenimiento real."
                )
            },
            confirmButton = {
                TextButton(onClick = { showMaintenanceConfirm = false; vm.toggleMaintenance() }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { showMaintenanceConfirm = false }) { Text("Cancelar") } }
        )
    }
}
