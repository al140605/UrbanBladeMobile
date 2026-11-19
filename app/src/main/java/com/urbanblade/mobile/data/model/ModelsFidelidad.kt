package com.urbanblade.mobile.data.model

import com.google.gson.annotations.SerializedName


// ── Wallet: membresías (memberships/*, autoservicio del cliente) ─────────
data class MembershipPlan(
    val id: String,
    val nombre: String,
    val descripcion: String? = null,
    @SerializedName("precio_mensual") val precioMensual: Double = 0.0,
    @SerializedName("descuento_pct") val descuentoPct: Double = 0.0
)
data class MembershipPlansResponse(val data: List<MembershipPlan> = emptyList())
data class MyMembership(
    val id: String,
    val estado: String,
    @SerializedName("cancelar_al_finalizar") val cancelarAlFinalizar: Boolean = false,
    @SerializedName("periodo_actual_fin") val periodoActualFin: String? = null,
    val plan: MembershipPlan? = null
)
data class MyMembershipResponse(val data: MyMembership? = null)
/** GET ai/briefing: resumen del día para recepción y administración; source = "ia" | "reglas". */
data class AiBriefing(val text: String = "", val source: String = "reglas", @SerializedName("generated_at") val generatedAt: String? = null)
data class AiBriefingResponse(val data: AiBriefing? = null)
/** Servicio que Bladebot recomendó (campo suggested_service de chatbot/query), para reservarlo directo. */
data class SuggestedService(val id: String, val nombre: String, val precio: Double = 0.0, @SerializedName("duracion_min") val duracionMin: Int = 0)
/** POST memberships/subscribe: el client_secret confirma el primer cobro con Stripe. */
data class SubscribeMembershipRequest(@SerializedName("membership_plan_id") val membershipPlanId: String)
data class SubscribeMembershipData(@SerializedName("client_secret") val clientSecret: String? = null, val membership: MyMembership? = null)
data class SubscribeMembershipResponse(val data: SubscribeMembershipData? = null)
/** GET memberships/invoices: cobros mensuales de la membresía, para Mis facturas. */
data class MembershipInvoiceRow(
    val id: String,
    val plan: String? = null,
    val monto: Double = 0.0,
    @SerializedName("pagado_en") val pagadoEn: String? = null
)
data class MembershipInvoicesMeta(@SerializedName("total_pagado") val totalPagado: Double = 0.0)
data class MembershipInvoicesResponse(val data: List<MembershipInvoiceRow> = emptyList(), val meta: MembershipInvoicesMeta? = null)
data class MembershipReceiptData(@SerializedName("receipt_url") val receiptUrl: String? = null)
data class MembershipReceiptResponse(val data: MembershipReceiptData? = null)

// ── Wallet: paquetes (packages, packages/catalog) ─────────────────────────
data class PackageServiceRef(val id: String? = null, val nombre: String? = null)
data class MyPackage(
    val id: String,
    val nombre: String? = null,
    val service: PackageServiceRef? = null,
    @SerializedName("usos_totales") val usosTotales: Int = 0,
    @SerializedName("usos_restantes") val usosRestantes: Int = 0,
    @SerializedName("precio_pagado") val precioPagado: Double = 0.0,
    @SerializedName("metodo_pago") val metodoPago: String? = null,
    @SerializedName("comprado_en") val compradoEn: String? = null,
    @SerializedName("expira_en") val expiraEn: String? = null,
    val estado: String? = null
)
data class MyPackagesResponse(val data: List<MyPackage> = emptyList())
data class PackageCatalogItem(
    val id: String,
    val nombre: String,
    val service: PackageServiceRef? = null,
    @SerializedName("cantidad_usos") val cantidadUsos: Int = 0,
    val precio: Double = 0.0,
    @SerializedName("vigencia_dias") val vigenciaDias: Int = 0
)
data class PackageCatalogResponse(val data: List<PackageCatalogItem> = emptyList())

// ── Wallet: gift cards (gift-cards/mine) ──────────────────────────────────
data class GiftCard(
    val code: String,
    val saldo: Double = 0.0,
    @SerializedName("monto_inicial") val montoInicial: Double = 0.0,
    val estado: String? = null,
    @SerializedName("comprado_en") val compradoEn: String? = null
)
data class GiftCardsResponse(val data: List<GiftCard> = emptyList())

/** Venta de una gift card en efectivo por el personal (POST /gift-cards). */
data class CashGiftCardRequest(
    val monto: Double,
    @SerializedName("comprador_nombre") val compradorNombre: String? = null,
    @SerializedName("destinatario_email") val destinatarioEmail: String? = null
)
data class GiftCardSold(val code: String, val saldo: Double = 0.0)
data class GiftCardSaleResponse(val message: String? = null, val data: GiftCardSold? = null)
data class GiftCardLookupResponse(val data: GiftCard? = null)

// ── Wallet: referidos (referrals/mine) ────────────────────────────────────
data class ReferralEntry(
    val referido: String? = null,
    val estado: String? = null,
    @SerializedName("recompensa_otorgada_en") val recompensaOtorgadaEn: String? = null
)
data class ReferralInfo(
    @SerializedName("codigo_referido") val codigoReferido: String? = null,
    @SerializedName("puntos_por_referido") val puntosPorReferido: Int = 0,
    val referidos: List<ReferralEntry> = emptyList(),
    val completados: Int = 0
)
data class ReferralInfoResponse(val data: ReferralInfo = ReferralInfo())

// ── Wallet: lealtad (embebida en GET dashboard, rol cliente -- ver
// DashboardService::clientMetrics(); no existe endpoint propio) ───────────
data class LoyaltyTransaction(val descripcion: String? = null, val puntos: Int = 0)
data class ClientLoyalty(
    val nivel: String? = null,
    val nivelLabel: String? = null,
    val puntos: Int = 0,
    val discountPct: Double = 0.0,
    val nextNivel: String? = null,
    val nextNivelLabel: String? = null,
    val citasFaltan: Int = 0,
    val progressPct: Double = 0.0,
    val recentTransactions: List<LoyaltyTransaction> = emptyList(),
    // Aditivos (barber, DashboardController::clientPayload): null o vacíos hasta que el backend
    // desplegado los mande; la Wallet oculta lo que no llegue.
    val nextDiscountPct: Int? = null,
    val levels: List<LoyaltyLevel> = emptyList(),
    val earnRules: List<LoyaltyEarnRule> = emptyList(),
    val maxRedeemPct: Int? = null,
    val wonRaffle: WonRaffle? = null
)
data class LoyaltyLevel(val nivel: String, val label: String, val citas: Int = 0, val discountPct: Int = 0)
data class LoyaltyEarnRule(val descripcion: String, val puntos: Int = 0)
data class WonRaffle(val mes: String? = null, val premio: String? = null, val isExpired: Boolean = false, val venceEn: String? = null)
