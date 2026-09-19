package com.urbanblade.mobile.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.urbanblade.mobile.ui.components.AuthBackdrop
import com.urbanblade.mobile.ui.components.AuthPrimaryButton
import com.urbanblade.mobile.ui.components.StaggerIn
import com.urbanblade.mobile.ui.components.UrbanBrandMark
import com.urbanblade.mobile.ui.components.UrbanOutlineButton
import com.urbanblade.mobile.ui.theme.MascotMood
import com.urbanblade.mobile.ui.theme.UrbanColors
import com.urbanblade.mobile.ui.theme.mascot

/**
 * Primera pantalla para quien aún no tiene sesión: identidad de marca y tres caminos claros
 * (crear cuenta, entrar, o explorar el catálogo sin cuenta).
 */
@Composable
fun WelcomeScreen(onRegister: () -> Unit, onLogin: () -> Unit, onExplore: () -> Unit) {
    // La mascota "flota" muy despacio; con "Quitar animaciones" del sistema se queda quieta.
    val transition = rememberInfiniteTransition(label = "float")
    val bob by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )

    AuthBackdrop {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            StaggerIn(0) {
                Image(
                    painter = painterResource(UrbanColors.current.mascot(MascotMood.WELCOME)),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(232.dp)
                        .graphicsLayer { translationY = bob.dp.toPx() }
                )
            }
            Spacer(Modifier.height(20.dp))
            StaggerIn(1) { UrbanBrandMark() }
            Spacer(Modifier.height(20.dp))
            StaggerIn(2) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Tu estilo. Tu tiempo.",
                        style = MaterialTheme.typography.displaySmall,
                        color = UrbanColors.Ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() }
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Reserva con tu barbero, compra lo mejor y sigue tus beneficios, todo desde tu bolsillo.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = UrbanColors.Muted,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.weight(1.1f))

            StaggerIn(4) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AuthPrimaryButton(
                        text = "Crear mi cuenta",
                        onClick = onRegister,
                        icon = Icons.Default.PersonAdd,
                        modifier = Modifier.fillMaxWidth()
                    )
                    UrbanOutlineButton(
                        text = "Ya tengo cuenta",
                        onClick = onLogin,
                        icon = Icons.Default.Login,
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = onExplore,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Text("Explorar sin cuenta", color = UrbanColors.Muted, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
