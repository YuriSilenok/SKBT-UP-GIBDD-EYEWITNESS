package com.example.skbt_up_gibdd_eyewitness.feature.welcome

import androidx.compose.ui.test.assertIsDisplayed
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.skbt_up_gibdd_eyewitness.ui.theme.SKBTUPGIBDDEYEWITNESSTheme
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WelcomeScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun startButtonCallsRegistrationAction() {
        val started = AtomicBoolean(false)
        composeRule.setContent {
            SKBTUPGIBDDEYEWITNESSTheme {
                WelcomeScreen(onStartClick = {
                    started.set(true)
                    Result.success(Unit)
                })
            }
        }

        composeRule.onNodeWithText("Уважаемые участники дорожного движения!").assertIsDisplayed()
        composeRule.onNodeWithText("Начать").performScrollTo().performClick()
        composeRule.waitUntil(2_000) { started.get() }

        assertTrue(started.get())
    }
}
