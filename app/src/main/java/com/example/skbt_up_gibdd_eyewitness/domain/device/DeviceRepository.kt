package com.example.skbt_up_gibdd_eyewitness.domain.device

interface DeviceRepository {
    suspend fun register(pushToken: String? = null): Result<DeviceSession>
    suspend fun updatePushToken(token: String): Result<Unit>
    suspend fun deletePushToken(): Result<Unit>
    suspend fun getActiveBan(): Result<ActiveBan?>
    fun savedSession(): DeviceSession?
    fun isWelcomeCompleted(): Boolean
    fun markWelcomeCompleted()
}
