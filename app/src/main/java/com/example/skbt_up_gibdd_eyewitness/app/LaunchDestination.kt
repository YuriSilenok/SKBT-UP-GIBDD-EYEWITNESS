package com.example.skbt_up_gibdd_eyewitness.app

enum class LaunchDestination { WELCOME, CHAT }

fun resolveLaunchDestination(
    welcomeCompleted: Boolean,
    hasSavedSession: Boolean,
): LaunchDestination = if (welcomeCompleted && hasSavedSession) {
    LaunchDestination.CHAT
} else {
    LaunchDestination.WELCOME
}
