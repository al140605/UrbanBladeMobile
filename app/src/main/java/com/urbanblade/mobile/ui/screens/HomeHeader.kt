package com.urbanblade.mobile.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.ui.components.UrbanHomeHeader
import com.urbanblade.mobile.ui.viewmodel.NotificationsViewModel

/**
 * Encabezado del Inicio de administración, recepción, barbero e ingeniero: saludo, actualizar, la campana
 * con los avisos sin leer y la foto, que abre Cuenta. El cliente usa los mismos componentes en su propio inicio.
 */
@Composable
fun HomeHeader(
    roleLabel: String,
    user: AuthUser,
    refreshing: Boolean,
    onNavigate: (String) -> Unit,
    notificationsVm: NotificationsViewModel = viewModel(),
    onRefresh: () -> Unit
) {
    val data by notificationsVm.data.collectAsState()
    LaunchedEffect(Unit) { notificationsVm.load() }
    UrbanHomeHeader(
        roleLabel = roleLabel,
        userName = user.name,
        avatarUrl = user.avatarUrl,
        refreshing = refreshing,
        unread = parseNotifications(data).unread,
        onNotifications = { onNavigate("notifications") },
        onAccount = { onNavigate("profile") },
        onRefresh = {
            notificationsVm.load()
            onRefresh()
        }
    )
}
