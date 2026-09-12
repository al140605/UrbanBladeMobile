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
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.ui.components.UrbanBladeBackground
import com.urbanblade.mobile.ui.components.UrbanBrandMark
import com.urbanblade.mobile.ui.screens.*
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.AuthState
import com.urbanblade.mobile.ui.viewmodel.AuthViewModel

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

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
    NavHost(navController = nav, startDestination = "login") {
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                onRegister = { nav.navigate("register") },
                onForgot = { nav.navigate("forgot") }
            )
        }
        composable("register") { RegisterScreen(authViewModel = authViewModel, onBack = { nav.popBackStack() }) }
        composable("forgot") { ForgotPasswordScreen(authViewModel = authViewModel, onBack = { nav.popBackStack() }) }
    }
}

@Composable
private fun AuthenticatedNav(user: AuthUser, authViewModel: AuthViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val engineerOnly = user.roles.contains("ingeniero") &&
        user.roles.none { it in listOf("administrador", "recepcionista", "barbero", "cliente") }

    val items = buildList {
        add(NavItem("home", "Inicio", Icons.Default.Home))
        if (!engineerOnly) add(NavItem("appointments", "Citas", Icons.Default.CalendarMonth))
        if (user.roles.contains("cliente")) add(NavItem("store", "Tienda", Icons.Default.Storefront))
        add(NavItem("more", "Más", Icons.Default.GridView))
        add(NavItem("profile", "Perfil", Icons.Default.Person))
    }
    val rootRoutes = items.map { it.route }

    Scaffold(
        containerColor = UrbanColors.Background,
        bottomBar = {
            if (current in rootRoutes) {
                Surface(
                    color = Color(0xFF0E0E0E),
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
                                    selectedIconColor = Color(0xFF090909),
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
        UrbanBladeBackground(Modifier.padding(padding)) {
            NavHost(navController = nav, startDestination = "home", modifier = Modifier.fillMaxSize()) {
                composable("home") {
                    DashboardScreen(
                        user = user,
                        onAppointments = { nav.navigate("appointments") },
                        onBook = { nav.navigate("booking") }
                    )
                }
                composable("appointments") {
                    AppointmentsScreen(user = user, onBook = { nav.navigate("booking") })
                }
                composable("booking") {
                    BookingScreen(
                        onBack = { nav.popBackStack() },
                        onCreated = {
                            nav.navigate("appointments") {
                                popUpTo("booking") { inclusive = true }
                            }
                        }
                    )
                }
                composable("catalog") { CatalogScreen(onBook = { nav.navigate("booking") }) }
                composable("store") { StoreScreen(user = user, onOrders = { nav.navigate("orders") }) }
                composable("orders") { OrdersScreen(user = user, onBack = { nav.popBackStack() }) }
                composable("payments") { PaymentsScreen(user = user, onBack = { nav.popBackStack() }) }
                composable("notifications") { NotificationsScreen(onBack = { nav.popBackStack() }) }
                composable("chatbot") { ChatbotScreen(onBack = { nav.popBackStack() }) }
                composable("more") { MoreScreen(user = user, onNavigate = { nav.navigate(it) }) }
                composable("profile") { ProfileScreen(user = user, onLogout = { authViewModel.logout() }) }

                composable("analytics") { AnalyticsScreen(onBack = { nav.popBackStack() }) }
                module("social", "Muro social", "social/feed", nav)
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
                module("insights", "Insights IA", "admin/predictions/insights", nav)
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

private fun androidx.navigation.NavGraphBuilder.module(
    route: String,
    title: String,
    endpoint: String,
    nav: androidx.navigation.NavHostController
) {
    composable(route) {
        GenericModuleScreen(title = title, endpoint = endpoint, onBack = { nav.popBackStack() })
    }
}
