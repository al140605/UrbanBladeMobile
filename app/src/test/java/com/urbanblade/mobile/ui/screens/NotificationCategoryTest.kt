package com.urbanblade.mobile.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationCategoryTest {
    @Test
    fun `agrupa los tipos de aviso en cuatro categorias`() {
        assertEquals("Citas", notificationCategory("appointment"))
        assertEquals("Citas", notificationCategory("REMINDER"))
        assertEquals("Pagos", notificationCategory("transfer_receipt"))
        assertEquals("Pedidos", notificationCategory("order_delivered"))
        assertEquals("Sistema", notificationCategory("inventory_low_stock"))
    }

    @Test
    fun `lo desconocido cae en Otros`() {
        assertEquals("Otros", notificationCategory("promotion"))
        assertEquals("Otros", notificationCategory(null))
    }
}
