package com.example.skbt_up_gibdd_eyewitness.data.message

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import com.example.skbt_up_gibdd_eyewitness.core.network.CreateMessageRequest
import com.example.skbt_up_gibdd_eyewitness.core.network.LocationPointRequest
import com.example.skbt_up_gibdd_eyewitness.core.network.MessageApi
import com.example.skbt_up_gibdd_eyewitness.core.network.MessageResponse
import com.example.skbt_up_gibdd_eyewitness.core.storage.SecureDeviceStorage
import com.example.skbt_up_gibdd_eyewitness.data.local.MessageDao
import com.example.skbt_up_gibdd_eyewitness.data.local.toDomain
import com.example.skbt_up_gibdd_eyewitness.data.local.toEntity
import com.example.skbt_up_gibdd_eyewitness.domain.message.ChatMessage
import com.example.skbt_up_gibdd_eyewitness.domain.message.MessageRepository
import com.example.skbt_up_gibdd_eyewitness.domain.message.PendingMessageEvent
import com.example.skbt_up_gibdd_eyewitness.domain.message.PendingMessageStatus
import com.example.skbt_up_gibdd_eyewitness.domain.message.PendingMediaMessage
import com.example.skbt_up_gibdd_eyewitness.domain.message.PendingMessageKind
import com.example.skbt_up_gibdd_eyewitness.domain.message.PendingStaticLocation
import com.example.skbt_up_gibdd_eyewitness.domain.message.PendingTextMessage
import com.example.skbt_up_gibdd_eyewitness.domain.message.MESSAGE_RETRY_WINDOW_MILLIS
import com.example.skbt_up_gibdd_eyewitness.domain.message.restoredPendingStatus
import com.example.skbt_up_gibdd_eyewitness.domain.message.shouldRetryMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException
import java.io.File
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class DefaultMessageRepository(
    private val api: MessageApi,
    private val storage: SecureDeviceStorage,
    private val contentResolver: ContentResolver,
    filesDir: File,
    private val baseUrl: String,
    private val messageDao: MessageDao,
) : MessageRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private val retryJobs = ConcurrentHashMap<String, Job>()
    private val mutablePendingTextMessages = MutableStateFlow(loadPendingTextMessages())
    override val pendingTextMessages = mutablePendingTextMessages.asStateFlow()
    private val mutablePendingStaticLocations = MutableStateFlow(loadPendingStaticLocations())
    override val pendingStaticLocations = mutablePendingStaticLocations.asStateFlow()
    private val pendingMediaDirectory = File(filesDir, "pending_media").apply { mkdirs() }
    private val mutablePendingMediaMessages = MutableStateFlow(loadPendingMediaMessages())
    override val pendingMediaMessages = mutablePendingMediaMessages.asStateFlow()
    private val mutablePendingMessageEvents = MutableSharedFlow<PendingMessageEvent>()
    override val pendingMessageEvents = mutablePendingMessageEvents.asSharedFlow()
    override val cachedMessages = messageDao.observeAll().map { messages -> messages.map { it.toDomain() } }

    init {
        mutablePendingTextMessages.value.forEach { message ->
            if (message.status == PendingMessageStatus.SENDING) startTextRetry(message.localId)
        }
        mutablePendingStaticLocations.value.forEach { location ->
            if (location.status == PendingMessageStatus.SENDING) startStaticLocationRetry(location.localId)
        }
        mutablePendingMediaMessages.value.forEach { media ->
            if (media.status == PendingMessageStatus.SENDING) startMediaRetry(media.localId)
        }
    }

    override fun currentDeviceId(): String? = storage.readSession()?.deviceId
    override fun currentAccessToken(): String? = storage.readSession()?.accessToken
    override fun mediaDownloadUrl(messageId: String): String =
        "${baseUrl}api/v1/messages/$messageId/media"

    override suspend fun sendText(text: String): Result<ChatMessage> = runCatching {
        api.sendText(CreateMessageRequest(text = text))
            .toDomain()
            .withSubmittedText(text)
            .also { messageDao.upsert(it.toEntity()) }
    }

    override fun enqueueText(text: String): PendingTextMessage {
        val now = System.currentTimeMillis()
        val message = PendingTextMessage(
            localId = UUID.randomUUID().toString(),
            text = text,
            createdAt = OffsetDateTime.now().toString(),
            retryUntilMillis = now + MESSAGE_RETRY_WINDOW_MILLIS,
            status = PendingMessageStatus.SENDING,
        )
        updatePending(mutablePendingTextMessages.value + message)
        startTextRetry(message.localId)
        return message
    }

    override fun retryPendingText(localId: String) {
        val now = System.currentTimeMillis()
        val updated = mutablePendingTextMessages.value.map { message ->
            if (message.localId == localId) {
                message.copy(retryUntilMillis = now + MESSAGE_RETRY_WINDOW_MILLIS, status = PendingMessageStatus.SENDING)
            } else message
        }
        updatePending(updated)
        startTextRetry(localId, restart = true)
    }

    override suspend fun getOwnMessages(): Result<List<ChatMessage>> = runCatching {
        val deviceId = requireNotNull(currentDeviceId()) { "Устройство не зарегистрировано" }
        api.getMessages(deviceId).messages.map { it.toDomain() }.also { messages ->
            messageDao.upsertAll(messages.map { it.toEntity() })
        }
    }

    override suspend fun markDelivered(messageId: String): Result<ChatMessage> = runCatching {
        api.markDelivered(messageId).toDomain().also { messageDao.upsert(it.toEntity()) }
    }

    override suspend fun sendStaticLocation(latitude: Double, longitude: Double): Result<ChatMessage> = runCatching {
        api.sendStaticLocation(LocationPointRequest(latitude, longitude)).toDomain()
            .also { messageDao.upsert(it.toEntity()) }
    }

    override fun enqueueStaticLocation(latitude: Double, longitude: Double): PendingStaticLocation {
        val now = System.currentTimeMillis()
        val location = PendingStaticLocation(
            localId = UUID.randomUUID().toString(),
            latitude = latitude,
            longitude = longitude,
            createdAt = OffsetDateTime.now().toString(),
            retryUntilMillis = now + MESSAGE_RETRY_WINDOW_MILLIS,
            status = PendingMessageStatus.SENDING,
        )
        updatePendingStaticLocations(mutablePendingStaticLocations.value + location)
        startStaticLocationRetry(location.localId)
        return location
    }

    override fun retryPendingStaticLocation(localId: String) {
        val now = System.currentTimeMillis()
        updatePendingStaticLocations(
            mutablePendingStaticLocations.value.map { location ->
                if (location.localId == localId) {
                    location.copy(
                        retryUntilMillis = now + MESSAGE_RETRY_WINDOW_MILLIS,
                        status = PendingMessageStatus.SENDING,
                    )
                } else location
            },
        )
        startStaticLocationRetry(localId, restart = true)
    }

    override suspend fun uploadMedia(uri: Uri, mimeType: String, sizeBytes: Long): Result<ChatMessage> = runCatching {
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
        val body = ContentUriRequestBody(contentResolver, uri, mimeType, sizeBytes)
        val part = MultipartBody.Part.createFormData("file", "upload.$extension", body)
        api.uploadMedia(part).toDomain().also { messageDao.upsert(it.toEntity()) }
    }

    override suspend fun enqueueMedia(
        uri: Uri,
        mimeType: String,
        sizeBytes: Long,
    ): Result<PendingMediaMessage> = runCatching {
        val now = System.currentTimeMillis()
        val localId = UUID.randomUUID().toString()
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
        val localFile = File(pendingMediaDirectory, "$localId.$extension")
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                localFile.outputStream().use(input::copyTo)
            } ?: throw IOException("Не удалось сохранить выбранный файл")
        } catch (error: Throwable) {
            localFile.delete()
            throw error
        }
        val actualSize = localFile.length()
        check(actualSize > 0L) { "Выбранный файл пуст" }
        val media = PendingMediaMessage(
            localId = localId,
            localPath = localFile.absolutePath,
            mimeType = mimeType,
            sizeBytes = actualSize,
            createdAt = OffsetDateTime.now().toString(),
            retryUntilMillis = now + MESSAGE_RETRY_WINDOW_MILLIS,
            status = PendingMessageStatus.SENDING,
        )
        updatePendingMedia(mutablePendingMediaMessages.value + media)
        startMediaRetry(media.localId)
        media
    }

    override fun retryPendingMedia(localId: String) {
        val now = System.currentTimeMillis()
        updatePendingMedia(
            mutablePendingMediaMessages.value.map { media ->
                if (media.localId == localId) {
                    media.copy(retryUntilMillis = now + MESSAGE_RETRY_WINDOW_MILLIS, status = PendingMessageStatus.SENDING)
                } else media
            },
        )
        startMediaRetry(localId, restart = true)
    }

    override suspend fun startLiveLocation(): Result<ChatMessage> = runCatching {
        api.startLiveLocation().toDomain().also { messageDao.upsert(it.toEntity()) }
    }

    override suspend fun sendLiveLocationPoint(
        messageId: String,
        latitude: Double,
        longitude: Double,
    ): Result<Unit> = runCatching {
        api.sendLiveLocationPoint(messageId, LocationPointRequest(latitude, longitude))
        Unit
    }

    override suspend fun stopLiveLocation(messageId: String): Result<ChatMessage> = runCatching {
        api.stopLiveLocation(messageId).toDomain().also { messageDao.upsert(it.toEntity()) }
    }

    private fun MessageResponse.toDomain() = ChatMessage(
        id = messageId,
        observerDeviceId = observerDeviceId,
        senderDeviceId = senderDeviceId,
        text = text.orEmpty(),
        type = messageType,
        staticLatitude = staticLocation?.latitude,
        staticLongitude = staticLocation?.longitude,
        mediaStorageKey = media?.storageKey,
        mediaMimeType = media?.mimeType,
        liveEndsAt = liveLocation?.endsAt,
        createdAt = createdAt,
        deliveredAt = deliveredAt,
    )

    private fun startTextRetry(localId: String, restart: Boolean = false) {
        if (restart) retryJobs.remove(localId)?.cancel()
        if (retryJobs[localId]?.isActive == true) return
        retryJobs[localId] = scope.launch {
            try {
                while (true) {
                    val pending = mutablePendingTextMessages.value.firstOrNull { it.localId == localId } ?: break
                    val result = sendText(pending.text)
                    if (result.isSuccess) {
                        val sentMessage = result.getOrThrow()
                        // Do not leave the UI waiting for the next WebSocket/polling cycle.
                        // The POST response can be shorter than the history representation,
                        // so synchronize Room before removing the optimistic pending item.
                        getOwnMessages()
                        mutablePendingMessageEvents.emit(PendingMessageEvent.Sent(PendingMessageKind.TEXT, sentMessage))
                        updatePending(mutablePendingTextMessages.value.filterNot { it.localId == localId })
                        break
                    }
                    if (!shouldRetryMessage(System.currentTimeMillis(), pending.retryUntilMillis)) {
                        updatePending(
                            mutablePendingTextMessages.value.map {
                                if (it.localId == localId) it.copy(status = PendingMessageStatus.FAILED) else it
                            },
                        )
                        mutablePendingMessageEvents.emit(PendingMessageEvent.Failed(PendingMessageKind.TEXT))
                        break
                    }
                    delay(RETRY_INTERVAL_MILLIS)
                }
            } finally {
                retryJobs.remove(localId)
            }
        }
    }

    private fun startStaticLocationRetry(localId: String, restart: Boolean = false) {
        val jobKey = "location-$localId"
        if (restart) retryJobs.remove(jobKey)?.cancel()
        if (retryJobs[jobKey]?.isActive == true) return
        retryJobs[jobKey] = scope.launch {
            try {
                while (true) {
                    val pending = mutablePendingStaticLocations.value
                        .firstOrNull { it.localId == localId } ?: break
                    val result = sendStaticLocation(pending.latitude, pending.longitude)
                    if (result.isSuccess) {
                        val sentMessage = result.getOrThrow()
                        // Keep the optimistic location visible until the confirmed
                        // server representation has been written to Room.
                        getOwnMessages()
                        mutablePendingMessageEvents.emit(
                            PendingMessageEvent.Sent(PendingMessageKind.STATIC_LOCATION, sentMessage),
                        )
                        updatePendingStaticLocations(
                            mutablePendingStaticLocations.value.filterNot { it.localId == localId },
                        )
                        break
                    }
                    if (!shouldRetryMessage(System.currentTimeMillis(), pending.retryUntilMillis)) {
                        updatePendingStaticLocations(
                            mutablePendingStaticLocations.value.map {
                                if (it.localId == localId) it.copy(status = PendingMessageStatus.FAILED) else it
                            },
                        )
                        mutablePendingMessageEvents.emit(PendingMessageEvent.Failed(PendingMessageKind.STATIC_LOCATION))
                        break
                    }
                    delay(RETRY_INTERVAL_MILLIS)
                }
            } finally {
                retryJobs.remove(jobKey)
            }
        }
    }

    private fun startMediaRetry(localId: String, restart: Boolean = false) {
        val jobKey = "media-$localId"
        if (restart) retryJobs.remove(jobKey)?.cancel()
        if (retryJobs[jobKey]?.isActive == true) return
        retryJobs[jobKey] = scope.launch {
            try {
                while (true) {
                    val pending = mutablePendingMediaMessages.value.firstOrNull { it.localId == localId } ?: break
                    val file = File(pending.localPath)
                    if (!file.isFile) {
                        markPendingMediaFailed(localId)
                        mutablePendingMessageEvents.emit(PendingMessageEvent.Failed(PendingMessageKind.MEDIA))
                        break
                    }
                    val result = uploadMediaFile(file, pending.mimeType)
                    if (result.isSuccess) {
                        val sentMessage = result.getOrThrow()
                        // Media upload responses may not contain the complete history
                        // representation, so refresh Room before removing the local file item.
                        getOwnMessages()
                        mutablePendingMessageEvents.emit(PendingMessageEvent.Sent(PendingMessageKind.MEDIA, sentMessage))
                        updatePendingMedia(mutablePendingMediaMessages.value.filterNot { it.localId == localId })
                        file.delete()
                        break
                    }
                    if (!shouldRetryMessage(System.currentTimeMillis(), pending.retryUntilMillis)) {
                        markPendingMediaFailed(localId)
                        mutablePendingMessageEvents.emit(PendingMessageEvent.Failed(PendingMessageKind.MEDIA))
                        break
                    }
                    delay(RETRY_INTERVAL_MILLIS)
                }
            } finally {
                retryJobs.remove(jobKey)
            }
        }
    }

    private suspend fun uploadMediaFile(file: File, mimeType: String): Result<ChatMessage> = runCatching {
        val body = FileRequestBody(file, mimeType)
        val part = MultipartBody.Part.createFormData("file", file.name, body)
        api.uploadMedia(part).toDomain().also { messageDao.upsert(it.toEntity()) }
    }

    private fun markPendingMediaFailed(localId: String) {
        updatePendingMedia(
            mutablePendingMediaMessages.value.map {
                if (it.localId == localId) it.copy(status = PendingMessageStatus.FAILED) else it
            },
        )
    }

    @Synchronized
    private fun updatePending(messages: List<PendingTextMessage>) {
        mutablePendingTextMessages.value = messages
        storage.savePendingTextMessages(gson.toJson(messages))
    }

    private fun loadPendingTextMessages(): List<PendingTextMessage> {
        val json = storage.readPendingTextMessages() ?: return emptyList()
        val type = object : TypeToken<List<PendingTextMessage>>() {}.type
        return runCatching { gson.fromJson<List<PendingTextMessage>>(json, type) }.getOrDefault(emptyList())
            .map { message ->
                message.copy(
                    status = restoredPendingStatus(
                        message.status,
                        System.currentTimeMillis(),
                        message.retryUntilMillis,
                    ),
                )
            }
    }

    @Synchronized
    private fun updatePendingStaticLocations(locations: List<PendingStaticLocation>) {
        mutablePendingStaticLocations.value = locations
        storage.savePendingStaticLocations(gson.toJson(locations))
    }

    private fun loadPendingStaticLocations(): List<PendingStaticLocation> {
        val json = storage.readPendingStaticLocations() ?: return emptyList()
        val type = object : TypeToken<List<PendingStaticLocation>>() {}.type
        return runCatching { gson.fromJson<List<PendingStaticLocation>>(json, type) }.getOrDefault(emptyList())
            .map { location ->
                location.copy(
                    status = restoredPendingStatus(
                        location.status,
                        System.currentTimeMillis(),
                        location.retryUntilMillis,
                    ),
                )
            }
    }

    @Synchronized
    private fun updatePendingMedia(messages: List<PendingMediaMessage>) {
        mutablePendingMediaMessages.value = messages
        storage.savePendingMediaMessages(gson.toJson(messages))
    }

    private fun loadPendingMediaMessages(): List<PendingMediaMessage> {
        val json = storage.readPendingMediaMessages() ?: return emptyList()
        val type = object : TypeToken<List<PendingMediaMessage>>() {}.type
        return runCatching { gson.fromJson<List<PendingMediaMessage>>(json, type) }.getOrDefault(emptyList())
            .map { media ->
                media.copy(
                    status = if (!File(media.localPath).isFile) PendingMessageStatus.FAILED else restoredPendingStatus(
                        media.status,
                        System.currentTimeMillis(),
                        media.retryUntilMillis,
                    ),
                )
            }
    }

    private companion object {
        const val RETRY_INTERVAL_MILLIS = 5_000L
    }
}

internal fun ChatMessage.withSubmittedText(submittedText: String): ChatMessage = copy(
    text = text.ifBlank { submittedText },
    type = type.ifBlank { "TEXT" },
)

private class ContentUriRequestBody(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
    private val mimeType: String,
    private val sizeBytes: Long,
) : RequestBody() {
    override fun contentType() = mimeType.toMediaType()
    override fun contentLength() = sizeBytes

    override fun writeTo(sink: BufferedSink) {
        val input = contentResolver.openInputStream(uri) ?: throw IOException("Не удалось открыть выбранный файл")
        input.use { source ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = source.read(buffer)
                if (count == -1) break
                sink.write(buffer, 0, count)
            }
        }
    }
}

private class FileRequestBody(
    private val file: File,
    private val mimeType: String,
) : RequestBody() {
    override fun contentType() = mimeType.toMediaType()
    override fun contentLength() = file.length()
    override fun writeTo(sink: BufferedSink) = file.inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count == -1) break
            sink.write(buffer, 0, count)
        }
    }
}
