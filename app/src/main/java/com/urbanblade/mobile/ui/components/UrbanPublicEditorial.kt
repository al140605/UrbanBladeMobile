package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.urbanblade.mobile.R
import com.urbanblade.mobile.ui.theme.UrbanColors

/** Fotografía decorativa con velo y tinta clara, también sobre el tema Libreta. */
@Composable
fun UrbanPublicHero(title: String, subtitle: String, modifier: Modifier = Modifier, eyebrow: String = "URBANBLADE", compact: Boolean = false) {
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))) {
        Image(painterResource(R.drawable.public_barber_editorial), null,
            Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(
            listOf(Color(0x55000000), Color(0xDD080808)))))
        Column(Modifier.padding(20.dp).heightIn(min = if (compact) 84.dp else 130.dp), verticalArrangement = Arrangement.Bottom) {
            Text(eyebrow, style = MaterialTheme.typography.labelMedium, color = Color(0xFFF0D68C))
            Spacer(Modifier.height(8.dp))
            Text(title, style = if (compact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium, color = Color.White,
                modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE5E0D8))
        }
    }
}

/** Panel opaco: no coloca fotografía detrás de campos, avisos ni acciones. */
@Composable
fun UrbanPublicPanel(
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    contentPadding: Dp = 20.dp,
    spacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = UrbanColors.Card,
        border = if (outlined) BorderStroke(1.dp, UrbanColors.Gold.copy(alpha = 0.22f)) else null,
        contentColor = UrbanColors.Ink, tonalElevation = 2.dp) {
        Column(Modifier.padding(contentPadding), verticalArrangement = Arrangement.spacedBy(spacing), content = content)
    }
}
