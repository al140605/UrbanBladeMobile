package com.urbanblade.mobile.ui.screens

import com.urbanblade.mobile.data.model.ModuleFeatures
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** La hoja de compra abierta se cierra si el backend apaga su módulo. */
class BenefitShopSheetTest {
    @Test fun `la hoja de paquetes solo sigue con paquetes encendidos`() {
        assertTrue(purchaseSheetAllowed("packages", ModuleFeatures(packages = true)))
        assertFalse(purchaseSheetAllowed("packages", ModuleFeatures(giftCards = true)))
    }

    @Test fun `la hoja de tarjeta regalo solo sigue con tarjetas encendidas`() {
        assertTrue(purchaseSheetAllowed("gift", ModuleFeatures(giftCards = true)))
        assertFalse(purchaseSheetAllowed("gift", ModuleFeatures(packages = true)))
    }

    @Test fun `sin hoja o con una desconocida no se permite`() {
        assertFalse(purchaseSheetAllowed(null, ModuleFeatures(packages = true, giftCards = true)))
        assertFalse(purchaseSheetAllowed("otra", ModuleFeatures(packages = true, giftCards = true)))
    }
}
