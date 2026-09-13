package com.example.skbt_up_gibdd_eyewitness.feature.chat

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.skbt_up_gibdd_eyewitness.R
import com.example.skbt_up_gibdd_eyewitness.feature.chat.media.MediaAttachment
import com.example.skbt_up_gibdd_eyewitness.feature.chat.media.MediaKind
import com.example.skbt_up_gibdd_eyewitness.feature.chat.media.displaySize
import com.example.skbt_up_gibdd_eyewitness.feature.chat.media.createCaptureUri
import com.example.skbt_up_gibdd_eyewitness.feature.chat.media.galleryPermissions
import com.example.skbt_up_gibdd_eyewitness.feature.chat.media.hasGalleryPermission
import com.example.skbt_up_gibdd_eyewitness.feature.chat.media.loadRecentPhotoUris
import com.example.skbt_up_gibdd_eyewitness.feature.chat.media.resolveMediaSelection
import com.example.skbt_up_gibdd_eyewitness.feature.location.StaticLocation
import com.example.skbt_up_gibdd_eyewitness.feature.location.LiveLocationService
import com.example.skbt_up_gibdd_eyewitness.feature.location.LiveLocationState
import com.example.skbt_up_gibdd_eyewitness.feature.location.LiveLocationTracker
import com.example.skbt_up_gibdd_eyewitness.feature.location.hasLocationPermission
import com.example.skbt_up_gibdd_eyewitness.feature.location.liveLocationPermissions
import com.example.skbt_up_gibdd_eyewitness.domain.message.ChatMessage
import com.example.skbt_up_gibdd_eyewitness.domain.message.MessageRepository
import com.example.skbt_up_gibdd_eyewitness.domain.message.PendingMessageEvent
import com.example.skbt_up_gibdd_eyewitness.domain.message.PendingMessageKind
import com.example.skbt_up_gibdd_eyewitness.domain.message.PendingMessageStatus
import com.example.skbt_up_gibdd_eyewitness.domain.device.ActiveBan
import com.example.skbt_up_gibdd_eyewitness.domain.device.DeviceRepository
import com.example.skbt_up_gibdd_eyewitness.domain.realtime.RealtimeConnectionState
import com.example.skbt_up_gibdd_eyewitness.domain.realtime.RealtimeRepository
import com.example.skbt_up_gibdd_eyewitness.feature.ban.BanCheckScreen
import com.example.skbt_up_gibdd_eyewitness.feature.ban.BanScreen
import com.example.skbt_up_gibdd_eyewitness.ui.components.AppTopBar
import com.example.skbt_up_gibdd_eyewitness.ui.theme.IncomingBubble
import com.example.skbt_up_gibdd_eyewitness.ui.theme.OutgoingBubble
import com.example.skbt_up_gibdd_eyewitness.ui.theme.SKBTUPGIBDDEYEWITNESSTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.coroutines.resume
import java.time.LocalTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import okhttp3.Headers

private data class PreviewMessage(
    val id: String,
    val text: String,
    val time: String,
    val outgoing: Boolean,
    val delivered: Boolean = true,
    val createdAt: String,
    val pendingId: String? = null,
    val pendingStatus: PendingMessageStatus? = null,
)
private data class StaticLocationMessage(
    val id: String,
    val location: StaticLocation,
    val createdAt: String,
    val delivered: Boolean,
    val pendingId: String? = null,
    val pendingStatus: PendingMessageStatus? = null,
)
private data class ConfirmedLiveLocationMessage(
    val id: String,
    val location: StaticLocation?,
    val endsAt: String,
    val createdAt: String,
    val delivered: Boolean,
)
private data class MediaMessage(
    val id: String,
    val attachment: MediaAttachment? = null,
    val mimeType: String,
    val remoteUrl: String? = null,
    val accessToken: String? = null,
    val createdAt: String,
    val delivered: Boolean,
    val pendingId: String? = null,
    val pendingStatus: PendingMessageStatus? = null,
)
private sealed interface ChatTimelineItem {
    val id: String
    val createdAt: String

    data class Text(val message: PreviewMessage) : ChatTimelineItem {
        override val id = "text-${message.id}"
        override val createdAt = message.createdAt
    }

    data class StaticLocation(val message: StaticLocationMessage) : ChatTimelineItem {
        override val id = "location-${message.id}"
        override val createdAt = message.createdAt
    }

    data class Media(val message: MediaMessage) : ChatTimelineItem {
        override val id = "media-${message.id}"
        override val createdAt = message.createdAt
    }

    data class LiveLocation(val message: ConfirmedLiveLocationMessage) : ChatTimelineItem {
        override val id = "live-${message.id}"
        override val createdAt = message.createdAt
    }
}
private enum class AttachmentTab { GALLERY, LOCATION }
private enum class LocationPermissionAction { CURRENT, LIVE }
private sealed interface BanUiState {
    data object Loading : BanUiState
    data object NotBanned : BanUiState
    data object Error : BanUiState
    data class Banned(val ban: ActiveBan) : BanUiState
}

private val previewMessages = listOf(
    PreviewMessage("preview-1", "Вижу автомобиль, водитель ведёт себя подозрительно", "14:22", true, createdAt = "2026-08-14T14:22:00Z"),
    PreviewMessage("preview-2", "Уточните государственный номер.", "14:23", false, createdAt = "2026-08-14T14:23:00Z"),
    PreviewMessage("preview-3", "А123БВ44", "14:24", true, createdAt = "2026-08-14T14:24:00Z"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    deviceRepository: DeviceRepository? = null,
    messageRepository: MessageRepository? = null,
    realtimeRepository: RealtimeRepository? = null,
    selectedStaticLocation: StaticLocation? = null,
    onStaticLocationConsumed: () -> Unit = {},
    onOpenStaticLocationPicker: () -> Unit = {},
    requestNotificationPermission: Boolean = true,
    requestGalleryPermission: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (
            requestNotificationPermission &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val attachmentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var attachmentSuggestionShown by rememberSaveable { mutableStateOf(false) }
    var showAttachmentSuggestion by rememberSaveable { mutableStateOf(false) }
    var attachmentInitialTab by remember { mutableStateOf(AttachmentTab.GALLERY) }
    var pendingSelection by remember { mutableStateOf<List<MediaAttachment>>(emptyList()) }
    var pendingCaptureUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var recentPhotoUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var galleryAccessGranted by remember { mutableStateOf(hasGalleryPermission(context)) }
    var locationPermissionAction by remember { mutableStateOf(LocationPermissionAction.LIVE) }
    val liveLocationState by LiveLocationTracker.state.collectAsState()
    val previewConnectionState = remember { MutableStateFlow(RealtimeConnectionState.CONNECTED) }
    val realtimeConnectionState by (realtimeRepository?.connectionState ?: previewConnectionState).collectAsState()
    val emptyPendingMessages = remember { MutableStateFlow(emptyList<com.example.skbt_up_gibdd_eyewitness.domain.message.PendingTextMessage>()) }
    val pendingTextMessages by (messageRepository?.pendingTextMessages ?: emptyPendingMessages).collectAsState()
    val emptyPendingLocations = remember { MutableStateFlow(emptyList<com.example.skbt_up_gibdd_eyewitness.domain.message.PendingStaticLocation>()) }
    val pendingStaticLocations by (messageRepository?.pendingStaticLocations ?: emptyPendingLocations).collectAsState()
    val emptyPendingMedia = remember { MutableStateFlow(emptyList<com.example.skbt_up_gibdd_eyewitness.domain.message.PendingMediaMessage>()) }
    val pendingMediaMessages by (messageRepository?.pendingMediaMessages ?: emptyPendingMedia).collectAsState()
    val emptyCachedMessages = remember { MutableStateFlow(emptyList<ChatMessage>()) }
    val cachedMessages by (messageRepository?.cachedMessages ?: emptyCachedMessages).collectAsState(initial = emptyList())
    val textMessages = remember(messageRepository) {
        mutableStateListOf<PreviewMessage>().apply {
            if (messageRepository == null) addAll(previewMessages)
        }
    }
    val localMediaMessages = remember { mutableStateListOf<MediaMessage>() }
    val staticLocationMessages = remember { mutableStateListOf<StaticLocationMessage>() }
    val confirmedLiveLocationMessages = remember { mutableStateListOf<ConfirmedLiveLocationMessage>() }
    val confirmedPendingIds = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    var isSending by remember { mutableStateOf(false) }
    var banUiState by remember(deviceRepository) {
        mutableStateOf<BanUiState>(if (deviceRepository == null) BanUiState.NotBanned else BanUiState.Loading)
    }
    var banCheckAttempt by remember { mutableIntStateOf(0) }
    var historyClearedForCurrentBan by remember { mutableStateOf(false) }
    fun mergeConfirmedMessages(messages: List<ChatMessage>) {
        val repository = messageRepository ?: return
        val deviceId = repository.currentDeviceId()
        val textUpdates = messages.filter { it.type.isTextMessageType() }.map { it.toPreview(deviceId) }
        textMessages.removeAll { current -> textUpdates.any { it.id == current.id } }
        textMessages.addAll(textUpdates)

        val locationUpdates = messages.filter { it.liveEndsAt == null }.mapNotNull { message ->
                val latitude = message.staticLatitude ?: return@mapNotNull null
                val longitude = message.staticLongitude ?: return@mapNotNull null
                StaticLocationMessage(
                    message.id,
                    StaticLocation(latitude, longitude),
                    message.createdAt,
                    message.deliveredAt != null,
                )
            }
        staticLocationMessages.removeAll { current -> locationUpdates.any { it.id == current.id } }
        staticLocationMessages.addAll(locationUpdates)

        val liveUpdates = messages.mapNotNull { message ->
            val endsAt = message.liveEndsAt ?: return@mapNotNull null
            ConfirmedLiveLocationMessage(
                id = message.id,
                location = if (message.staticLatitude != null && message.staticLongitude != null) {
                    StaticLocation(message.staticLatitude, message.staticLongitude)
                } else null,
                endsAt = endsAt,
                createdAt = message.createdAt,
                delivered = message.deliveredAt != null,
            )
        }
        confirmedLiveLocationMessages.removeAll { current -> liveUpdates.any { it.id == current.id } }
        confirmedLiveLocationMessages.addAll(liveUpdates)

        val mediaUpdates = messages.mapNotNull { message ->
                val mimeType = message.mediaMimeType ?: return@mapNotNull null
                MediaMessage(
                    id = message.id,
                    mimeType = mimeType,
                    remoteUrl = repository.mediaDownloadUrl(message.mediaStorageKey ?: message.id),
                    accessToken = repository.currentAccessToken(),
                    createdAt = message.createdAt,
                    delivered = message.deliveredAt != null,
                )
            }
        localMediaMessages.removeAll { current -> mediaUpdates.any { it.id == current.id } }
        localMediaMessages.addAll(mediaUpdates)
    }
    LaunchedEffect(cachedMessages, banUiState) {
        if (banUiState is BanUiState.NotBanned) {
            mergeConfirmedMessages(cachedMessages)
        } else if (banUiState is BanUiState.Banned) {
            textMessages.clear()
            localMediaMessages.clear()
            staticLocationMessages.clear()
            confirmedLiveLocationMessages.clear()
            confirmedPendingIds.clear()
        }
    }
    val submitStaticLocation: suspend (StaticLocation) -> Unit = { location ->
        if (messageRepository == null) {
            staticLocationMessages += StaticLocationMessage(
                "local-${System.currentTimeMillis()}",
                location,
                OffsetDateTime.now().toString(),
                false,
            )
            Toast.makeText(context, "Геолокация отправлена", Toast.LENGTH_SHORT).show()
        } else {
            messageRepository.enqueueStaticLocation(location.latitude, location.longitude)
        }
    }
    val sendTextMessage: () -> Unit = {
        val text = draft.trim()
        if (text.isNotEmpty() && !isSending) {
            if (messageRepository == null) {
                textMessages += PreviewMessage(
                    id = "local-${System.currentTimeMillis()}",
                    text = text,
                    time = LocalTime.now(MOSCOW_ZONE).format(DateTimeFormatter.ofPattern("HH:mm")),
                    outgoing = true,
                    delivered = false,
                    createdAt = OffsetDateTime.now().toString(),
                )
                draft = ""
                Toast.makeText(context, "Сообщение отправлено", Toast.LENGTH_SHORT).show()
            } else {
                messageRepository.enqueueText(text)
                draft = ""
            }
        }
    }
    val acceptMedia: (List<android.net.Uri>) -> Unit = { uris ->
        val result = resolveMediaSelection(context, uris)
        pendingSelection = (pendingSelection + result.accepted).distinctBy { it.uri }
        if (result.rejectionMessages.isNotEmpty()) {
            Toast.makeText(context, result.rejectionMessages.joinToString("\n"), Toast.LENGTH_LONG).show()
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10), acceptMedia)
    val refreshRecentPhotos: () -> Unit = {
        scope.launch {
            recentPhotoUris = withContext(Dispatchers.IO) { loadRecentPhotoUris(context, limit = 5) }
        }
    }
    val galleryPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        galleryAccessGranted = hasGalleryPermission(context)
        if (galleryAccessGranted) refreshRecentPhotos()
    }
    val liveLocationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (hasLocationPermission(context)) {
            when (locationPermissionAction) {
                LocationPermissionAction.LIVE -> {
                    LiveLocationService.start(context)
                    Toast.makeText(context, "Live-геолокация включена на 15 минут", Toast.LENGTH_SHORT).show()
                }
                LocationPermissionAction.CURRENT -> scope.launch {
                    currentStaticLocation(context)?.let { submitStaticLocation(it) }
                        ?: Toast.makeText(context, "Не удалось определить местоположение", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(context, "Нужен доступ к местоположению", Toast.LENGTH_LONG).show()
        }
    }
    val photoCapture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        pendingCaptureUri?.let { uri -> if (success) acceptMedia(listOf(uri)) }
        pendingCaptureUri = null
    }
    val videoCapture = rememberLauncherForActivityResult(ActivityResultContracts.CaptureVideo()) { success ->
        pendingCaptureUri?.let { uri -> if (success) acceptMedia(listOf(uri)) }
        pendingCaptureUri = null
    }

    LaunchedEffect(deviceRepository, messageRepository, realtimeRepository, banCheckAttempt) {
        val repository = messageRepository ?: return@LaunchedEffect
        suspend fun refreshBan() {
            val banRepository = deviceRepository ?: return
            banRepository.getActiveBan()
                .onSuccess { ban ->
                    if (ban != null && !historyClearedForCurrentBan) {
                        repository.clearLocalHistory(ban.startedAt.toEpochMilli())
                        textMessages.clear()
                        localMediaMessages.clear()
                        staticLocationMessages.clear()
                        confirmedLiveLocationMessages.clear()
                        confirmedPendingIds.clear()
                        historyClearedForCurrentBan = true
                    } else if (ban == null) {
                        historyClearedForCurrentBan = false
                    }
                    banUiState = ban?.let(BanUiState::Banned) ?: BanUiState.NotBanned
                }
                .onFailure {
                    if (banUiState is BanUiState.Loading) banUiState = BanUiState.Error
                }
        }
        suspend fun refreshMessages() {
            repository.getOwnMessages().onSuccess { messages ->
                val deviceId = repository.currentDeviceId()
                messages
                    .filter { it.senderDeviceId != deviceId && it.deliveredAt == null }
                    .forEach { repository.markDelivered(it.id) }
            }
        }

        coroutineScope {
            val realtimeEventsJob = realtimeRepository?.let { realtime ->
                launch {
                    var firstConnectionEvent = true
                    realtime.events.collect { event ->
                        if (event.name == "connected") {
                            if (firstConnectionEvent) {
                                firstConnectionEvent = false
                                return@collect
                            }
                            refreshBan()
                            if (banUiState !is BanUiState.Banned) refreshMessages()
                            return@collect
                        }
                        if (event.name in BAN_STATE_EVENTS) refreshBan()
                        if (
                            event.name != "live_location_point" &&
                            event.name != "location.point" &&
                            banUiState !is BanUiState.Banned
                        ) {
                            refreshMessages()
                        }
                    }
                }
            }
            realtimeRepository?.start()
            try {
                while (isActive) {
                    refreshBan()
                    if (banUiState !is BanUiState.Banned) refreshMessages()
                    val pollingDelay = if (
                        realtimeRepository?.connectionState?.value == RealtimeConnectionState.CONNECTED
                    ) {
                        30_000L
                    } else {
                        5_000L
                    }
                    delay(pollingDelay)
                }
            } finally {
                realtimeEventsJob?.cancel()
                realtimeRepository?.stop()
            }
        }
    }

    LaunchedEffect(messageRepository) {
        val repository = messageRepository ?: return@LaunchedEffect
        repository.pendingMessageEvents.collect { event ->
            when (event) {
                is PendingMessageEvent.Sent -> {
                    if (event.localId !in confirmedPendingIds) confirmedPendingIds += event.localId
                    val message = event.message
                    when (event.kind) {
                        PendingMessageKind.TEXT -> {
                            textMessages.removeAll { it.id == message.id }
                            textMessages += message.toPreview(repository.currentDeviceId())
                            if (!attachmentSuggestionShown) {
                                attachmentSuggestionShown = true
                                showAttachmentSuggestion = true
                            }
                        }
                        PendingMessageKind.STATIC_LOCATION -> {
                            val latitude = message.staticLatitude
                            val longitude = message.staticLongitude
                            if (latitude != null && longitude != null) {
                                staticLocationMessages.removeAll { it.id == message.id }
                                staticLocationMessages += StaticLocationMessage(
                                    id = message.id,
                                    location = StaticLocation(latitude, longitude),
                                    createdAt = message.createdAt,
                                    delivered = message.deliveredAt != null,
                                )
                            }
                        }
                        PendingMessageKind.MEDIA -> {
                            val mimeType = message.mediaMimeType
                            if (mimeType != null) {
                                localMediaMessages.removeAll { it.id == message.id }
                                localMediaMessages += MediaMessage(
                                    id = message.id,
                                    mimeType = mimeType,
                                    remoteUrl = repository.mediaDownloadUrl(message.mediaStorageKey ?: message.id),
                                    accessToken = repository.currentAccessToken(),
                                    createdAt = message.createdAt,
                                    delivered = message.deliveredAt != null,
                                )
                            }
                        }
                    }
                }
                is PendingMessageEvent.Failed -> Toast.makeText(
                    context,
                    "Сообщение сохранено. Нажмите «Повторить отправку».",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    LaunchedEffect(banUiState) {
        if (banUiState is BanUiState.Banned) LiveLocationService.stop(context)
    }

    when (val state = banUiState) {
        BanUiState.Loading -> {
            BanCheckScreen(error = false, onRetry = {})
            return
        }
        BanUiState.Error -> {
            BanCheckScreen(
                error = true,
                onRetry = {
                    banUiState = BanUiState.Loading
                    banCheckAttempt++
                },
            )
            return
        }
        is BanUiState.Banned -> {
            BanScreen(state.ban)
            return
        }
        BanUiState.NotBanned -> Unit
    }

    LaunchedEffect(selectedStaticLocation) {
        selectedStaticLocation?.let { location ->
            val repository = messageRepository
            if (repository == null) {
                submitStaticLocation(location)
            } else {
                submitStaticLocation(location)
            }
            onStaticLocationConsumed()
        }
    }

    val timelineItems = buildList<ChatTimelineItem> {
        textMessages.forEach { add(ChatTimelineItem.Text(it)) }
        pendingTextMessages.filterNot { it.localId in confirmedPendingIds }.forEach { pending ->
            add(
                ChatTimelineItem.Text(
                    PreviewMessage(
                        id = "pending-${pending.localId}",
                        text = pending.text,
                        time = pending.createdAt.toMessageTime(),
                        outgoing = true,
                        delivered = false,
                        createdAt = pending.createdAt,
                        pendingId = pending.localId,
                        pendingStatus = pending.status,
                    ),
                ),
            )
        }
        localMediaMessages.forEach { add(ChatTimelineItem.Media(it)) }
        pendingMediaMessages.filterNot { it.localId in confirmedPendingIds }.forEach { pending ->
            val kind = when {
                pending.mimeType == "image/gif" -> MediaKind.GIF
                pending.mimeType.startsWith("video/") -> MediaKind.VIDEO
                else -> MediaKind.PHOTO
            }
            add(
                ChatTimelineItem.Media(
                    MediaMessage(
                        id = "pending-${pending.localId}",
                        attachment = MediaAttachment(
                            id = pending.localId,
                            uri = android.net.Uri.fromFile(java.io.File(pending.localPath)),
                            mimeType = pending.mimeType,
                            sizeBytes = pending.sizeBytes,
                            kind = kind,
                        ),
                        mimeType = pending.mimeType,
                        createdAt = pending.createdAt,
                        delivered = false,
                        pendingId = pending.localId,
                        pendingStatus = pending.status,
                    ),
                ),
            )
        }
        staticLocationMessages.forEach { add(ChatTimelineItem.StaticLocation(it)) }
        val trackerActive = liveLocationState is LiveLocationState.Active
        confirmedLiveLocationMessages
            .filterNot { trackerActive && it.endsAt.toTimelineMillis() > System.currentTimeMillis() }
            .forEach { add(ChatTimelineItem.LiveLocation(it)) }
        pendingStaticLocations.filterNot { it.localId in confirmedPendingIds }.forEach { pending ->
            add(
                ChatTimelineItem.StaticLocation(
                    StaticLocationMessage(
                        id = "pending-${pending.localId}",
                        location = StaticLocation(pending.latitude, pending.longitude),
                        createdAt = pending.createdAt,
                        delivered = false,
                        pendingId = pending.localId,
                        pendingStatus = pending.status,
                    ),
                ),
            )
        }
    }.sortedBy { it.createdAt.toTimelineMillis() }

    LaunchedEffect(timelineItems.lastOrNull()?.id, liveLocationState) {
        val timelineDates = timelineItems.map { it.createdAt.toMessageDate() }.distinct()
        val activeLive = liveLocationState as? LiveLocationState.Active
        val liveDate = activeLive?.let {
            java.time.Instant.ofEpochMilli(it.startedAtMillis).atZone(MOSCOW_ZONE).toLocalDate()
        }
        val liveItemCount = if (activeLive == null) 0 else 1
        val liveHeaderCount = if (liveDate != null && liveDate != timelineDates.lastOrNull()) 1 else 0
        val itemCount = timelineItems.size + timelineDates.size + liveItemCount + liveHeaderCount
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    LaunchedEffect(listState, messageRepository) {
        val repository = messageRepository ?: return@LaunchedEffect
        snapshotFlow { listState.isScrollInProgress && listState.firstVisibleItemIndex == 0 }
            .distinctUntilChanged()
            .collect { reachedTop ->
                if (reachedTop) repository.loadOlderMessages()
            }
    }

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        AppTopBar()
        ConnectionStatusBar(realtimeConnectionState.toConnectionStatus())
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            var previousDate: LocalDate? = null
            timelineItems.forEach { timelineItem ->
                val messageDate = timelineItem.createdAt.toMessageDate()
                if (messageDate != previousDate) {
                    item(key = "date-$messageDate") { DateSeparator(messageDate) }
                    previousDate = messageDate
                }
                item(key = timelineItem.id) {
                    when (timelineItem) {
                        is ChatTimelineItem.Text -> MessageBubble(
                            timelineItem.message,
                            onRetry = { pendingId -> messageRepository?.retryPendingText(pendingId) },
                        )
                        is ChatTimelineItem.Media -> MediaBubble(
                            timelineItem.message,
                            onRetry = { pendingId -> messageRepository?.retryPendingMedia(pendingId) },
                        )
                        is ChatTimelineItem.StaticLocation -> StaticLocationBubble(
                            timelineItem.message,
                            onRetry = { pendingId -> messageRepository?.retryPendingStaticLocation(pendingId) },
                        )
                        is ChatTimelineItem.LiveLocation -> ConfirmedLiveLocationBubble(timelineItem.message)
                    }
                }
            }
            (liveLocationState as? LiveLocationState.Active)?.let { active ->
                val liveDate = java.time.Instant.ofEpochMilli(active.startedAtMillis)
                    .atZone(MOSCOW_ZONE).toLocalDate()
                if (liveDate != previousDate) {
                    item(key = "date-live-$liveDate") { DateSeparator(liveDate) }
                }
                item(key = "live-location") {
                    LiveLocationBubble(
                        state = active,
                        onStop = { LiveLocationService.stop(context) },
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = showAttachmentSuggestion,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            AttachmentSuggestion(
                onDismiss = { showAttachmentSuggestion = false },
                onMedia = {
                    showAttachmentSuggestion = false
                    attachmentInitialTab = AttachmentTab.GALLERY
                    showAttachmentSheet = true
                    galleryAccessGranted = hasGalleryPermission(context)
                    if (galleryAccessGranted) refreshRecentPhotos()
                    else if (requestGalleryPermission) galleryPermissionLauncher.launch(galleryPermissions())
                },
                onLocation = {
                    showAttachmentSuggestion = false
                    attachmentInitialTab = AttachmentTab.LOCATION
                    showAttachmentSheet = true
                },
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    attachmentInitialTab = AttachmentTab.GALLERY
                    showAttachmentSheet = true
                    galleryAccessGranted = hasGalleryPermission(context)
                    if (galleryAccessGranted) {
                        refreshRecentPhotos()
                    } else if (requestGalleryPermission) {
                        galleryPermissionLauncher.launch(galleryPermissions())
                    }
                },
                modifier = Modifier.size(44.dp),
            ) { Icon(Icons.Rounded.AttachFile, "Прикрепить фото, видео или GIF", tint = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.width(6.dp))
            TextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f).height(56.dp),
                placeholder = { Text("Сообщение") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { sendTextMessage() }),
                shape = RoundedCornerShape(22.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = sendTextMessage,
                enabled = draft.isNotBlank() && !isSending,
                modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, "Отправить", tint = Color.White)
            }
        }
    }

    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showAttachmentSheet = false
                pendingSelection = emptyList()
            },
            sheetState = attachmentSheetState,
            containerColor = Color.White,
        ) {
            AttachmentSheet(
                initialTab = attachmentInitialTab,
                media = pendingSelection,
                recentPhotoUris = recentPhotoUris,
                galleryAccessGranted = galleryAccessGranted,
                onClose = {
                    showAttachmentSheet = false
                    pendingSelection = emptyList()
                },
                onOpenPicker = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                },
                onRequestGalleryAccess = { galleryPermissionLauncher.launch(galleryPermissions()) },
                onRecentPhoto = { uri -> acceptMedia(listOf(uri)) },
                onTakePhoto = {
                    runCatching {
                        createCaptureUri(context, MediaKind.PHOTO).also { uri ->
                            pendingCaptureUri = uri
                            photoCapture.launch(uri)
                        }
                    }.onFailure { Toast.makeText(context, "Камера для фото недоступна", Toast.LENGTH_SHORT).show() }
                },
                onTakeVideo = {
                    runCatching {
                        createCaptureUri(context, MediaKind.VIDEO).also { uri ->
                            pendingCaptureUri = uri
                            videoCapture.launch(uri)
                        }
                    }.onFailure { Toast.makeText(context, "Камера для видео недоступна", Toast.LENGTH_SHORT).show() }
                },
                onRemove = { removed -> pendingSelection = pendingSelection.filterNot { it.id == removed.id } },
                onAdd = {
                    val attachments = pendingSelection
                    pendingSelection = emptyList()
                    showAttachmentSheet = false
                    scope.launch {
                        var queuedCount = 0
                        attachments.forEach { attachment ->
                            if (messageRepository == null) {
                                localMediaMessages += MediaMessage(
                                    id = attachment.id,
                                    attachment = attachment,
                                    mimeType = attachment.mimeType,
                                    createdAt = OffsetDateTime.now().toString(),
                                    delivered = false,
                                )
                                queuedCount++
                            } else {
                                messageRepository.enqueueMedia(
                                    attachment.uri,
                                    attachment.mimeType,
                                    attachment.sizeBytes,
                                ).onSuccess { queuedCount++ }
                            }
                        }
                        val message = when {
                            queuedCount == attachments.size -> "Медиа добавлено в очередь отправки"
                            queuedCount > 0 -> "Добавлено файлов: $queuedCount из ${attachments.size}"
                            else -> "Не удалось сохранить медиа"
                        }
                        Toast.makeText(context, message, if (queuedCount > 0) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
                    }
                },
                onCurrentLocation = {
                    showAttachmentSheet = false
                    locationPermissionAction = LocationPermissionAction.CURRENT
                    if (hasLocationPermission(context)) {
                        scope.launch {
                            currentStaticLocation(context)?.let { submitStaticLocation(it) }
                                ?: Toast.makeText(context, "Не удалось определить местоположение", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        liveLocationPermissionLauncher.launch(liveLocationPermissions())
                    }
                },
                onOpenStaticLocationPicker = {
                    showAttachmentSheet = false
                    onOpenStaticLocationPicker()
                },
                onStartLiveLocation = {
                    showAttachmentSheet = false
                    if (liveLocationState is LiveLocationState.Active) {
                        Toast.makeText(context, "Live-геолокация уже активна", Toast.LENGTH_SHORT).show()
                    } else if (hasLocationPermission(context)) {
                        locationPermissionAction = LocationPermissionAction.LIVE
                        LiveLocationService.start(context)
                        Toast.makeText(context, "Live-геолокация включена на 15 минут", Toast.LENGTH_SHORT).show()
                    } else {
                        locationPermissionAction = LocationPermissionAction.LIVE
                        liveLocationPermissionLauncher.launch(liveLocationPermissions())
                    }
                },
            )
        }
    }
}

private val BAN_STATE_EVENTS = setOf(
    "observer_banned",
    "observer_ban_revoked",
    "observer_ban_expired",
)

@Composable
private fun AttachmentSuggestion(
    onDismiss: () -> Unit,
    onMedia: () -> Unit,
    onLocation: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Можно отправить фото, видео или геолокацию.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Close, "Закрыть подсказку")
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onMedia, modifier = Modifier.weight(0.8f)) {
                    Icon(Icons.Rounded.PhotoCamera, null)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Медиа",
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                FilledTonalButton(onClick = onLocation, modifier = Modifier.weight(1.2f)) {
                    Icon(Icons.Rounded.LocationOn, null)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Геолокация",
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusBar(status: ConnectionStatus) {
    val statusColor = when (status) {
        ConnectionStatus.ONLINE -> Color(0xFF2E7D32)
        ConnectionStatus.CONNECTING -> Color(0xFFF57C00)
        ConnectionStatus.OFFLINE -> MaterialTheme.colorScheme.error
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(statusColor.copy(alpha = 0.10f))
            .padding(horizontal = 16.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(statusColor))
        Spacer(Modifier.width(7.dp))
        Text(
            text = status.text,
            style = MaterialTheme.typography.labelSmall,
            color = statusColor,
        )
    }
}

@Composable
private fun AttachmentSheet(
    initialTab: AttachmentTab,
    media: List<MediaAttachment>,
    recentPhotoUris: List<android.net.Uri>,
    galleryAccessGranted: Boolean,
    onClose: () -> Unit,
    onOpenPicker: () -> Unit,
    onRequestGalleryAccess: () -> Unit,
    onRecentPhoto: (android.net.Uri) -> Unit,
    onTakePhoto: () -> Unit,
    onTakeVideo: () -> Unit,
    onRemove: (MediaAttachment) -> Unit,
    onAdd: () -> Unit,
    onCurrentLocation: () -> Unit,
    onOpenStaticLocationPicker: () -> Unit,
    onStartLiveLocation: () -> Unit,
) {
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    Column(Modifier.fillMaxWidth().heightIn(min = 420.dp, max = 620.dp)) {
        Box(Modifier.fillMaxWidth().height(54.dp)) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)) {
                Icon(Icons.Rounded.Close, "Закрыть", tint = Color(0xFF455A7A))
            }
            Text(
                if (selectedTab == AttachmentTab.GALLERY) "Фото и видео" else "Геолокация",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (selectedTab == AttachmentTab.GALLERY) {
                GalleryAttachmentContent(
                    media = media,
                    recentPhotoUris = recentPhotoUris,
                    galleryAccessGranted = galleryAccessGranted,
                    onOpenPicker = onOpenPicker,
                    onRequestGalleryAccess = onRequestGalleryAccess,
                    onRecentPhoto = onRecentPhoto,
                    onTakePhoto = onTakePhoto,
                    onTakeVideo = onTakeVideo,
                    onRemove = onRemove,
                    onAdd = onAdd,
                )
            } else {
                LocationAttachmentContent(onCurrentLocation, onOpenStaticLocationPicker, onStartLiveLocation)
            }
        }

        Row(Modifier.fillMaxWidth().height(82.dp).background(Color.White)) {
            AttachmentTabButton(
                selected = selectedTab == AttachmentTab.GALLERY,
                icon = { tint -> Icon(Icons.Rounded.PhotoLibrary, null, tint = tint) },
                label = "Галерея",
                modifier = Modifier.weight(1f),
                onClick = { selectedTab = AttachmentTab.GALLERY },
            )
            AttachmentTabButton(
                selected = selectedTab == AttachmentTab.LOCATION,
                icon = { tint -> Icon(Icons.Rounded.LocationOn, null, tint = tint) },
                label = "Геолокация",
                modifier = Modifier.weight(1f),
                onClick = { selectedTab = AttachmentTab.LOCATION },
            )
        }
    }
}

@Composable
private fun GalleryAttachmentContent(
    media: List<MediaAttachment>,
    recentPhotoUris: List<android.net.Uri>,
    galleryAccessGranted: Boolean,
    onOpenPicker: () -> Unit,
    onRequestGalleryAccess: () -> Unit,
    onRecentPhoto: (android.net.Uri) -> Unit,
    onTakePhoto: () -> Unit,
    onTakeVideo: () -> Unit,
    onRemove: (MediaAttachment) -> Unit,
    onAdd: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        if (media.isEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CaptureAction(Icons.Rounded.PhotoCamera, "Снять фото", Modifier.weight(1f), onTakePhoto)
                CaptureAction(Icons.Rounded.Videocam, "Снять видео", Modifier.weight(1f), onTakeVideo)
            }
            Text(
                "Последние фото",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            when {
                !galleryAccessGranted -> {
                    OutlinedButton(onClick = onRequestGalleryAccess, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Разрешить доступ к фото")
                    }
                }
                recentPhotoUris.isEmpty() -> {
                    Text(
                        "Недавние фотографии не найдены",
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxWidth().height(190.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(recentPhotoUris, key = { it.toString() }) { uri ->
                            Surface(onClick = { onRecentPhoto(uri) }, modifier = Modifier.aspectRatio(1f), color = Color(0xFFE2E8F0)) {
                                AsyncImage(model = uri, contentDescription = "Недавнее фото", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onOpenPicker,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2840)),
            ) {
                Icon(Icons.Rounded.PhotoLibrary, null)
                Spacer(Modifier.width(8.dp))
                Text("Выбрать из галереи", fontWeight = FontWeight.Bold)
            }
            Text("Фото до 10 МБ · Видео и GIF до 100 МБ", modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.labelSmall)
        } else {
            Spacer(Modifier.weight(1f))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(media, key = { it.id }) { attachment -> MediaPreviewTile(attachment, onRemove) }
            }
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Добавить (${media.size})", fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onOpenPicker, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Выбрать другие")
            }
        }
    }
}

@Composable
private fun CaptureAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, modifier = modifier.height(128.dp), color = Color(0xFF1A2840), shape = RoundedCornerShape(8.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(38.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LocationAttachmentContent(
    onCurrentLocation: () -> Unit,
    onOpenStaticLocationPicker: () -> Unit,
    onStartLiveLocation: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Как поделиться местоположением?", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
        LocationOption(Icons.Rounded.MyLocation, "Текущее местоположение", "Отправить координаты устройства", onCurrentLocation)
        LocationOption(Icons.Rounded.Map, "Выбрать точку на карте", "Указать место вручную", onOpenStaticLocationPicker)
        LocationOption(Icons.Rounded.ShareLocation, "Live-геолокация", "Передавать координаты 15 минут", onStartLiveLocation)
    }
}

@Composable
private fun LocationOption(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.background) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Color(0xFFD9E4F5), modifier = Modifier.size(44.dp)) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun AttachmentTabButton(
    selected: Boolean,
    icon: @Composable (Color) -> Unit,
    label: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val color = if (selected) MaterialTheme.colorScheme.primary else Color(0xFF90A4AE)
    Column(
        modifier.background(if (selected) Color(0xFFF4F5F7) else Color.White).clickable(onClick = onClick).padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent))
        Spacer(Modifier.height(8.dp))
        icon(color)
        Text(label, color = color, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun MediaPreviewTile(attachment: MediaAttachment, onRemove: (MediaAttachment) -> Unit) {
    Box(Modifier.width(118.dp).aspectRatio(1f).clip(RoundedCornerShape(4.dp)).background(Color(0xFF1A2840))) {
        MediaContent(attachment, Modifier.fillMaxSize())
        IconButton(
            onClick = { onRemove(attachment) },
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(26.dp).background(Color(0x99000000), CircleShape),
        ) { Icon(Icons.Rounded.Close, "Удалить", tint = Color.White, modifier = Modifier.size(17.dp)) }
        Text(
            attachment.displaySize(),
            modifier = Modifier.align(Alignment.BottomStart).background(Color(0x99000000)).padding(horizontal = 5.dp, vertical = 2.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@androidx.annotation.OptIn(markerClass = [UnstableApi::class])
@Composable
private fun MediaBubble(message: MediaMessage, onRetry: (String) -> Unit) {
    val context = LocalContext.current
    var showVideoPlayer by remember { mutableStateOf(false) }
    var showImageViewer by remember { mutableStateOf(false) }
    val isVideo = message.mimeType.startsWith("video/")
    val kindLabel = when {
        message.mimeType == "image/gif" -> "GIF"
        message.mimeType.startsWith("video/") -> "Видео"
        else -> "Фото"
    }
    val remoteModel = remember(message.remoteUrl, message.accessToken) {
        message.remoteUrl?.let { url ->
            val headers = Headers.Builder()
                .add("X-Client-App", "eyewitness")
                .apply { message.accessToken?.let { add("Authorization", "Bearer $it") } }
                .build()
            ImageRequest.Builder(context).data(url).headers(headers).build()
        }
    }
    val videoThumbnailModel = remember(message.attachment?.uri, message.remoteUrl, message.accessToken) {
        if (!isVideo) return@remember null
        val data = message.attachment?.uri ?: message.remoteUrl ?: return@remember null
        val builder = ImageRequest.Builder(context)
            .data(data)
            .decoderFactory(VideoFrameDecoder.Factory())
            .videoFrameMillis(0)
        if (message.remoteUrl != null) {
            val headers = Headers.Builder()
                .add("X-Client-App", "eyewitness")
                .apply { message.accessToken?.let { add("Authorization", "Bearer $it") } }
                .build()
            builder.headers(headers)
        }
        builder.build()
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Box(
            Modifier.width(200.dp).height(140.dp)
                .clip(RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp))
                .background(Color(0xFF1A2840))
                .clickable {
                    if (isVideo) showVideoPlayer = true else showImageViewer = true
                },
        ) {
            when {
                isVideo && videoThumbnailModel != null -> AsyncImage(
                    model = videoThumbnailModel,
                    contentDescription = "Первый кадр видео",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                message.attachment != null -> MediaContent(message.attachment, Modifier.fillMaxSize())
                remoteModel != null -> AsyncImage(
                    model = remoteModel,
                    contentDescription = kindLabel,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            if (isVideo) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    "Видео",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp).align(Alignment.Center),
                )
            }
            Text(
                kindLabel,
                modifier = Modifier.align(Alignment.BottomStart).background(Color(0x99000000)).padding(horizontal = 8.dp, vertical = 4.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp, end = 2.dp)) {
            when (message.pendingStatus) {
                PendingMessageStatus.SENDING -> {
                    Text(message.createdAt.toMessageTime(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(4.dp))
                    Text("Отправляется…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
                PendingMessageStatus.FAILED -> FailedMessageStatus(
                    time = message.createdAt.toMessageTime(),
                    onRetry = { message.pendingId?.let(onRetry) },
                )
                null -> {
                    Text(message.createdAt.toMessageTime(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(4.dp))
                    DeliveryChecks(message.delivered)
                }
            }
        }
    }
    if (showVideoPlayer) VideoPlayerDialog(message, onDismiss = { showVideoPlayer = false })
    if (showImageViewer) ImageViewerDialog(message, remoteModel, onDismiss = { showImageViewer = false })
}

@Composable
private fun ImageViewerDialog(
    message: MediaMessage,
    remoteModel: ImageRequest?,
    onDismiss: () -> Unit,
) {
    val imageModel: Any = message.attachment?.uri ?: remoteModel ?: return
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = if (message.mimeType == "image/gif") "GIF" else "Фото",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp)
                        .background(Color(0x99000000), CircleShape),
                ) { Icon(Icons.Rounded.Close, "Закрыть фото", tint = Color.White) }
            }
        }
    }
}

@UnstableApi
@Composable
private fun VideoPlayerDialog(message: MediaMessage, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val mediaUri = message.attachment?.uri ?: message.remoteUrl?.let(android.net.Uri::parse) ?: return
    val player = remember(mediaUri, message.accessToken) {
        val builder = ExoPlayer.Builder(context)
        if (message.remoteUrl != null) {
            val headers = buildMap {
                put("X-Client-App", "eyewitness")
                message.accessToken?.let { put("Authorization", "Bearer $it") }
            }
            val httpFactory = DefaultHttpDataSource.Factory().setDefaultRequestProperties(headers)
            builder.setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(httpFactory))
        }
        builder.build().apply {
            setMediaItem(MediaItem.fromUri(mediaUri))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { playerContext ->
                        PlayerView(playerContext).apply {
                            this.player = player
                            useController = true
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp)
                        .background(Color(0x99000000), CircleShape),
                ) { Icon(Icons.Rounded.Close, "Закрыть видео", tint = Color.White) }
            }
        }
    }
}

@Composable
private fun StaticLocationBubble(message: StaticLocationMessage, onRetry: (String) -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Surface(
            color = OutgoingBubble,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.width(230.dp),
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Rounded.LocationOn, null, tint = Color.White, modifier = Modifier.padding(9.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Точка на карте", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Text(
                        String.format(java.util.Locale.US, "%.6f, %.6f", message.location.latitude, message.location.longitude),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        Row(modifier = Modifier.padding(top = 3.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            when (message.pendingStatus) {
                PendingMessageStatus.SENDING -> {
                    Text(message.createdAt.toMessageTime(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(4.dp))
                    Text("Отправляется…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
                PendingMessageStatus.FAILED -> FailedMessageStatus(
                    time = message.createdAt.toMessageTime(),
                    onRetry = { message.pendingId?.let(onRetry) },
                )
                null -> {
                    Text(message.createdAt.toMessageTime(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(4.dp))
                    DeliveryChecks(message.delivered)
                }
            }
        }
    }
}

@Composable
private fun ConfirmedLiveLocationBubble(message: ConfirmedLiveLocationMessage) {
    val active = message.endsAt.toTimelineMillis() > System.currentTimeMillis()
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Surface(
            color = OutgoingBubble,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.width(270.dp),
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp)) {
                        Icon(Icons.Rounded.ShareLocation, null, tint = Color.White, modifier = Modifier.padding(9.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Live-геолокация", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            if (active) "Передача геопозиции активна" else "Передача геопозиции завершена",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                Spacer(Modifier.height(9.dp))
                Text(
                    message.location?.let {
                        String.format(java.util.Locale.US, "%.6f, %.6f", it.latitude, it.longitude)
                    } ?: "Координаты ещё не получены",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Row(modifier = Modifier.padding(top = 3.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message.createdAt.toMessageTime(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.width(4.dp))
            DeliveryChecks(message.delivered)
        }
    }
}

@Composable
private fun LiveLocationBubble(
    state: LiveLocationState.Active,
    onStop: () -> Unit,
) {
    var nowMillis by remember(state.endsAtMillis) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.endsAtMillis) {
        while (nowMillis < state.endsAtMillis) {
            delay(1_000)
            nowMillis = System.currentTimeMillis()
        }
    }
    val remainingSeconds = ((state.endsAtMillis - nowMillis).coerceAtLeast(0L) / 1_000L).toInt()
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        Surface(
            color = OutgoingBubble,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.width(270.dp),
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp)) {
                        Icon(Icons.Rounded.ShareLocation, null, tint = Color.White, modifier = Modifier.padding(9.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Live-геолокация", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "Осталось %02d:%02d".format(minutes, seconds),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                Spacer(Modifier.height(9.dp))
                Text(
                    state.latestLocation?.let {
                        String.format(java.util.Locale.US, "%.6f, %.6f", it.latitude, it.longitude)
                    } ?: "Ожидание координат GPS…",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Получено точек: ${state.pointsRecorded}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                TextButton(onClick = onStop, modifier = Modifier.align(Alignment.End)) {
                    Text("Остановить")
                }
            }
        }
        Text(
            "${java.time.Instant.ofEpochMilli(state.startedAtMillis).atZone(MOSCOW_ZONE).format(DateTimeFormatter.ofPattern("HH:mm"))} · Точки отправляются на сервер каждую секунду",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 3.dp, end = 2.dp),
        )
    }
}

@Composable
private fun MediaContent(attachment: MediaAttachment, modifier: Modifier) {
    if (attachment.kind == MediaKind.VIDEO) {
        val context = LocalContext.current
        val thumbnail = remember(attachment.uri) {
            ImageRequest.Builder(context)
                .data(attachment.uri)
                .decoderFactory(VideoFrameDecoder.Factory())
                .videoFrameMillis(0)
                .build()
        }
        Box(modifier) {
            AsyncImage(
                model = thumbnail,
                contentDescription = "Первый кадр видео",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Icon(Icons.Rounded.PlayArrow, "Видео", tint = Color.White, modifier = Modifier.size(48.dp).align(Alignment.Center))
        }
    } else {
        AsyncImage(
            model = attachment.uri,
            contentDescription = if (attachment.kind == MediaKind.GIF) "GIF" else "Фото",
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun MessageBubble(message: PreviewMessage, onRetry: (String) -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.outgoing) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Bottom) {
        if (!message.outgoing) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(31.dp)) {
                Image(painterResource(R.drawable.ic_shield), null, Modifier.padding(7.dp))
            }
            Spacer(Modifier.width(9.dp))
        }
        Column(horizontalAlignment = if (message.outgoing) Alignment.End else Alignment.Start) {
            Surface(
                color = if (message.outgoing) OutgoingBubble else IncomingBubble,
                shape = RoundedCornerShape(15.dp),
                shadowElevation = if (message.outgoing) 0.dp else 1.dp,
                modifier = Modifier.widthIn(max = 264.dp),
            ) {
                Column(Modifier.padding(horizontal = 15.dp, vertical = 10.dp)) {
                    Text(
                        message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (message.pendingStatus != PendingMessageStatus.FAILED) {
                        Row(Modifier.align(if (message.outgoing) Alignment.End else Alignment.Start), verticalAlignment = Alignment.CenterVertically) {
                            Text(message.time, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            if (message.pendingStatus == PendingMessageStatus.SENDING) {
                                Spacer(Modifier.width(6.dp))
                                Text("Отправляется…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            } else if (message.outgoing) {
                                Spacer(Modifier.width(4.dp))
                                DeliveryChecks(message.delivered)
                            }
                        }
                    }
                }
            }
            if (message.pendingStatus == PendingMessageStatus.FAILED && message.pendingId != null) {
                FailedMessageStatus(message.time, onRetry = { onRetry(message.pendingId) })
            }
        }
    }
}

@Composable
private fun FailedMessageStatus(
    time: String,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier.padding(top = 5.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.WarningAmber,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            "Не отправлено",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold,
        )
        Text(" · $time · ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        Row(
            modifier = Modifier.clickable(onClick = onRetry),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text(
                "Повторить",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun DeliveryChecks(delivered: Boolean) {
    Icon(
        imageVector = if (delivered) Icons.Rounded.DoneAll else Icons.Rounded.Done,
        contentDescription = if (delivered) "Доставлено сотруднику" else "Отправлено на сервер",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(18.dp),
    )
}

private fun ChatMessage.toPreview(currentDeviceId: String?) = PreviewMessage(
    id = id,
    text = text,
    time = createdAt.toMessageTime(),
    outgoing = senderDeviceId == currentDeviceId,
    delivered = deliveredAt != null,
    createdAt = createdAt,
)

@Composable
private fun DateSeparator(date: LocalDate) {
    Text(
        text = date.toDateHeader(),
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.outline,
    )
}

private val MOSCOW_ZONE: ZoneId = ZoneId.of("Europe/Moscow")

private fun String.toMessageTime(): String = runCatching {
    OffsetDateTime.parse(this).atZoneSameInstant(MOSCOW_ZONE).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
}.recoverCatching {
    LocalDateTime.parse(this).atZone(java.time.ZoneOffset.UTC).withZoneSameInstant(MOSCOW_ZONE)
        .toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
}.getOrElse {
    substringAfter('T', "--:--").take(5).takeIf { value -> value.matches(Regex("\\d{2}:\\d{2}")) } ?: "--:--"
}

private fun LocalDate.toDateHeader(): String {
    val today = LocalDate.now(MOSCOW_ZONE)
    val monthName = month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ru"))
    val dateText = "$dayOfMonth $monthName" + if (year != today.year) " $year" else ""
    return when (this) {
        today -> "Сегодня, $dateText"
        today.minusDays(1) -> "Вчера, $dateText"
        else -> dateText.replaceFirstChar { it.uppercase(Locale.forLanguageTag("ru")) }
    }
}

private fun String.toMessageDate(): LocalDate = runCatching {
    OffsetDateTime.parse(this).atZoneSameInstant(MOSCOW_ZONE).toLocalDate()
}.recoverCatching {
    LocalDateTime.parse(this).atZone(java.time.ZoneOffset.UTC).withZoneSameInstant(MOSCOW_ZONE).toLocalDate()
}.getOrElse { LocalDate.now(MOSCOW_ZONE) }

private fun String.toTimelineMillis(): Long = runCatching {
    OffsetDateTime.parse(this).toInstant().toEpochMilli()
}.recoverCatching {
    LocalDateTime.parse(this).toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
}.getOrElse { Long.MAX_VALUE }

@SuppressLint("MissingPermission")
private suspend fun currentStaticLocation(context: Context): StaticLocation? {
    if (!hasLocationPermission(context)) return null
    val manager = context.getSystemService(LocationManager::class.java)
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter(manager::isProviderEnabled)
    if (providers.isEmpty()) return null

    val freshLocation = withTimeoutOrNull(10_000L) {
        suspendCancellableCoroutine<Location?> { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    if (continuation.isActive) continuation.resume(location)
                }
            }
            continuation.invokeOnCancellation { manager.removeUpdates(listener) }
            runCatching {
                manager.requestSingleUpdate(providers.first(), listener, Looper.getMainLooper())
            }.onFailure {
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }
    val location = freshLocation ?: providers.mapNotNull(manager::getLastKnownLocation).maxByOrNull(Location::getTime)
    return location?.let { StaticLocation(it.latitude, it.longitude) }
}

@Preview(showBackground = true, widthDp = 403, heightDp = 874)
@Composable
private fun ChatPreview() = SKBTUPGIBDDEYEWITNESSTheme { ChatScreen() }
