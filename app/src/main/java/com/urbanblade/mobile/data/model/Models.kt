package com.urbanblade.mobile.data.model

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val password: String,
    @SerializedName("device_name") val deviceName: String = "Android UrbanBlade"
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    @SerializedName("password_confirmation") val passwordConfirmation: String,
    @SerializedName("device_name") val deviceName: String = "Android UrbanBlade"
)

data class ForgotPasswordRequest(val email: String)
data class GoogleLoginRequest(@SerializedName("id_token") val idToken: String)

data class LoginResponse(
    val message: String? = null,
    @SerializedName("token_type") val tokenType: String? = null,
    val token: String,
    val user: AuthUser
)

data class MeResponse(val user: AuthUser)

data class AuthUser(
    val id: String? = null,
    val name: String,
    val email: String,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    val roles: List<String> = emptyList(),
    @SerializedName("profile_complete") val profileComplete: Boolean = true,
    @SerializedName("profile_missing") val profileMissing: List<String> = emptyList(),
    @SerializedName("client_id") val clientId: String? = null,
    @SerializedName("barber_id") val barberId: String? = null,
    val client: ClientInfo? = null
)

data class ClientInfo(
    val telefono: String? = null,
    @SerializedName("fecha_nacimiento") val fechaNacimiento: String? = null,
    val sexo: String? = null
)

data class MessageResponse(val message: String? = null)

data class ApiList<T>(val data: List<T> = emptyList())

data class ServiceItem(
    val id: String,
    val nombre: String,
    val precio: Double = 0.0,
    @SerializedName("duracion_min") val duracionMin: Int = 0,
    val descripcion: String? = null
)

data class BarberUser(val id: String? = null, val name: String? = null)

data class BarberItem(
    val id: String,
    val slug: String? = null,
    val user: BarberUser? = null,
    val descripcion: String? = null,
    val foto: String? = null,
    val especialidades: String? = null
)

data class SlotItem(
    val time: String,
    val label: String,
    @SerializedName("end_time") val endTime: String? = null,
    @SerializedName("end_label") val endLabel: String? = null
)

data class SlotsResponse(val slots: List<SlotItem> = emptyList())

data class AppointmentBarber(
    val id: String? = null,
    val slug: String? = null,
    val user: BarberUser? = null
)

data class AppointmentService(
    val id: String? = null,
    val nombre: String? = null,
    val precio: Double? = null,
    @SerializedName("duracion_min") val duracionMin: Int? = null
)

data class AppointmentClientUser(val name: String? = null)
data class AppointmentClientRef(val id: String? = null, val user: AppointmentClientUser? = null)
data class AppointmentRow(
    val id: String,
    val code: String? = null,
    val fecha: String,
    @SerializedName("hora_inicio") val horaInicio: String,
    @SerializedName("hora_fin") val horaFin: String? = null,
    val estado: String,
    val notas: String? = null,
    @SerializedName("precio_cobrado") val precioCobrado: Double? = null,
    @SerializedName("has_payment") val hasPayment: Boolean = false,
    @SerializedName("is_chargeable") val isChargeable: Boolean = false,
    val client: AppointmentClientRef? = null,
    val barber: AppointmentBarber? = null,
    val service: AppointmentService? = null
)

data class AppointmentStats(
    val total: Int = 0,
    val proximas: Int = 0,
    val completadas: Int = 0,
    val canceladas: Int = 0
)

data class AppointmentsResponse(
    val data: List<AppointmentRow> = emptyList(),
    val stats: AppointmentStats = AppointmentStats(),
    val next: AppointmentRow? = null,
    @SerializedName("cancellation_policy_hours") val cancellationPolicyHours: Int = 24
)

data class AppointmentRequest(
    @SerializedName("barber_id") val barberId: String,
    @SerializedName("service_id") val serviceId: String,
    val fecha: String,
    @SerializedName("hora_inicio") val horaInicio: String,
    val notas: String? = null
)

data class DashboardResponse(
    val role: String? = null,
    val data: JsonObject = JsonObject()
)

data class ProfileResponse(val user: ProfileUser)

data class ProfileUser(
    val name: String,
    val email: String,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    val roles: List<String> = emptyList(),
    val client: ClientInfo? = null
)

data class UpdateProfileRequest(
    val name: String,
    val email: String,
    val telefono: String? = null,
    @SerializedName("fecha_nacimiento") val fechaNacimiento: String? = null,
    val sexo: String? = null
)

data class UpdateProfileResponse(
    val message: String? = null,
    val user: AuthUser? = null
)

data class ChangePasswordRequest(
    @SerializedName("current_password") val currentPassword: String,
    val password: String,
    @SerializedName("password_confirmation") val passwordConfirmation: String
)

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
    val items: List<OrderLine> = emptyList(),
    val client: OrderClient? = null
)
data class OrderMeta(val current_page: Int? = null, val last_page: Int? = null, val total: Int? = null)
data class OrdersResponse(val data: List<OrderRow> = emptyList(), val meta: JsonObject? = null)
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
    val appointment: PaymentAppointment? = null
)
data class PaymentsResponse(val data: List<PaymentRow> = emptyList(), val meta: JsonObject? = null)
data class PendingPaymentRow(
    val id: String,
    val monto: Double = 0.0,
    @SerializedName("comprobante_url") val comprobanteUrl: String? = null,
    @SerializedName("ocr_texto") val ocrTexto: String? = null,
    @SerializedName("ocr_monto_detectado") val ocrMontoDetectado: Double? = null,
    val appointment: JsonObject? = null
)
data class PendingPaymentsResponse(val data: List<PendingPaymentRow> = emptyList())
data class RejectPaymentRequest(@SerializedName("motivo_rechazo") val motivo: String)
data class StripeIntentRequest(@SerializedName("appointment_id") val appointmentId: String, @SerializedName("puntos_canjeados") val puntos: Int = 0)

// ── Horario de barbero ──────────────────────────────────────────────────
data class BarberScheduleDay(
    @SerializedName("day_of_week") val dayOfWeek: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("is_active") val isActive: Boolean = true
)
data class BarberScheduleResponse(val schedules: List<BarberScheduleDay> = emptyList())
data class UpdateBarberScheduleRequest(val schedules: List<BarberScheduleDay>)

// ── Corte de caja ────────────────────────────────────────────────────────
data class CashCloseRecord(
    val id: String? = null,
    @SerializedName("efectivo_contado") val efectivoContado: Double = 0.0,
    val diferencia: Double = 0.0,
    val notas: String? = null,
    @SerializedName("cerrado_por_nombre") val cerradoPorNombre: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)
data class CashClosePreview(
    val fecha: String? = null,
    val esperado: Map<String, Double> = emptyMap(),
    @SerializedName("esperado_total") val esperadoTotal: Double = 0.0,
    @SerializedName("efectivo_esperado") val efectivoEsperado: Double = 0.0,
    val propinas: Double = 0.0,
    val pagos: Int = 0,
    val pedidos: Int = 0,
    val paquetes: Int = 0,
    @SerializedName("gift_cards") val giftCards: Int = 0,
    val membresias: Int = 0,
    val cierre: CashCloseRecord? = null
)
data class CashClosePreviewResponse(val data: CashClosePreview = CashClosePreview())
data class RegisterCashCloseRequest(
    @SerializedName("efectivo_contado") val efectivoContado: Double,
    val notas: String? = null
)

// ── Métricas de admin ────────────────────────────────────────────────────
data class AdminMetrics(
    val totalClients: Int = 0,
    val activeBarbers: Int = 0,
    val cancellationRate: Double = 0.0,
    val averageRevenuePerAppointment: Double = 0.0,
    val totalRevenue: Double = 0.0
)
data class AdminMetricsResponse(val metrics: AdminMetrics = AdminMetrics())

// ── Estado del sistema ───────────────────────────────────────────────────
data class SystemAppInfo(
    val name: String? = null,
    val env: String? = null,
    @SerializedName("laravel_version") val laravelVersion: String? = null,
    @SerializedName("php_version") val phpVersion: String? = null
)
data class SystemServiceStatus(
    val status: String? = null,
    @SerializedName("latency_ms") val latencyMs: Int? = null,
    val error: String? = null
)
data class SystemQueueStatus(
    val connection: String? = null,
    val pending: Int? = null,
    val failed: Int? = null
)
data class SystemScheduledTask(
    val name: String? = null,
    val expression: String? = null,
    val status: String? = null,
    @SerializedName("ran_at") val ranAt: String? = null,
    @SerializedName("runtime_ms") val runtimeMs: Int? = null,
    val error: String? = null
)
data class SystemStatusResponse(
    val app: SystemAppInfo = SystemAppInfo(),
    val database: SystemServiceStatus = SystemServiceStatus(),
    val redis: SystemServiceStatus = SystemServiceStatus(),
    val queue: SystemQueueStatus = SystemQueueStatus(),
    @SerializedName("scheduled_tasks") val scheduledTasks: List<SystemScheduledTask> = emptyList()
)

// ── Clientes (admin/clients) ─────────────────────────────────────────────
data class ClientRow(
    val id: String,
    val slug: String? = null,
    val name: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    val email: String? = null,
    val telefono: String? = null,
    val segment: String? = null,
    val totalAppointments: Int = 0,
    val totalSpent: Double = 0.0,
    val lastAppointment: String? = null,
    val joinedAt: String? = null
)
data class ClientsResponse(
    val success: Boolean = true,
    val data: List<ClientRow> = emptyList(),
    val total: Int? = null,
    @SerializedName("current_page") val currentPage: Int? = null,
    @SerializedName("last_page") val lastPage: Int? = null
)
data class ClientAppointmentRow(
    val id: String,
    val code: String? = null,
    val fecha: String? = null,
    @SerializedName("hora_inicio") val horaInicio: String? = null,
    val barber: String? = null,
    val service: String? = null,
    val precio: Double? = null,
    val estado: String? = null
)
data class ClientDetail(
    val id: String,
    val slug: String? = null,
    val name: String? = null,
    val email: String? = null,
    val telefono: String? = null,
    val segment: String? = null,
    val nivel: String? = null,
    val puntos: Int = 0,
    val notas: String? = null,
    val joinedAt: String? = null,
    val totalAppointments: Int = 0,
    val totalSpent: Double = 0.0,
    val averageSpent: Double = 0.0,
    val lastAppointment: String? = null,
    val daysSinceLastAppointment: Int? = null,
    val preferredBarber: String? = null,
    val appointments: List<ClientAppointmentRow> = emptyList()
)
data class ClientDetailResponse(val success: Boolean = true, val data: ClientDetail? = null)
data class ClientMutationResponse(val success: Boolean = true, val message: String? = null, val data: ClientRow? = null)
data class CreateClientRequest(val name: String, val email: String, val telefono: String?, val password: String)
data class UpdateClientRequest(val name: String?, val email: String?, val telefono: String?, val notas: String?)

// ── Inventario (inventory/products, staff: admin + recepcionista) ────────
data class InventoryProductRow(
    val id: String,
    val nombre: String,
    val categoria: String? = null,
    val descripcion: String? = null,
    val tipo: String? = null,
    @SerializedName("stock_actual") val stockActual: Int = 0,
    @SerializedName("stock_minimo") val stockMinimo: Int = 0,
    @SerializedName("precio_compra") val precioCompra: Double = 0.0,
    @SerializedName("precio_venta") val precioVenta: Double = 0.0,
    val activo: Boolean = true,
    @SerializedName("low_stock") val lowStock: Boolean = false,
    @SerializedName("pending_restock") val pendingRestock: Boolean = false,
    @SerializedName("imagen_url") val imagenUrl: String? = null
)
data class InventoryStats(val total: Int = 0, @SerializedName("bajo_stock") val bajoStock: Int = 0, @SerializedName("valor_total") val valorTotal: Double = 0.0)
data class InventoryMeta(
    @SerializedName("current_page") val currentPage: Int? = null,
    @SerializedName("last_page") val lastPage: Int? = null,
    val total: Int? = null,
    val stats: InventoryStats = InventoryStats(),
    val categorias: List<String> = emptyList(),
    val tipos: List<String> = emptyList()
)
data class InventoryResponse(val data: List<InventoryProductRow> = emptyList(), val meta: InventoryMeta = InventoryMeta())
data class CreateProductRequest(
    val nombre: String,
    val categoria: String,
    val descripcion: String?,
    @SerializedName("precio_compra") val precioCompra: Double,
    @SerializedName("precio_venta") val precioVenta: Double,
    @SerializedName("stock_actual") val stockActual: Int,
    @SerializedName("stock_minimo") val stockMinimo: Int,
    val tipo: String,
    val activo: Boolean = true
)
data class UpdateProductRequest(
    val nombre: String?,
    val categoria: String?,
    val descripcion: String?,
    @SerializedName("precio_compra") val precioCompra: Double?,
    @SerializedName("precio_venta") val precioVenta: Double?,
    @SerializedName("stock_actual") val stockActual: Int?,
    @SerializedName("stock_minimo") val stockMinimo: Int?,
    val tipo: String?,
    val activo: Boolean?
)
data class RegisterMovementRequest(
    @SerializedName("product_id") val productId: String,
    val tipo: String,
    val cantidad: Int,
    val motivo: String?
)

// ── Reseñas (reviews, admin-only) ────────────────────────────────────────
data class ReviewBarberRef(val id: String? = null, val name: String? = null)
data class ReviewClientRef(val id: String? = null, val name: String? = null)
data class ReviewRow(
    val id: String,
    val rating: Int = 0,
    val comment: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val barber: ReviewBarberRef? = null,
    val client: ReviewClientRef? = null
)
data class ReviewStats(val total: Int = 0, val promedio: Double = 0.0, val bajas: Int = 0)
data class ReviewsResponse(
    val data: List<ReviewRow> = emptyList(),
    val meta: JsonObject? = null,
    val barbers: List<ReviewBarberRef> = emptyList(),
    val stats: ReviewStats = ReviewStats()
)

// ── Logs de auditoría (admin + ingeniero, solo lectura) ──────────────────
data class LogCauser(val id: String? = null, val name: String? = null, val email: String? = null)
data class LogRow(
    val id: String,
    @SerializedName("log_name") val logName: String? = null,
    val description: String? = null,
    val event: String? = null,
    @SerializedName("subject_type") val subjectType: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val causer: LogCauser? = null
)
data class LogStats(val total: Int = 0, val hoy: Int = 0, val creates: Int = 0, val updates: Int = 0, val deletes: Int = 0)
data class LogsResponse(
    val data: List<LogRow> = emptyList(),
    val meta: JsonObject? = null,
    @SerializedName("log_names") val logNames: List<String> = emptyList(),
    val events: List<String> = emptyList(),
    val stats: LogStats = LogStats()
)

// ── Usuarios del sistema (admin-only) ────────────────────────────────────
data class SystemUserRow(
    val id: String,
    val name: String,
    val email: String,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("email_verified_at") val emailVerifiedAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val roles: List<String> = emptyList()
)
data class SystemUsersResponse(
    val data: List<SystemUserRow> = emptyList(),
    val meta: JsonObject? = null,
    val roles: List<String> = emptyList()
)
data class SystemUserMutationResponse(val message: String? = null)
data class CreateUserRequest(val name: String, val email: String, val password: String, @SerializedName("password_confirmation") val passwordConfirmation: String, val role: String)
data class UpdateUserRequest(val name: String, val email: String, val password: String?, @SerializedName("password_confirmation") val passwordConfirmation: String?, val role: String)

// ── Analítica (analytics) ────────────────────────────────────────────────
data class AnalyticsGraph(val tipo: String? = null, val labels: List<String> = emptyList(), val valores: List<JsonElement> = emptyList())
data class AnalyticsKpi(
    val label: String? = null,
    val value: String? = null,
    val detail: String? = null,
    val tone: String? = null,
    val type: String? = null,
    val graph: AnalyticsGraph? = null,
    val message: String? = null
)
data class AnalyticsInsightItem(
    val tipo: String? = null,
    val titulo: String? = null,
    val mensaje: String? = null,
    @SerializedName("valor_destacado") val valorDestacado: String? = null,
    val color: String? = null,
    val grafica: AnalyticsGraph? = null,
    @SerializedName("visual_type") val visualType: String? = null,
    @SerializedName("has_renderable_visual") val hasRenderableVisual: Boolean = false,
    @SerializedName("generado_en") val generadoEn: String? = null
)
data class AnalyticsSection(
    val titulo: String? = null,
    val subtitulo: String? = null,
    val intro: String? = null,
    val acento: String? = null,
    val insights: List<AnalyticsInsightItem> = emptyList()
)
data class AnalyticsSecciones(
    val resumen: AnalyticsSection = AnalyticsSection(),
    val operacion: AnalyticsSection = AnalyticsSection(),
    val clientes: AnalyticsSection = AnalyticsSection(),
    val prediccion: AnalyticsSection = AnalyticsSection()
)
data class OperationalKpi(val label: String? = null, val value: Double = 0.0, val detail: String? = null)
data class OperationalAction(val label: String? = null, val detail: String? = null)
data class OperationalSummary(val kpis: List<OperationalKpi> = emptyList(), val actions: List<OperationalAction> = emptyList())
data class SparkFlowStep(
    val titulo: String? = null,
    val descripcion: String? = null,
    val color: String? = null,
    val count: Int = 0,
    val total: Int = 1,
    val progress: Double = 0.0
)
data class AnalyticsResponse(
    @SerializedName("rol_label") val rolLabel: String? = null,
    val operational: OperationalSummary? = null,
    val kpis: List<AnalyticsKpi> = emptyList(),
    @SerializedName("ultima_actualizacion") val ultimaActualizacion: String? = null,
    val secciones: AnalyticsSecciones = AnalyticsSecciones(),
    @SerializedName("diagnostico_insights") val diagnosticoInsights: List<AnalyticsInsightItem> = emptyList(),
    @SerializedName("spark_flow") val sparkFlow: List<SparkFlowStep> = emptyList()
)

// ── Reportes (reports, admin + ingeniero solo lectura) ───────────────────
data class ReportManifest(val types: List<String> = emptyList(), val formats: List<String> = emptyList())
data class ReportData(
    val title: String? = null,
    val headings: List<String> = emptyList(),
    val keys: List<String> = emptyList(),
    val rows: List<Map<String, JsonElement>> = emptyList()
)

// ── Configuración de la barbería (settings, admin-only) ──────────────────
data class RedesSociales(val instagram: String? = null, val facebook: String? = null, val tiktok: String? = null)
data class DatosBancarios(val clabe: String? = null, val banco: String? = null, val beneficiario: String? = null, val concepto: String? = null)
data class BarbershopSetting(
    val id: String? = null,
    val nombre: String = "",
    val direccion: String? = null,
    val telefono: String? = null,
    @SerializedName("horario_apertura") val horarioApertura: String? = null,
    @SerializedName("horario_cierre") val horarioCierre: String? = null,
    @SerializedName("politica_cancelacion") val politicaCancelacion: Int = 24,
    @SerializedName("deposito_no_show_umbral") val depositoNoShowUmbral: Int = 2,
    @SerializedName("deposito_no_show_porcentaje") val depositoNoShowPorcentaje: Int = 50,
    @SerializedName("maintenance_mode") val maintenanceMode: Boolean = false,
    @SerializedName("redes_sociales") val redesSociales: RedesSociales = RedesSociales(),
    @SerializedName("datos_bancarios") val datosBancarios: DatosBancarios = DatosBancarios()
)
data class BarbershopSettingResponse(val data: BarbershopSetting = BarbershopSetting())
data class UpdateSettingRequest(
    val nombre: String,
    val direccion: String?,
    val telefono: String?,
    @SerializedName("horario_apertura") val horarioApertura: String?,
    @SerializedName("horario_cierre") val horarioCierre: String?,
    @SerializedName("politica_cancelacion") val politicaCancelacion: Int,
    @SerializedName("deposito_no_show_umbral") val depositoNoShowUmbral: Int?,
    @SerializedName("deposito_no_show_porcentaje") val depositoNoShowPorcentaje: Int?,
    val instagram: String?,
    val facebook: String?,
    val tiktok: String?,
    val clabe: String?,
    val banco: String?,
    val beneficiario: String?,
    val concepto: String?
)
data class MaintenanceToggleResponse(val message: String? = null, val data: MaintenanceStatus = MaintenanceStatus())
data class MaintenanceStatus(@SerializedName("maintenance_mode") val maintenanceMode: Boolean = false)

// ── Agenda de barbero (barber/agenda) ────────────────────────────────────
data class AgendaRange(val start: String? = null, val end: String? = null, val label: String? = null)
data class AgendaStats(
    @SerializedName("completed_count") val completedCount: Int = 0,
    @SerializedName("income_total") val incomeTotal: Double = 0.0,
    val productivity: Int = 0,
    @SerializedName("total_period") val totalPeriod: Int = 0,
    @SerializedName("pending_period") val pendingPeriod: Int = 0,
    @SerializedName("confirmed_period") val confirmedPeriod: Int = 0,
    @SerializedName("in_process_period") val inProcessPeriod: Int = 0,
    @SerializedName("completed_period") val completedPeriod: Int = 0,
    @SerializedName("cancelled_period") val cancelledPeriod: Int = 0,
    @SerializedName("no_show_period") val noShowPeriod: Int = 0
)
data class BarberAgendaResponse(
    val data: List<AppointmentRow> = emptyList(),
    val period: String = "day",
    val estado: String? = null,
    val offset: Int = 0,
    val range: AgendaRange = AgendaRange(),
    val stats: AgendaStats = AgendaStats()
)

// ── Sorteos ──────────────────────────────────────────────────────────────
data class RaffleClientUser(val name: String? = null)
data class RaffleClient(val id: String? = null, val user: RaffleClientUser? = null)
data class RaffleRow(
    val id: String,
    val mes: String? = null,
    val premio: String? = null,
    @SerializedName("nivel_ganador") val nivelGanador: String? = null,
    @SerializedName("vence_en") val venceEn: String? = null,
    @SerializedName("reclamado_en") val reclamadoEn: String? = null,
    val client: RaffleClient? = null,
    @SerializedName("is_claimed") val isClaimed: Boolean = false,
    @SerializedName("is_expired") val isExpired: Boolean = false
)
data class RaffleStats(val total: Int = 0, val reclamados: Int = 0, val vigentes: Int = 0)
data class RaffleResponse(
    val data: List<RaffleRow> = emptyList(),
    val meta: JsonObject? = null,
    val stats: RaffleStats = RaffleStats()
)
