package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.urbanblade.mobile.ui.theme.UrbanColors

/**
 * Formulario único de UrbanBlade (prototipo aprobado el 13-nov): una hoja a pantalla completa con
 * cabecera fija, secciones con título y pie fijo con la acción principal. Sirve igual para crear y
 * para editar: solo cambian [title], los datos con los que se abre y [primaryLabel].
 *
 * @param blocker por qué el botón está apagado («Falta: nombre y 1 más»); null si se puede enviar.
 * @param error error del servidor al guardar, mostrado arriba del contenido.
 */
@Composable
fun UrbanFormSheet(
    eyebrow: String,
    title: String,
    primaryLabel: String,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    saving: Boolean = false,
    blocker: String? = null,
    error: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = { if (!saving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(modifier.fillMaxSize(), color = UrbanColors.Background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, enabled = !saving) {
                        Icon(Icons.Default.Close, "Cerrar sin guardar", tint = UrbanColors.Ink)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            eyebrow.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = UrbanColors.Gold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = UrbanColors.Ink,
                            modifier = Modifier.semantics { heading() }
                        )
                    }
                }
                HorizontalDivider(color = UrbanColors.Line)
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    error?.let { UrbanErrorBanner(it) }
                    content()
                }
                HorizontalDivider(color = UrbanColors.Line)
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    UrbanPrimaryButton(
                        text = primaryLabel,
                        onClick = onSubmit,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = blocker == null && !saving,
                        loading = saving
                    )
                    Text(
                        blocker.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = UrbanColors.Muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().height(18.dp)
                    )
                }
            }
        }
    }
}

/** Bloque del formulario: título en mayúsculas con su ícono y los campos separados por igual. */
@Composable
fun UrbanFormSection(title: String, modifier: Modifier = Modifier, icon: ImageVector? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            icon?.let { Icon(it, null, tint = UrbanColors.Gold, modifier = Modifier.size(16.dp)) }
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = UrbanColors.Muted,
                modifier = Modifier.semantics { heading() }
            )
        }
        content()
    }
}

/** Dos campos lado a lado con el mismo ancho (precio y duración, compra y venta). */
@Composable
fun UrbanFormRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) { content() }
}

/** Elegir una opción entre varias con botones (categoría, rol, canal). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> UrbanFormChoice(
    label: String,
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 4.dp)) {
            icon?.let { Icon(it, null, tint = UrbanColors.Gold, modifier = Modifier.size(16.dp)) }
            Text(label, style = MaterialTheme.typography.labelMedium, color = UrbanColors.Muted)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(optionLabel(option)) },
                    colors = urbanFilterChipColors()
                )
            }
        }
    }
}

/** Interruptor activo/inactivo dentro de una tarjeta con su explicación. */
@Composable
fun UrbanFormSwitch(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, UrbanColors.Line), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let { Icon(it, null, tint = UrbanColors.Gold, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)) }
        UrbanSwitchRow(title, subtitle, checked, onCheckedChange, Modifier.weight(1f))
    }
}

/** Zona aparte al final de «Editar» para eliminar; pide confirmar antes de llamar a [onDelete]. */
@Composable
fun UrbanFormDangerZone(
    noun: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    note: String = "Se quita de la lista y no se puede deshacer."
) {
    var confirming by remember { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, UrbanColors.Danger.copy(alpha = 0.45f)), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Eliminar $noun", style = MaterialTheme.typography.titleSmall, color = UrbanColors.Ink)
        Text(note, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
        OutlinedButton(
            onClick = { confirming = true },
            enabled = enabled,
            border = BorderStroke(1.dp, UrbanColors.Danger),
            modifier = Modifier.height(44.dp)
        ) { Text("Eliminar $noun", color = UrbanColors.Danger) }
    }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("¿Eliminar $noun?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = { TextButton(onClick = { confirming = false; onDelete() }) { Text("Sí, eliminar", color = UrbanColors.Danger) } },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Cancelar") } }
        )
    }
}

/**
 * Qué campos ya se tocaron: el error de un campo solo se muestra después de salir de él, así que
 * nadie ve una pantalla roja al abrir el formulario.
 */
@Stable
class UrbanFormState {
    private var touched by mutableStateOf(emptySet<String>())

    fun touch(key: String) { if (key !in touched) touched = touched + key }

    /** [message] solo si el campo ya se tocó. */
    fun error(key: String, message: String?): String? = if (key in touched) message else null
}

@Composable
fun rememberUrbanFormState(): UrbanFormState = remember { UrbanFormState() }

/** «Falta: nombre», «Falta: nombre y 2 más» o null cuando no falta nada. */
fun formBlockerText(missing: List<String>): String? = when {
    missing.isEmpty() -> null
    missing.size == 1 -> "Falta: ${missing[0].lowercase()}"
    else -> "Falta: ${missing[0].lowercase()} y ${missing.size - 1} más"
}
