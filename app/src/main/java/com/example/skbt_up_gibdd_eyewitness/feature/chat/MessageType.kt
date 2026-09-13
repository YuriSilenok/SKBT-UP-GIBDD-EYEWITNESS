package com.example.skbt_up_gibdd_eyewitness.feature.chat

internal fun String.isTextMessageType(): Boolean = equals("text", ignoreCase = true)
