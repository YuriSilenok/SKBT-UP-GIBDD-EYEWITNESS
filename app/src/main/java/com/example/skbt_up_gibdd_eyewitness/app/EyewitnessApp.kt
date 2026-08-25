package com.example.skbt_up_gibdd_eyewitness.app

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.skbt_up_gibdd_eyewitness.feature.chat.ChatScreen
import com.example.skbt_up_gibdd_eyewitness.feature.welcome.WelcomeScreen
import com.example.skbt_up_gibdd_eyewitness.EyewitnessApplication
import com.example.skbt_up_gibdd_eyewitness.feature.location.LocationPickerScreen
import com.example.skbt_up_gibdd_eyewitness.feature.location.StaticLocation

private object Route {
    const val Welcome = "welcome"
    const val Chat = "chat"
    const val LocationPicker = "location_picker"
}

@Composable
fun EyewitnessApp() {
    val container = (LocalContext.current.applicationContext as EyewitnessApplication).container
    val deviceRepository = container.deviceRepository
    val initialDestination = remember {
        resolveLaunchDestination(
            welcomeCompleted = deviceRepository.isWelcomeCompleted(),
            hasSavedSession = deviceRepository.savedSession() != null,
        )
    }
    var startupReady by remember { mutableStateOf(initialDestination == LaunchDestination.WELCOME) }
    var startupError by remember { mutableStateOf(false) }
    var startupAttempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(initialDestination, startupAttempt) {
        if (initialDestination == LaunchDestination.CHAT) {
            startupError = false
            container.registerDevice()
                .onSuccess { startupReady = true }
                .onFailure { startupError = true }
        }
    }

    if (!startupReady) {
        StartupConnectionScreen(
            hasError = startupError,
            onRetry = {
                startupError = false
                startupAttempt++
            },
        )
        return
    }

    val navController = rememberNavController()
    var selectedStaticLocation by remember { mutableStateOf<StaticLocation?>(null) }

    NavHost(
        navController = navController,
        startDestination = if (initialDestination == LaunchDestination.CHAT) Route.Chat else Route.Welcome,
    ) {
        composable(Route.Welcome) {
            WelcomeScreen(onStartClick = {
                container.registerDevice().map {
                    deviceRepository.markWelcomeCompleted()
                    navController.navigate(Route.Chat) {
                        launchSingleTop = true
                        popUpTo(Route.Welcome) { inclusive = true }
                    }
                }
            })
        }
        composable(Route.Chat) {
            ChatScreen(
                onBackClick = navController::navigateUp,
                deviceRepository = container.deviceRepository,
                messageRepository = container.messageRepository,
                realtimeRepository = container.realtimeRepository,
                selectedStaticLocation = selectedStaticLocation,
                onStaticLocationConsumed = { selectedStaticLocation = null },
                onOpenStaticLocationPicker = { navController.navigate(Route.LocationPicker) },
            )
        }
        composable(Route.LocationPicker) {
            LocationPickerScreen(
                onBackClick = navController::navigateUp,
                onLocationSelected = { location ->
                    selectedStaticLocation = location
                    navController.navigateUp()
                },
            )
        }
    }
}
