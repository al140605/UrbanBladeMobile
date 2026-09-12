package com.urbanblade.mobile.core.network

import com.google.gson.JsonObject
import com.urbanblade.mobile.data.model.*
import retrofit2.http.*

interface UrbanBladeApi {
    @POST("auth/login") suspend fun login(@Body body: LoginRequest): LoginResponse
    @POST("auth/register") suspend fun register(@Body body: RegisterRequest): LoginResponse
    @POST("auth/forgot-password") suspend fun forgotPassword(@Body body: ForgotPasswordRequest): MessageResponse
    @POST("auth/google/token") suspend fun googleLogin(@Body body: GoogleLoginRequest): LoginResponse
    @GET("auth/me") suspend fun me(): MeResponse
    @POST("auth/logout") suspend fun logout(): MessageResponse?
    @POST("auth/refresh-token") suspend fun refreshToken(): LoginResponse

    @GET("dashboard") suspend fun dashboard(): DashboardResponse
    @GET("analytics") suspend fun analytics(): AnalyticsResponse

    @GET("barbershop") suspend fun barbershop(): JsonObject
    @GET("services") suspend fun services(): ApiList<ServiceItem>
    @GET("barbers") suspend fun barbers(): ApiList<BarberItem>
    @GET("products") suspend fun products(@Query("q") q: String? = null, @Query("categoria") categoria: String? = null): ApiList<ProductItem>
    @GET("barbers/{barber}") suspend fun barberDetail(@Path("barber") barber: String): JsonObject

    @GET("availability/slots")
    suspend fun slots(@Query("barber_id") barberId: String, @Query("service_id") serviceId: String, @Query("date") date: String): SlotsResponse

    @GET("appointments") suspend fun appointments(): AppointmentsResponse
    @POST("appointments") suspend fun createAppointment(@Body body: AppointmentRequest): MessageResponse
    @PUT("appointments/{code}") suspend fun updateAppointment(@Path("code") code: String, @Body body: AppointmentRequest): MessageResponse
    @PATCH("appointments/{code}/status") suspend fun updateAppointmentStatus(@Path("code") code: String, @Body body: JsonObject): MessageResponse
    @DELETE("appointments/{code}") suspend fun cancelAppointment(@Path("code") code: String): MessageResponse

    @GET("orders") suspend fun orders(): OrdersResponse
    @POST("orders") suspend fun createOrder(@Body body: OrderRequest): OrderMutationResponse
    @PATCH("orders/{id}/cancel") suspend fun cancelOrder(@Path("id") id: String): OrderMutationResponse
    @PATCH("orders/{id}/deliver") suspend fun deliverOrder(@Path("id") id: String, @Body body: DeliverOrderRequest): OrderMutationResponse

    @GET("payments") suspend fun payments(): PaymentsResponse
    @GET("payments/pending") suspend fun pendingPayments(): PendingPaymentsResponse
    @POST("payments/{id}/approve") suspend fun approvePayment(@Path("id") id: String): MessageResponse
    @POST("payments/{id}/reject") suspend fun rejectPayment(@Path("id") id: String, @Body body: RejectPaymentRequest): MessageResponse
    @POST("payments/stripe-intent") suspend fun stripeIntent(@Body body: StripeIntentRequest): JsonObject

    @GET("profile") suspend fun profile(): ProfileResponse
    @PUT("profile") suspend fun updateProfile(@Body body: UpdateProfileRequest): UpdateProfileResponse
    @PUT("profile/password") suspend fun updatePassword(@Body body: ChangePasswordRequest): MessageResponse

    @GET("notifications") suspend fun notifications(): JsonObject
    @POST("notifications/read-all") suspend fun markNotificationsRead(): MessageResponse
    @POST("notifications/{id}/read") suspend fun markNotificationRead(@Path("id") id: String): MessageResponse
    @DELETE("notifications/{id}") suspend fun deleteNotification(@Path("id") id: String): MessageResponse

    @GET("social/feed") suspend fun socialFeed(): JsonObject
    @POST("social/work/{id}/react") suspend fun reactWork(@Path("id") id: String): JsonObject
    @POST("social/work/{id}/save") suspend fun saveWork(@Path("id") id: String): JsonObject
    @POST("social/work/{id}/comment") suspend fun commentWork(@Path("id") id: String, @Body body: JsonObject): JsonObject

    @POST("chatbot/query") suspend fun chatbot(@Body body: JsonObject): JsonObject
    @GET("chatbot/history") suspend fun chatbotHistory(): JsonObject
    @POST("chatbot/clear-history") suspend fun clearChatbot(): MessageResponse

    @GET("barber/agenda") suspend fun barberAgenda(@QueryMap query: Map<String, String> = emptyMap()): BarberAgendaResponse
    @GET("barber/me") suspend fun barberMe(): JsonObject
    @GET("barber/portfolio") suspend fun barberPortfolio(): JsonObject
    @GET("barber/schedule") suspend fun barberSchedule(): BarberScheduleResponse
    @PUT("barber/schedule") suspend fun updateBarberSchedule(@Body body: UpdateBarberScheduleRequest): BarberScheduleResponse

    @GET("clients") suspend fun clients(): JsonObject
    @GET("inventory/products") suspend fun inventoryProducts(): InventoryResponse
    @POST("inventory/products") suspend fun createProduct(@Body body: CreateProductRequest): JsonObject
    @PUT("inventory/products/{id}") suspend fun updateProduct(@Path("id") id: String, @Body body: UpdateProductRequest): JsonObject
    @DELETE("inventory/products/{id}") suspend fun deleteProduct(@Path("id") id: String): MessageResponse
    @POST("inventory/movements") suspend fun registerMovement(@Body body: RegisterMovementRequest): JsonObject
    @GET("inventory/low-stock") suspend fun lowStock(): JsonObject
    @GET("inventory/movements") suspend fun inventoryMovements(): JsonObject
    @GET("cash-closes") suspend fun cashCloses(): JsonObject
    @GET("cash-closes/preview") suspend fun cashClosePreview(): CashClosePreviewResponse
    @POST("cash-closes") suspend fun registerCashClose(@Body body: RegisterCashCloseRequest): JsonObject

    @GET("reports") suspend fun reports(): ReportManifest
    @GET("reports/{type}/{format}") suspend fun exportReport(
        @Path("type") type: String,
        @Path("format") format: String,
        @QueryMap query: Map<String, String> = emptyMap()
    ): ReportData
    @GET("logs") suspend fun logs(@QueryMap query: Map<String, String> = emptyMap()): LogsResponse
    @GET("campaigns") suspend fun campaigns(): JsonObject
    @GET("raffles") suspend fun raffles(): RaffleResponse
    @GET("reviews") suspend fun reviews(@QueryMap query: Map<String, String> = emptyMap()): ReviewsResponse
    @GET("users") suspend fun users(@QueryMap query: Map<String, String> = emptyMap()): SystemUsersResponse
    @POST("users") suspend fun createUser(@Body body: CreateUserRequest): SystemUserMutationResponse
    @PUT("users/{id}") suspend fun updateUser(@Path("id") id: String, @Body body: UpdateUserRequest): SystemUserMutationResponse
    @DELETE("users/{id}") suspend fun deleteUser(@Path("id") id: String): MessageResponse
    @GET("settings") suspend fun settings(): BarbershopSettingResponse
    @PUT("settings") suspend fun updateSettings(@Body body: UpdateSettingRequest): BarbershopSettingResponse
    @POST("settings/maintenance") suspend fun toggleMaintenance(): MaintenanceToggleResponse

    @GET("admin/dashboard/stats") suspend fun adminStats(): JsonObject
    @GET("admin/dashboard/appointments") suspend fun adminAppointments(): JsonObject
    @GET("admin/dashboard/revenue") suspend fun adminRevenue(): JsonObject
    @GET("admin/dashboard/alerts") suspend fun adminAlerts(): JsonObject
    @GET("admin/dashboard/metrics") suspend fun adminMetrics(): AdminMetricsResponse
    @GET("admin/predictions/insights") suspend fun predictionInsights(): JsonObject
    @GET("admin/system/status") suspend fun systemStatus(): SystemStatusResponse
    @GET("admin/clients") suspend fun adminClients(@QueryMap query: Map<String, String> = emptyMap()): ClientsResponse
    @GET("admin/clients/{id}") suspend fun adminClientDetail(@Path("id") id: String): ClientDetailResponse
    @POST("admin/clients") suspend fun createClient(@Body body: CreateClientRequest): ClientMutationResponse
    @PUT("admin/clients/{id}") suspend fun updateClient(@Path("id") id: String, @Body body: UpdateClientRequest): ClientMutationResponse
    @DELETE("admin/clients/{id}") suspend fun deleteClient(@Path("id") id: String): MessageResponse
    @GET("admin/inventory/summary") suspend fun adminInventorySummary(): JsonObject

    @GET suspend fun genericGet(@Url url: String, @QueryMap query: Map<String, String> = emptyMap()): JsonObject
    @POST suspend fun genericPost(@Url url: String, @Body body: JsonObject = JsonObject()): JsonObject
    @PUT suspend fun genericPut(@Url url: String, @Body body: JsonObject): JsonObject
    @PATCH suspend fun genericPatch(@Url url: String, @Body body: JsonObject): JsonObject
    @DELETE suspend fun genericDelete(@Url url: String): JsonObject
}
