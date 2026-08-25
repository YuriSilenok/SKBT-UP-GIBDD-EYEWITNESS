package com.example.skbt_up_gibdd_eyewitness.data.device

import com.example.skbt_up_gibdd_eyewitness.core.device.FingerprintProvider
import com.example.skbt_up_gibdd_eyewitness.core.network.DeviceApi
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
    @Volatile
    private var lastRegistrationAtMillis: Long = 0L

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
            lastRegistrationAtMillis = System.currentTimeMillis()
            pushToken?.let { token -> runCatching { api.updatePushToken(session.deviceId, PushTokenUpdateRequest(token)) } }
        }
    }

    override suspend fun updatePushToken(token: String): Result<Unit> = runCatching {
        val session = requireNotNull(storage.readSession()) { "Устройство не зарегистрировано" }
        api.updatePushToken(session.deviceId, PushTokenUpdateRequest(token))
        Unit
    }

    override fun savedSession(): DeviceSession? = storage.readSession()
    override fun isWelcomeCompleted(): Boolean = storage.isWelcomeCompleted()
    override fun markWelcomeCompleted() = storage.markWelcomeCompleted()

    override suspend fun getActiveBan(): Result<ActiveBan?> = runCatching {
        val cachedSession = storage.readSession()
        if (cachedSession != null && System.currentTimeMillis() - lastRegistrationAtMillis < BAN_REFRESH_INTERVAL_MILLIS) {
            return@runCatching cachedSession.toActiveBanOrNull()
        }
        val response = api.register(
            RegisterDeviceRequest(
                fingerprintHash = fingerprintProvider.fingerprintHash(),
                appVersion = BuildConfig.VERSION_NAME,
            ),
        )
        val refreshedSession = DeviceSession(
            deviceId = response.deviceId,
            witnessId = requireNotNull(response.witnessId),
            chatId = requireNotNull(response.chatId),
            accessToken = response.accessToken,
            banLevel = response.banLevel ?: 0,
        ).also {
            storage.saveSession(it)
            lastRegistrationAtMillis = System.currentTimeMillis()
        }
        refreshedSession.toActiveBanOrNull()
    }

    private fun DeviceSession.toActiveBanOrNull(): ActiveBan? = takeIf { it.banLevel > 0 }?.let { session ->
        ActiveBan(
            id = "current-ban",
            startedAt = Instant.now(),
            endsAt = null,
            number = session.banLevel,
        )
    }

    private companion object {
        const val BAN_REFRESH_INTERVAL_MILLIS = 25_000L
    }
}
