package com.example.skbt_up_gibdd_eyewitness.data.local

import com.example.skbt_up_gibdd_eyewitness.domain.message.ChatMessage
import java.time.OffsetDateTime

fun ChatMessage.toEntity() = MessageEntity(
    id = id,
    observerDeviceId = observerDeviceId,
    senderDeviceId = senderDeviceId,
    text = text,
    type = type,
    staticLatitude = staticLatitude,
    staticLongitude = staticLongitude,
    mediaStorageKey = mediaStorageKey,
    mediaMimeType = mediaMimeType,
    liveEndsAt = liveEndsAt,
    createdAt = createdAt,
    createdAtEpochMillis = runCatching { OffsetDateTime.parse(createdAt).toInstant().toEpochMilli() }.getOrDefault(0L),
    deliveredAt = deliveredAt,
)

fun MessageEntity.toDomain() = ChatMessage(
    id = id,
    observerDeviceId = observerDeviceId,
    senderDeviceId = senderDeviceId,
    text = text,
    type = type,
    staticLatitude = staticLatitude,
    staticLongitude = staticLongitude,
    mediaStorageKey = mediaStorageKey,
    mediaMimeType = mediaMimeType,
    liveEndsAt = liveEndsAt,
    createdAt = createdAt,
    deliveredAt = deliveredAt,
)
