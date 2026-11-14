package com.urbanblade.mobile.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.Alignment
import com.urbanblade.mobile.ui.components.UrbanCard
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.BuildConfig
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.ui.components.UrbanAttentionRow
import com.urbanblade.mobile.ui.components.UrbanActionRow
import com.urbanblade.mobile.ui.components.UrbanRowGroup
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.components.UrbanMascotState
import com.urbanblade.mobile.ui.components.UrbanOutlineButton
import com.urbanblade.mobile.ui.components.UrbanPageHeader
import com.urbanblade.mobile.ui.components.UrbanSectionTitle
import com.urbanblade.mobile.ui.components.UrbanPillTabs
import com.urbanblade.mobile.ui.components.UrbanSkeletonList
import com.urbanblade.mobile.ui.components.UrbanStateKind
import com.urbanblade.mobile.ui.components.rememberSingleImagePicker
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.theme.UrbanSpace
import com.urbanblade.mobile.ui.viewmodel.AccountSection
import com.urbanblade.mobile.ui.viewmodel.AccountViewModel
import kotlinx.coroutines.launch

/** Un acceso rápido de la cabecera de Cuenta: lo que cada rol usa a diario. */
private data class AccountShortcut(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private fun shortcutsFor(roles: List<String>): List<AccountShortcut> = when {
    "administrador" in roles -> listOf(
        AccountShortcut("admin_metrics", "Análisis", Icons.Default.QueryStats),
        AccountShortcut("users", "Usuarios", Icons.Default.ManageAccounts),
        AccountShortcut("settings", "Ajustes", Icons.Default.Settings),
        AccountShortcut("system", "Sistema", Icons.Default.Dns)
    )
    "recepcionista" in roles -> listOf(
        AccountShortcut("payments", "Caja", Icons.Default.PointOfSale),
        AccountShortcut("cash", "Corte", Icons.Default.Receipt),
        AccountShortcut("clients", "Clientes", Icons.Default.People)
    )
    "ingeniero" in roles -> listOf(
        AccountShortcut("system", "Estado", Icons.Default.Dns),
        AccountShortcut("logs", "Bitácora", Icons.Default.Terminal)
    )
    "barbero" in roles -> listOf(
        AccountShortcut("barber_me", "Perfil", Icons.Default.Face),
        AccountShortcut("barber_schedule", "Horario", Icons.Default.Schedule),
        AccountShortcut("barber_portfolio", "Portafolio", Icons.Default.PhotoLibrary),
        AccountShortcut("barber_agenda", "Agenda", Icons.Default.Event)
    )
    else -> listOf(
        AccountShortcut("wallet", "Beneficios", Icons.Default.Stars),
        AccountShortcut("payments", "Facturas", Icons.Default.ReceiptLong),
        AccountShortcut("orders", "Pedidos", Icons.Default.ShoppingBag)
    )
}

@Composable
private fun ShortcutTile(item: AccountShortcut, modifier: Modifier, onClick: () -> Unit) {
    UrbanCard(modifier, onClick = onClick) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(item.icon, null, tint = UrbanColors.Gold)
            Text(item.title, style = MaterialTheme.typography.labelLarge, color = UrbanColors.Ink, maxLines = 1)
        }
    }
}

/**
 * Mi cuenta, la misma hoja para los cinco roles y pensada para verse casi sin desplazarse: tarjeta de
 * miembro, los accesos propios del rol en una fila y cinco renglones. Datos, seguridad, preferencias y
 * legal se abren en una hoja en lugar de ocupar la pantalla; solo cambia lo que el rol tiene.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    user: AuthUser,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit,
    onUserChanged: () -> Unit,
    vm: AccountViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val isClient = com.urbanblade.mobile.core.access.UserExperience.isClient(user.roles)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pickPhoto = rememberSingleImagePicker { uri -> vm.uploadAvatar(context, uri, onUserChanged) }
    val openLegal = com.urbanblade.mobile.ui.components.rememberOpenLegal()

    var sheet by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.load(isClient) }
    LaunchedEffect(sheet) { if (sheet == "security") vm.checkSessionKind() }

    val profile = state.profile
    val notice = state.notice
    val prefsSummary = state.preferences?.summary() ?: "avisos"

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = UrbanSpace.Gutter, end = UrbanSpace.Gutter, top = UrbanSpace.Md, bottom = com.urbanblade.mobile.ui.components.BladebotClearance),
        verticalArrangement = Arrangement.spacedBy(UrbanSpace.Md)
    ) {
        when {
            profile == null && state.loading -> item { UrbanSkeletonList(3) }
            profile == null && state.loadError != null -> item {
                UrbanMascotState(
                    kind = UrbanStateKind.ERROR,
                    title = "No pudimos abrir tu cuenta",
                    subtitle = state.loadError,
                    actionLabel = "Reintentar",
                    onAction = { vm.load(isClient) }
                )
            }
            profile != null -> {
                item {
                    val loyalty = state.loyalty
                    val discount = profile.client?.descuentoActivoPct ?: loyalty?.discountPct ?: 0.0
                    val stats = if (isClient) listOf(
                        Triple("Nivel", loyalty?.nivelLabel ?: "Nuevo", Icons.Default.EmojiEvents),
                        Triple("Puntos", "${loyalty?.puntos ?: 0}", Icons.Default.Stars)
                    ) else listOfNotNull(
                        profile.createdAt?.let { Triple("Miembro desde", UrbanFormat.monthYear(it), Icons.Default.CalendarMonth) },
                        if (profile.emailVerifiedAt != null) Triple("Correo", "Verificado", Icons.Default.MarkEmailRead)
                        else Triple("Correo", "Sin verificar", Icons.Default.MarkEmailUnread)
                    )
                    Column { AccountMemberCard(
                        name = profile.name,
                        email = profile.email,
                        avatarUrl = profile.avatarUrl,
                        roles = profile.roles.ifEmpty { user.roles },
                        uploading = state.uploadingAvatar,
                        onChangePhoto = pickPhoto,
                        stats = stats,
                        footnote = when {
                            isClient && discount > 0 -> "Tu descuento activo: ${discount.toInt()} % en cada servicio."
                            isClient && (loyalty?.citasFaltan ?: 0) > 0 && loyalty?.nextNivelLabel != null ->
                                "Te faltan ${UrbanFormat.count(loyalty.citasFaltan, "cita", "citas")} para ${loyalty.nextNivelLabel}."
                            else -> "Toca tu foto para cambiarla."
                        }
                    )
                    if (notice?.section == AccountSection.AVATAR) AccountNoticeBanner(notice) }
                }

                // Lo que este rol usa a diario, en una sola fila.
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        shortcutsFor(user.roles).forEach { shortcut ->
                            ShortcutTile(shortcut, Modifier.weight(1f)) { onNavigate(shortcut.route) }
                        }
                    }
                }

                item {
                    UrbanRowGroup(listOf("data", "security", "prefs", "legal", "logout")) { key ->
                        when (key) {
                            "data" -> UrbanActionRow(
                                "Datos personales",
                                if (isClient) "Nombre, correo, teléfono y cumpleaños" else "Nombre y correo",
                                Icons.Default.Person
                            ) { vm.dismissNotice(); sheet = "data" }
                            "security" -> UrbanActionRow(
                                "Seguridad",
                                if (profile.emailVerifiedAt != null) "Contraseña · correo verificado" else "Contraseña · correo sin verificar",
                                Icons.Default.Security
                            ) { vm.dismissNotice(); sheet = "security" }
                            "prefs" -> UrbanActionRow(
                                "Preferencias",
                                "Tema " + UrbanColors.current.label + " · " + prefsSummary,
                                Icons.Default.Tune
                            ) { vm.dismissNotice(); sheet = "prefs" }
                            "legal" -> UrbanActionRow("Legal y privacidad", "Aviso de privacidad y términos", Icons.Default.PrivacyTip) { sheet = "legal" }
                            else -> UrbanActionRow("Cerrar sesión", "UrbanBlade " + BuildConfig.VERSION_NAME, Icons.AutoMirrored.Filled.Logout) { onLogout() }
                        }
                    }
                }
            }
        }
    }

    if (sheet.isNotEmpty() && profile != null) {
        ModalBottomSheet(onDismissRequest = { sheet = ""; vm.dismissNotice() }, containerColor = UrbanColors.Surface) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (sheet) {
                    "data" -> {
                        Text("Datos personales", style = MaterialTheme.typography.titleLarge, color = UrbanColors.Ink)
                        PersonalDataForm(
                            profile = profile,
                            isClient = isClient,
                            saving = state.savingData,
                            notice = notice?.takeIf { it.section == AccountSection.DATA },
                            onSave = { name, email, phone, birth, sex -> vm.saveData(name, email, phone, birth, sex, onUserChanged) }
                        )
                    }
                    "security" -> {
                        Text("Seguridad", style = MaterialTheme.typography.titleLarge, color = UrbanColors.Ink)
                        val verified = profile.emailVerifiedAt != null
                        UrbanAttentionRow(
                            icon = if (verified) Icons.Default.VerifiedUser else Icons.Default.MarkEmailUnread,
                            text = if (verified) "Correo verificado" else "Correo sin verificar",
                            subtitle = if (verified) profile.email else "Revisa el enlace que te enviamos a ${profile.email}.",
                            tone = if (verified) UrbanColors.Success else UrbanColors.Warning
                        )
                        Text("Contraseña", style = MaterialTheme.typography.titleMedium, color = UrbanColors.Ink)
                        ChangePasswordForm(
                            saving = state.savingPassword,
                            resetKey = state.passwordChanges,
                            notice = notice?.takeIf { it.section == AccountSection.PASSWORD },
                            onSubmit = vm::changePassword
                        )
                        // Los administradores no pueden borrarse a sí mismos, igual que en la web.
                        if ("administrador" !in user.roles) {
                            Text("Eliminar mi cuenta", style = MaterialTheme.typography.titleMedium, color = UrbanColors.Danger)
                            DeleteAccountForm(
                                viaGoogle = state.sessionViaGoogle,
                                deleting = state.deletingAccount,
                                notice = notice?.takeIf { it.section == AccountSection.DELETE },
                                onDelete = { secret -> vm.deleteAccount(secret, onLogout) }
                            )
                        }
                    }
                    "prefs" -> {
                        Text("Preferencias", style = MaterialTheme.typography.titleLarge, color = UrbanColors.Ink)
                        Text("Apariencia", style = MaterialTheme.typography.titleMedium, color = UrbanColors.Ink)
                        ThemePicker(UrbanColors.current) { theme ->
                            UrbanColors.applyTheme(theme)
                            scope.launch { AppContainer.sessionManager.saveTheme(theme.key) }
                        }
                        Text("Notificaciones", style = MaterialTheme.typography.titleMedium, color = UrbanColors.Ink)
                        val prefs = state.preferences
                        if (prefs != null) {
                            NotificationChannels(prefs, notice?.takeIf { it.section == AccountSection.NOTIFICATIONS }, vm::setPreference)
                            PushTestButton()
                        } else {
                            Text("No se pudieron cargar tus avisos.", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
                            UrbanOutlineButton("Reintentar", onClick = { vm.load(isClient) }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    else -> {
                        Text("Legal y privacidad", style = MaterialTheme.typography.titleLarge, color = UrbanColors.Ink)
                        UrbanRowGroup(listOf("privacy", "terms")) { key ->
                            if (key == "privacy") UrbanActionRow("Aviso de Privacidad", "Cómo cuidamos y usamos tus datos.", Icons.Default.PrivacyTip) {
                                openLegal(com.urbanblade.mobile.ui.components.UrbanLegal.PRIVACY_URL)
                            } else UrbanActionRow("Términos y Condiciones", "Las reglas de uso de UrbanBlade.", Icons.Default.Description) {
                                openLegal(com.urbanblade.mobile.ui.components.UrbanLegal.TERMS_URL)
                            }
                        }
                        Text(
                            "Para ejercer tus derechos sobre tus datos (acceso, corrección o borrado del historial), escribe a ${com.urbanblade.mobile.ui.components.UrbanLegal.CONTACT_EMAIL}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = UrbanColors.Muted
                        )
                    }
                }
            }
        }
    }
}

/**
 * T061 (HT-21): pide al servidor un push de prueba a este mismo teléfono. Primero vuelve a
 * registrar el token de FCM (por si se guardó antes de que el backend distinguiera FCM de Expo).
 */
@Composable
private fun PushTestButton() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var sending by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    UrbanOutlineButton(
        text = if (sending) "Enviando…" else "Probar notificación en este teléfono",
        onClick = {
            if (sending) return@UrbanOutlineButton
            sending = true
            scope.launch {
                result = try {
                    val repo = AppContainer.urbanRepository
                    com.urbanblade.mobile.core.push.PushNotifications.registerCurrentToken(context, repo)
                    repo.sendTestPush().message ?: "Notificación de prueba enviada."
                } catch (e: Exception) {
                    (e as? retrofit2.HttpException)?.let { http ->
                        runCatching { org.json.JSONObject(http.response()?.errorBody()?.string().orEmpty()).optString("message") }
                            .getOrNull()?.takeIf { it.isNotBlank() }
                    } ?: "No se pudo enviar la notificación de prueba."
                } finally {
                    sending = false
                }
            }
        },
        icon = Icons.Default.NotificationsActive,
        modifier = Modifier.fillMaxWidth()
    )
    result?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
    }
}
