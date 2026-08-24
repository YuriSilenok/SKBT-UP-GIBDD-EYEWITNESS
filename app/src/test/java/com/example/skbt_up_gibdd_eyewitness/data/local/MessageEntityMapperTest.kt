package com.example.skbt_up_gibdd_eyewitness.data.local

import com.example.skbt_up_gibdd_eyewitness.domain.message.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageEntityMapperTest {
    @Test
    fun `entity round trip preserves server message`() {
        val message = ChatMessage(
            id = "message-1",
            observerDeviceId = "observer-1",
            senderDeviceId = "observer-1",
            text = "Сообщение",
            type = "TEXT",
            createdAt = "2026-08-24T11:15:30+03:00",
            deliveredAt = null,
        )

        val entity = message.toEntity()

        assertEquals(1_787_559_330_000L, entity.createdAtEpochMillis)
        assertEquals(message, entity.toDomain())
    }

    @Test
    fun `invalid server date does not prevent caching`() {
        val message = ChatMessage(
            id = "message-2",
            observerDeviceId = "observer-1",
            senderDeviceId = "inspector-1",
            text = "Ответ",
            type = "TEXT",
            createdAt = "unknown",
            deliveredAt = "2026-08-24T11:16:00+03:00",
        )

        assertEquals(0L, message.toEntity().createdAtEpochMillis)
    }
}
