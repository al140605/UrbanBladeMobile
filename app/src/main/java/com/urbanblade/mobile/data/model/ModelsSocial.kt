package com.urbanblade.mobile.data.model

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName


// ── Campañas (campaigns, admin-only) ─────────────────────────────────────
data class CampaignRow(
    val id: String,
    val titulo: String? = null,
    val cuerpo: String? = null,
    @SerializedName("cta_label") val ctaLabel: String? = null,
    @SerializedName("cta_url") val ctaUrl: String? = null,
    val segmento: String? = null,
    val destinatarios: Int = 0,
    val estado: String? = null,
    @SerializedName("programada_para") val programadaPara: String? = null,
    @SerializedName("enviada_en") val enviadaEn: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val opens: Int = 0,
    val clicks: Int = 0,
    @SerializedName("open_rate") val openRate: Double = 0.0,
    @SerializedName("click_rate") val clickRate: Double = 0.0
)
data class CampaignsResponse(
    val levels: Map<String, String> = emptyMap(),
    @SerializedName("segment_counts") val segmentCounts: Map<String, Int> = emptyMap(),
    val data: List<CampaignRow> = emptyList()
)
data class CreateCampaignRequest(
    val titulo: String,
    val cuerpo: String,
    @SerializedName("cta_label") val ctaLabel: String?,
    @SerializedName("cta_url") val ctaUrl: String?,
    val segmento: String,
    val modo: String,
    @SerializedName("programada_para") val programadaPara: String?
)

// ── Portafolio de barbero (barber/portfolio, barber/works) ───────────────
data class WorkMediaItem(val id: String? = null, val url: String? = null, val type: String? = null)
data class WorkRow(
    val id: String,
    val title: String? = null,
    val description: String? = null,
    val media: List<WorkMediaItem> = emptyList(),
    @SerializedName("reactions_count") val reactionsCount: Int = 0,
    @SerializedName("comments_count") val commentsCount: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null
)
data class PortfolioStats(
    @SerializedName("total_works") val totalWorks: Int = 0,
    @SerializedName("total_reactions") val totalReactions: Int = 0,
    @SerializedName("total_comments") val totalComments: Int = 0,
    @SerializedName("total_saves") val totalSaves: Int = 0
)
data class PortfolioResponse(val works: List<WorkRow> = emptyList(), val stats: PortfolioStats = PortfolioStats())
data class CreateWorkResponse(val message: String? = null, val work: WorkRow? = null)

// ── Insights IA (admin/predictions/insights) ─────────────────────────────
// generateInsights() en barber es cómputo local sobre datos ya cacheados
// (PredictionService::historicalData) -- a diferencia de sus endpoints
// hermanos (income/appointments/services), NO llama a Ollama, así que
// responde rápido pese al set_time_limit(120) defensivo del controlador.
data class RevenueInsight(val status: String? = null, val message: String? = null, @SerializedName("avg_daily") val avgDaily: Double? = null)
data class AppointmentsInsight(
    val status: String? = null,
    val message: String? = null,
    @SerializedName("avg_daily") val avgDaily: Double? = null,
    @SerializedName("trend_7d") val trend7d: Double? = null
)
data class ServiceConcentrationInsight(
    val status: String? = null,
    val message: String? = null,
    @SerializedName("top_service") val topService: String? = null,
    val percentage: Double? = null
)
data class InsightsData(
    val revenue: RevenueInsight? = null,
    val appointments: AppointmentsInsight? = null,
    @SerializedName("service_concentration") val serviceConcentration: ServiceConcentrationInsight? = null
)
data class InsightsResponse(val type: String? = null, val data: InsightsData = InsightsData())

// ── Muro social (social/feed) ────────────────────────────────────────────
data class SocialBarber(val id: String? = null, val name: String? = null, val slug: String? = null, val foto: String? = null)
data class SocialMedia(val url: String? = null, val type: String? = null)
data class SocialCommentUser(val name: String? = null)
data class SocialComment(
    val id: String? = null,
    val comment: String? = null,
    val user: SocialCommentUser? = null,
    @SerializedName("is_mine") val isMine: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null
)
data class SocialWork(
    val id: String,
    val title: String? = null,
    val description: String? = null,
    @SerializedName("work_date") val workDate: String? = null,
    val barber: SocialBarber? = null,
    val media: List<SocialMedia> = emptyList(),
    @SerializedName("reactions_count") val reactionsCount: Int = 0,
    @SerializedName("comments_count") val commentsCount: Int = 0,
    @SerializedName("saved_count") val savedCount: Int = 0,
    @SerializedName("is_reacted") val isReacted: Boolean = false,
    @SerializedName("is_saved") val isSaved: Boolean = false,
    val comments: List<SocialComment> = emptyList()
)
data class SocialFeedResponse(val data: List<SocialWork> = emptyList(), val meta: JsonObject? = null)
data class SocialActionResponse(val status: String? = null, val count: Int? = null)
data class PostCommentRequest(val comment: String)
data class ReportCommentRequest(val reason: String, val details: String? = null)
data class SocialModerationResponse(val message: String? = null)
data class PostCommentResponse(val message: String? = null, val data: SocialComment? = null)

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

// ── Lista de espera (waitlist) ───────────────────────────────────────────
data class WaitlistRef(val id: String? = null, val name: String? = null)
data class WaitlistServiceRef(val id: String? = null, val nombre: String? = null)
data class WaitlistEntry(
    val id: String,
    val estado: String,
    val fecha: String? = null,
    @SerializedName("notificado_en") val notificadoEn: String? = null,
    val client: WaitlistRef? = null,
    val barber: WaitlistRef? = null,
    val service: WaitlistServiceRef? = null
)
data class WaitlistResponse(val data: List<WaitlistEntry> = emptyList())

// ── Historial de movimientos de inventario (GET /inventory/movements, personal) ──
data class MovementProduct(val id: String? = null, val nombre: String? = null)
data class MovementUser(val id: String? = null, val name: String? = null)
data class MovementAppointment(val id: String? = null, val fecha: String? = null, val client: String? = null)
data class InventoryMovementRow(
    val id: String,
    val tipo: String = "",
    val cantidad: Int = 0,
    val motivo: String? = null,
    val fecha: String? = null,
    val product: MovementProduct? = null,
    val user: MovementUser? = null,
    val appointment: MovementAppointment? = null
)
data class InventoryMovementStats(val total: Int = 0, val entradas: Int = 0, val salidas: Int = 0, val hoy: Int = 0)
data class InventoryMovementsMeta(
    @SerializedName("current_page") val currentPage: Int = 1,
    @SerializedName("last_page") val lastPage: Int = 1,
    val total: Int = 0,
    val stats: InventoryMovementStats = InventoryMovementStats()
)
data class InventoryMovementsResponse(
    val data: List<InventoryMovementRow> = emptyList(),
    val meta: InventoryMovementsMeta = InventoryMovementsMeta()
)
data class WaitlistJoinResponse(val message: String? = null, val data: WaitlistEntry? = null)
data class WaitlistRequest(
    @SerializedName("barber_id") val barberId: String,
    @SerializedName("service_id") val serviceId: String,
    val fecha: String
)
