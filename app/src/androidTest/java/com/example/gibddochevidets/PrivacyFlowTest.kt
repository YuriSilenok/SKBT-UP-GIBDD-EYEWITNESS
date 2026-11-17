package com.example.gibddochevidets

import android.widget.Button
import android.widget.CheckBox
import android.view.View
import android.view.ViewGroup
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.gibddochevidets.data.SessionManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PrivacyFlowTest {
    @Test fun consent_requires_active_choice_before_creating_technical_session() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("privacy", 0).edit().clear().commit()
        context.getSharedPreferences("eyewitness_session", 0).edit().clear().commit()
        ActivityScenario.launch(PrivacyActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                fun views(view: View): List<View> = listOf(view) + if (view is ViewGroup)
                    (0 until view.childCount).flatMap { views(view.getChildAt(it)) } else emptyList()
                val all = views(activity.window.decorView)
                val consent = all.filterIsInstance<CheckBox>().single()
                val enter = all.filterIsInstance<Button>().single { it.text == "Продолжить" }
                assertFalse(consent.isChecked)
                assertFalse(enter.isEnabled)
                assertNull(SessionManager(context).deviceId)
                consent.isChecked = true
                assertTrue(enter.isEnabled)
                assertNull(SessionManager(context).deviceId)
                consent.isChecked = false
                assertFalse(enter.isEnabled)
                assertFalse(PrivacyActivity.hasLocalConsent(context))
                for (name in listOf("policy", "consent", "regulation", "licenses")) {
                    assertTrue(context.assets.open("legal/$name.txt").use { it.readBytes() }.size > 100)
                }
            }
        }
    }
}
