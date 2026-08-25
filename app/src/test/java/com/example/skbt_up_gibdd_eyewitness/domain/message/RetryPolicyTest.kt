package com.example.skbt_up_gibdd_eyewitness.domain.message

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetryPolicyTest {
    @Test
    fun messageRetriesBeforeDeadline() {
        assertTrue(shouldRetryMessage(nowMillis = 59_999, retryUntilMillis = 60_000))
    }

    @Test
    fun messageStopsRetryingAtDeadline() {
        assertFalse(shouldRetryMessage(nowMillis = 60_000, retryUntilMillis = 60_000))
    }

    @Test
    fun expiredSendingMessageRestoresAsFailed() {
        assertEquals(
            PendingMessageStatus.FAILED,
            restoredPendingStatus(PendingMessageStatus.SENDING, nowMillis = 61_000, retryUntilMillis = 60_000),
        )
    }

    @Test
    fun failedMessageRemainsFailedUntilManualRetry() {
        assertEquals(
            PendingMessageStatus.FAILED,
            restoredPendingStatus(PendingMessageStatus.FAILED, nowMillis = 10_000, retryUntilMillis = 60_000),
        )
    }
}
