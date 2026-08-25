package com.example.skbt_up_gibdd_eyewitness.data.message

import com.example.skbt_up_gibdd_eyewitness.domain.message.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class SubmittedTextFallbackTest {
    @Test
    fun `empty post response keeps submitted text for optimistic replacement`() {
        val response = message(text = "", type = "TEXT")

        assertEquals("Текст очевидца", response.withSubmittedText("Текст очевидца").text)
    }

    @Test
    fun `server text is preserved when response contains it`() {
        val response = message(text = "Текст сервера", type = "TEXT")

        assertEquals("Текст сервера", response.withSubmittedText("Текст очевидца").text)
    }

    private fun message(text: String, type: String) = ChatMessage(
        id = "message-1",
        observerDeviceId = "observer-1",
        senderDeviceId = "observer-1",
        text = text,
        type = type,
        createdAt = "2026-08-24T12:20:00Z",
        deliveredAt = null,
    )
}
