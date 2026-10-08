package com.shatrughna.drivemate.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shatrughna.drivemate.car.CarConnectionManager
import com.shatrughna.drivemate.car.CarConnectionState
import com.shatrughna.drivemate.care.VehicleCareManager
import com.shatrughna.drivemate.data.model.Destination
import com.shatrughna.drivemate.data.model.DriveMateSettings
import com.shatrughna.drivemate.data.model.RoutePoint
import com.shatrughna.drivemate.data.model.TripReport
import com.shatrughna.drivemate.data.model.TripStats
import com.shatrughna.drivemate.data.model.VehicleCareInfo
import com.shatrughna.drivemate.data.model.WeatherInfo
import com.shatrughna.drivemate.data.preferences.DriveMatePreferencesRepository
import com.shatrughna.drivemate.destination.DestinationManager
import com.shatrughna.drivemate.driving.DrivingSessionManager
import com.shatrughna.drivemate.driving.TripHistoryRepository
import com.shatrughna.drivemate.driving.TripTracker
import com.shatrughna.drivemate.greeting.GreetingController
import com.shatrughna.drivemate.greeting.GreetingGenerator
import com.shatrughna.drivemate.location.DeviceLocationProvider
import com.shatrughna.drivemate.location.WeatherLocationResolver
import com.shatrughna.drivemate.location.WeatherLocationResolverImpl
import com.shatrughna.drivemate.util.AppLogger
import com.shatrughna.drivemate.core.telemetry.VehicleTelemetry
import com.shatrughna.drivemate.core.telemetry.TelemetryAvailability
import com.shatrughna.drivemate.core.telemetry.VehicleTelemetryRepository
import com.shatrughna.drivemate.voice.AudioInputCoordinator
import com.shatrughna.drivemate.voice.AudioOwnerState
import com.shatrughna.drivemate.voice.VoiceAssistantManager
import com.shatrughna.drivemate.voice.VoiceAssistantState
import com.shatrughna.drivemate.voice.wakeword.WakeWordManager
import com.shatrughna.drivemate.voice.wakeword.WakeWordState
import com.shatrughna.drivemate.weather.WeatherRepository
import com.shatrughna.drivemate.core.capabilities.ClimateControlProvider
import com.shatrughna.drivemate.core.capabilities.VehicleCapabilitiesState
import com.shatrughna.drivemate.core.capabilities.VehicleCapabilityManager
import com.shatrughna.drivemate.core.insights.AiCarInsight
import com.shatrughna.drivemate.core.insights.AiCarInsightsEngine
import com.shatrughna.drivemate.data.analytics.DrivingAnalyticsEngine
import com.shatrughna.drivemate.data.analytics.MonthlyDrivingSummary
import com.shatrughna.drivemate.data.model.ExpenseSummary
import com.shatrughna.drivemate.data.model.ServiceRecord
import com.shatrughna.drivemate.data.model.ServiceSchedule
import com.shatrughna.drivemate.data.model.VehicleDocument
import com.shatrughna.drivemate.data.model.VehicleExpense
import com.shatrughna.drivemate.data.repository.DocumentVaultRepository
import com.shatrughna.drivemate.data.repository.ExpenseRepository
import com.shatrughna.drivemate.data.repository.MaintenanceRepository
import com.shatrughna.drivemate.data.timeline.VehicleTimelineItem
import com.shatrughna.drivemate.data.timeline.VehicleTimelineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val preferencesRepository: DriveMatePreferencesRepository,
    private val carConnectionManager: CarConnectionManager,
    private val sessionManager: DrivingSessionManager,
    private val greetingController: GreetingController,
    private val greetingGenerator: GreetingGenerator,
    private val weatherRepository: WeatherRepository,
    private val vehicleCareManager: VehicleCareManager,
    private val destinationManager: DestinationManager,
    private val tripTracker: TripTracker,
    private val locationProvider: DeviceLocationProvider? = null,
    private val voiceAssistantManager: VoiceAssistantManager? = null,
    private val tripHistoryRepository: TripHistoryRepository? = null,
    private val locationResolver: WeatherLocationResolver? = null,
    private val wakeWordManager: WakeWordManager? = null,
    private val capabilityManager: VehicleCapabilityManager? = null,
    val climateControlProvider: ClimateControlProvider? = null,
    private val documentVaultRepository: DocumentVaultRepository? = null,
    private val maintenanceRepository: MaintenanceRepository? = null,
    private val expenseRepository: ExpenseRepository? = null,
    private val timelineRepository: VehicleTimelineRepository? = null,
    private val vehicleTelemetryRepository: VehicleTelemetryRepository? = null,
    private val audioCoordinator: AudioInputCoordinator? = null
) : ViewModel() {

    /**
     * Vehicle providers may emit faster than a phone display needs to redraw.
     * Keep the source real-time while presenting a calm, bounded UI cadence.
     */
    val telemetry: StateFlow<VehicleTelemetry> = vehicleTelemetryRepository?.telemetry
        ?.sample(250L)
        ?.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = VehicleTelemetry()
        )
        ?: MutableStateFlow(VehicleTelemetry())

    val audioOwnerState: StateFlow<AudioOwnerState> = audioCoordinator?.state
        ?: MutableStateFlow(AudioOwnerState.IDLE)

    val settings: StateFlow<DriveMateSettings> = preferencesRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DriveMateSettings()
        )

    /** Service calculations prefer a live/stale vehicle odometer, then explicit manual calibration. */
    val serviceOdometerKm: StateFlow<Double?> = combine(settings, telemetry) { currentSettings, currentTelemetry ->
        currentTelemetry.vehicleOdometerKm?.takeIf {
            currentTelemetry.odometerAvailability == TelemetryAvailability.LIVE ||
                currentTelemetry.odometerAvailability == TelemetryAvailability.STALE
        } ?: currentSettings.effectiveOdometerKm
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val connectionState: StateFlow<CarConnectionState> = carConnectionManager.connectionState
    val isSessionActive: StateFlow<Boolean> = sessionManager.isSessionActive
    val hasGreetingPlayed: StateFlow<Boolean> = sessionManager.hasGreetingPlayed
    val isSpeaking: StateFlow<Boolean> = greetingController.isSpeaking
    val tripStats: StateFlow<TripStats> = tripTracker.tripStats
    val activeRoutePoints: StateFlow<List<RoutePoint>> = tripTracker.activeRoutePoints

    val latestTrip: StateFlow<TripReport?> = combine(
        tripTracker.latestCompletedTrip,
        tripHistoryRepository?.latestTrip ?: MutableStateFlow(null)
    ) { fromTracker, fromRepo ->
        fromTracker ?: fromRepo
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val voiceAssistantState: StateFlow<VoiceAssistantState> = voiceAssistantManager?.state
        ?: MutableStateFlow(VoiceAssistantState.Idle)

    val wakeWordState: StateFlow<WakeWordState> = wakeWordManager?.state
        ?: MutableStateFlow(WakeWordState.STOPPED)

    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    private val _weather = MutableStateFlow(WeatherInfo.unavailable())
    val weather: StateFlow<WeatherInfo> = _weather.asStateFlow()

    val vehicleCareInfo: StateFlow<VehicleCareInfo> = combine(settings, serviceOdometerKm) { currentSettings, odometerKm ->
        vehicleCareManager.getVehicleCareInfo(
            currentSettings.copy(
                manualOdometerKm = odometerKm,
                odometerKm = odometerKm
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VehicleCareInfo()
    )

    val suggestedDestinations: StateFlow<List<Destination>> = combine(settings, serviceOdometerKm) { currentSettings, odometerKm ->
        destinationManager.getSuggestedDestinations(
            currentSettings.copy(
                manualOdometerKm = odometerKm,
                odometerKm = odometerKm
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentDestinations: StateFlow<List<Destination>> = destinationManager.recentDestinations

    // V4 Vehicle Capabilities Flow
    val capabilities: StateFlow<VehicleCapabilitiesState> = capabilityManager?.capabilities
        ?: MutableStateFlow(VehicleCapabilitiesState())

    // V4 Documents Flow
    val documents: StateFlow<List<VehicleDocument>> = documentVaultRepository?.documents
        ?: MutableStateFlow(emptyList())

    // V4 Maintenance Flows
    val serviceSchedule: StateFlow<ServiceSchedule> = maintenanceRepository?.serviceSchedule
        ?: MutableStateFlow(ServiceSchedule())
    val serviceRecords: StateFlow<List<ServiceRecord>> = maintenanceRepository?.serviceRecords
        ?: MutableStateFlow(emptyList())

    // V4 Expense Flows
    val expenses: StateFlow<List<VehicleExpense>> = expenseRepository?.expenses
        ?: MutableStateFlow(emptyList())
    val expenseSummary: StateFlow<ExpenseSummary> = combine(expenses, settings) { _, sett ->
        sett.odometerKm?.let { expenseRepository?.getSummary(it) }
            ?: ExpenseSummary(0.0, 0.0, emptyMap(), 0.0)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ExpenseSummary(0.0, 0.0, emptyMap(), 0.0)
    )

    // V4 Timeline Flow
    val timelineItems: StateFlow<List<VehicleTimelineItem>> = timelineRepository?.timelineItems
        ?: MutableStateFlow(emptyList())

    // V4 Recent Trips Flow
    val recentTrips: StateFlow<List<TripReport>> = tripHistoryRepository?.recentTrips
        ?: MutableStateFlow(emptyList())

    // V4 Monthly Driving Analytics
    val monthlyDrivingSummary: StateFlow<MonthlyDrivingSummary> = combine(
        recentTrips
    ) { (trips) ->
        DrivingAnalyticsEngine.computeMonthlySummary(trips)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DrivingAnalyticsEngine.computeMonthlySummary(emptyList())
    )

    // V4 Deterministic AI Insights
    val topInsight: StateFlow<AiCarInsight?> = combine(
        settings,
        documents,
        serviceSchedule,
        expenseSummary,
        monthlyDrivingSummary
    ) { sett, docs, sched, expSum, driveSum ->
        sett.odometerKm?.let { AiCarInsightsEngine.generateInsights(it, docs, sched, expSum, driveSum).firstOrNull() }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun addDocument(doc: VehicleDocument) {
        viewModelScope.launch {
            documentVaultRepository?.addOrUpdateDocument(doc)
        }
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch {
            documentVaultRepository?.deleteDocument(id)
        }
    }

    fun addServiceRecord(record: ServiceRecord) {
        viewModelScope.launch {
            maintenanceRepository?.addServiceRecord(record)
        }
    }

    fun deleteServiceRecord(id: String) {
        viewModelScope.launch {
            maintenanceRepository?.deleteServiceRecord(id)
        }
    }

    fun addExpense(expense: VehicleExpense) {
        viewModelScope.launch {
            expenseRepository?.addExpense(expense)
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            expenseRepository?.deleteExpense(id)
        }
    }

    fun saveCurrentParkingLocation() {
        viewModelScope.launch {
            val loc = locationProvider?.getCurrentLocation()
            if (loc != null) {
                preferencesRepository.updateLastParkedLocation(loc.latitude, loc.longitude, "GPS Location Saved")
            }
        }
    }

    // Dynamically prepared greeting text for the dashboard
    val currentGreetingText: StateFlow<String> = combine(
        settings,
        greetingController.lastSpokenGreeting,
        weather
    ) { currentSettings, lastSpoken, currentWeather ->
        if (lastSpoken.isNotBlank()) {
            lastSpoken
        } else {
            val weatherForGreeting = if (currentSettings.includeWeatherInGreeting && currentWeather.isAvailable) currentWeather else null
            greetingGenerator.generateGreeting(
                driverName = currentSettings.driverName,
                vehicleBrand = currentSettings.vehicleBrand,
                vehicleModel = currentSettings.vehicleModel,
                vehicleVariant = currentSettings.vehicleVariant,
                style = currentSettings.greetingStyle,
                customTemplate = currentSettings.customGreetingTemplate,
                weatherInfo = weatherForGreeting,
                careReminder = vehicleCareManager.generateCareReminderPhrase(currentSettings)
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Welcome to DriveMate. Vehicle profile unavailable."
    )

    init {
        refreshWeather(forceRefresh = false)
    }

    fun hasLocationPermission(): Boolean {
        return locationProvider?.hasLocationPermission() == true
    }

    fun refreshWeather(forceRefresh: Boolean = true) {
        viewModelScope.launch {
            val currentSettings = settings.value
            val resolver = locationResolver ?: WeatherLocationResolverImpl(locationProvider)
            val resolvedLoc = resolver.resolveLocation(currentSettings)

            if (resolvedLoc.isAvailable) {
                _weather.value = weatherRepository.getCurrentWeather(
                    cityName = resolvedLoc.displayName ?: "",
                    latitude = resolvedLoc.latitude,
                    longitude = resolvedLoc.longitude,
                    forceRefresh = forceRefresh
                )
            } else {
                _weather.value = WeatherInfo.unavailable()
            }
        }
    }

    fun launchDestination(context: Context, destination: Destination) {
        destinationManager.launchNavigation(context, destination)
    }

    fun searchAndLaunchDestination(context: Context, query: String) {
        destinationManager.launchNavigationQuery(context, query)
    }

    fun clearRecentDestinations() {
        destinationManager.clearRecentDestinations()
    }

    fun toggleGreetingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateGreetingEnabled(enabled)
        }
    }

    fun toggleHeyDriveMate(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateHeyDriveMateEnabled(enabled)
            if (enabled) {
                wakeWordManager?.start()
            } else {
                wakeWordManager?.stop()
            }
        }
    }

    fun toggleVoiceAssistant(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateVoiceAssistantEnabled(enabled)
        }
    }

    fun previewGreeting() {
        viewModelScope.launch {
            AppLogger.i(AppLogger.Tag.GREETING, "User tapped Preview Greeting on Dashboard")
            greetingController.previewGreeting(settings.value)
        }
    }

    fun stopSpeaking() {
        greetingController.stopSpeaking()
    }

    fun toggleSimulation(enabled: Boolean) {
        _isSimulating.value = enabled
        carConnectionManager.setSimulatedConnection(enabled)
    }

    fun startVoiceAssistant() {
        voiceAssistantManager?.startListening()
    }

    fun stopVoiceAssistant() {
        voiceAssistantManager?.stopListening()
    }

    fun processVoiceTextCommand(commandText: String) {
        voiceAssistantManager?.processTextCommand(commandText)
    }

    fun navigateToParkedCar(context: Context) {
        val s = settings.value
        val lat = s.lastParkedLatitude ?: return
        val lon = s.lastParkedLongitude ?: return
        try {
            val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lon&mode=w")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                setPackage("com.google.android.apps.maps")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$lat,$lon(Parked vehicle)")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(geoIntent)
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.APP, "Failed to launch walking navigation: ${e.message}", e)
        }
    }

    fun setDemoMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateDemoModeEnabled(enabled)
            if (enabled) {
                documentVaultRepository?.seedDemoDocuments()
                maintenanceRepository?.seedDemoMaintenance()
                expenseRepository?.seedDemoExpenses()
            } else {
                documentVaultRepository?.clearDemoDocuments()
                maintenanceRepository?.clearDemoMaintenance()
                expenseRepository?.clearDemoExpenses()
            }
        }
    }

    fun seedDemoData() {
        viewModelScope.launch {
            documentVaultRepository?.seedDemoDocuments()
            maintenanceRepository?.seedDemoMaintenance()
            expenseRepository?.seedDemoExpenses()
        }
    }

    fun clearDemoData() {
        viewModelScope.launch {
            documentVaultRepository?.clearDemoDocuments()
            maintenanceRepository?.clearDemoMaintenance()
            expenseRepository?.clearDemoExpenses()
        }
    }

    class Factory(
        private val preferencesRepository: DriveMatePreferencesRepository,
        private val carConnectionManager: CarConnectionManager,
        private val sessionManager: DrivingSessionManager,
        private val greetingController: GreetingController,
        private val greetingGenerator: GreetingGenerator,
        private val weatherRepository: WeatherRepository,
        private val vehicleCareManager: VehicleCareManager,
        private val destinationManager: DestinationManager,
        private val tripTracker: TripTracker,
        private val locationProvider: DeviceLocationProvider? = null,
        private val voiceAssistantManager: VoiceAssistantManager? = null,
        private val tripHistoryRepository: TripHistoryRepository? = null,
        private val locationResolver: WeatherLocationResolver? = null,
        private val wakeWordManager: WakeWordManager? = null,
        private val capabilityManager: VehicleCapabilityManager? = null,
        private val climateControlProvider: ClimateControlProvider? = null,
        private val documentVaultRepository: DocumentVaultRepository? = null,
        private val maintenanceRepository: MaintenanceRepository? = null,
        private val expenseRepository: ExpenseRepository? = null,
        private val timelineRepository: VehicleTimelineRepository? = null,
        private val vehicleTelemetryRepository: VehicleTelemetryRepository? = null,
        private val audioCoordinator: AudioInputCoordinator? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(
                preferencesRepository,
                carConnectionManager,
                sessionManager,
                greetingController,
                greetingGenerator,
                weatherRepository,
                vehicleCareManager,
                destinationManager,
                tripTracker,
                locationProvider,
                voiceAssistantManager,
                tripHistoryRepository,
                locationResolver,
                wakeWordManager,
                capabilityManager,
                climateControlProvider,
                documentVaultRepository,
                maintenanceRepository,
                expenseRepository,
                timelineRepository,
                vehicleTelemetryRepository,
                audioCoordinator
            ) as T
        }
    }
}
