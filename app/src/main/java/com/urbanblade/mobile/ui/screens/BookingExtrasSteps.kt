package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.background
import com.urbanblade.mobile.core.validation.FormLimits
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.rememberScrollState
import coil.compose.AsyncImage
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.data.model.BarberItem
import com.urbanblade.mobile.data.model.ServiceItem
import com.urbanblade.mobile.data.model.ProductItem
import com.urbanblade.mobile.data.model.SlotItem
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.theme.UrbanColors
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import com.urbanblade.mobile.ui.components.UrbanTextButton


/** Barbero dentro del paso de horario: si solo hay uno, se muestra ya elegido. */
@Composable
internal fun BarberPicker(barbers: List<BarberItem>, selectedId: String, onSelect: (String) -> Unit) {
    if (barbers.isEmpty()) {
        UrbanSkeletonList(1)
        Spacer(Modifier.height(12.dp))
        return
    }
    UrbanFieldLabel("Barbero")
    Spacer(Modifier.height(8.dp))
    if (barbers.size == 1) {
        val only = barbers.first()
        Row(verticalAlignment = Alignment.CenterVertically) {
            UrbanAvatar(only.user?.name ?: "Barbero", Modifier.size(36.dp), imageUrl = only.foto)
            Spacer(Modifier.width(10.dp))
            Text("Con ${only.user?.name ?: "tu barbero"}", style = MaterialTheme.typography.titleSmall, color = UrbanColors.Ink)
        }
    } else {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            barbers.forEach { b ->
                FilterChip(
                    selected = b.id == selectedId,
                    onClick = { onSelect(b.id) },
                    label = { Text(b.user?.name?.substringBefore(' ') ?: "Barbero") },
                    leadingIcon = { UrbanAvatar(b.user?.name ?: "B", Modifier.size(22.dp), imageUrl = b.foto) },
                    colors = bookingChipColors()
                )
            }
        }
    }
    Spacer(Modifier.height(16.dp))
}

/** Paso 3: productos opcionales (se pagan en el salón) y notas para el barbero. */
@Composable
internal fun ExtrasStep(
    products: List<ProductItem>,
    cart: Map<String, Int>,
    onQuantity: (String, Int) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (products.isNotEmpty()) {
            UrbanFieldLabel("Productos (opcional)")
            Text("Te los apartamos para tu visita y los pagas en el salón.", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
            products.forEach { p ->
                val qty = cart[p.id] ?: 0
                UrbanCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!p.imagen.isNullOrBlank()) {
                            AsyncImage(p.imagen, null, contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(UrbanColors.CardAlt))
                        } else {
                            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(UrbanColors.Gold.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ShoppingBag, null, tint = UrbanColors.Gold)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.nombre, style = MaterialTheme.typography.titleSmall, color = UrbanColors.Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${UrbanFormat.moneyShort(p.precioVenta)}", style = MaterialTheme.typography.bodyMedium, color = UrbanColors.GoldText)
                        }
                        if (qty == 0) {
                            UrbanTextButton(onClick = { onQuantity(p.id, 1) }) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp), tint = UrbanColors.Gold)
                                Spacer(Modifier.width(4.dp))
                                Text("Agregar", color = UrbanColors.GoldText)
                            }
                        } else {
                            IconButton(onClick = { onQuantity(p.id, qty - 1) }) { Icon(Icons.Default.Remove, "Quitar uno", tint = UrbanColors.Gold) }
                            Text("$qty", style = MaterialTheme.typography.titleMedium, color = UrbanColors.Ink)
                            IconButton(onClick = { onQuantity(p.id, qty + 1) }, enabled = qty < minOf(p.stockActual, 5)) {
                                Icon(Icons.Default.Add, "Agregar uno", tint = UrbanColors.Gold)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        UrbanTextField(
            value = notes,
            onValueChange = onNotesChange,
            label = "Notas para tu barbero (opcional)",
            placeholder = "Ej. degradado bajo, barba corta…",
            helper = if (FormLimits.nearLimit(notes, FormLimits.NOTES_MAX)) "${notes.length}/${FormLimits.NOTES_MAX}" else null,
            leadingIcon = Icons.Default.EditNote,
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Default,
            minLines = 3
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
internal fun bookingChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = UrbanColors.Gold,
    selectedLabelColor = UrbanColors.OnGold
)

@Composable
internal fun SlotChip(slot: SlotItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) UrbanColors.Gold else UrbanColors.Card,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) UrbanColors.Gold else UrbanColors.Line),
        modifier = modifier.semantics { role = Role.RadioButton; this.selected = selected }
    ) {
        Box(Modifier.padding(vertical = 12.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                slot.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) UrbanColors.OnGold else UrbanColors.Ink
            )
        }
    }
}

@Composable
internal fun ReviewStep(
    service: ServiceItem?,
    barber: BarberItem?,
    date: String,
    time: String,
    extras: List<Pair<ProductItem, Int>>,
    notes: String,
    payment: @Composable ColumnScope.() -> Unit
) {
    // El contenedor del flujo es el único dueño del desplazamiento vertical.
    Column {
        UrbanHeroCard {
            UrbanHeroLabel("Tu cita")
            Spacer(Modifier.height(6.dp))
            Text(time.takeIf { it.isNotBlank() }?.let { UrbanFormat.time(it) } ?: "—", style = MaterialTheme.typography.displaySmall, color = UrbanColors.Ink)
            Text(UrbanFormat.date(date), style = MaterialTheme.typography.titleMedium, color = UrbanColors.GoldText)
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = UrbanColors.Ink.copy(alpha = 0.22f))
            Spacer(Modifier.height(12.dp))
            ReviewRow(serviceIcon(service?.nombre.orEmpty()), service?.nombre ?: "—", service?.let { "${it.duracionMin} min · ${UrbanFormat.moneyShort(it.precio)}" })
            Spacer(Modifier.height(10.dp))
            ReviewRow(Icons.Default.Person, barber?.user?.name ?: "—", "Te confirma la cita desde su agenda")
        }
        if (notes.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            UrbanInfoBanner("Nota para tu barbero: $notes", Icons.Default.EditNote)
        }
        if (extras.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            UrbanCard(Modifier.fillMaxWidth()) {
                Text("Productos de tu visita", style = MaterialTheme.typography.titleSmall, color = UrbanColors.Ink)
                Text("Se pagan en el salón al recogerlos, con su propio comprobante.", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                Spacer(Modifier.height(8.dp))
                extras.forEach { (p, qty) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text("${p.nombre}${if (qty > 1) " ×$qty" else ""}", style = MaterialTheme.typography.bodyMedium, color = UrbanColors.Ink, modifier = Modifier.weight(1f))
                        Text("${UrbanFormat.moneyShort(p.precioVenta * qty)}", style = MaterialTheme.typography.bodyMedium, color = UrbanColors.GoldText)
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        payment()
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ReviewRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = UrbanColors.Gold, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = UrbanColors.Ink)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted) }
        }
    }
}
