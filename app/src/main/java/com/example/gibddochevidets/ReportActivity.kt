package com.example.gibddochevidets

import android.app.Activity
import android.os.Bundle
import android.widget.*
import com.example.gibddochevidets.network.ApiRepository
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ReportActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        if (!PrivacyActivity.hasLocalConsent(this)) { finish(); return }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 48, 28, 28) }
        root.addView(TextView(this).apply {
            text = "Сообщить о событии\n\nУкажите известные сведения. ФИО и контакты не нужны. Фото, видео и координаты можно добавить в основном окне."
            textSize = 18f
        })
        fun field(label: String, initial: String = "") = EditText(this).apply {
            hint = label; setText(initial); filters = arrayOf(android.text.InputFilter.LengthFilter(800)); root.addView(this)
        }
        val time = field("Дата и время события", java.time.ZonedDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm XXX")))
        val place = field("Место, адрес или ориентир")
        val plate = field("Государственный номер (если известен)")
        val vehicle = field("Марка, модель и цвет автомобиля")
        val details = field("Признаки опьянения, поведение, направление движения")
        root.addView(Button(this).apply {
            text = "Отправить сообщение"
            setOnClickListener {
                if (place.text.isBlank() || details.text.isBlank()) {
                    Toast.makeText(this@ReportActivity, "Укажите место и обстоятельства события", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val body = "Дата и время: ${time.text}\nМесто: ${place.text}\nГосномер: ${plate.text}\nАвтомобиль: ${vehicle.text}\nОбстоятельства: ${details.text}"
                if (body.length > 4096) { Toast.makeText(this@ReportActivity, "Сократите описание", Toast.LENGTH_LONG).show(); return@setOnClickListener }
                isEnabled = false
                scope.launch {
                    try {
                        val result = withContext(Dispatchers.IO) { ApiRepository(this@ReportActivity).sendMessage(body) }
                        android.app.AlertDialog.Builder(this@ReportActivity).setTitle("Сообщение зарегистрировано")
                            .setMessage("Номер: ${result.message_id}\nСообщение доступно уполномоченным сотрудникам Госавтоинспекции. Ответ по существу не направляется.")
                            .setPositiveButton("Готово") { _, _ -> finish() }.setCancelable(false).show()
                    } catch (_: Exception) {
                        isEnabled = true
                        Toast.makeText(this@ReportActivity, "Отправка не подтверждена. Проверьте соединение и повторите.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
        setContentView(ScrollView(this).apply { addView(root) })
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
