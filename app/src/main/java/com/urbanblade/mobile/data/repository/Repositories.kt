package com.urbanblade.mobile.data.repository

import com.urbanblade.mobile.core.network.UrbanBladeApi
import com.urbanblade.mobile.core.session.SessionManager
import com.urbanblade.mobile.data.model.*

class AuthRepository(
    private val api: UrbanBladeApi,
    private val session: SessionManager
) {
    suspend fun login(email: String, password: String): AuthUser {
        val response = api.login(LoginRequest(email, password))
        session.saveToken(response.token)
        return response.user
    }

    suspend fun register(name: String, email: String, password: String): AuthUser {
        val response = api.register(
            RegisterRequest(name, email, password, password)
        )
        session.saveToken(response.token)
        return response.user
    }

    suspend fun googleLogin(idToken: String): AuthUser {
        val response = api.googleLogin(GoogleLoginRequest(idToken))
        session.saveToken(response.token)
        return response.user
    }

    suspend fun forgotPassword(email: String): String {
        return api.forgotPassword(ForgotPasswordRequest(email)).message
            ?: "Si el correo existe, recibirás instrucciones."
    }

    suspend fun restoreSession(): AuthUser? {
        if (session.currentToken().isNullOrBlank()) return null
        return try {
            api.me().user
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 401) session.clear()
            null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun logout() {
        try { api.logout() } catch (_: Exception) { }
        session.clear()
    }
}

class UrbanRepository(private val api: UrbanBladeApi) {
    suspend fun dashboard() = api.dashboard()
    suspend fun services() = api.services().data
    suspend fun barbers() = api.barbers().data
    suspend fun slots(barberId: String, serviceId: String, date: String) =
        api.slots(barberId, serviceId, date).slots

    suspend fun appointments() = api.appointments()
    suspend fun createAppointment(body: AppointmentRequest) = api.createAppointment(body)
    suspend fun updateAppointment(code: String, body: AppointmentRequest) = api.updateAppointment(code, body)
    suspend fun cancelAppointment(code: String) = api.cancelAppointment(code)
    suspend fun profile() = api.profile().user
    suspend fun updateProfile(body: UpdateProfileRequest) = api.updateProfile(body)
    suspend fun products(query: String? = null) = api.products(query).data
    suspend fun orders() = api.orders()
    suspend fun createOrder(body: OrderRequest) = api.createOrder(body)
    suspend fun cancelOrder(id: String) = api.cancelOrder(id)
    suspend fun deliverOrder(id: String, method: String) = api.deliverOrder(id, DeliverOrderRequest(method))
    suspend fun payments() = api.payments()
    suspend fun pendingPayments() = api.pendingPayments()
    suspend fun approvePayment(id: String) = api.approvePayment(id)
    suspend fun rejectPayment(id: String, reason: String) = api.rejectPayment(id, RejectPaymentRequest(reason))
    suspend fun notifications() = api.notifications()
    suspend fun markAllNotificationsRead() = api.markNotificationsRead()
    suspend fun analytics() = api.analytics()
    suspend fun socialFeed() = api.socialFeed()
    suspend fun chatbot(message: String) = api.chatbot(com.google.gson.JsonObject().apply { addProperty("message", message) })
    suspend fun module(url: String, query: Map<String, String> = emptyMap()) = api.genericGet(url, query)

    suspend fun barberSchedule() = api.barberSchedule().schedules
    suspend fun updateBarberSchedule(schedules: List<BarberScheduleDay>) =
        api.updateBarberSchedule(UpdateBarberScheduleRequest(schedules)).schedules

    suspend fun cashClosePreview() = api.cashClosePreview().data
    suspend fun registerCashClose(efectivoContado: Double, notas: String?) =
        api.registerCashClose(RegisterCashCloseRequest(efectivoContado, notas))

    suspend fun adminMetrics() = api.adminMetrics().metrics
    suspend fun systemStatus() = api.systemStatus()
    suspend fun raffles() = api.raffles()

    suspend fun clients(search: String? = null, segment: String? = null): ClientsResponse {
        val query = buildMap {
            if (!search.isNullOrBlank()) put("search", search)
            if (!segment.isNullOrBlank()) put("segment", segment)
        }
        return api.adminClients(query)
    }
    suspend fun clientDetail(id: String) = api.adminClientDetail(id).data
    suspend fun createClient(name: String, email: String, telefono: String?, password: String) =
        api.createClient(CreateClientRequest(name, email, telefono, password))
    suspend fun updateClient(id: String, name: String?, email: String?, telefono: String?, notas: String?) =
        api.updateClient(id, UpdateClientRequest(name, email, telefono, notas))
    suspend fun deleteClient(id: String) = api.deleteClient(id)

    suspend fun inventoryProducts() = api.inventoryProducts()
    suspend fun createProduct(body: CreateProductRequest) = api.createProduct(body)
    suspend fun updateProduct(id: String, body: UpdateProductRequest) = api.updateProduct(id, body)
    suspend fun deleteProduct(id: String) = api.deleteProduct(id)
    suspend fun registerMovement(productId: String, tipo: String, cantidad: Int, motivo: String?) =
        api.registerMovement(RegisterMovementRequest(productId, tipo, cantidad, motivo))

    suspend fun reviews(barberId: String? = null, rating: Int? = null): ReviewsResponse {
        val query = buildMap {
            if (!barberId.isNullOrBlank()) put("barber_id", barberId)
            if (rating != null) put("rating", rating.toString())
        }
        return api.reviews(query)
    }

    suspend fun logs(search: String? = null, logName: String? = null, event: String? = null): LogsResponse {
        val query = buildMap {
            if (!search.isNullOrBlank()) put("q", search)
            if (!logName.isNullOrBlank()) put("log_name", logName)
            if (!event.isNullOrBlank()) put("event", event)
        }
        return api.logs(query)
    }

    suspend fun systemUsers(search: String? = null, role: String? = null): SystemUsersResponse {
        val query = buildMap {
            if (!search.isNullOrBlank()) put("q", search)
            if (!role.isNullOrBlank()) put("role", role)
        }
        return api.users(query)
    }
    suspend fun createUser(name: String, email: String, password: String, role: String) =
        api.createUser(CreateUserRequest(name, email, password, password, role))
    suspend fun updateUser(id: String, name: String, email: String, password: String?, role: String) =
        api.updateUser(id, UpdateUserRequest(name, email, password?.takeIf { it.isNotBlank() }, password?.takeIf { it.isNotBlank() }, role))
    suspend fun deleteUser(id: String) = api.deleteUser(id)

    suspend fun reportManifest() = api.reports()
    suspend fun exportReport(type: String, format: String, startDate: String? = null, endDate: String? = null): ReportData {
        val query = buildMap {
            if (!startDate.isNullOrBlank()) put("start_date", startDate)
            if (!endDate.isNullOrBlank()) put("end_date", endDate)
        }
        return api.exportReport(type, format, query)
    }

    suspend fun settings() = api.settings().data
    suspend fun updateSettings(body: UpdateSettingRequest) = api.updateSettings(body).data
    suspend fun toggleMaintenance() = api.toggleMaintenance()
}
