package com.example.skbt_up_gibdd_eyewitness.core.network

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface DeviceApi {
    @POST("api/v1/devices/register")
    suspend fun register(@Body request: RegisterDeviceRequest): RegisterDeviceResponse

    @GET("api/v1/devices/me/bans/active")
    suspend fun getActiveBan(): ActiveBanResponse
}

data class RegisterDeviceRequest(
    @SerializedName("fingerprint_hash") val fingerprintHash: String,
    @SerializedName("push_token") val pushToken: String?,
)

data class RegisterDeviceResponse(
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("role") val role: String?,
    @SerializedName("access_token") val accessToken: String,
)

data class ActiveBanResponse(val ban: BanResponse?)

data class BanResponse(
    @SerializedName("ban_id") val banId: String,
    @SerializedName("observer_device_id") val observerDeviceId: String,
    @SerializedName("issued_by_device_id") val issuedByDeviceId: String,
    @SerializedName("started_at") val startedAt: String,
    @SerializedName("ends_at") val endsAt: String?,
    @SerializedName("ban_number") val banNumber: Int,
    @SerializedName("is_active") val isActive: Boolean,
)
