package com.example.skbt_up_gibdd_eyewitness.core.network
import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.POST
interface DeviceApi {
    @POST("api/v1/devices/register") suspend fun register(@Body request: RegisterDeviceRequest): RegisterDeviceResponse
    @PUT("api/v1/devices/{deviceId}/push-token") suspend fun updatePushToken(@Path("deviceId") deviceId: String, @Body request: PushTokenUpdateRequest): PushTokenResponse
    @DELETE("api/v1/devices/{deviceId}/push-token") suspend fun deletePushToken(@Path("deviceId") deviceId: String)
    @GET("api/v1/witnesses/me/active-ban") suspend fun getActiveBan(@Query("requester_device_id") deviceId: String): ActiveBanResponse
}
data class RegisterDeviceRequest(@SerializedName("fingerprint_hash") val fingerprintHash: String, val type: String = "witness", val platform: String = "android", @SerializedName("app_version") val appVersion: String)
data class RegisterDeviceResponse(@SerializedName("device_id") val deviceId: String, @SerializedName("witness_id") val witnessId: String?, @SerializedName("chat_id") val chatId: String?, @SerializedName("access_token") val accessToken: String, @SerializedName("ban_level") val banLevel: Int?)
data class PushTokenUpdateRequest(val token: String)
data class PushTokenResponse(@SerializedName("device_id") val deviceId: String, val registered: Boolean, @SerializedName("updated_at") val updatedAt: String)
data class ActiveBanResponse(
    val active: Boolean,
    val id: String?,
    @SerializedName("ban_level") val banLevel: Int?,
    @SerializedName("issued_at") val issuedAt: String?,
    @SerializedName("expires_at") val expiresAt: String?,
    val reason: String?,
)
