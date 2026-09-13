package com.example.skbt_up_gibdd_eyewitness.app

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchDestinationTest {
    @Test
    fun firstLaunch_opensWelcome() {
        assertEquals(
            LaunchDestination.WELCOME,
            resolveLaunchDestination(welcomeCompleted = false, hasSavedSession = false),
        )
    }

    @Test
    fun completedWelcomeWithSession_opensChat() {
        assertEquals(
            LaunchDestination.CHAT,
            resolveLaunchDestination(welcomeCompleted = true, hasSavedSession = true),
        )
    }

    @Test
    fun completedFlagWithoutSession_returnsToWelcome() {
        assertEquals(
            LaunchDestination.WELCOME,
            resolveLaunchDestination(welcomeCompleted = true, hasSavedSession = false),
        )
    }

    @Test
    fun savedSessionBeforeConsent_stillShowsWelcome() {
        assertEquals(
            LaunchDestination.WELCOME,
            resolveLaunchDestination(welcomeCompleted = false, hasSavedSession = true),
        )
    }
}
