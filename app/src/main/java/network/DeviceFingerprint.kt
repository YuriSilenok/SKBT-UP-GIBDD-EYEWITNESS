package com.example.gibddochevidets.data

import android.content.Context
import java.security.MessageDigest
import java.util.UUID

object DeviceFingerprint {
    fun get(context: Context): String {
        val session = SessionManager(context)
        session.fingerprintHash?.let { return it }
        val prefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
        val id = prefs.getString("install_id", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("install_id", it).apply()
        }
        return MessageDigest.getInstance("SHA-256").digest(id.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
