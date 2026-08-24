package com.example.skbt_up_gibdd_eyewitness.domain.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class ActiveBanTest {
    @Test
    fun permanentBan_hasNoEndDate() {
        val ban = ActiveBan(
            id = "ban-1",
            startedAt = Instant.parse("2026-08-23T12:00:00Z"),
            endsAt = null,
            number = 3,
        )

        assertTrue(ban.isPermanent)
        assertNull(ban.formattedEnd())
    }

    @Test
    fun temporaryBan_formatsUtcTimeInMoscowZone() {
        val ban = ActiveBan(
            id = "ban-2",
            startedAt = Instant.parse("2026-08-23T12:00:00Z"),
            endsAt = Instant.parse("2026-08-24T12:00:00Z"),
            number = 1,
        )

        assertFalse(ban.isPermanent)
        assertEquals(
            "24 августа 2026, 15:00",
            ban.formattedEnd(ZoneId.of("Europe/Moscow"), Locale.forLanguageTag("ru")),
        )
    }
}
