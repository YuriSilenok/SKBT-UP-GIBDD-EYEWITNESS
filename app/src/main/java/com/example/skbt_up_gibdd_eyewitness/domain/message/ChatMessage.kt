package com.example.skbt_up_gibdd_eyewitness.domain.message

data class ChatMessage(
    val id: String,
    val observerDeviceId: String,
    val senderDeviceId: String,
    val text: String,
    val type: String,
    val staticLatitude: Double? = null,
    val staticLongitude: Double? = null,
    val mediaStorageKey: String? = null,
    val mediaMimeType: String? = null,
    val locationSessionId: String? = null,
    val liveEndsAt: String? = null,
    val createdAt: String,
    val deliveredAt: String?,
)
