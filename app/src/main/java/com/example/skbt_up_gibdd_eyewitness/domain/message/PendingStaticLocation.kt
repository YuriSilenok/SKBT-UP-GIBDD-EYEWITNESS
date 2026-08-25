package com.example.skbt_up_gibdd_eyewitness.domain.message

data class PendingStaticLocation(
    val localId: String,
    val latitude: Double,
    val longitude: Double,
    val createdAt: String,
    val retryUntilMillis: Long,
    val status: PendingMessageStatus,
)
