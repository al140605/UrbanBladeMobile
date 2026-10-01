package com.urbanblade.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.urbanblade.mobile.data.model.SavedCard
import com.urbanblade.mobile.ui.theme.UrbanColors
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * Tarjetas guardadas del cliente (GET /payments/cards) como un carrusel con profundidad: la del centro
 * es la elegida y las laterales se ven más chicas, tenues y giradas. La última página es «Otra tarjeta».
 * Solo muestra lo que manda barber (marca, últimos 4 y vencimiento): nunca datos inventados.
 *
 * [onSelect] recibe el id de la tarjeta al centro, o null cuando el centro es «Otra tarjeta».
 */
@Composable
fun UrbanCreditCardCarousel(
    cards: List<SavedCard>,
    selectedCardId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val pageCount = cards.size + 1
    val initialPage = cards.indexOfFirst { it.id == selectedCardId }.takeIf { it >= 0 } ?: 0
    val pagerState = rememberPagerState(initialPage = initialPage) { pageCount }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val currentOnSelect by rememberUpdatedState(onSelect)

    // La página al centro es la selección; cada cambio se siente como un «clic» (la primera emisión es
    // la página inicial y no vibra).
    LaunchedEffect(pagerState, cards) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page -> currentOnSelect(cards.getOrNull(page)?.id) }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .drop(1)
            .collect { haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 44.dp),
            pageSpacing = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            // 0 = al centro; ±1 = una posición a un lado (fraccionario mientras se arrastra).
            val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val focus = 1f - offset.absoluteValue.coerceIn(0f, 1f)
            val card = cards.getOrNull(page)
            val label = card?.let { "${cardBrandName(it.brand)} terminada en ${it.last4}" } ?: "Usar otra tarjeta"

            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(CARD_RATIO)
                    .graphicsLayer {
                        val scale = lerp(0.85f, 1f, focus)
                        scaleX = scale
                        scaleY = scale
                        alpha = lerp(0.6f, 1f, focus)
                        rotationY = offset.coerceIn(-1f, 1f) * 14f
                        cameraDistance = 12f * density.density
                    }
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(role = Role.RadioButton, onClickLabel = "Elegir") {
                        scope.launch { pagerState.animateScrollToPage(page) }
                    }
                    .semantics {
                        contentDescription = label
                        selected = pagerState.currentPage == page
                    }
            ) {
                if (card != null) CardFace(card) else NewCardFace()
            }
        }

        Spacer(Modifier.height(10.dp))
        PagerDots(count = pageCount, current = pagerState.currentPage)
    }
}

/** Proporción de una tarjeta física (ISO/IEC 7810 ID-1). */
private const val CARD_RATIO = 1.586f

/** Colores de la cara de la tarjeta según la marca. */
private data class CardSkin(val background: Brush, val text: Color, val accent: Color, val border: Color)

/**
 * Visa en plata, Mastercard en el dorado del tema y Amex como Black Card con letras doradas. La cara de
 * una tarjeta no cambia con el tema de la app (una tarjeta negra es negra también en el tema claro);
 * solo el dorado sigue al tema. Cualquier otra marca usa las superficies del tema.
 */
@Composable
private fun skinFor(brand: String): CardSkin = when (brand.lowercase()) {
    "visa" -> CardSkin(
        background = Brush.linearGradient(listOf(Color(0xFFE6E8EB), Color(0xFFB9BEC5), Color(0xFF8E949C))),
        text = Color(0xFF15171A),
        accent = Color(0xFF1A1F71),
        border = Color.White.copy(alpha = 0.45f)
    )
    "mastercard" -> CardSkin(
        background = Brush.linearGradient(listOf(UrbanColors.Gold, UrbanColors.GoldDim)),
        text = UrbanColors.OnGold,
        accent = UrbanColors.OnGold,
        border = Color.White.copy(alpha = 0.30f)
    )
    "amex" -> CardSkin(
        background = Brush.linearGradient(listOf(Color(0xFF1B1B1D), Color(0xFF070708))),
        text = UrbanColors.Gold,
        accent = UrbanColors.Gold,
        border = UrbanColors.Gold.copy(alpha = 0.45f)
    )
    else -> CardSkin(
        background = Brush.linearGradient(listOf(UrbanColors.CardAlt, UrbanColors.Card)),
        text = UrbanColors.Ink,
        accent = UrbanColors.Gold,
        border = UrbanColors.Line
    )
}

private fun cardBrandName(brand: String): String = when (brand.lowercase()) {
    "visa" -> "Visa"
    "mastercard" -> "Mastercard"
    "amex" -> "American Express"
    "discover" -> "Discover"
    else -> brand.replaceFirstChar { it.uppercase() }.ifBlank { "Tarjeta" }
}

@Composable
private fun CardFace(card: SavedCard) {
    val skin = skinFor(card.brand)
    Column(
        Modifier
            .fillMaxSize()
            .background(skin.background)
            .border(BorderStroke(1.dp, skin.border), RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("URBANBLADE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = skin.text.copy(alpha = 0.75f))
            BrandMark(card.brand, skin)
        }
        // Chip
        Box(
            Modifier
                .size(width = 36.dp, height = 26.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFE9D9A6), Color(0xFFB8963F))))
        )
        Text(
            "••••  ••••  ••••  ${card.last4}",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = skin.text
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(cardBrandName(card.brand), style = MaterialTheme.typography.labelMedium, color = skin.text.copy(alpha = 0.8f))
            if (card.expMonth > 0 && card.expYear > 0) {
                Text(
                    "%02d/%02d".format(card.expMonth, card.expYear % 100),
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    color = skin.text.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/** Marca dibujada con formas simples (sin logotipos de terceros en los recursos de la app). */
@Composable
private fun BrandMark(brand: String, skin: CardSkin) {
    when (brand.lowercase()) {
        "mastercard" -> Box(Modifier.size(width = 34.dp, height = 22.dp)) {
            Box(Modifier.size(22.dp).clip(CircleShape).background(Color(0xFFEB001B).copy(alpha = 0.9f)))
            Box(Modifier.size(22.dp).offset(x = 12.dp).clip(CircleShape).background(Color(0xFFF79E1B).copy(alpha = 0.85f)))
        }
        "visa" -> Text("VISA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = skin.accent)
        "amex" -> Text("AMEX", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = skin.accent)
        else -> Text(cardBrandName(brand).uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = skin.accent)
    }
}

@Composable
private fun NewCardFace() {
    Column(
        Modifier
            .fillMaxSize()
            .background(UrbanColors.Card)
            .border(BorderStroke(1.5.dp, UrbanColors.Gold.copy(alpha = 0.6f)), RoundedCornerShape(18.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.AddCard, null, tint = UrbanColors.Gold, modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(8.dp))
        Text("Otra tarjeta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = UrbanColors.Ink)
        Text("Crédito o débito", style = MaterialTheme.typography.bodySmall, color = UrbanColors.Muted)
    }
}

@Composable
private fun PagerDots(count: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { index ->
            val active = index == current
            Box(
                Modifier
                    .size(width = if (active) 18.dp else 6.dp, height = 6.dp)
                    .clip(CircleShape)
                    .background(if (active) UrbanColors.Gold else UrbanColors.Muted.copy(alpha = 0.35f))
            )
        }
    }
}
