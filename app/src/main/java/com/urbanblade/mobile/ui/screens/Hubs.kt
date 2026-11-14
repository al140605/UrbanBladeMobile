package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.ui.components.LocalEmbeddedInHub
import com.urbanblade.mobile.ui.components.UrbanPageHeader
import com.urbanblade.mobile.ui.components.UrbanTopBar
import com.urbanblade.mobile.ui.components.urbanFilterChipColors
import com.urbanblade.mobile.ui.theme.UrbanSpace

/**
 * Pestaña de un grupo. [key] es lo que usan las rutas antiguas para abrir el grupo ya en esa pestaña;
 * [content] recibe una función para pasar a otra pestaña por su clave (Inventario a Movimientos).
 */
class HubTab(val key: String, val label: String, val content: @Composable (select: (String) -> Unit) -> Unit)

/**
 * Pantalla de grupo: barra, encabezado y pestañas. Cada pestaña es un módulo que antes era una pantalla
 * suelta; se dibujan incrustados (sin su propia barra ni encabezado). La pestaña elegida se conserva
 * al volver, y volver regresa a Más, no a otra pestaña.
 */
@Composable
fun HubScreen(eyebrow: String, title: String, subtitle: String, tabs: List<HubTab>, initial: String?, onBack: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf(initial ?: tabs.first().key) }
    val current = tabs.firstOrNull { it.key == selected } ?: tabs.first()
    Scaffold(containerColor = Color.Transparent, topBar = { UrbanTopBar("", onBack) }) { padding ->
        // consumeWindowInsets: sin esto el Scaffold de cada pestaña vuelve a sumar el margen de la barra de estado
        // y aparece un hueco vacío sobre el contenido.
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            Column(
                Modifier.fillMaxWidth().padding(start = UrbanSpace.Gutter, end = UrbanSpace.Gutter, top = UrbanSpace.Xxs),
                verticalArrangement = Arrangement.spacedBy(UrbanSpace.Md)
            ) {
                UrbanPageHeader(title = title, subtitle = subtitle, eyebrow = eyebrow, compact = true)
                if (tabs.size > 1) {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tabs.forEach { tab ->
                            FilterChip(
                                selected = tab.key == current.key,
                                onClick = { selected = tab.key },
                                label = { Text(tab.label) },
                                colors = urbanFilterChipColors()
                            )
                        }
                    }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(top = UrbanSpace.Xs)) {
                CompositionLocalProvider(LocalEmbeddedInHub provides true) {
                    current.content { selected = it }
                }
            }
        }
    }
}

private fun AuthUser.has(role: String) = roles.contains(role)
private fun AuthUser.isStaff() = has("administrador") || has("recepcionista")

@Composable
fun AnalysisHub(user: AuthUser, initial: String?, onBack: () -> Unit) {
    val power = user.has("administrador") || user.has("ingeniero")
    val tabs = buildList {
        if (power) add(HubTab("metricas", "Métricas") { AdminMetricsScreen(onBack = {}) })
        add(HubTab("actividad", "Actividad") { AnalyticsScreen(onBack = {}) })
        if (power) {
            add(HubTab("reportes", "Reportes") { ReportsScreen(onBack = {}) })
            add(HubTab("insights", "Insights") { InsightsScreen(onBack = {}) })
            add(HubTab("bitacora", "Bitácora") { LogsScreen(onBack = {}) })
        }
    }
    HubScreen("Análisis", "Análisis", "Cómo va el negocio, en un solo lugar.", tabs, initial, onBack)
}

@Composable
fun CashHub(user: AuthUser, initial: String?, onBack: () -> Unit) {
    val staff = user.isStaff()
    val tabs = buildList {
        add(HubTab("pagos", if (staff) "Cobros" else "Facturas") { PaymentsScreen(user = user, onBack = {}) })
        add(HubTab("pedidos", "Pedidos") { OrdersScreen(user = user, onBack = {}) })
        if (staff) {
            add(HubTab("corte", "Corte de caja") { CashCloseScreen(onBack = {}) })
            add(HubTab("regalo", "Tarjetas regalo") { GiftCardsStaffScreen(onBack = {}) })
        }
    }
    if (staff) HubScreen("Operación", "Caja", "Cobros, pedidos y corte del turno.", tabs, initial, onBack)
    else HubScreen("Mi cuenta", "Mis compras", "Tus facturas y pedidos de la tienda.", tabs, initial, onBack)
}

@Composable
fun ClientsHub(user: AuthUser, initial: String?, onBack: () -> Unit, onClientClick: (String) -> Unit) {
    val tabs = listOf(
        HubTab("clientes", "Clientes") { ClientsListScreen(user = user, onClientClick = onClientClick, onBack = {}) },
        HubTab("espera", "Lista de espera") { WaitlistStaffScreen(onBack = {}) }
    )
    HubScreen("Operación", "Clientes", "Atención y clientes esperando un horario.", tabs, initial, onBack)
}

@Composable
fun InventoryHub(user: AuthUser, initial: String?, onBack: () -> Unit) {
    val tabs = listOf(
        HubTab("stock", "Existencias") { select -> InventoryListScreen(user = user, onBack = {}, onHistory = { select("movimientos") }) },
        HubTab("movimientos", "Movimientos") { InventoryMovementsScreen(onBack = {}) }
    )
    HubScreen("Operación", "Inventario", "Existencias y su historial.", tabs, initial, onBack)
}

@Composable
fun TeamHub(currentUserId: String?, initial: String?, onBack: () -> Unit) {
    val tabs = listOf(
        HubTab("barberos", "Barberos") { BarbersAdminScreen(onBack = {}) },
        HubTab("servicios", "Servicios") { ServicesAdminScreen(onBack = {}) },
        HubTab("usuarios", "Usuarios") { UsersScreen(onBack = {}, currentUserId = currentUserId) }
    )
    HubScreen("Administración", "Equipo y catálogo", "Quién trabaja, qué ofrece y quién entra.", tabs, initial, onBack)
}

@Composable
fun MarketingHub(initial: String?, onBack: () -> Unit) {
    val tabs = listOf(
        HubTab("campanas", "Campañas") { CampaignsScreen(onBack = {}) },
        HubTab("sorteos", "Sorteos") { RafflesScreen(onBack = {}) },
        HubTab("membresias", "Membresías y paquetes") { OffersAdminScreen(onBack = {}) }
    )
    HubScreen("Administración", "Marketing", "Campañas, sorteos y planes.", tabs, initial, onBack)
}

@Composable
fun CommunityHub(user: AuthUser, initial: String?, onBack: () -> Unit) {
    val admin = user.has("administrador")
    val tabs = buildList {
        if (admin) {
            add(HubTab("resenas", "Reseñas") { ReviewsScreen(onBack = {}) })
            add(HubTab("moderacion", "Moderación") { ModerationScreen(onBack = {}) })
        }
        add(HubTab("bloqueados", "Autores bloqueados") { BlockedAuthorsScreen(onBack = {}) })
    }
    HubScreen("Comunidad", if (admin) "Comunidad" else "Autores bloqueados", "Opiniones y orden del muro.", tabs, initial, onBack)
}

@Composable
fun SettingsHub(user: AuthUser, initial: String?, onBack: () -> Unit) {
    val tabs = buildList {
        if (user.has("administrador")) add(HubTab("config", "Configuración") { SettingsScreen(onBack = {}) })
        add(HubTab("sistema", "Estado del sistema") { SystemStatusScreen(onBack = {}) })
    }
    HubScreen("Administración", "Ajustes", "Configuración del negocio y salud del sistema.", tabs, initial, onBack)
}

@Composable
fun MyProfileHub(initial: String?, onBack: () -> Unit) {
    val tabs = listOf(
        HubTab("perfil", "Perfil") { BarberMeScreen(onBack = {}) },
        HubTab("horario", "Horario") { BarberScheduleScreen(onBack = {}) },
        HubTab("portafolio", "Portafolio") { BarberPortfolioScreen(onBack = {}) }
    )
    HubScreen("Barbero", "Mi perfil", "Cómo te ven los clientes y cuándo trabajas.", tabs, initial, onBack)
}

/** Tarjeta de Más: [route] es la ruta de la primera pestaña del grupo. */
data class HubEntry(val route: String, val title: String, val subtitle: String, val icon: ImageVector, val tabs: Int)

/** Los grupos que ve cada rol en Más. Cada uno junta pantallas que antes eran módulos sueltos. */
fun hubEntries(user: AuthUser): List<HubEntry> {
    val admin = user.has("administrador")
    val engineer = user.has("ingeniero")
    val staff = user.isStaff()
    val client = !staff && !engineer && !user.has("barbero")
    return buildList {
        if (admin || engineer) add(HubEntry("admin_metrics", "Análisis", "Métricas, reportes, insights", Icons.Default.QueryStats, 5))
        else add(HubEntry("analytics", "Análisis", "Actividad y rendimiento", Icons.Default.QueryStats, 1))
        if (staff) add(HubEntry("payments", "Caja", "Cobros, pedidos, corte", Icons.Default.PointOfSale, 4))
        else if (client) add(HubEntry("payments", "Mis compras", "Facturas y pedidos", Icons.Default.PointOfSale, 2))
        if (staff) {
            add(HubEntry("clients", "Clientes", "Atención y lista de espera", Icons.Default.People, 2))
            add(HubEntry("inventory", "Inventario", "Existencias y movimientos", Icons.Default.Inventory2, 2))
        }
        if (admin) {
            add(HubEntry("barbers_admin", "Equipo y catálogo", "Barberos, servicios, usuarios", Icons.Default.Badge, 3))
            add(HubEntry("campaigns", "Marketing", "Campañas, sorteos, planes", Icons.Default.Campaign, 3))
            add(HubEntry("reviews", "Comunidad", "Reseñas y moderación", Icons.Default.Chat, 3))
        }
        if (admin) add(HubEntry("settings", "Ajustes", "Configuración y sistema", Icons.Default.Settings, 2))
        else if (engineer) add(HubEntry("system", "Ajustes", "Estado del sistema", Icons.Default.Settings, 1))
        if (user.has("barbero")) add(HubEntry("barber_me", "Mi perfil", "Perfil, horario, portafolio", Icons.Default.Face, 3))
    }
}
