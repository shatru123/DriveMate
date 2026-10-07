package com.shatrughna.drivemate

import android.app.Application
import com.shatrughna.drivemate.car.AndroidAutoCarConnectionManager
import com.shatrughna.drivemate.car.CarConnectionManager
import com.shatrughna.drivemate.care.VehicleCareManager
import com.shatrughna.drivemate.care.VehicleCareManagerImpl
import com.shatrughna.drivemate.data.preferences.DriveMatePreferencesRepository
import com.shatrughna.drivemate.data.preferences.DriveMatePreferencesRepositoryImpl
import com.shatrughna.drivemate.destination.DestinationManager
import com.shatrughna.drivemate.destination.DestinationManagerImpl
import com.shatrughna.drivemate.driving.DriveMateSessionService
import com.shatrughna.drivemate.driving.DrivingSessionManager
import com.shatrughna.drivemate.driving.DrivingSessionManagerImpl
import com.shatrughna.drivemate.driving.TripHistoryRepository
import com.shatrughna.drivemate.driving.TripHistoryRepositoryImpl
import com.shatrughna.drivemate.driving.TripTracker
import com.shatrughna.drivemate.driving.TripTrackerImpl
import com.shatrughna.drivemate.greeting.GreetingController
import com.shatrughna.drivemate.greeting.GreetingControllerImpl
import com.shatrughna.drivemate.greeting.GreetingGenerator
import com.shatrughna.drivemate.greeting.GreetingGeneratorImpl
import com.shatrughna.drivemate.greeting.GreetingTtsManager
import com.shatrughna.drivemate.greeting.GreetingTtsManagerImpl
import com.shatrughna.drivemate.location.DeviceLocationProvider
import com.shatrughna.drivemate.location.DeviceLocationProviderImpl
import com.shatrughna.drivemate.location.WeatherLocationResolver
import com.shatrughna.drivemate.location.WeatherLocationResolverImpl
import com.shatrughna.drivemate.util.AppLogger
import com.shatrughna.drivemate.voice.VoiceAssistantManager
import com.shatrughna.drivemate.voice.VoiceAssistantManagerImpl
import com.shatrughna.drivemate.voice.wakeword.SpeechRecognizerWakeWordEngine
import com.shatrughna.drivemate.voice.wakeword.WakeWordEngine
import com.shatrughna.drivemate.voice.wakeword.WakeWordManager
import com.shatrughna.drivemate.voice.wakeword.WakeWordManagerImpl
import com.shatrughna.drivemate.weather.OpenMeteoWeatherRepository
import com.shatrughna.drivemate.weather.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DriveMateApplication : Application() {

    lateinit var preferencesRepository: DriveMatePreferencesRepository
        private set

    lateinit var carConnectionManager: CarConnectionManager
        private set

    lateinit var sessionManager: DrivingSessionManager
        private set

    lateinit var greetingGenerator: GreetingGenerator
        private set

    lateinit var ttsManager: GreetingTtsManager
        private set

    lateinit var weatherRepository: WeatherRepository
        private set

    lateinit var vehicleCareManager: VehicleCareManager
        private set

    lateinit var destinationManager: DestinationManager
        private set

    lateinit var locationProvider: DeviceLocationProvider
        private set

    lateinit var locationResolver: WeatherLocationResolver
        private set

    lateinit var tripHistoryRepository: TripHistoryRepository
        private set

    lateinit var tripTracker: TripTracker
        private set

    lateinit var greetingController: GreetingController
        private set

    lateinit var voiceAssistantManager: VoiceAssistantManager
        private set

    lateinit var wakeWordEngine: WakeWordEngine
        private set

    lateinit var wakeWordManager: WakeWordManager
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        AppLogger.i(AppLogger.Tag.APP, "Initializing DriveMate Application (Production Automotive Architecture)...")

        preferencesRepository = DriveMatePreferencesRepositoryImpl(this)
        carConnectionManager = AndroidAutoCarConnectionManager(this)
        sessionManager = DrivingSessionManagerImpl(carConnectionManager)
        greetingGenerator = GreetingGeneratorImpl()
        ttsManager = GreetingTtsManagerImpl(this)
        weatherRepository = OpenMeteoWeatherRepository()
        vehicleCareManager = VehicleCareManagerImpl()
        destinationManager = DestinationManagerImpl()
        locationProvider = DeviceLocationProviderImpl(this)
        locationResolver = WeatherLocationResolverImpl(locationProvider)
        tripHistoryRepository = TripHistoryRepositoryImpl(this)

        tripTracker = TripTrackerImpl(
            sessionManager = sessionManager,
            preferencesRepository = preferencesRepository,
            locationProvider = locationProvider,
            tripHistoryRepository = tripHistoryRepository,
            ttsManager = ttsManager
        )

        greetingController = GreetingControllerImpl(
            preferencesRepository = preferencesRepository,
            sessionManager = sessionManager,
            greetingGenerator = greetingGenerator,
            ttsManager = ttsManager,
            weatherRepository = weatherRepository,
            vehicleCareManager = vehicleCareManager,
            locationProvider = locationProvider,
            locationResolver = locationResolver
        )

        voiceAssistantManager = VoiceAssistantManagerImpl(
            context = this,
            ttsManager = ttsManager,
            preferencesRepository = preferencesRepository,
            weatherRepository = weatherRepository,
            destinationManager = destinationManager,
            tripTracker = tripTracker,
            carConnectionManager = carConnectionManager,
            locationResolver = locationResolver
        )

        wakeWordEngine = SpeechRecognizerWakeWordEngine(this)
        wakeWordManager = WakeWordManagerImpl(
            wakeWordEngine = wakeWordEngine,
            voiceAssistantManager = voiceAssistantManager,
            greetingController = greetingController,
            ttsManager = ttsManager,
            sessionManager = sessionManager,
            preferencesRepository = preferencesRepository
        )

        // Session Lifecycle Sync: Start/Stop Foreground Service and release mic on disconnect
        applicationScope.launch {
            sessionManager.isSessionActive.collectLatest { isActive ->
                if (isActive) {
                    AppLogger.i(AppLogger.Tag.SESSION, "Starting DriveMateSessionService for active driving session.")
                    DriveMateSessionService.startService(this@DriveMateApplication)
                } else {
                    AppLogger.i(AppLogger.Tag.SESSION, "Stopping DriveMateSessionService and releasing voice resources.")
                    DriveMateSessionService.stopService(this@DriveMateApplication)
                    voiceAssistantManager.release()
                    ttsManager.stop()
                }
            }
        }

        // Start connection monitoring, trip tracking, and greeting controller
        carConnectionManager.startMonitoring()
        tripTracker.start()
        greetingController.start()
    }
}
