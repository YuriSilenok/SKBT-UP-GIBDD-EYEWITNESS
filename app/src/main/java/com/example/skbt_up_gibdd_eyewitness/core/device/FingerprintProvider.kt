package com.example.skbt_up_gibdd_eyewitness.core.device

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.example.skbt_up_gibdd_eyewitness.core.storage.SecureDeviceStorage
import java.security.MessageDigest
import java.util.UUID

class FingerprintProvider(
    private val context: Context,
    private val storage: SecureDeviceStorage,
) {
    fun fingerprintHash(): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?.takeUnless { it.isBlank() || it == UNRELIABLE_ANDROID_ID }
            ?: storage.readFallbackUuid()
            ?: UUID.randomUUID().toString().also(storage::saveFallbackUuid)

        val source = listOf(
            androidId,
            Build.BOARD,
            Build.BRAND,
            Build.DEVICE,
            Build.HARDWARE,
            Build.MANUFACTURER,
            Build.MODEL,
            Build.PRODUCT,
        ).joinToString(separator = "|")

        return sha256Hex(source)
    }

    companion object {
        private const val UNRELIABLE_ANDROID_ID = "9774d56d682e549c"

        internal fun sha256Hex(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
