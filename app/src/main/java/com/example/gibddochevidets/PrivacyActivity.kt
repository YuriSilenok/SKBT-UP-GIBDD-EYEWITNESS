package com.example.gibddochevidets

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import com.example.gibddochevidets.network.ApiRepository
import kotlinx.coroutines.*

class PrivacyActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 48, 28, 32) }
        root.addView(TextView(this).apply {
            text = (if (BuildConfig.DEBUG) "Тестовая сборка. Используйте только вымышленные сведения.\n\n" else "") + "Трезвый водитель 44\n\nАнонимные сообщения о признаках опьянения водителей. Сообщения передаются уполномоченным сотрудникам Госавтоинспекции. ФИО, телефон и другие контакты не требуются. Ответ по существу сообщения не направляется. При непосредственной опасности звоните 112."
            textSize = 18f
        })
        for ((label, document) in listOf("Политика обработки персональных данных" to "policy", "Текст согласия" to "consent", "Регламент работы" to "regulation", "Сторонние компоненты и лицензии" to "licenses")) {
            root.addView(Button(this).apply {
                text = label
                setOnClickListener { startActivity(Intent(this@PrivacyActivity, LegalActivity::class.java).putExtra("document", document)) }
            })
        }
        val checkbox = CheckBox(this).apply { text = "Я ознакомлен(а) с Политикой и даю согласие на обработку персональных данных"; isChecked = false }
        root.addView(checkbox)
        val enter = Button(this).apply { text = "Продолжить"; isEnabled = false }
        var checkedAt = 0L
        checkbox.setOnCheckedChangeListener { _, checked ->
            checkedAt = if (checked) System.currentTimeMillis() else 0L
            enter.isEnabled = checked
        }
        enter.setOnClickListener {
            // Record the active action before creating any technical identity or calling the server.
            getSharedPreferences("privacy", MODE_PRIVATE).edit().putString("version", VERSION)
                .putString("policy_url", BuildConfig.PRIVACY_POLICY_URL)
                .putString("consent_url", BuildConfig.PRIVACY_CONSENT_URL)
                .putLong("accepted_at", checkedAt).remove("submitted_at").apply()
            startActivity(Intent(this, MainActivity::class.java)); finish()
        }
        root.addView(enter)
        if (com.example.gibddochevidets.data.SessionManager(this).deviceId != null) {
            root.addView(Button(this).apply {
                text = "Отозвать согласие и удалить мои данные"
                setOnClickListener {
                    AlertDialog.Builder(this@PrivacyActivity).setMessage("Удалить сообщения, вложения и очередь отправки из приложения? Уже полученные Госавтоинспекцией сведения удаляются по обращению в Госавтоинспекцию.")
                        .setNegativeButton("Отмена", null).setPositiveButton("Удалить") { _, _ ->
                            isEnabled = false
                            scope.launch {
                                try {
                                    val result = withContext(Dispatchers.IO) { ApiRepository(this@PrivacyActivity).withdrawPrivacyConsent() }
                                    getSharedPreferences("privacy", MODE_PRIVATE).edit().clear().apply()
                                    getSharedPreferences("offline_queue", MODE_PRIVATE).edit().clear().apply()
                                    getSharedPreferences("receipt_notices", MODE_PRIVATE).edit().clear().apply()
                                    cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                                    getExternalFilesDir(null)?.listFiles()?.forEach { it.deleteRecursively() }
                                    Toast.makeText(this@PrivacyActivity, result["notice"].toString(), Toast.LENGTH_LONG).show()
                                    startActivity(Intent(this@PrivacyActivity, PrivacyActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)); finish()
                                } catch (_: Exception) {
                                    isEnabled = true
                                    Toast.makeText(this@PrivacyActivity, "Удаление не подтверждено сервером. Проверьте соединение и повторите.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }.show()
                }
            })
        }
        setContentView(ScrollView(this).apply { addView(root) })
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
    companion object {
        const val VERSION = BuildConfig.CONSENT_VERSION
        fun hasLocalConsent(context: Context): Boolean {
            val p = context.getSharedPreferences("privacy", Context.MODE_PRIVATE)
            return p.getString("version", null) == VERSION &&
                p.getString("policy_url", null) == BuildConfig.PRIVACY_POLICY_URL &&
                p.getString("consent_url", null) == BuildConfig.PRIVACY_CONSENT_URL &&
                System.currentTimeMillis() - p.getLong("accepted_at", 0) in 0 until 30L * 24 * 60 * 60 * 1000
        }
    }
}
