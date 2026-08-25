package com.example.skbt_up_gibdd_eyewitness.domain.message

data class PendingMediaMessage(
    val localId: String,
    val localPath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val createdAt: String,
    val retryUntilMillis: Long,
    val status: PendingMessageStatus,
)
