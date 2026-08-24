package com.example.skbt_up_gibdd_eyewitness.domain.realtime

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

enum class RealtimeConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
}

data class RealtimeEvent(val name: String)

interface RealtimeRepository {
    val connectionState: StateFlow<RealtimeConnectionState>
    val events: SharedFlow<RealtimeEvent>

    fun start()
    fun stop()
}
