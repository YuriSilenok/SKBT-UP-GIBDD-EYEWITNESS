package com.example.skbt_up_gibdd_eyewitness.data.message

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import com.example.skbt_up_gibdd_eyewitness.core.network.CreateMessageRequest
import com.example.skbt_up_gibdd_eyewitness.core.network.LocationPointRequest
import com.example.skbt_up_gibdd_eyewitness.core.network.LiveLocationStartRequest
import com.example.skbt_up_gibdd_eyewitness.core.network.LocationFinishRequest
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
import okhttp3.RequestBody.Companion.toRequestBody
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
    private var olderMessagesCursor: String? = null
    private var paginationInitialized = false
    private var loadingOlderMessages = false
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
        "${baseUrl}api/v1/media/$messageId?requester_device_id=${currentDeviceId().orEmpty()}"

    override suspend fun sendText(text: String): Result<ChatMessage> = runCatching {
        val session = requireNotNull(storage.readSession()) { "Устройство не зарегистрировано" }
        api.sendText(session.chatId, CreateMessageRequest(session.deviceId, text))
            .toDomain()
            .withSubmittedText(text)
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
        val session = requireNotNull(storage.readSession()) { "Устройство не зарегистрировано" }
        val page = api.getMessages(session.chatId, session.deviceId, limit = PAGE_SIZE)
        val initializePagination = !paginationInitialized
        if (initializePagination) {
            olderMessagesCursor = page.nextBefore
            paginationInitialized = true
        }
        mapAndCacheMessages(page.items, session.deviceId, replaceCache = initializePagination)
    }

    override suspend fun loadOlderMessages(): Result<List<ChatMessage>> = runCatching {
        val session = requireNotNull(storage.readSession()) { "Устройство не зарегистрировано" }
        val cursor = olderMessagesCursor ?: return@runCatching emptyList()
        check(!loadingOlderMessages) { "Предыдущая страница уже загружается" }
        loadingOlderMessages = true
        try {
            val page = api.getMessages(session.chatId, session.deviceId, limit = PAGE_SIZE, before = cursor)
            olderMessagesCursor = page.nextBefore
            mapAndCacheMessages(page.items, session.deviceId)
        } finally {
            loadingOlderMessages = false
        }
    }

    private suspend fun mapAndCacheMessages(
        responses: List<MessageResponse>,
        deviceId: String,
        replaceCache: Boolean = false,
    ): List<ChatMessage> {
        val historyCutoff = storage.readHistoryCutoff()
        return responses.filter { response ->
            runCatching { OffsetDateTime.parse(response.sentAt).toInstant().toEpochMilli() > historyCutoff }
                .getOrDefault(true)
        }.map { response ->
            val location = response.locationSessionId?.let { id ->
                runCatching { api.getLocationSession(id, deviceId) }.getOrNull()
            }
            val mediaMimeType = response.attachmentId?.let { attachmentId ->
                runCatching {
                    val mediaResponse = api.getMediaMetadata(attachmentId, deviceId)
                    val body = mediaResponse.body()
                    val rawMimeType = body?.contentType()?.toString()
                        ?: mediaResponse.headers()["Content-Type"]
                    runCatching { body?.close() }
                    rawMimeType
                }.getOrNull().normalizedMediaMimeType()
            }
            val point = location?.points?.firstOrNull()
            response.toDomain(
                mediaMimeTypeOverride = mediaMimeType,
                staticLatitudeOverride = point?.latitude,
                staticLongitudeOverride = point?.longitude,
                liveEndsAtOverride = location?.expiresAt,
            )
        }.also { messages ->
            val entities = messages.map { it.toEntity() }
            if (replaceCache) messageDao.replaceAll(entities) else messageDao.upsertAll(entities)
        }
    }

    override suspend fun clearLocalHistory() {
        storage.saveHistoryCutoff(System.currentTimeMillis())
        olderMessagesCursor = null
        paginationInitialized = false
        retryJobs.values.forEach(Job::cancel)
        retryJobs.clear()
        mutablePendingMediaMessages.value.forEach { runCatching { File(it.localPath).delete() } }
        updatePending(emptyList())
        updatePendingStaticLocations(emptyList())
        updatePendingMedia(emptyList())
        messageDao.deleteAll()
    }

    override suspend fun markDelivered(messageId: String): Result<ChatMessage> = runCatching {
        val session = requireNotNull(storage.readSession()) { "Устройство не зарегистрировано" }
        api.markRead(session.chatId, messageId, session.deviceId).toDomain().also { messageDao.upsert(it.toEntity()) }
    }

    override suspend fun sendStaticLocation(latitude: Double, longitude: Double): Result<ChatMessage> = runCatching {
        val session = requireNotNull(storage.readSession()) { "Устройство не зарегистрировано" }
        api.sendStaticLocation(session.chatId, LocationPointRequest(session.deviceId, latitude, longitude)).let { response ->
            val point = response.session.points.firstOrNull()
            response.message.toDomain(
                staticLatitudeOverride = point?.latitude ?: latitude,
                staticLongitudeOverride = point?.longitude ?: longitude,
            )
        }
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
        uploadMediaPart(part)
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
        val session = requireNotNull(storage.readSession()) { "Устройство не зарегистрировано" }
        api.startLiveLocation(session.chatId, LiveLocationStartRequest(session.deviceId)).let { response ->
            response.message.toDomain(idOverride = response.session.id, liveEndsAtOverride = response.session.expiresAt)
        }.also { messageDao.upsert(it.toEntity()) }
    }

    override suspend fun sendLiveLocationPoint(
        messageId: String,
        latitude: Double,
        longitude: Double,
    ): Result<Unit> = runCatching {
        val session = requireNotNull(storage.readSession()) { "Устройство не зарегистрировано" }
        api.sendLiveLocationPoint(messageId, LocationPointRequest(session.deviceId, latitude, longitude))
        Unit
    }

    override suspend fun stopLiveLocation(messageId: String): Result<ChatMessage> = runCatching {
        val session = requireNotNull(storage.readSession()) { "Устройство не зарегистрировано" }
        api.stopLiveLocation(messageId, LocationFinishRequest(session.deviceId))
        ChatMessage(messageId, session.chatId, session.deviceId, "", "geolocation", createdAt = OffsetDateTime.now().toString(), deliveredAt = null)
    }

    private fun MessageResponse.toDomain(
        idOverride: String? = null,
        mediaStorageKeyOverride: String? = null,
        mediaMimeTypeOverride: String? = null,
        liveEndsAtOverride: String? = null,
        staticLatitudeOverride: Double? = null,
        staticLongitudeOverride: Double? = null,
    ) = ChatMessage(
        id = idOverride ?: id,
        observerDeviceId = chatId,
        senderDeviceId = senderDeviceId,
        text = text.orEmpty(),
        type = messageType,
        staticLatitude = staticLatitudeOverride,
        staticLongitude = staticLongitudeOverride,
        mediaStorageKey = mediaStorageKeyOverride ?: attachmentId,
        mediaMimeType = mediaMimeTypeOverride,
        liveEndsAt = liveEndsAtOverride,
        createdAt = sentAt,
        deliveredAt = readAt,
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
                        mutablePendingMessageEvents.emit(
                            PendingMessageEvent.Sent(localId, PendingMessageKind.TEXT, sentMessage),
                        )
                        updatePending(mutablePendingTextMessages.value.filterNot { it.localId == localId })
                        messageDao.upsert(sentMessage.toEntity())
                        getOwnMessages()
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
                        mutablePendingMessageEvents.emit(
                            PendingMessageEvent.Sent(localId, PendingMessageKind.STATIC_LOCATION, sentMessage),
                        )
                        updatePendingStaticLocations(
                            mutablePendingStaticLocations.value.filterNot { it.localId == localId },
                        )
                        messageDao.upsert(sentMessage.toEntity())
                        getOwnMessages()
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
                        mutablePendingMessageEvents.emit(
                            PendingMessageEvent.Sent(localId, PendingMessageKind.MEDIA, sentMessage),
                        )
                        updatePendingMedia(mutablePendingMediaMessages.value.filterNot { it.localId == localId })
                        messageDao.upsert(sentMessage.toEntity())
                        getOwnMessages()
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
        uploadMediaPart(part)
    }

    private suspend fun uploadMediaPart(part: MultipartBody.Part): ChatMessage {
        val session = requireNotNull(storage.readSession()) { "Устройство не зарегистрировано" }
        return api.uploadMedia(
            session.chatId,
            session.deviceId.toRequestBody("text/plain".toMediaType()),
            part,
        ).let { response ->
            response.message.toDomain(
                mediaStorageKeyOverride = response.attachment.id,
                mediaMimeTypeOverride = response.attachment.mimeType,
            )
        }
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
        const val PAGE_SIZE = 50
        const val RETRY_INTERVAL_MILLIS = 5_000L
    }
}

internal fun ChatMessage.withSubmittedText(submittedText: String): ChatMessage = copy(
    text = text.ifBlank { submittedText },
    type = type.ifBlank { "TEXT" },
)

internal fun String?.normalizedMediaMimeType(): String? = this
    ?.substringBefore(';')
    ?.trim()
    ?.lowercase()
    ?.takeIf { it.startsWith("image/") || it.startsWith("video/") }

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
