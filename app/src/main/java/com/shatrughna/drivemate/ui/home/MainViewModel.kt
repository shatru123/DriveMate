package com.shatrughna.drivemate.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shatrughna.drivemate.car.CarConnectionManager
import com.shatrughna.drivemate.car.CarConnectionState
import com.shatrughna.drivemate.care.VehicleCareManager
import com.shatrughna.drivemate.data.model.Destination
import com.shatrughna.drivemate.data.model.DriveMateSettings
import com.shatrughna.drivemate.data.model.TripStats
import com.shatrughna.drivemate.data.model.VehicleCareInfo
import com.shatrughna.drivemate.data.model.WeatherInfo
import com.shatrughna.drivemate.data.preferences.DriveMatePreferencesRepository
import com.shatrughna.drivemate.destination.DestinationManager
import com.shatrughna.drivemate.driving.DrivingSessionManager
import com.shatrughna.drivemate.driving.TripTracker
import com.shatrughna.drivemate.greeting.GreetingController
import com.shatrughna.drivemate.greeting.GreetingGenerator
import com.shatrughna.drivemate.util.AppLogger
import com.shatrughna.drivemate.location.DeviceLocationProvider
import com.shatrughna.drivemate.weather.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.shatrughna.drivemate.data.model.RoutePoint
import com.shatrughna.drivemate.data.model.TripReport
import com.shatrughna.drivemate.driving.TripHistoryRepository
import com.shatrughna.drivemate.voice.VoiceAssistantManager
import com.shatrughna.drivemate.voice.VoiceAssistantState
import android.net.Uri
import android.content.Intent

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
    private val tripHistoryRepository: TripHistoryRepository? = null
) : ViewModel() {

    val settings: StateFlow<DriveMateSettings> = preferencesRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DriveMateSettings()
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

    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    private val _weather = MutableStateFlow(WeatherInfo())
    val weather: StateFlow<WeatherInfo> = _weather.asStateFlow()

    val vehicleCareInfo: StateFlow<VehicleCareInfo> = combine(settings) { (currentSettings) ->
        vehicleCareManager.getVehicleCareInfo(currentSettings)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VehicleCareInfo()
    )

    val suggestedDestinations: StateFlow<List<Destination>> = combine(settings) { (currentSettings) ->
        destinationManager.getSuggestedDestinations(currentSettings)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dynamically prepared greeting text for the dashboard
    val currentGreetingText: StateFlow<String> = combine(
        settings,
        greetingController.lastSpokenGreeting,
        weather
    ) { currentSettings, lastSpoken, currentWeather ->
        if (lastSpoken.isNotBlank()) {
            lastSpoken
        } else {
            greetingGenerator.generateGreeting(
                driverName = currentSettings.driverName,
                vehicleBrand = currentSettings.vehicleBrand,
                vehicleModel = currentSettings.vehicleModel,
                vehicleVariant = currentSettings.vehicleVariant,
                style = currentSettings.greetingStyle,
                customTemplate = currentSettings.customGreetingTemplate,
                weatherInfo = if (currentSettings.includeWeatherInGreeting) currentWeather else null,
                careReminder = vehicleCareManager.generateCareReminderPhrase(currentSettings)
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Good evening, Shatrughna. Welcome to your Tata Nexon. Have a safe drive."
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
            val location = if (currentSettings.autoDetectLocation && locationProvider?.hasLocationPermission() == true) {
                locationProvider.getCurrentLocation()
            } else null

            val queryCity = location?.cityName ?: currentSettings.weatherCityName
            val queryLat = location?.latitude ?: currentSettings.weatherLatitude
            val queryLon = location?.longitude ?: currentSettings.weatherLongitude

            _weather.value = weatherRepository.getCurrentWeather(
                cityName = queryCity,
                latitude = queryLat,
                longitude = queryLon,
                forceRefresh = forceRefresh
            )
        }
    }

    fun launchDestination(context: Context, destination: Destination) {
        destinationManager.launchNavigation(context, destination)
    }

    fun toggleGreetingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateGreetingEnabled(enabled)
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
                val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$lat,$lon(Parked Nexon)")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(geoIntent)
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.APP, "Failed to launch walking navigation: ${e.message}", e)
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
        private val tripHistoryRepository: TripHistoryRepository? = null
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
                tripHistoryRepository
            ) as T
        }
    }
}
