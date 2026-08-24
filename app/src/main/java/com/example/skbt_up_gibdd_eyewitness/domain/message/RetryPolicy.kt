package com.example.skbt_up_gibdd_eyewitness.domain.message

const val MESSAGE_RETRY_WINDOW_MILLIS = 60_000L

fun shouldRetryMessage(nowMillis: Long, retryUntilMillis: Long): Boolean = nowMillis < retryUntilMillis

fun restoredPendingStatus(
    status: PendingMessageStatus,
    nowMillis: Long,
    retryUntilMillis: Long,
): PendingMessageStatus = if (
    status == PendingMessageStatus.SENDING && !shouldRetryMessage(nowMillis, retryUntilMillis)
) {
    PendingMessageStatus.FAILED
} else {
    status
}
