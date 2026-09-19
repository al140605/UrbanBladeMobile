package com.urbanblade.mobile.core.network

import android.content.Context
import com.urbanblade.mobile.BuildConfig
import com.urbanblade.mobile.core.session.SessionEvents
import com.urbanblade.mobile.core.session.SessionManager
import com.urbanblade.mobile.data.repository.AuthRepository
import com.urbanblade.mobile.data.repository.UrbanRepository
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object AppContainer {
    lateinit var sessionManager: SessionManager
        private set
    lateinit var api: UrbanBladeApi
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var urbanRepository: UrbanRepository
        private set

    fun init(context: Context) {
        sessionManager = SessionManager(context.applicationContext)

        val authInterceptor = okhttp3.Interceptor { chain ->
            val token = runBlocking { sessionManager.currentToken() }
            val request = chain.request().newBuilder()
                .header("Accept", "application/json")
                .apply {
                    if (!token.isNullOrBlank()) {
                        header("Authorization", "Bearer $token")
                    }
                }
                .build()
            val response = chain.proceed(request)
            // Un 401 con token en una llamada que no es de acceso significa sesión vencida o cuenta
            // inexistente: se avisa para que la app cierre la sesión y vuelva al acceso. Las llamadas de
            // login/registro/recuperación devuelven 401 por credenciales malas y no cuentan.
            val path = request.url.encodedPath
            val isAccessCall = listOf("/auth/login", "/auth/register", "/auth/google", "/auth/forgot-password", "/auth/reset")
                .any { path.contains(it) }
            if (response.code == 401 && !token.isNullOrBlank() && !isAccessCall) {
                SessionEvents.notifyExpired()
            }
            response
        }

        val logging = okhttp3.logging.HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
            } else {
                okhttp3.logging.HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        api = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UrbanBladeApi::class.java)

        authRepository = AuthRepository(api, sessionManager)
        urbanRepository = UrbanRepository(api)
    }
}
