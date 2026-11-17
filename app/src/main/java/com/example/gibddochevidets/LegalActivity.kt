package com.example.gibddochevidets

import android.app.Activity
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView

class LegalActivity : Activity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val document = intent.getStringExtra("document") ?: "policy"
        if (document !in setOf("policy", "consent", "regulation", "licenses")) { finish(); return }
        val text = assets.open("legal/$document.txt").bufferedReader().use { it.readText() }
        setContentView(ScrollView(this).apply {
            addView(TextView(this@LegalActivity).apply { this.text = text; textSize = 16f; setPadding(28, 48, 28, 48) })
        })
    }
}
