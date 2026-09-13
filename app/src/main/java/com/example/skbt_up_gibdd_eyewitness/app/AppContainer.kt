package com.example.skbt_up_gibdd_eyewitness.app

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.skbt_up_gibdd_eyewitness.BuildConfig
import com.example.skbt_up_gibdd_eyewitness.core.device.FingerprintProvider
import com.example.skbt_up_gibdd_eyewitness.core.network.NetworkFactory
import com.example.skbt_up_gibdd_eyewitness.core.storage.SecureDeviceStorage
import com.example.skbt_up_gibdd_eyewitness.core.push.PushTokenProvider
import com.example.skbt_up_gibdd_eyewitness.data.device.DefaultDeviceRepository
import com.example.skbt_up_gibdd_eyewitness.data.message.DefaultMessageRepository
import com.example.skbt_up_gibdd_eyewitness.data.local.EyewitnessDatabase
import com.example.skbt_up_gibdd_eyewitness.data.realtime.DefaultRealtimeRepository
import com.example.skbt_up_gibdd_eyewitness.domain.device.DeviceRepository
import com.example.skbt_up_gibdd_eyewitness.domain.message.MessageRepository
import com.example.skbt_up_gibdd_eyewitness.domain.realtime.RealtimeRepository

class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        EyewitnessDatabase::class.java,
        "eyewitness.db",
    ).addMigrations(MIGRATION_1_2).build()
    private val storage = SecureDeviceStorage(context)
    private val fingerprintProvider = FingerprintProvider(context, storage)
    private val pushTokenProvider = PushTokenProvider(context)
    private val deviceApi = NetworkFactory.createDeviceApi(
        baseUrl = BuildConfig.API_BASE_URL,
        sessionProvider = storage::readSession,
        enableHttpLogs = BuildConfig.DEBUG,
    )
    private val messageApi = NetworkFactory.createMessageApi(
        baseUrl = BuildConfig.API_BASE_URL,
        sessionProvider = storage::readSession,
        enableHttpLogs = BuildConfig.DEBUG,
    )

    val deviceRepository: DeviceRepository = DefaultDeviceRepository(
        api = deviceApi,
        fingerprintProvider = fingerprintProvider,
        storage = storage,
    )
    val messageRepository: MessageRepository = DefaultMessageRepository(
        messageApi,
        storage,
        context.contentResolver,
        context.filesDir,
        BuildConfig.API_BASE_URL,
        database.messageDao(),
    )
    val realtimeRepository: RealtimeRepository = DefaultRealtimeRepository(
        baseUrl = BuildConfig.API_BASE_URL,
        sessionProvider = storage::readSession,
    )

    suspend fun registerDevice() = run {
        val token = pushTokenProvider.currentToken() ?: storage.readPushToken()
        token?.let(storage::savePushToken)
        deviceRepository.register(token)
    }

    suspend fun syncPushToken(token: String) = run {
        storage.savePushToken(token)
        deviceRepository.updatePushToken(token)
    }
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE messages ADD COLUMN locationSessionId TEXT")
    }
}
