package com.example.skbt_up_gibdd_eyewitness.domain.message

import android.net.Uri
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface MessageRepository {
    val cachedMessages: Flow<List<ChatMessage>> get() = flowOf(emptyList())
    val pendingTextMessages: StateFlow<List<PendingTextMessage>>
    val pendingStaticLocations: StateFlow<List<PendingStaticLocation>>
    val pendingMediaMessages: StateFlow<List<PendingMediaMessage>>
    val pendingMessageEvents: SharedFlow<PendingMessageEvent>
    fun currentDeviceId(): String?
    fun currentAccessToken(): String?
    fun mediaDownloadUrl(messageId: String): String
    suspend fun sendText(text: String): Result<ChatMessage>
    fun enqueueText(text: String): PendingTextMessage
    fun retryPendingText(localId: String)
    suspend fun getOwnMessages(): Result<List<ChatMessage>>
    suspend fun loadOlderMessages(): Result<List<ChatMessage>>
    suspend fun clearLocalHistory(cutoffEpochMillis: Long = System.currentTimeMillis())
    suspend fun markDelivered(messageId: String): Result<ChatMessage>
    suspend fun sendStaticLocation(latitude: Double, longitude: Double): Result<ChatMessage>
    fun enqueueStaticLocation(latitude: Double, longitude: Double): PendingStaticLocation
    fun retryPendingStaticLocation(localId: String)
    suspend fun uploadMedia(uri: Uri, mimeType: String, sizeBytes: Long): Result<ChatMessage>
    suspend fun enqueueMedia(uri: Uri, mimeType: String, sizeBytes: Long): Result<PendingMediaMessage>
    fun retryPendingMedia(localId: String)
    suspend fun startLiveLocation(): Result<ChatMessage>
    suspend fun sendLiveLocationPoint(messageId: String, latitude: Double, longitude: Double): Result<Unit>
    suspend fun stopLiveLocation(messageId: String): Result<ChatMessage>
}
