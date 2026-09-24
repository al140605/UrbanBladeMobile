package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.urbanblade.mobile.data.model.BarberItem
import com.urbanblade.mobile.data.model.ServiceItem
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.CatalogViewModel

@Composable
fun CatalogScreen(
    isGuest: Boolean = false,
    onBook: (serviceId: String?, barberId: String?) -> Unit,
    onOpenStore: (() -> Unit)? = null,
    onOpenInspiration: (() -> Unit)? = null,
    onLogin: (() -> Unit)? = null,
    vm: CatalogViewModel = viewModel()
) {
    val services by vm.services.collectAsState()
    val barbers by vm.barbers.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    LaunchedEffect(Unit) { vm.load() }

    LazyColumn(
        // Como invitado no hay Scaffold que reserve la barra de estado (AuthenticatedNav sí),
        // así que el contenido quedaba debajo del reloj y la barra de gestos.
        Modifier.fillMaxSize().then(if (isGuest) Modifier.systemBarsPadding() else Modifier),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            UrbanPageHeader(
                title = "Explora UrbanBlade",
                subtitle = "Servicios precisos. Barberos con estilo propio.",
                eyebrow = "Catálogo"
            )
        }
        if (isGuest) {
            item {
                UrbanInfoBanner(
                    "Explora como invitado. Para confirmar una reserva, te pediremos iniciar sesión o crear tu cuenta.",
                    Icons.Default.Info
                )
            }
            if (onLogin != null) {
                item {
                    UrbanOutlineButton(
                        text = "Ya tengo cuenta, iniciar sesión",
                        onClick = onLogin,
                        icon = Icons.Default.Login,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        item {
            UrbanHeroCard {
                UrbanHeroLabel("¿Ya sabes qué quieres?")
                Spacer(Modifier.height(6.dp))
                Text("Reserva en menos de un minuto", style = MaterialTheme.typography.headlineSmall, color = UrbanColors.Ink)
                Text(
                    "${UrbanFormat.count(services.size, "servicio", "servicios")} · ${UrbanFormat.count(barbers.size, "barbero", "barberos")} · horarios reales",
                    style = MaterialTheme.typography.bodySmall,
                    color = UrbanColors.Muted
                )
                Spacer(Modifier.height(16.dp))
                UrbanPrimaryButton(
                    text = "Reservar ahora",
                    onClick = { onBook(null, null) },
                    icon = Icons.Default.CalendarMonth,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (!isGuest && onOpenInspiration != null) {
            item {
                UrbanAttentionRow(
                    Icons.Default.Groups,
                    "Muro de Inspiración",
                    "¿No sabes qué corte hacerte? Mira los últimos trabajos del equipo.",
                    UrbanColors.Gold,
                    onClick = onOpenInspiration
                )
            }
        }
        if (!isGuest && onOpenStore != null) {
            item {
                UrbanOutlineButton(
                    text = "Ver tienda de productos",
                    onClick = onOpenStore,
                    icon = Icons.Default.Storefront,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (loading && services.isEmpty()) item { UrbanSkeletonList(3) }
        if (error != null && services.isEmpty() && !loading) {
            item { UrbanMascotState(UrbanStateKind.ERROR, "No pudimos cargar el catálogo", error, "Reintentar") { vm.load() } }
        } else {
            error?.let { item { UrbanErrorBanner(it) } }
        }

        item { UrbanSectionTitle("Servicios", "Elige el acabado que va contigo") }
        items(services, key = { it.id }) { service -> ServiceCard(service) { onBook(service.id, null) } }
        if (!loading && error == null && services.isEmpty()) item { UrbanMascotState(UrbanStateKind.EMPTY, "Sin servicios disponibles", "Vuelve a intentarlo más tarde.") }

        item { UrbanSectionTitle("Nuestro equipo", "Conoce a los profesionales de UrbanBlade") }
        items(barbers, key = { it.id }) { barber -> BarberCard(barber) { onBook(null, barber.id) } }
        if (!loading && barbers.isEmpty()) item { UrbanEmptyState("Sin barberos disponibles", null, Icons.Default.Groups) }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ServiceCard(service: ServiceItem, onBook: () -> Unit) {
    UrbanPremiumCard(Modifier.fillMaxWidth(), onClick = onBook) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = UrbanColors.Gold.copy(alpha = 0.09f),
                modifier = Modifier.size(54.dp),
                border = BorderStroke(1.dp, UrbanColors.Gold.copy(alpha = 0.21f))
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(serviceIcon(service.nombre), null, tint = UrbanColors.Gold)
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(service.nombre, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(3.dp))
                Text("${service.duracionMin} min", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                service.descripcion?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("\$${"%.0f".format(service.precio)}", style = MaterialTheme.typography.titleLarge, color = UrbanColors.Gold)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Reservar", style = MaterialTheme.typography.labelLarge, color = UrbanColors.Gold)
                    Icon(Icons.Default.ChevronRight, null, tint = UrbanColors.Gold, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun BarberCard(barber: BarberItem, onBook: () -> Unit) {
    UrbanCard(Modifier.fillMaxWidth(), onClick = onBook) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!barber.foto.isNullOrBlank()) {
                AsyncImage(
                    model = barber.foto,
                    contentDescription = barber.user?.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(58.dp).clip(CircleShape)
                )
            } else {
                UrbanAvatar(barber.user?.name ?: "Barbero", Modifier.size(58.dp), imageUrl = barber.foto)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(barber.user?.name ?: "Barbero", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                barber.especialidades?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = UrbanColors.Gold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                barber.descripcion?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CalendarMonth, null, tint = UrbanColors.Gold, modifier = Modifier.size(20.dp))
                Text("Reservar", style = MaterialTheme.typography.labelMedium, color = UrbanColors.Gold)
            }
        }
    }
}
