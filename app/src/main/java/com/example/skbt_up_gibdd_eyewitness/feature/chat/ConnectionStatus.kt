package com.example.skbt_up_gibdd_eyewitness.feature.chat

import com.example.skbt_up_gibdd_eyewitness.domain.realtime.RealtimeConnectionState

enum class ConnectionStatus(val text: String) {
    CONNECTING("Подключение…"),
    ONLINE("В сети"),
    OFFLINE("Нет соединения. Повторное подключение…"),
}

fun RealtimeConnectionState.toConnectionStatus(): ConnectionStatus = when (this) {
    RealtimeConnectionState.CONNECTING -> ConnectionStatus.CONNECTING
    RealtimeConnectionState.CONNECTED -> ConnectionStatus.ONLINE
    RealtimeConnectionState.DISCONNECTED -> ConnectionStatus.OFFLINE
}
