package com.example.skbt_up_gibdd_eyewitness.feature.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.skbt_up_gibdd_eyewitness.EyewitnessApplication
import com.example.skbt_up_gibdd_eyewitness.MainActivity
import com.example.skbt_up_gibdd_eyewitness.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EyewitnessMessagingService : FirebaseMessagingService() {
    override fun onRegistered(installationId: String) {
        val application = applicationContext as EyewitnessApplication
        serviceScope.launch {
            application.container.syncPushToken(installationId)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "ГИБДД-Очевидец"
        val body = message.notification?.body ?: message.data["body"] ?: "В чате новое сообщение"
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Сообщения ГИБДД", NotificationManager.IMPORTANCE_HIGH),
            )
        }
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.notify(
            messageNotificationId(),
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_shield)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build(),
        )
    }

    private companion object {
        const val CHANNEL_ID = "chat_messages"
        val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        fun messageNotificationId(): Int = (System.currentTimeMillis() and 0x7FFFFFFF).toInt()
    }
}
