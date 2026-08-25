package com.example.skbt_up_gibdd_eyewitness.core.push

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class PushTokenProvider(private val context: Context) {
    suspend fun currentToken(): String? {
        if (FirebaseApp.getApps(context).isEmpty()) return null
        return suspendCancellableCoroutine { continuation ->
            FirebaseMessaging.getInstance().register().addOnCompleteListener { registration ->
                if (!registration.isSuccessful) {
                    if (continuation.isActive) continuation.resume(null)
                    return@addOnCompleteListener
                }
                FirebaseInstallations.getInstance().id.addOnCompleteListener { installation ->
                    if (continuation.isActive) {
                        continuation.resume(if (installation.isSuccessful) installation.result else null)
                    }
                }
            }
        }
    }
}
