package com.example.skbt_up_gibdd_eyewitness.domain.message

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class PendingMediaMessageTest {
    @Test
    fun `local media metadata survives serialization`() {
        val expected = PendingMediaMessage(
            localId = "local-media-id",
            localPath = "/data/user/0/app/files/pending_media/local-media-id.mp4",
            mimeType = "video/mp4",
            sizeBytes = 1_024L,
            createdAt = "2026-08-24T10:00:00Z",
            retryUntilMillis = 60_000L,
            status = PendingMessageStatus.SENDING,
        )

        val restored = Gson().fromJson(Gson().toJson(expected), PendingMediaMessage::class.java)

        assertEquals(expected, restored)
    }
}
