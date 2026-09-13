package com.example.skbt_up_gibdd_eyewitness.data.realtime

import com.example.skbt_up_gibdd_eyewitness.domain.device.DeviceSession
import com.example.skbt_up_gibdd_eyewitness.domain.realtime.RealtimeConnectionState
import com.example.skbt_up_gibdd_eyewitness.domain.realtime.RealtimeEvent
import com.example.skbt_up_gibdd_eyewitness.domain.realtime.RealtimeRepository
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class DefaultRealtimeRepository(
    private val baseUrl: String,
    private val sessionProvider: () -> DeviceSession?,
) : RealtimeRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()
    private val mutableConnectionState = MutableStateFlow(RealtimeConnectionState.DISCONNECTED)
    override val connectionState = mutableConnectionState.asStateFlow()

    private val mutableEvents = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 32)
    override val events = mutableEvents.asSharedFlow()

    @Volatile
    private var shouldRun = false
    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0

    @Synchronized
    override fun start() {
        if (shouldRun) return
        shouldRun = true
        reconnectAttempt = 0
        connect()
    }

    @Synchronized
    override fun stop() {
        shouldRun = false
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(NORMAL_CLOSURE_STATUS, "Chat closed")
        webSocket = null
        mutableConnectionState.value = RealtimeConnectionState.DISCONNECTED
    }

    @Synchronized
    private fun connect() {
        if (!shouldRun || webSocket != null) return
        val session = sessionProvider()
        if (session == null) {
            scheduleReconnect()
            return
        }

        mutableConnectionState.value = RealtimeConnectionState.CONNECTING
        val realtimeUrl = baseUrl
            .replaceFirst("https://", "wss://")
            .replaceFirst("http://", "ws://") +
            "api/v1/ws/chats/${session.chatId}?token=${session.accessToken}"
        val request = Request.Builder()
            .url(realtimeUrl)
            .header("X-Client-App", "eyewitness")
            .build()
        webSocket = client.newWebSocket(request, listener)
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            reconnectAttempt = 0
            mutableConnectionState.value = RealtimeConnectionState.CONNECTED
            mutableEvents.tryEmit(RealtimeEvent("connected"))
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val eventName = runCatching {
                JsonParser.parseString(text).asJsonObject.get("event")?.asString
            }.getOrNull() ?: return
            mutableEvents.tryEmit(RealtimeEvent(eventName))
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            handleDisconnect(webSocket)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            handleDisconnect(webSocket)
        }
    }

    @Synchronized
    private fun handleDisconnect(disconnectedSocket: WebSocket) {
        if (webSocket !== disconnectedSocket) return
        webSocket = null
        mutableConnectionState.value = RealtimeConnectionState.DISCONNECTED
        if (shouldRun) scheduleReconnect()
    }

    @Synchronized
    private fun scheduleReconnect() {
        if (!shouldRun || reconnectJob?.isActive == true) return
        val delayMillis = RECONNECT_DELAYS_MILLIS[
            reconnectAttempt.coerceAtMost(RECONNECT_DELAYS_MILLIS.lastIndex)
        ]
        reconnectAttempt++
        reconnectJob = scope.launch {
            delay(delayMillis)
            synchronized(this@DefaultRealtimeRepository) {
                reconnectJob = null
                connect()
            }
        }
    }

    private companion object {
        const val NORMAL_CLOSURE_STATUS = 1000
        val RECONNECT_DELAYS_MILLIS = longArrayOf(1_000, 2_000, 5_000, 10_000, 30_000)
    }
}
