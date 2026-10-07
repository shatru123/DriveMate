package com.shatrughna.drivemate

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.shatrughna.drivemate.ui.home.DashboardScreen
import com.shatrughna.drivemate.ui.home.MainViewModel
import com.shatrughna.drivemate.ui.settings.SettingsScreen
import com.shatrughna.drivemate.ui.settings.SettingsViewModel
import com.shatrughna.drivemate.ui.theme.DarkBackground
import com.shatrughna.drivemate.ui.theme.DriveMateTheme
import com.shatrughna.drivemate.util.AppLogger

sealed class Screen {
    data object Dashboard : Screen()
    data object Settings : Screen()
}

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels {
        val app = application as DriveMateApplication
        MainViewModel.Factory(
            preferencesRepository = app.preferencesRepository,
            carConnectionManager = app.carConnectionManager,
            sessionManager = app.sessionManager,
            greetingController = app.greetingController,
            greetingGenerator = app.greetingGenerator,
            weatherRepository = app.weatherRepository,
            vehicleCareManager = app.vehicleCareManager,
            destinationManager = app.destinationManager,
            tripTracker = app.tripTracker,
            locationProvider = app.locationProvider,
            voiceAssistantManager = app.voiceAssistantManager,
            tripHistoryRepository = app.tripHistoryRepository,
            locationResolver = app.locationResolver,
            wakeWordManager = app.wakeWordManager
        )
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        val app = application as DriveMateApplication
        SettingsViewModel.Factory(
            preferencesRepository = app.preferencesRepository,
            greetingController = app.greetingController,
            greetingGenerator = app.greetingGenerator,
            ttsManager = app.ttsManager,
            destinationManager = app.destinationManager
        )
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        AppLogger.i(AppLogger.Tag.APP, "Notification permission result: isGranted=$isGranted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.i(AppLogger.Tag.APP, "MainActivity onCreate called.")

        checkNotificationPermission()

        setContent {
            DriveMateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    DriveMateAppNavigation(
                        mainViewModel = mainViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(permission)
            }
        }
    }
}

@Composable
fun DriveMateAppNavigation(
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState == Screen.Settings) {
                // Navigating to Settings: smooth slide in from right + fade in
                (slideInHorizontally(
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    initialOffsetX = { fullWidth -> (fullWidth * 0.25f).toInt() }
                ) + fadeIn(animationSpec = tween(260))).togetherWith(
                    slideOutHorizontally(
                        animationSpec = tween(260, easing = FastOutSlowInEasing),
                        targetOffsetX = { fullWidth -> -(fullWidth * 0.25f).toInt() }
                    ) + fadeOut(animationSpec = tween(260))
                )
            } else {
                // Navigating back to Dashboard: smooth slide in from left + fade in
                (slideInHorizontally(
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    initialOffsetX = { fullWidth -> -(fullWidth * 0.25f).toInt() }
                ) + fadeIn(animationSpec = tween(260))).togetherWith(
                    slideOutHorizontally(
                        animationSpec = tween(260, easing = FastOutSlowInEasing),
                        targetOffsetX = { fullWidth -> (fullWidth * 0.25f).toInt() }
                    ) + fadeOut(animationSpec = tween(260))
                )
            }
        },
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            Screen.Dashboard -> {
                DashboardScreen(
                    viewModel = mainViewModel,
                    onNavigateToSettings = { currentScreen = Screen.Settings }
                )
            }
            Screen.Settings -> {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { currentScreen = Screen.Dashboard }
                )
            }
        }
    }
}
