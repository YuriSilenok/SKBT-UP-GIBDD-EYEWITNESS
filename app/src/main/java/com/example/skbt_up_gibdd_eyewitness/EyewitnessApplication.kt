package com.example.skbt_up_gibdd_eyewitness

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.skbt_up_gibdd_eyewitness.app.AppContainer
import com.yandex.mapkit.MapKitFactory
import com.example.skbt_up_gibdd_eyewitness.feature.push.EyewitnessMessagingService

class EyewitnessApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    EyewitnessMessagingService.CHANNEL_ID,
                    "События ГИБДД",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = "Сообщения и изменения блокировки" },
            )
        }
        if (BuildConfig.MAPKIT_API_KEY.isNotBlank()) {
            MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
            MapKitFactory.initialize(this)
        }
    }
}
