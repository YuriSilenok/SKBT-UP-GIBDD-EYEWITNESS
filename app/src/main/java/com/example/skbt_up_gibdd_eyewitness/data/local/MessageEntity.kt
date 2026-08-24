package com.example.skbt_up_gibdd_eyewitness.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val observerDeviceId: String,
    val senderDeviceId: String,
    val text: String,
    val type: String,
    val staticLatitude: Double?,
    val staticLongitude: Double?,
    val mediaStorageKey: String?,
    val mediaMimeType: String?,
    val liveEndsAt: String?,
    val createdAt: String,
    val createdAtEpochMillis: Long,
    val deliveredAt: String?,
)
