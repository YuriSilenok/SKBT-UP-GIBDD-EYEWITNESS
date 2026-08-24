package com.example.skbt_up_gibdd_eyewitness.feature.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.skbt_up_gibdd_eyewitness.ui.theme.SKBTUPGIBDDEYEWITNESSTheme
import org.junit.Rule
import org.junit.Test

class ChatScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun textCanBeEnteredAndSent() {
        composeRule.setContent {
            SKBTUPGIBDDEYEWITNESSTheme {
                ChatScreen(onBackClick = {}, requestNotificationPermission = false, requestGalleryPermission = false)
            }
        }

        composeRule.onNodeWithText("Сообщение").performClick().performTextInput("Тестовое сообщение")
        composeRule.onNodeWithContentDescription("Отправить").performClick()

        composeRule.onNodeWithText("Тестовое сообщение").assertIsDisplayed()
    }

    @Test
    fun attachmentButtonOpensMediaSheet() {
        composeRule.setContent {
            SKBTUPGIBDDEYEWITNESSTheme {
                ChatScreen(onBackClick = {}, requestNotificationPermission = false, requestGalleryPermission = false)
            }
        }

        composeRule.onNodeWithContentDescription("Прикрепить фото, видео или GIF").performClick()

        composeRule.onNodeWithText("Фото и видео").assertIsDisplayed()
        composeRule.onNodeWithText("Галерея").assertIsDisplayed()
        composeRule.onNodeWithText("Геолокация").assertIsDisplayed()
    }
}
