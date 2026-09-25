package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.ui.theme.UrbanColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Campo de fecha con calendario, con la misma etiqueta arriba que [UrbanTextField]. Recibe y
 * entrega la fecha en ISO ("AAAA-MM-DD"), que es lo que espera la API, y la muestra legible.
 * [onlyPast] limita el calendario a días anteriores a hoy (fecha de nacimiento).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrbanDateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "Elegir fecha",
    onlyPast: Boolean = false,
    clearable: Boolean = true
) {
    var open by remember { mutableStateOf(false) }
    val selected = runCatching { LocalDate.parse(value.take(10)) }.getOrNull()

    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted, modifier = Modifier.padding(start = 4.dp))
        Spacer(Modifier.height(6.dp))
        Surface(
            onClick = { open = true },
            shape = MaterialTheme.shapes.medium,
            color = UrbanColors.Card,
            border = BorderStroke(1.dp, UrbanColors.Line),
            modifier = Modifier.fillMaxWidth().semantics { role = Role.Button }
        ) {
            Row(Modifier.padding(start = 14.dp, end = 4.dp).height(54.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Event, null, tint = UrbanColors.Gold)
                Spacer(Modifier.width(12.dp))
                Text(
                    selected?.let { UrbanFormat.dateLong(it.toString()) } ?: placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected != null) UrbanColors.Ink else UrbanColors.Muted,
                    modifier = Modifier.weight(1f)
                )
                if (clearable && selected != null) {
                    IconButton(onClick = { onValueChange("") }) { Icon(Icons.Default.Close, "Quitar fecha", tint = UrbanColors.Muted) }
                }
            }
        }
    }

    if (open) {
        val todayMillis = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = selected?.atStartOfDay()?.toInstant(ZoneOffset.UTC)?.toEpochMilli(),
            initialDisplayedMonthMillis = (selected ?: LocalDate.now().minusYears(if (onlyPast) 25 else 0))
                .atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            yearRange = if (onlyPast) 1920..LocalDate.now().year else LocalDate.now().year..(LocalDate.now().year + 1),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = !onlyPast || utcTimeMillis < todayMillis
            }
        )
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let {
                            onValueChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString())
                        }
                        open = false
                    },
                    enabled = state.selectedDateMillis != null
                ) { Text("Listo", color = if (state.selectedDateMillis != null) UrbanColors.Gold else UrbanColors.Muted) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancelar", color = UrbanColors.Muted) } }
        ) {
            DatePicker(
                state = state,
                showModeToggle = true,
                title = { Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)) }
            )
        }
    }
}
