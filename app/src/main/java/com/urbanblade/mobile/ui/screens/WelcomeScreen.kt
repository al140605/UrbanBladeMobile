package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.R
import com.urbanblade.mobile.ui.components.*
import com.urbanblade.mobile.ui.theme.UrbanColors

@Composable
fun WelcomeScreen(onRegister: () -> Unit, onLogin: () -> Unit, sessionExpired: Boolean = false, onExplore: (() -> Unit)? = null) {
    Box(Modifier.fillMaxSize().background(UrbanColors.Background)) {
        Image(
            painterResource(R.drawable.public_welcome_portrait), null,
            Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter
        )
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(
            0f to Color(0xA0000000),
            0.30f to Color(0x30000000),
            0.65f to Color(0xB0080808),
            1f to Color(0xF2080808)
        )))
        BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding()) {
            val viewportHeight = maxHeight
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .heightIn(min = viewportHeight).padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(48.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("URBANBLADE", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("BARBERÍA · ESTILO · CUIDADO", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF0D68C))
                        }
                    }
                }
                Column(Modifier.padding(top = 48.dp)) {
                    Text(
                        buildAnnotatedString {
                            append("Tu próximo\n")
                            withStyle(SpanStyle(color = Color(0xFFF0D68C))) { append("gran corte.") }
                        },
                        style = MaterialTheme.typography.displaySmall, color = Color.White,
                        modifier = Modifier.semantics { heading() }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Elige tu servicio. Nosotros cuidamos el detalle.", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFE5E0D8))
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        WelcomePerk(Icons.Default.EventAvailable, "Reserva en línea")
                        WelcomePerk(Icons.Default.VerifiedUser, "Pago seguro")
                        WelcomePerk(Icons.Default.CardGiftcard, "Beneficios")
                    }
                    Spacer(Modifier.height(24.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (sessionExpired) UrbanInfoBanner("Tu sesión venció. Inicia sesión de nuevo para continuar.", Icons.Default.Lock)
                        AuthPrimaryButton("Crear mi cuenta", onRegister, icon = Icons.Default.PersonAdd, modifier = Modifier.fillMaxWidth())
                        UrbanOutlineButton("Iniciar sesión", onLogin, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

/** Beneficio breve de la bienvenida: icono dorado y una línea. */
@Composable
private fun WelcomePerk(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color(0xFFF0D68C), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = Color(0xFFE5E0D8))
    }
}
