package com.example.skbt_up_gibdd_eyewitness.core.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.skbt_up_gibdd_eyewitness.domain.device.DeviceSession
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureDeviceStorage(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun saveSession(session: DeviceSession) {
        writeEncrypted(KEY_DEVICE_ID, session.deviceId)
        writeEncrypted(KEY_WITNESS_ID, session.witnessId)
        writeEncrypted(KEY_CHAT_ID, session.chatId)
        writeEncrypted(KEY_ACCESS_TOKEN, session.accessToken)
        writeEncrypted(KEY_BAN_LEVEL, session.banLevel.toString())
    }

    fun readSession(): DeviceSession? {
        val deviceId = readEncrypted(KEY_DEVICE_ID) ?: return null
        val witnessId = readEncrypted(KEY_WITNESS_ID) ?: return null
        val chatId = readEncrypted(KEY_CHAT_ID) ?: return null
        val accessToken = readEncrypted(KEY_ACCESS_TOKEN) ?: return null
        return DeviceSession(deviceId, witnessId, chatId, accessToken, readEncrypted(KEY_BAN_LEVEL)?.toIntOrNull() ?: 0)
    }

    fun saveFallbackUuid(uuid: String) = writeEncrypted(KEY_FALLBACK_UUID, uuid)
    fun readFallbackUuid(): String? = readEncrypted(KEY_FALLBACK_UUID)
    fun markWelcomeCompleted() = preferences.edit().putBoolean(KEY_WELCOME_COMPLETED, true).apply()
    fun isWelcomeCompleted(): Boolean = preferences.getBoolean(KEY_WELCOME_COMPLETED, false)
    fun savePendingTextMessages(json: String) = writeEncrypted(KEY_PENDING_TEXT_MESSAGES, json)
    fun readPendingTextMessages(): String? = readEncrypted(KEY_PENDING_TEXT_MESSAGES)
    fun savePendingStaticLocations(json: String) = writeEncrypted(KEY_PENDING_STATIC_LOCATIONS, json)
    fun readPendingStaticLocations(): String? = readEncrypted(KEY_PENDING_STATIC_LOCATIONS)
    fun savePendingMediaMessages(json: String) = writeEncrypted(KEY_PENDING_MEDIA_MESSAGES, json)
    fun readPendingMediaMessages(): String? = readEncrypted(KEY_PENDING_MEDIA_MESSAGES)
    fun saveHistoryCutoff(epochMillis: Long) = writeEncrypted(KEY_HISTORY_CUTOFF, epochMillis.toString())
    fun readHistoryCutoff(): Long = readEncrypted(KEY_HISTORY_CUTOFF)?.toLongOrNull() ?: 0L
    fun savePushToken(token: String) = writeEncrypted(KEY_PUSH_TOKEN, token)
    fun readPushToken(): String? = readEncrypted(KEY_PUSH_TOKEN)

    private fun writeEncrypted(key: String, value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, secretKey())
        }
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val packed = ByteBuffer.allocate(Int.SIZE_BYTES + cipher.iv.size + ciphertext.size)
            .putInt(cipher.iv.size)
            .put(cipher.iv)
            .put(ciphertext)
            .array()
        preferences.edit().putString(key, Base64.encodeToString(packed, Base64.NO_WRAP)).apply()
    }

    private fun readEncrypted(key: String): String? {
        val encoded = preferences.getString(key, null) ?: return null
        return runCatching {
            val packed = ByteBuffer.wrap(Base64.decode(encoded, Base64.NO_WRAP))
            val iv = ByteArray(packed.int).also(packed::get)
            val ciphertext = ByteArray(packed.remaining()).also(packed::get)
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            }
            cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
        }.getOrNull()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "secure_device_session"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_WITNESS_ID = "witness_id"
        const val KEY_CHAT_ID = "chat_id"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_BAN_LEVEL = "ban_level"
        const val KEY_FALLBACK_UUID = "fallback_uuid"
        const val KEY_WELCOME_COMPLETED = "welcome_completed"
        const val KEY_PENDING_TEXT_MESSAGES = "pending_text_messages"
        const val KEY_PENDING_STATIC_LOCATIONS = "pending_static_locations"
        const val KEY_PENDING_MEDIA_MESSAGES = "pending_media_messages"
        const val KEY_HISTORY_CUTOFF = "history_cutoff"
        const val KEY_PUSH_TOKEN = "push_token"
        const val KEY_ALIAS = "eyewitness_device_key"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
