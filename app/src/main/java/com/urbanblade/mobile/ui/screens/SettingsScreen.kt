package com.urbanblade.mobile.ui.screens

import com.urbanblade.mobile.ui.components.UrbanSwitch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.UpdateSettingRequest
import com.urbanblade.mobile.ui.components.UrbanCard
import com.urbanblade.mobile.ui.components.UrbanErrorBanner
import com.urbanblade.mobile.ui.components.UrbanFieldLabel
import com.urbanblade.mobile.ui.components.UrbanInfoBanner
import com.urbanblade.mobile.ui.components.UrbanOutlineButton
import com.urbanblade.mobile.ui.components.UrbanPrimaryButton
import com.urbanblade.mobile.ui.components.UrbanSectionTitle
import com.urbanblade.mobile.ui.components.UrbanTextField
import com.urbanblade.mobile.ui.components.UrbanTopBar
import com.urbanblade.mobile.ui.components.UrbanMascotState
import com.urbanblade.mobile.ui.components.UrbanPageHeader
import com.urbanblade.mobile.ui.components.UrbanStatStrip
import com.urbanblade.mobile.ui.components.UrbanStateKind
import com.urbanblade.mobile.ui.components.urbanFilterChipColors
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.SettingsViewModel

/** Configuración del negocio (solo administrador). */
@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val setting = state.setting
    var draft by remember { mutableStateOf(SettingsDraft()) }
    var submitted by remember { mutableStateOf(false) }
    var confirmMaintenance by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.load() }
    // Al cargar (o tras guardar) el formulario se llena con lo que tiene el servidor.
    LaunchedEffect(setting) {
        draft = SettingsDraft(
            nombre = setting.nombre,
            direccion = setting.direccion.orEmpty(),
            telefono = setting.telefono.orEmpty(),
            apertura = setting.horarioApertura.orEmpty().take(5),
            cierre = setting.horarioCierre.orEmpty().take(5),
            politica = setting.politicaCancelacion.toString(),
            instagram = setting.redesSociales.instagram.orEmpty(),
            facebook = setting.redesSociales.facebook.orEmpty(),
            tiktok = setting.redesSociales.tiktok.orEmpty(),
            clabe = setting.datosBancarios.clabe.orEmpty(),
            banco = setting.datosBancarios.banco.orEmpty(),
            beneficiario = setting.datosBancarios.beneficiario.orEmpty(),
            concepto = setting.datosBancarios.concepto.orEmpty()
        )
    }
    val errors = validateSettings(draft)
    fun err(field: String) = if (submitted) errors[field] else null

    Scaffold(containerColor = Color.Transparent, topBar = { UrbanTopBar("", onBack) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { UrbanPageHeader(title = "Configuración", subtitle = "Datos del negocio, horario, datos bancarios y modo mantenimiento.", eyebrow = "Sistema") }
            if (state.loading || state.saving) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = UrbanColors.Gold) }
            state.error?.let { item { UrbanErrorBanner(it) } }
            state.notice?.let { item { UrbanInfoBanner(it, Icons.Default.CheckCircle) } }

            item {
                UrbanCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Modo mantenimiento", style = MaterialTheme.typography.titleMedium, color = UrbanColors.Ink)
                            Text(
                                if (setting.maintenanceMode) "El sistema está fuera de línea para todos" else "El sistema está en línea",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (setting.maintenanceMode) UrbanColors.Danger else UrbanColors.Muted
                            )
                        }
                        UrbanSwitch(
                            checked = setting.maintenanceMode,
                            onCheckedChange = { confirmMaintenance = true },
                            tone = UrbanColors.Danger
                        )
                    }
                }
            }

            item { UrbanSectionTitle("Identidad y contacto") }
            item {
                UrbanCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        UrbanTextField(draft.nombre, { draft = draft.copy(nombre = it) }, "Nombre del negocio", Modifier.fillMaxWidth(),
                            error = err(SettingsField.NOMBRE), capitalization = KeyboardCapitalization.Words)
                        UrbanTextField(draft.direccion, { draft = draft.copy(direccion = it) }, "Dirección", Modifier.fillMaxWidth(),
                            error = err(SettingsField.DIRECCION), capitalization = KeyboardCapitalization.Sentences)
                        UrbanTextField(draft.telefono, { draft = draft.copy(telefono = it) }, "Teléfono", Modifier.fillMaxWidth(),
                            error = err(SettingsField.TELEFONO), keyboardType = KeyboardType.Phone)
                    }
                }
            }

            item { UrbanSectionTitle("Horario y reservas") }
            item {
                UrbanCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TimeField("Apertura", draft.apertura, err(SettingsField.APERTURA), Modifier.weight(1f)) { draft = draft.copy(apertura = it) }
                            TimeField("Cierre", draft.cierre, err(SettingsField.CIERRE), Modifier.weight(1f)) { draft = draft.copy(cierre = it) }
                        }
                        UrbanTextField(
                            draft.politica, { draft = draft.copy(politica = it.filter(Char::isDigit)) }, "Política de cancelación (horas)", Modifier.fillMaxWidth(),
                            error = err(SettingsField.POLITICA), keyboardType = KeyboardType.Number,
                            helper = "Horas de anticipación con las que un cliente puede cancelar (1 a 168)."
                        )
                    }
                }
            }

            item { UrbanSectionTitle("Redes sociales") }
            item {
                UrbanCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        UrbanTextField(draft.instagram, { draft = draft.copy(instagram = it) }, "Instagram", Modifier.fillMaxWidth(), error = err(SettingsField.INSTAGRAM))
                        UrbanTextField(draft.facebook, { draft = draft.copy(facebook = it) }, "Facebook", Modifier.fillMaxWidth(), error = err(SettingsField.FACEBOOK))
                        UrbanTextField(draft.tiktok, { draft = draft.copy(tiktok = it) }, "TikTok", Modifier.fillMaxWidth(), error = err(SettingsField.TIKTOK))
                    }
                }
            }

            item { UrbanSectionTitle("Datos para transferencias", "Los ven los clientes que pagan por transferencia") }
            item {
                UrbanCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        UrbanTextField(
                            draft.clabe, { draft = draft.copy(clabe = it.filter(Char::isDigit).take(18)) }, "CLABE", Modifier.fillMaxWidth(),
                            error = err(SettingsField.CLABE), keyboardType = KeyboardType.Number, helper = "${draft.clabe.length}/18 dígitos"
                        )
                        UrbanTextField(draft.banco, { draft = draft.copy(banco = it) }, "Banco", Modifier.fillMaxWidth(),
                            error = err(SettingsField.BANCO), capitalization = KeyboardCapitalization.Words)
                        UrbanTextField(draft.beneficiario, { draft = draft.copy(beneficiario = it) }, "Beneficiario", Modifier.fillMaxWidth(),
                            error = err(SettingsField.BENEFICIARIO), capitalization = KeyboardCapitalization.Words)
                        UrbanTextField(draft.concepto, { draft = draft.copy(concepto = it) }, "Concepto", Modifier.fillMaxWidth(),
                            error = err(SettingsField.CONCEPTO), imeAction = ImeAction.Done)
                    }
                }
            }

            item {
                UrbanPrimaryButton(
                    text = "Guardar configuración",
                    onClick = {
                        submitted = true
                        if (errors.isEmpty()) {
                            vm.clearMessages()
                            vm.save(draft.toRequest())
                        }
                    },
                    loading = state.saving,
                    icon = Icons.Default.Save,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (confirmMaintenance) {
        AlertDialog(
            onDismissRequest = { confirmMaintenance = false },
            containerColor = UrbanColors.Card,
            title = { Text(if (setting.maintenanceMode) "¿Reactivar el sistema?" else "¿Activar modo mantenimiento?") },
            text = {
                Text(
                    if (setting.maintenanceMode) "El sistema volverá a estar disponible para todos los usuarios."
                    else "Nadie podrá usar el sistema hasta que lo reactives. Úsalo solo para mantenimiento real.",
                    color = UrbanColors.Muted
                )
            },
            confirmButton = { TextButton(onClick = { confirmMaintenance = false; vm.toggleMaintenance() }) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = { confirmMaintenance = false }) { Text("Cancelar") } }
        )
    }
}

/** Convierte el formulario en la petición; los campos vacíos se mandan como ausentes. */
private fun SettingsDraft.toRequest() = UpdateSettingRequest(
    nombre = nombre.trim(),
    direccion = direccion.trim().ifEmpty { null },
    telefono = telefono.trim().ifEmpty { null },
    horarioApertura = apertura.trim().ifEmpty { null },
    horarioCierre = cierre.trim().ifEmpty { null },
    politicaCancelacion = politica.trim().toInt(),
    // El umbral y el porcentaje del depósito por inasistencias no se editan aquí: null los conserva.
    depositoNoShowUmbral = null,
    depositoNoShowPorcentaje = null,
    instagram = instagram.trim().ifEmpty { null },
    facebook = facebook.trim().ifEmpty { null },
    tiktok = tiktok.trim().ifEmpty { null },
    clabe = clabe.trim().ifEmpty { null },
    banco = banco.trim().ifEmpty { null },
    beneficiario = beneficiario.trim().ifEmpty { null },
    concepto = concepto.trim().ifEmpty { null }
)

/** Campo de hora con selector; se puede dejar vacío (sin horario definido). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(label: String, value: String, error: String?, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    Column(modifier) {
        UrbanFieldLabel(label)
        Spacer(Modifier.height(6.dp))
        UrbanOutlineButton(
            text = value.ifEmpty { "Sin definir" },
            onClick = { picking = true },
            icon = Icons.Default.Schedule,
            modifier = Modifier.fillMaxWidth()
        )
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Danger) }
    }
    if (picking) {
        val initial = value.takeIf { it.length == 5 && it[2] == ':' }
        val timeState = rememberTimePickerState(
            initialHour = initial?.substring(0, 2)?.toIntOrNull() ?: 9,
            initialMinute = initial?.substring(3, 5)?.toIntOrNull() ?: 0,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { picking = false },
            containerColor = UrbanColors.Card,
            title = { Text(label) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    onChange("%02d:%02d".format(timeState.hour, timeState.minute))
                    picking = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                Row {
                    if (value.isNotEmpty()) TextButton(onClick = { onChange(""); picking = false }) { Text("Quitar") }
                    TextButton(onClick = { picking = false }) { Text("Cancelar") }
                }
            }
        )
    }
}
