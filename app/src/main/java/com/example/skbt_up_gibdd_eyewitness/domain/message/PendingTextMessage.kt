package com.example.skbt_up_gibdd_eyewitness.domain.message

enum class PendingMessageStatus { SENDING, FAILED }

data class PendingTextMessage(
    val localId: String,
    val text: String,
    val createdAt: String,
    val retryUntilMillis: Long,
    val status: PendingMessageStatus,
)

enum class PendingMessageKind { TEXT, STATIC_LOCATION, MEDIA }

sealed interface PendingMessageEvent {
    data class Sent(
        val localId: String,
        val kind: PendingMessageKind,
        val message: ChatMessage,
    ) : PendingMessageEvent
    data class Failed(val kind: PendingMessageKind) : PendingMessageEvent
}
