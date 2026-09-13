package com.example.skbt_up_gibdd_eyewitness.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Navy,
    secondary = OutgoingBubble,
    background = AppBackground,
    surface = IncomingBubble,
    onPrimary = IncomingBubble,
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = Muted,
)

@Composable
fun SKBTUPGIBDDEYEWITNESSTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
