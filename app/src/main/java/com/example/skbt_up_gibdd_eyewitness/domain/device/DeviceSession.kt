package com.example.skbt_up_gibdd_eyewitness.domain.device

data class DeviceSession(
    val deviceId: String,
    val witnessId: String,
    val chatId: String,
    val accessToken: String,
    val banLevel: Int,
)
