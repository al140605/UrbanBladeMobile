package com.urbanblade.mobile.ui.screens

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.unit.Dp
import org.junit.Assert.assertTrue

/**
 * Área que realmente responde al dedo: incluye la ampliación automática que Compose aplica a los elementos tocables
 * pequeños hasta 48 dp (el mínimo de accesibilidad de Android). Es lo que importa para poder tocar con precisión.
 * La versión de Compose del proyecto no trae una aserción de «al menos», por eso se mide aquí.
 */
internal fun SemanticsNodeInteraction.assertTouchTargetAtLeast(minimo: Dp): SemanticsNodeInteraction {
    val nodo = fetchSemanticsNode()
    val densidad = nodo.layoutInfo.density.density
    val alto = nodo.touchBoundsInRoot.height / densidad
    val ancho = nodo.touchBoundsInRoot.width / densidad
    // 0,5 dp de tolerancia por el redondeo a píxeles.
    assertTrue("Área táctil de ${ancho}x$alto dp; el mínimo es ${minimo.value} dp", alto >= minimo.value - 0.5f && ancho >= minimo.value - 0.5f)
    return this
}

/**
 * Tamaño con el que el elemento se dibuja y se mide (sin la ampliación automática del toque). Úsese solo donde 48 dp
 * es una decisión explícita del componente, para que nadie lo reduzca sin darse cuenta; los componentes estándar de
 * Material, como los chips de filtro (32 dp), dependen de la ampliación y se comprueban con [assertTouchTargetAtLeast].
 */
internal fun SemanticsNodeInteraction.assertLayoutSizeAtLeast(minimo: Dp): SemanticsNodeInteraction {
    val nodo = fetchSemanticsNode()
    val densidad = nodo.layoutInfo.density.density
    val alto = nodo.boundsInRoot.height / densidad
    val ancho = nodo.boundsInRoot.width / densidad
    assertTrue("Tamaño de ${ancho}x$alto dp; el mínimo es ${minimo.value} dp", alto >= minimo.value - 0.5f && ancho >= minimo.value - 0.5f)
    return this
}
