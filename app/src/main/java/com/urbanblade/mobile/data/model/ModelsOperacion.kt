package com.urbanblade.mobile.data.model

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName


// ── Horario de barbero ──────────────────────────────────────────────────
data class BarberScheduleDay(
    @SerializedName("day_of_week") val dayOfWeek: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    // Al guardar, el servidor devuelve los registros tal como los almacena (campo `is_working`); al consultar, ya
    // normalizados (`is_active`). Se aceptan los dos: antes, tras guardar, todos los días quedaban en descanso.
    @SerializedName(value = "is_active", alternate = ["is_working"]) val isActive: Boolean = true
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
    @SerializedName("avatar_url") val avatarUrl: String? = null,
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

// ── Logs de auditoría (administrador, solo lectura) ──────────────────
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
data class UsersMeta(
    @SerializedName("current_page") val currentPage: Int = 1,
    @SerializedName("last_page") val lastPage: Int = 1,
    val total: Int = 0
)
data class SystemUsersResponse(
    val data: List<SystemUserRow> = emptyList(),
    val meta: UsersMeta? = null,
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

// ── Reportes (reports, administrador, solo lectura) ───────────────────
data class ReportManifest(val types: List<String> = emptyList(), val formats: List<String> = emptyList())
/** Gráfica que el servidor calcula para cada reporte (misma que lleva el PDF). */
data class ReportChart(
    val title: String? = null,
    val labels: List<String> = emptyList(),
    val values: List<Double> = emptyList(),
    val unit: String? = null
)
data class ReportData(
    val title: String? = null,
    val headings: List<String> = emptyList(),
    val keys: List<String> = emptyList(),
    val rows: List<Map<String, JsonElement>> = emptyList(),
    val chart: ReportChart? = null
)
