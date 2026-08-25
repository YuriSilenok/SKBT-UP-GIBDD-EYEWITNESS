package com.example.skbt_up_gibdd_eyewitness.domain.message

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class PendingStaticLocationTest {
    @Test
    fun `coordinates and retry state survive serialization`() {
        val expected = PendingStaticLocation(
            localId = "local-location-id",
            latitude = 57.767961,
            longitude = 40.926858,
            createdAt = "2026-08-24T10:00:00Z",
            retryUntilMillis = 60_000L,
            status = PendingMessageStatus.SENDING,
        )

        val restored = Gson().fromJson(Gson().toJson(expected), PendingStaticLocation::class.java)

        assertEquals(expected, restored)
    }
}
