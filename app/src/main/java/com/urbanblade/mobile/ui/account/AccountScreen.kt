package com.urbanblade.mobile.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urbanblade.mobile.BuildConfig
import com.urbanblade.mobile.core.network.AppContainer
import com.urbanblade.mobile.data.model.AuthUser
import com.urbanblade.mobile.ui.components.UrbanAttentionRow
import com.urbanblade.mobile.ui.components.UrbanFormat
import com.urbanblade.mobile.ui.components.UrbanMascotState
import com.urbanblade.mobile.ui.components.UrbanOutlineButton
import com.urbanblade.mobile.ui.components.UrbanPageHeader
import com.urbanblade.mobile.ui.components.UrbanSkeletonList
import com.urbanblade.mobile.ui.components.UrbanStateKind
import com.urbanblade.mobile.ui.components.rememberSingleImagePicker
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.viewmodel.AccountSection
import com.urbanblade.mobile.ui.viewmodel.AccountViewModel
import kotlinx.coroutines.launch

private enum class AccountTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DATA("Datos", Icons.Default.Person),
    SECURITY("Seguridad", Icons.Default.Security),
    PREFERENCES("Ajustes", Icons.Default.Tune)
}

/**
 * Mi cuenta, la misma para los cinco roles: tarjeta de miembro arriba, tres pestañas (Datos,
 * Seguridad, Ajustes) y en cada una sus secciones plegables. Solo cambia lo que el rol tiene:
 * el cliente ve su nivel y puntos, teléfono y cumpleaños, y el acceso a Bladebot.
 */
@Composable
fun AccountScreen(
    user: AuthUser,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit,
    onUserChanged: () -> Unit,
    vm: AccountViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val isClient = "cliente" in user.roles
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pickPhoto = rememberSingleImagePicker { uri -> vm.uploadAvatar(context, uri, onUserChanged) }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    // Una sección abierta a la vez por pestaña: la primera empieza abierta para no llegar a una lista vacía.
    var openData by rememberSaveable { mutableStateOf("personal") }
    var openSecurity by rememberSaveable { mutableStateOf("password") }
    var openPrefs by rememberSaveable { mutableStateOf("") }
    fun toggle(current: String, key: String) = if (current == key) "" else key

    LaunchedEffect(Unit) { vm.load(isClient) }

    val profile = state.profile
    val notice = state.notice

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            UrbanPageHeader(
                title = "Mi cuenta",
                subtitle = "Tu perfil, tu seguridad y tus ajustes.",
                eyebrow = accountRoleLabel(user.roles)
            )
        }

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

                item {
                    AccountTabs(AccountTab.entries.map { it.label to it.icon }, tab) { tab = it; vm.dismissNotice() }
                }

                when (AccountTab.entries[tab]) {
                    AccountTab.DATA -> item {
                        AccountAccordion(
                            title = "Información personal",
                            summary = if (isClient) "Nombre, correo, teléfono y cumpleaños" else "Nombre y correo",
                            icon = Icons.Default.Person,
                            expanded = openData == "personal",
                            onToggle = { openData = toggle(openData, "personal") }
                        ) {
                            PersonalDataForm(
                                profile = profile,
                                isClient = isClient,
                                saving = state.savingData,
                                notice = notice?.takeIf { it.section == AccountSection.DATA },
                                onSave = { name, email, phone, birth, sex -> vm.saveData(name, email, phone, birth, sex, onUserChanged) }
                            )
                        }
                    }

                    AccountTab.SECURITY -> {
                        item {
                            AccountAccordion(
                                title = "Contraseña",
                                summary = "Cámbiala cuando quieras; se pide la actual.",
                                icon = Icons.Default.Security,
                                expanded = openSecurity == "password",
                                onToggle = { openSecurity = toggle(openSecurity, "password") }
                            ) {
                                ChangePasswordForm(
                                    saving = state.savingPassword,
                                    resetKey = state.passwordChanges,
                                    notice = notice?.takeIf { it.section == AccountSection.PASSWORD },
                                    onSubmit = vm::changePassword
                                )
                            }
                        }
                        item {
                            val verified = profile.emailVerifiedAt != null
                            UrbanAttentionRow(
                                icon = if (verified) Icons.Default.VerifiedUser else Icons.Default.MarkEmailUnread,
                                text = if (verified) "Correo verificado" else "Correo sin verificar",
                                subtitle = if (verified) profile.email else "Revisa el enlace que te enviamos a ${profile.email}.",
                                tone = if (verified) UrbanColors.Success else UrbanColors.Warning
                            )
                        }
                    }

                    AccountTab.PREFERENCES -> {
                        item {
                            AccountAccordion(
                                title = "Apariencia",
                                summary = "Tema: ${UrbanColors.current.label}",
                                icon = Icons.Default.Palette,
                                expanded = openPrefs == "theme",
                                onToggle = { openPrefs = toggle(openPrefs, "theme") }
                            ) {
                                ThemePicker(UrbanColors.current) { theme ->
                                    UrbanColors.applyTheme(theme)
                                    scope.launch { AppContainer.sessionManager.saveTheme(theme.key) }
                                }
                            }
                        }
                        item {
                            val prefs = state.preferences
                            AccountAccordion(
                                title = "Notificaciones",
                                summary = prefs?.summary() ?: "No se pudieron cargar tus avisos",
                                icon = Icons.Default.Notifications,
                                expanded = openPrefs == "notifications",
                                onToggle = { openPrefs = toggle(openPrefs, "notifications") }
                            ) {
                                if (prefs != null) {
                                    NotificationChannels(prefs, notice?.takeIf { it.section == AccountSection.NOTIFICATIONS }, vm::setPreference)
                                } else {
                                    UrbanOutlineButton("Reintentar", onClick = { vm.load(isClient) }, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                        if (isClient) item {
                            UrbanAttentionRow(
                                icon = Icons.Default.SmartToy,
                                text = "Ayuda con Bladebot",
                                subtitle = "Pregunta por horarios, servicios o tus citas.",
                                tone = UrbanColors.Gold,
                                onClick = { onNavigate("chatbot") }
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                UrbanOutlineButton(
                    text = "Cerrar sesión",
                    onClick = onLogout,
                    icon = Icons.AutoMirrored.Filled.Logout,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "UrbanBlade ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = UrbanColors.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        }
    }
}
