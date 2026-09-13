package com.example.skbt_up_gibdd_eyewitness.core.network

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface MessageApi {
    @POST("api/v1/chats/{chatId}/messages") suspend fun sendText(@Path("chatId") chatId: String, @Body body: CreateMessageRequest): MessageResponse
    @GET("api/v1/chats/{chatId}/messages") suspend fun getMessages(@Path("chatId") chatId: String, @Query("requester_device_id") deviceId: String, @Query("limit") limit: Int = 100, @Query("before") before: String? = null): ChatMessagesResponse
    @PATCH("api/v1/chats/{chatId}/messages/{messageId}/read") suspend fun markRead(@Path("chatId") chatId: String, @Path("messageId") messageId: String, @Query("requester_device_id") deviceId: String): MessageResponse
    @POST("api/v1/chats/{chatId}/locations/static") suspend fun sendStaticLocation(@Path("chatId") chatId: String, @Body body: LocationPointRequest): LocationMessageResponse
    @Multipart @POST("api/v1/chats/{chatId}/media") suspend fun uploadMedia(@Path("chatId") chatId: String, @Part("sender_device_id") deviceId: RequestBody, @Part file: MultipartBody.Part): MediaMessageResponse
    @Streaming @GET("api/v1/media/{attachmentId}") suspend fun getMediaMetadata(@Path("attachmentId") attachmentId: String, @Query("requester_device_id") deviceId: String, @Header("Range") range: String = "bytes=0-0"): Response<ResponseBody>
    @POST("api/v1/chats/{chatId}/locations/live") suspend fun startLiveLocation(@Path("chatId") chatId: String, @Body body: LiveLocationStartRequest): LocationMessageResponse
    @POST("api/v1/location-sessions/{sessionId}/points") suspend fun sendLiveLocationPoint(@Path("sessionId") sessionId: String, @Body body: LocationPointRequest): LocationPointResponse
    @GET("api/v1/location-sessions/{sessionId}") suspend fun getLocationSession(@Path("sessionId") sessionId: String, @Query("requester_device_id") deviceId: String): LocationSessionResponse
    @PATCH("api/v1/location-sessions/{sessionId}/finish") suspend fun stopLiveLocation(@Path("sessionId") sessionId: String, @Body body: LocationFinishRequest): LocationSessionResponse
}

data class CreateMessageRequest(@SerializedName("sender_device_id") val senderDeviceId: String, val text: String)
data class ChatMessagesResponse(val items: List<MessageResponse>, @SerializedName("next_before") val nextBefore: String?)
data class MessageResponse(val id: String, @SerializedName("chat_id") val chatId: String, @SerializedName("sender_device_id") val senderDeviceId: String, @SerializedName("message_type") val messageType: String, val text: String?, @SerializedName("sent_at") val sentAt: String, @SerializedName("read_at") val readAt: String?, @SerializedName("attachment_id") val attachmentId: String?, @SerializedName("location_session_id") val locationSessionId: String?)
data class LocationPointRequest(@SerializedName("sender_device_id") val senderDeviceId: String, val latitude: Double, val longitude: Double)
data class LiveLocationStartRequest(@SerializedName("sender_device_id") val senderDeviceId: String, @SerializedName("duration_seconds") val durationSeconds: Int = 900)
data class LocationFinishRequest(@SerializedName("sender_device_id") val senderDeviceId: String)
data class LocationMessageResponse(val message: MessageResponse, val session: LocationSessionResponse)
data class MediaMessageResponse(val message: MessageResponse, val attachment: AttachmentResponse)
data class AttachmentResponse(val id: String, @SerializedName("mime_type") val mimeType: String)
data class LocationSessionResponse(val id: String, @SerializedName("message_id") val messageId: String, val status: String, @SerializedName("expires_at") val expiresAt: String?, val points: List<LocationPointResponse> = emptyList())
data class LocationPointResponse(val latitude: Double, val longitude: Double, @SerializedName("captured_at") val capturedAt: String)
