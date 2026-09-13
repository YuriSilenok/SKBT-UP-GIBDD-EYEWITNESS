package com.example.skbt_up_gibdd_eyewitness.data.device

import com.example.skbt_up_gibdd_eyewitness.core.device.FingerprintProvider
import com.example.skbt_up_gibdd_eyewitness.core.network.DeviceApi
import com.example.skbt_up_gibdd_eyewitness.core.network.ActiveBanResponse
import com.example.skbt_up_gibdd_eyewitness.core.network.RegisterDeviceRequest
import com.example.skbt_up_gibdd_eyewitness.core.network.PushTokenUpdateRequest
import com.example.skbt_up_gibdd_eyewitness.core.storage.SecureDeviceStorage
import com.example.skbt_up_gibdd_eyewitness.domain.device.DeviceRepository
import com.example.skbt_up_gibdd_eyewitness.domain.device.DeviceSession
import com.example.skbt_up_gibdd_eyewitness.domain.device.ActiveBan
import java.time.Instant
import com.example.skbt_up_gibdd_eyewitness.BuildConfig

class DefaultDeviceRepository(
    private val api: DeviceApi,
    private val fingerprintProvider: FingerprintProvider,
    private val storage: SecureDeviceStorage,
) : DeviceRepository {
    override suspend fun register(pushToken: String?): Result<DeviceSession> = runCatching {
        val response = api.register(
            RegisterDeviceRequest(
                fingerprintHash = fingerprintProvider.fingerprintHash(),
                appVersion = BuildConfig.VERSION_NAME,
            ),
        )
        DeviceSession(
            deviceId = response.deviceId,
            witnessId = requireNotNull(response.witnessId),
            chatId = requireNotNull(response.chatId),
            accessToken = response.accessToken,
            banLevel = response.banLevel ?: 0,
        ).also { session ->
            storage.saveSession(session)
            pushToken?.let { token -> runCatching { api.updatePushToken(session.deviceId, PushTokenUpdateRequest(token)) } }
        }
    }

    override suspend fun updatePushToken(token: String): Result<Unit> = runCatching {
        val session = requireNotNull(storage.readSession()) { "Устройство не зарегистрировано" }
        api.updatePushToken(session.deviceId, PushTokenUpdateRequest(token))
        Unit
    }

    override suspend fun deletePushToken(): Result<Unit> = runCatching {
        val session = requireNotNull(storage.readSession()) { "Устройство не зарегистрировано" }
        api.deletePushToken(session.deviceId)
        storage.clearPushToken()
    }

    override fun savedSession(): DeviceSession? = storage.readSession()
    override fun isWelcomeCompleted(): Boolean = storage.isWelcomeCompleted()
    override fun markWelcomeCompleted() = storage.markWelcomeCompleted()

    override suspend fun getActiveBan(): Result<ActiveBan?> = runCatching {
        val session = requireNotNull(storage.readSession()) { "Устройство не зарегистрировано" }
        val response = api.getActiveBan(session.deviceId)
        storage.saveSession(session.copy(banLevel = response.banLevel ?: 0))
        response.toDomain()
    }

    private fun ActiveBanResponse.toDomain(): ActiveBan? {
        if (!active) return null
        return ActiveBan(
            id = requireNotNull(id) { "Backend не вернул id активной блокировки" },
            startedAt = Instant.parse(requireNotNull(issuedAt) { "Backend не вернул issued_at" }),
            endsAt = expiresAt?.let(Instant::parse),
            number = requireNotNull(banLevel) { "Backend не вернул ban_level" },
            reason = reason,
        )
    }
}
