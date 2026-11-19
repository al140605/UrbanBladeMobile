package com.urbanblade.mobile.data.model

import com.google.gson.annotations.SerializedName


data class NotificationItem(
    val id: String,
    val title: String? = null,
    val message: String? = null,
    @SerializedName("read_at") val readAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

data class ProductItem(
    val id: String,
    val nombre: String,
    val categoria: String? = null,
    val descripcion: String? = null,
    @SerializedName("precio_venta") val precioVenta: Double = 0.0,
    @SerializedName("stock_actual") val stockActual: Int = 0,
    val imagen: String? = null
)

data class OrderItemRequest(
    @SerializedName("product_id") val productId: String,
    val cantidad: Int
)
data class OrderRequest(val items: List<OrderItemRequest>)
data class DeliverOrderRequest(@SerializedName("metodo_pago") val metodoPago: String)
data class OrderLine(
    @SerializedName("product_id") val productId: String? = null,
    val nombre: String? = null,
    val precio: Double = 0.0,
    val cantidad: Int = 0,
    val subtotal: Double = 0.0
)
data class OrderClient(val id: String? = null, val name: String? = null)
data class OrderRow(
    val id: String,
    val folio: String? = null,
    val estado: String = "pendiente",
    val tipo: String? = null,
    val total: Double = 0.0,
    @SerializedName("metodo_pago") val metodoPago: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("entregado_en") val entregadoEn: String? = null,
    val items: List<OrderLine> = emptyList(),
    val client: OrderClient? = null
)
/** GET orders/{id}/receipt-link: liga firmada al comprobante PDF de un pedido entregado. */
data class OrderReceiptData(@SerializedName("order_id") val orderId: String? = null, @SerializedName("receipt_url") val receiptUrl: String? = null)
data class OrderReceiptResponse(val data: OrderReceiptData? = null)
/** Totales que el servidor calcula solo para el personal. */
data class OrderStats(
    val pendientes: Int = 0,
    val entregados: Int = 0,
    @SerializedName("por_cobrar") val porCobrar: Double = 0.0
)
data class OrderMeta(
    @SerializedName("current_page") val currentPage: Int = 1,
    @SerializedName("last_page") val lastPage: Int = 1,
    val total: Int = 0,
    val stats: OrderStats? = null
)
data class OrdersResponse(val data: List<OrderRow> = emptyList(), val meta: OrderMeta? = null)
data class OrderMutationResponse(val message: String? = null, val data: OrderRow? = null)

data class PaymentAppointment(
    val id: String? = null,
    val fecha: String? = null,
    @SerializedName("hora_inicio") val horaInicio: String? = null,
    val service: String? = null,
    val client: String? = null,
    val barber: String? = null
)
data class PaymentRow(
    val id: String,
    val monto: Double = 0.0,
    @SerializedName("metodo_pago") val metodoPago: String? = null,
    val propina: Double = 0.0,
    @SerializedName("receipt_url") val receiptUrl: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val appointment: PaymentAppointment? = null,
    /** verificado | pendiente_verificacion | rechazado | reembolsado (null en pagos antiguos = cobrado). */
    val estado: String? = null,
    /** Pagado al reservar (anticipo o pago completo por adelantado). */
    @SerializedName("es_deposito") val esDeposito: Boolean = false
) {
    /** Dinero que de verdad se quedó en la barbería: solo estos llevan comprobante. */
    val collected: Boolean get() = estado == null || estado == "verificado"
}
/** Estadísticas globales que el servidor calcula para el personal (no dependen de la página cargada). */
data class PaymentsStats(
    @SerializedName("total_hoy") val totalHoy: Double = 0.0,
    @SerializedName("total_mes") val totalMes: Double = 0.0,
    val count: Int = 0,
    val metodos: Map<String, Int> = emptyMap()
)
/** `meta` del listado: el personal recibe paginación y estadísticas; el cliente, sus totales. */
data class PaymentsMeta(
    @SerializedName("current_page") val currentPage: Int = 1,
    @SerializedName("last_page") val lastPage: Int = 1,
    val total: Int = 0,
    val stats: PaymentsStats? = null,
    @SerializedName("pending_count") val pendingCount: Int = 0,
    @SerializedName("total_pagado") val totalPagado: Double = 0.0,
    @SerializedName("total_citas") val totalCitas: Int = 0
)
data class PaymentsResponse(val data: List<PaymentRow> = emptyList(), val meta: PaymentsMeta? = null)
data class ReceiptData(@SerializedName("payment_id") val paymentId: String? = null, @SerializedName("receipt_url") val receiptUrl: String? = null)
data class ReceiptResponse(val data: ReceiptData? = null)
data class PendingAppointment(
    val id: String? = null,
    val client: String? = null,
    val service: String? = null,
    @SerializedName("service_price") val servicePrice: Double? = null
)
data class PendingPaymentRow(
    val id: String,
    val monto: Double = 0.0,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("comprobante_url") val comprobanteUrl: String? = null,
    @SerializedName("ocr_texto") val ocrTexto: String? = null,
    @SerializedName("ocr_monto_detectado") val ocrMontoDetectado: Double? = null,
    val appointment: PendingAppointment? = null
)
/**
 * Cobro manual del personal. `monto` es solo informativo: PaymentService relee el precio del servicio
 * en el servidor y calcula descuentos y puntos (nunca se confía en el monto del cliente).
 */
data class CreatePaymentRequest(
    @SerializedName("appointment_id") val appointmentId: String,
    val monto: Double,
    @SerializedName("metodo_pago") val metodoPago: String,
    val propina: Double = 0.0
)
data class PendingPaymentsResponse(val data: List<PendingPaymentRow> = emptyList())
data class RejectPaymentRequest(@SerializedName("motivo_rechazo") val motivo: String)
data class StripeIntentRequest(
    @SerializedName("appointment_id") val appointmentId: String,
    @SerializedName("puntos_canjeados") val puntos: Int = 0,
    @SerializedName("codigo_gift_card") val codigoGiftCard: String? = null,
    val propina: Double = 0.0,
    /** Tarjeta nueva que el cliente quiere guardar para la próxima vez. */
    @SerializedName("guardar_tarjeta") val guardarTarjeta: Boolean? = null,
    /** El cliente paga con una tarjeta que ya tenía guardada. */
    @SerializedName("tarjeta_guardada") val tarjetaGuardada: Boolean? = null
)

/** GET /payments/transfer-info: datos para transferir que configura el administrador. */
data class TransferInfo(
    val configurado: Boolean = false,
    val banco: String? = null,
    val beneficiario: String? = null,
    val clabe: String? = null,
    val concepto: String? = null,
    val cotizacion: TransferQuote? = null
)
data class TransferQuote(
    @com.google.gson.annotations.SerializedName("appointment_id") val appointmentId: String? = null,
    @com.google.gson.annotations.SerializedName("service_id") val serviceId: String? = null,
    @com.google.gson.annotations.SerializedName("monto_servicio") val serviceAmount: Double = 0.0,
    val propina: Double = 0.0,
    val total: Double = 0.0,
    val moneda: String = ""
) {
    fun matches(appointmentId: String? = null, serviceId: String? = null, tip: Double): Boolean =
        moneda == "mxn" && total.isFinite() && total > 0 && serviceAmount.isFinite() && serviceAmount >= 0 &&
            propina.isFinite() && propina >= 0 && kotlin.math.abs(propina - tip) < 0.005 &&
            kotlin.math.abs(total - serviceAmount - propina) < 0.005 &&
            ((appointmentId != null && this.appointmentId == appointmentId) ||
                (appointmentId == null && serviceId != null && this.appointmentId == null && this.serviceId == serviceId))
}

data class TransferInfoResponse(val data: TransferInfo = TransferInfo())

/** GET /payments/cards: tarjeta guardada en Stripe (solo datos seguros de mostrar). */
data class SavedCard(
    val id: String,
    val brand: String = "card",
    val last4: String = "",
    @SerializedName("exp_month") val expMonth: Int = 0,
    @SerializedName("exp_year") val expYear: Int = 0,
    /** Nombre del titular tal como se guardó en Stripe; null en tarjetas guardadas antes de pedirlo. */
    val holder: String? = null
)
data class SavedCardsResponse(val data: List<SavedCard> = emptyList())
data class StripeIntentResponseData(
    @SerializedName("client_secret") val clientSecret: String,
    @SerializedName("payment_intent_id") val paymentIntentId: String
)
data class StripeIntentResponse(val data: StripeIntentResponseData)
data class UploadPaymentReceiptResponse(val message: String? = null, val data: UploadPaymentReceiptData? = null)
data class UploadPaymentReceiptData(val id: String, val estado: String)
