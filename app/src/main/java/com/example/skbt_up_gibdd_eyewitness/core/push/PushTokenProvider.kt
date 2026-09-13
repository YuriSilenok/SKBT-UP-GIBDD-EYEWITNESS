package com.example.skbt_up_gibdd_eyewitness.core.push

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class PushTokenProvider(private val context: Context) {
    suspend fun currentToken(): String? {
        if (FirebaseApp.getApps(context).isEmpty()) return null
        return suspendCancellableCoroutine { continuation ->
            @Suppress("DEPRECATION")
            FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                if (continuation.isActive) {
                    continuation.resume(if (tokenTask.isSuccessful) tokenTask.result else null)
                }
            }
        }
    }
}
