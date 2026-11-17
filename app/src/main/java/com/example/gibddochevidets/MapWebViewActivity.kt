package com.example.gibddochevidets

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.text.InputType
import android.widget.*

class MapWebViewActivity : Activity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 48, 32, 32) }
        root.addView(TextView(this).apply { text = "Место события\nВведите координаты. Они передаются только в Госавтоинспекцию через сервер приложения."; textSize = 18f })
        fun field(hintText: String, key: String): EditText = EditText(this).apply {
            hint = hintText; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            if (intent.hasExtra(key)) setText(intent.getDoubleExtra(key, 0.0).toString())
            root.addView(this)
        }
        val latitude = field("Широта от −90 до 90", EXTRA_LATITUDE)
        val longitude = field("Долгота от −180 до 180", EXTRA_LONGITUDE)
        root.addView(Button(this).apply {
            text = "Выбрать координаты"
            setOnClickListener {
                val lat = latitude.text.toString().replace(',', '.').toDoubleOrNull()
                val lon = longitude.text.toString().replace(',', '.').toDoubleOrNull()
                if (lat == null || lon == null || !lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0) {
                    Toast.makeText(this@MapWebViewActivity, "Проверьте координаты", Toast.LENGTH_LONG).show()
                } else {
                    setResult(RESULT_OK, Intent().putExtra(EXTRA_LATITUDE, lat).putExtra(EXTRA_LONGITUDE, lon)); finish()
                }
            }
        })
        setContentView(root)
    }
    companion object {
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
    }
}
