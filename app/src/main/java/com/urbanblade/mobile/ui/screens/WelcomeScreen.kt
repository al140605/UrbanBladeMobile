package com.urbanblade.mobile.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
            0f to Color(0x55000000),
            0.38f to Color(0x22000000),
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
                        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(36.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("URBANBLADE", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Text("BARBERÍA · ESTILO · CUIDADO", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF0D68C))
                }
                Column(Modifier.padding(top = 48.dp)) {
                    Text("Tu próximo\ngran corte.", style = MaterialTheme.typography.displaySmall, color = Color.White,
                        modifier = Modifier.semantics { heading() })
                    Spacer(Modifier.height(8.dp))
                    Text("Elige tu servicio. Nosotros cuidamos el detalle.", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFE5E0D8))
                    Spacer(Modifier.height(24.dp))
                    UrbanPublicPanel(outlined = true, contentPadding = 16.dp, spacing = 10.dp) {
                        if (sessionExpired) UrbanInfoBanner("Tu sesión venció. Inicia sesión de nuevo para continuar.", Icons.Default.Lock)
                        AuthPrimaryButton("Crear mi cuenta", onRegister, icon = Icons.Default.PersonAdd, modifier = Modifier.fillMaxWidth())
                        UrbanOutlineButton("Iniciar sesión", onLogin, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
