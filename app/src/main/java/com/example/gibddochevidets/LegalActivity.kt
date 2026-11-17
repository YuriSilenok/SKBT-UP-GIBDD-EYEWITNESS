package com.example.gibddochevidets

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class LegalActivity : Activity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val document = intent.getStringExtra("document") ?: "policy"
        if (document !in setOf("policy", "consent", "regulation", "licenses")) { finish(); return }
        val url = when (document) {
            "policy" -> BuildConfig.PRIVACY_POLICY_URL
            "consent" -> BuildConfig.PRIVACY_CONSENT_URL
            "regulation" -> BuildConfig.PRIVACY_REGULATION_URL
            else -> null
        }
        if (url != null) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, "Установите браузер для просмотра документа", Toast.LENGTH_LONG).show()
            }
            finish()
            return
        }
        val text = assets.open("legal/$document.txt").bufferedReader().use { it.readText() }
        setContentView(ScrollView(this).apply {
            addView(TextView(this@LegalActivity).apply { this.text = text; textSize = 16f; setPadding(28, 48, 28, 48) })
        })
    }
}
