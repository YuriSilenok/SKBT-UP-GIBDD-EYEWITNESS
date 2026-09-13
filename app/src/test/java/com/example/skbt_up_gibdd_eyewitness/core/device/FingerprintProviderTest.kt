package com.example.skbt_up_gibdd_eyewitness.core.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FingerprintProviderTest {
    @Test
    fun `sha256 is stable lowercase 64 character hex`() {
        val hash = FingerprintProvider.sha256Hex("ГИБДД-Очевидец")

        assertEquals(hash, FingerprintProvider.sha256Hex("ГИБДД-Очевидец"))
        assertEquals(64, hash.length)
        assertTrue(hash.matches(Regex("[0-9a-f]{64}")))
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            FingerprintProvider.sha256Hex("abc"),
        )
    }
}
