package com.example.skbt_up_gibdd_eyewitness.feature.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageTypeTest {
    @Test
    fun `server lowercase text message is recognized`() {
        assertTrue("text".isTextMessageType())
    }

    @Test
    fun `legacy uppercase text message is recognized`() {
        assertTrue("TEXT".isTextMessageType())
    }

    @Test
    fun `non-text messages are not recognized as text`() {
        assertFalse("location".isTextMessageType())
    }
}
