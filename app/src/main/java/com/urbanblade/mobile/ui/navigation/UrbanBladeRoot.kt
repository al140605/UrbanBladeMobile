package com.urbanblade.mobile.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.urbanblade.mobile.core.booking.PendingBooking
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.ui.components.UrbanBladeBackground
import com.urbanblade.mobile.ui.components.UrbanBrandMark
import com.urbanblade.mobile.ui.screens.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.AuthState
import com.urbanblade.mobile.ui.viewmodel.AuthViewModel

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

private const val BOOKING_ROUTE_PATTERN = "booking?serviceId={serviceId}&barberId={barberId}"

private fun bookingRoute(serviceId: String?, barberId: String?): String {
    val params = buildList {
        serviceId?.let { add("serviceId=$it") }
        barberId?.let { add("barberId=$it") }
    }
    return if (params.isEmpty()) "booking" else "booking?" + params.joinToString("&")
}

@Composable
fun UrbanBladeRoot(authViewModel: AuthViewModel = viewModel()) {
    val authState by authViewModel.state.collectAsState()
    when (val state = authState) {
        AuthState.Loading -> UrbanBladeBackground {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                UrbanBrandMark()
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator(color = UrbanColors.Gold, strokeWidth = 2.dp)
                Spacer(Modifier.height(12.dp))
                Text("Preparando tu espacio…", color = UrbanColors.Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        AuthState.Guest -> GuestNav(authViewModel)
        is AuthState.Authenticated -> AuthenticatedNav(state.user, authViewModel)
    }
}

@Composable
private fun GuestNav(authViewModel: AuthViewModel) {
    val nav = rememberNavController()
    // Sin Scaffold/fondo propio (AuthenticatedNav sí lo tiene), el catálogo de invitado salía sobre gris
    // y con texto negro sin color explícito (nombres de servicio ilegibles): se le da fondo y color base.
    UrbanBladeBackground {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides UrbanColors.Ink
        ) {
    NavHost(navController = nav, startDestination = "welcome") {
        composable("welcome") {
            com.urbanblade.mobile.ui.screens.WelcomeScreen(
                onRegister = { nav.navigate("register") },
                onLogin = { nav.navigate("login") },
                onExplore = { nav.navigate("catalog") }
            )
        }
        composable("catalog") {
            CatalogScreen(
                isGuest = true,
                onBook = { serviceId, barberId ->
                    // Conserva la selección y exige login/registro para
                    // confirmar -- BookingScreen la recoge de PendingBooking
                    // en cuanto AuthenticatedNav monta (ver más abajo).
                    PendingBooking.set(serviceId, barberId)
                    nav.navigate("login")
                },
                onLogin = { nav.navigate("login") }
            )
        }
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onRegister = { nav.navigate("register") },
                onForgot = { nav.navigate("forgot") },
                onBack = { nav.popBackStack() }
            )
        }
        composable("register") {
            RegisterScreen(
                authViewModel = authViewModel,
                onBack = { nav.popBackStack() },
                onLogin = { nav.navigate("login") { popUpTo("register") { inclusive = true } } }
            )
        }
        composable("forgot") { ForgotPasswordScreen(authViewModel = authViewModel, onBack = { nav.popBackStack() }) }
    }
        }
    }
}

@Composable
private fun AuthenticatedNav(user: AuthUser, authViewModel: AuthViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val isClient = user.roles.contains("cliente")
    val engineerOnly = user.roles.contains("ingeniero") &&
        user.roles.none { it in listOf("administrador", "recepcionista", "barbero", "cliente") }

    // Nav por rol: el cliente tiene su propia barra (Inicio/Explorar/Mis
    // citas/Wallet/Perfil); el resto de roles conserva la barra de siempre
    // (Inicio/Citas/Más/Perfil) sin cambios.
    val items = if (isClient) {
        listOf(
            NavItem("home", "Inicio", Icons.Default.Home),
            NavItem("catalog", "Explorar", Icons.Default.Explore),
            NavItem("appointments", "Mis citas", Icons.Default.CalendarMonth),
            NavItem("wallet", "Wallet", Icons.Default.AccountBalanceWallet),
            NavItem("profile", "Perfil", Icons.Default.Person)
        )
    } else {
        buildList {
            add(NavItem("home", "Inicio", Icons.Default.Home))
            if (!engineerOnly) add(NavItem("appointments", "Citas", Icons.Default.CalendarMonth))
            add(NavItem("more", "Más", Icons.Default.GridView))
            add(NavItem("profile", "Perfil", Icons.Default.Person))
        }
    }
    val rootRoutes = items.map { it.route }

    // Selección hecha como invitado en el catálogo (ver GuestNav): se
    // consume UNA sola vez, al montar la sesión autenticada, y redirige
    // directo al wizard de reserva con esa preselección.
    LaunchedEffect(Unit) {
        val (pendingService, pendingBarber) = PendingBooking.consume()
        if (pendingService != null || pendingBarber != null) {
            nav.navigate(bookingRoute(pendingService, pendingBarber))
        }
    }

    Scaffold(
        containerColor = UrbanColors.Background,
        bottomBar = {
            if (current in rootRoutes) {
                Surface(
                    color = UrbanColors.Surface,
                    shadowElevation = 18.dp,
                    tonalElevation = 0.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
                        items.forEach { item ->
                            NavigationBarItem(
                                selected = current == item.route,
                                onClick = {
                                    nav.navigate(item.route) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(item.icon, item.label) },
                                label = { Text(item.label, style = MaterialTheme.typography.labelMedium) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = UrbanColors.OnGold,
                                    selectedTextColor = UrbanColors.Gold,
                                    indicatorColor = UrbanColors.Gold,
                                    unselectedIconColor = UrbanColors.Muted,
                                    unselectedTextColor = UrbanColors.Muted
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        // consumeWindowInsets: el Scaffold externo ya reservó barra de estado y barra inferior; sin
        // marcarlos como consumidos, cada pantalla con su propio Scaffold/TopAppBar los sumaba otra vez
        // (hueco vacío sobre el título en todas las pantallas secundarias).
        UrbanBladeBackground(Modifier.padding(padding).consumeWindowInsets(padding)) {
            NavHost(navController = nav, startDestination = "home", modifier = Modifier.fillMaxSize()) {
                composable("home") {
                    DashboardScreen(
                        user = user,
                        onAppointments = { nav.navigate("appointments") },
                        onBook = { nav.navigate("booking") },
                        onWallet = { nav.navigate("wallet") },
                        onStore = { nav.navigate("store") },
                        onExplore = { nav.navigate("catalog") }
                    )
                }
                composable("appointments") {
                    AppointmentsScreen(user = user, onBook = { nav.navigate("booking") })
                }
                composable(
                    BOOKING_ROUTE_PATTERN,
                    arguments = listOf(
                        navArgument("serviceId") { type = NavType.StringType; nullable = true; defaultValue = null },
                        navArgument("barberId") { type = NavType.StringType; nullable = true; defaultValue = null }
                    )
                ) { entry ->
                    BookingScreen(
                        initialServiceId = entry.arguments?.getString("serviceId"),
                        initialBarberId = entry.arguments?.getString("barberId"),
                        onBack = { nav.popBackStack() },
                        onCreated = {
                            nav.navigate("appointments") {
                                popUpTo(BOOKING_ROUTE_PATTERN) { inclusive = true }
                            }
                        }
                    )
                }
                composable("catalog") {
                    CatalogScreen(
                        onBook = { serviceId, barberId -> nav.navigate(bookingRoute(serviceId, barberId)) },
                        onOpenStore = { nav.navigate("store") }
                    )
                }
                composable("wallet") { WalletScreen(onBack = { nav.popBackStack() }) }
                composable("store") { StoreScreen(user = user, onOrders = { nav.navigate("orders") }) }
                composable("orders") { OrdersScreen(user = user, onBack = { nav.popBackStack() }) }
                composable("payments") { PaymentsScreen(user = user, onBack = { nav.popBackStack() }) }
                composable("notifications") { NotificationsScreen(onBack = { nav.popBackStack() }) }
                composable("chatbot") { ChatbotScreen(onBack = { nav.popBackStack() }) }
                composable("more") { MoreScreen(user = user, onNavigate = { nav.navigate(it) }) }
                composable("profile") { ProfileScreen(user = user, onLogout = { authViewModel.logout() }) }

                composable("analytics") { AnalyticsScreen(onBack = { nav.popBackStack() }) }
                composable("social") { SocialFeedScreen(onBack = { nav.popBackStack() }) }
                composable("clients") {
                    ClientsListScreen(
                        user = user,
                        onClientClick = { id -> nav.navigate("client_detail/$id") },
                        onBack = { nav.popBackStack() }
                    )
                }
                composable(
                    "client_detail/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.StringType })
                ) { backStackEntry ->
                    ClientDetailScreen(
                        user = user,
                        clientId = backStackEntry.arguments?.getString("id").orEmpty(),
                        onBack = { nav.popBackStack() }
                    )
                }
                composable("inventory") { InventoryListScreen(user = user, onBack = { nav.popBackStack() }) }
                composable("cash") { CashCloseScreen(onBack = { nav.popBackStack() }) }
                composable("barber_agenda") { BarberAgendaScreen(onBack = { nav.popBackStack() }) }
                composable("barber_portfolio") { BarberPortfolioScreen(onBack = { nav.popBackStack() }) }
                composable("barber_schedule") { BarberScheduleScreen(onBack = { nav.popBackStack() }) }
                composable("reports") { ReportsScreen(onBack = { nav.popBackStack() }) }
                composable("logs") { LogsScreen(onBack = { nav.popBackStack() }) }
                composable("admin_metrics") { AdminMetricsScreen(onBack = { nav.popBackStack() }) }
                composable("insights") { InsightsScreen(onBack = { nav.popBackStack() }) }
                composable("campaigns") { CampaignsScreen(onBack = { nav.popBackStack() }) }
                composable("raffles") { RafflesScreen(onBack = { nav.popBackStack() }) }
                composable("reviews") { ReviewsScreen(onBack = { nav.popBackStack() }) }
                composable("users") { UsersScreen(onBack = { nav.popBackStack() }) }
                composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
                composable("system") { SystemStatusScreen(onBack = { nav.popBackStack() }) }
            }
        }
    }
}
