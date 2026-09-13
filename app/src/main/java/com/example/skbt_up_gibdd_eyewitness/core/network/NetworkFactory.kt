package com.example.skbt_up_gibdd_eyewitness.core.network

import com.example.skbt_up_gibdd_eyewitness.domain.device.DeviceSession
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkFactory {
    fun createDeviceApi(
        baseUrl: String,
        sessionProvider: () -> DeviceSession?,
        enableHttpLogs: Boolean,
    ): DeviceApi {
        require(baseUrl.endsWith('/')) { "API base URL must end with /" }
        val client = OkHttpClient.Builder()
            .addInterceptor(eyewitnessHeaders(sessionProvider))
            .apply {
                if (enableHttpLogs) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                        redactHeader("Authorization")
                    })
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeviceApi::class.java)
    }

    fun createMessageApi(
        baseUrl: String,
        sessionProvider: () -> DeviceSession?,
        enableHttpLogs: Boolean,
    ): MessageApi {
        require(baseUrl.endsWith('/')) { "API base URL must end with /" }
        val client = OkHttpClient.Builder()
            .addInterceptor(eyewitnessHeaders(sessionProvider))
            .apply {
                if (enableHttpLogs) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                        redactHeader("Authorization")
                    })
                }
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MessageApi::class.java)
    }

    private fun eyewitnessHeaders(sessionProvider: () -> DeviceSession?) = Interceptor { chain ->
        val original = chain.request()
        val request = original.newBuilder()
            .header(CLIENT_APP_HEADER, EYEWITNESS_CLIENT)
            .apply {
                if (!original.url.encodedPath.endsWith(REGISTER_PATH)) {
                    sessionProvider()?.accessToken?.let { token -> header("Authorization", "Bearer $token") }
                }
            }
            .build()
        chain.proceed(request)
    }

    private const val CLIENT_APP_HEADER = "X-Client-App"
    private const val EYEWITNESS_CLIENT = "eyewitness"
    private const val REGISTER_PATH = "/api/v1/devices/register"
}
