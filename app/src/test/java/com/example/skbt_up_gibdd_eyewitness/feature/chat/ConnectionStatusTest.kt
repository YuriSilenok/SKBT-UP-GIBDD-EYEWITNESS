package com.example.skbt_up_gibdd_eyewitness.feature.chat

import com.example.skbt_up_gibdd_eyewitness.domain.realtime.RealtimeConnectionState
import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionStatusTest {
    @Test
    fun connectingState_showsConnectingStatus() {
        assertEquals(
            ConnectionStatus.CONNECTING,
            RealtimeConnectionState.CONNECTING.toConnectionStatus(),
        )
    }

    @Test
    fun connectedState_showsOnlineStatus() {
        assertEquals(
            ConnectionStatus.ONLINE,
            RealtimeConnectionState.CONNECTED.toConnectionStatus(),
        )
    }

    @Test
    fun disconnectedState_showsOfflineStatus() {
        assertEquals(
            ConnectionStatus.OFFLINE,
            RealtimeConnectionState.DISCONNECTED.toConnectionStatus(),
        )
    }
}
