package com.example.skbt_up_gibdd_eyewitness.domain.device

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ActiveBan(
    val id: String,
    val startedAt: Instant,
    val endsAt: Instant?,
    val number: Int,
) {
    val isPermanent: Boolean get() = number >= 3 && endsAt == null
}

fun ActiveBan.formattedEnd(
    zoneId: ZoneId = ZoneId.of("Europe/Moscow"),
    locale: Locale = Locale.forLanguageTag("ru"),
): String? = endsAt?.atZone(zoneId)?.format(
    DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", locale),
)
