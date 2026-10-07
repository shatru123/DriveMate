package com.shatrughna.drivemate

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shatrughna.drivemate.ui.analytics.DrivingAnalyticsScreen
import com.shatrughna.drivemate.ui.climate.ClimateScreen
import com.shatrughna.drivemate.ui.documents.DocumentVaultScreen
import com.shatrughna.drivemate.ui.expenses.ExpenseScreen
import com.shatrughna.drivemate.ui.home.DashboardScreen
import com.shatrughna.drivemate.ui.home.MainViewModel
import com.shatrughna.drivemate.ui.maintenance.MaintenanceScreen
import com.shatrughna.drivemate.ui.mycar.MyCarScreen
import com.shatrughna.drivemate.ui.parking.ParkingModeScreen
import com.shatrughna.drivemate.ui.settings.SettingsScreen
import com.shatrughna.drivemate.ui.settings.SettingsViewModel
import com.shatrughna.drivemate.ui.splash.SplashScreen
import com.shatrughna.drivemate.ui.theme.DarkBackground
import com.shatrughna.drivemate.ui.theme.DriveMateTheme
import com.shatrughna.drivemate.ui.timeline.VehicleTimelineScreen
import com.shatrughna.drivemate.util.AppLogger

sealed class Screen {
    data object Splash : Screen()
    data object Dashboard : Screen()
    data object MyCar : Screen()
    data object DocumentVault : Screen()
    data object Maintenance : Screen()
    data object Expenses : Screen()
    data object DrivingAnalytics : Screen()
    data object VehicleTimeline : Screen()
    data object Climate : Screen()
    data object ParkingMode : Screen()
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
            wakeWordManager = app.wakeWordManager,
            capabilityManager = app.capabilityManager,
            climateControlProvider = app.climateControlProvider,
            documentVaultRepository = app.documentVaultRepository,
            maintenanceRepository = app.maintenanceRepository,
            expenseRepository = app.expenseRepository,
            timelineRepository = app.timelineRepository
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
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }

    // Intercept back presses when on sub-screens to return safely to Dashboard or MyCar
    BackHandler(enabled = currentScreen != Screen.Dashboard && currentScreen != Screen.Splash) {
        currentScreen = when (currentScreen) {
            Screen.DocumentVault, Screen.Maintenance, Screen.Expenses,
            Screen.Climate, Screen.ParkingMode, Screen.VehicleTimeline -> Screen.MyCar
            else -> Screen.Dashboard
        }
    }

    val settings by mainViewModel.settings.collectAsStateWithLifecycle()
    val capabilities by mainViewModel.capabilities.collectAsStateWithLifecycle()
    val documents by mainViewModel.documents.collectAsStateWithLifecycle()
    val serviceSchedule by mainViewModel.serviceSchedule.collectAsStateWithLifecycle()
    val serviceRecords by mainViewModel.serviceRecords.collectAsStateWithLifecycle()
    val expenses by mainViewModel.expenses.collectAsStateWithLifecycle()
    val expenseSummary by mainViewModel.expenseSummary.collectAsStateWithLifecycle()
    val timelineItems by mainViewModel.timelineItems.collectAsStateWithLifecycle()
    val monthlyDrivingSummary by mainViewModel.monthlyDrivingSummary.collectAsStateWithLifecycle()
    val topInsight by mainViewModel.topInsight.collectAsStateWithLifecycle()
    val recentTrips by mainViewModel.recentTrips.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState == Screen.Splash) {
                fadeIn(animationSpec = tween(200)).togetherWith(fadeOut(animationSpec = tween(200)))
            } else if (targetState != Screen.Dashboard) {
                // Navigating deeper: smooth slide in from right + fade in
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
            Screen.Splash -> {
                SplashScreen(
                    onSplashComplete = { currentScreen = Screen.Dashboard }
                )
            }
            Screen.Dashboard -> {
                DashboardScreen(
                    viewModel = mainViewModel,
                    onNavigateToSettings = { currentScreen = Screen.Settings },
                    onNavigateToMyCar = { currentScreen = Screen.MyCar },
                    onNavigateToDocuments = { currentScreen = Screen.DocumentVault },
                    onNavigateToMaintenance = { currentScreen = Screen.Maintenance },
                    onNavigateToExpenses = { currentScreen = Screen.Expenses },
                    onNavigateToAnalytics = { currentScreen = Screen.DrivingAnalytics },
                    onNavigateToParking = { currentScreen = Screen.ParkingMode }
                )
            }
            Screen.MyCar -> {
                MyCarScreen(
                    settings = settings,
                    capabilities = capabilities,
                    topInsight = topInsight,
                    documentCount = documents.size,
                    nextServiceDueKm = serviceSchedule.nextServiceOdometerKm,
                    totalExpenses = expenseSummary.totalSpent,
                    onNavigateToDocuments = { currentScreen = Screen.DocumentVault },
                    onNavigateToMaintenance = { currentScreen = Screen.Maintenance },
                    onNavigateToExpenses = { currentScreen = Screen.Expenses },
                    onNavigateToClimate = { currentScreen = Screen.Climate },
                    onNavigateToParking = { currentScreen = Screen.ParkingMode },
                    onNavigateToAnalytics = { currentScreen = Screen.DrivingAnalytics },
                    onNavigateToTimeline = { currentScreen = Screen.VehicleTimeline },
                    onNavigateBack = { currentScreen = Screen.Dashboard }
                )
            }
            Screen.DocumentVault -> {
                DocumentVaultScreen(
                    documents = documents,
                    onAddDocument = mainViewModel::addDocument,
                    onDeleteDocument = mainViewModel::deleteDocument,
                    onNavigateBack = { currentScreen = Screen.MyCar }
                )
            }
            Screen.Maintenance -> {
                MaintenanceScreen(
                    currentOdometerKm = settings.odometerKm,
                    schedule = serviceSchedule,
                    records = serviceRecords,
                    onAddRecord = mainViewModel::addServiceRecord,
                    onDeleteRecord = mainViewModel::deleteServiceRecord,
                    onNavigateBack = { currentScreen = Screen.MyCar }
                )
            }
            Screen.Expenses -> {
                ExpenseScreen(
                    summary = expenseSummary,
                    expenses = expenses,
                    onAddExpense = mainViewModel::addExpense,
                    onDeleteExpense = mainViewModel::deleteExpense,
                    onNavigateBack = { currentScreen = Screen.MyCar }
                )
            }
            Screen.DrivingAnalytics -> {
                DrivingAnalyticsScreen(
                    summary = monthlyDrivingSummary,
                    trips = recentTrips,
                    onNavigateBack = { currentScreen = Screen.MyCar }
                )
            }
            Screen.VehicleTimeline -> {
                VehicleTimelineScreen(
                    items = timelineItems,
                    onNavigateBack = { currentScreen = Screen.MyCar }
                )
            }
            Screen.Climate -> {
                mainViewModel.climateControlProvider?.let { provider ->
                    ClimateScreen(
                        climateProvider = provider,
                        onNavigateBack = { currentScreen = Screen.MyCar }
                    )
                } ?: run {
                    currentScreen = Screen.MyCar
                }
            }
            Screen.ParkingMode -> {
                ParkingModeScreen(
                    settings = settings,
                    onSaveCurrentLocation = mainViewModel::saveCurrentParkingLocation,
                    onNavigateBack = { currentScreen = Screen.MyCar }
                )
            }
            Screen.Settings -> {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onToggleDemoMode = mainViewModel::setDemoMode,
                    onSeedDemoData = mainViewModel::seedDemoData,
                    onClearDemoData = mainViewModel::clearDemoData,
                    onNavigateBack = { currentScreen = Screen.Dashboard }
                )
            }
        }
    }
}
