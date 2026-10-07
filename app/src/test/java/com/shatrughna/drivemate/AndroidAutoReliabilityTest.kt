package com.shatrughna.drivemate

import com.shatrughna.drivemate.care.VehicleCareManagerImpl
import com.shatrughna.drivemate.car.CarConnectionManager
import com.shatrughna.drivemate.car.CarConnectionState
import com.shatrughna.drivemate.car.CarConnectionType
import com.shatrughna.drivemate.data.model.DriveMateSettings
import com.shatrughna.drivemate.data.model.GreetingStyle
import com.shatrughna.drivemate.data.model.TripStats
import com.shatrughna.drivemate.data.model.WeatherInfo
import com.shatrughna.drivemate.data.preferences.DriveMatePreferencesRepository
import com.shatrughna.drivemate.driving.DrivingSessionManagerImpl
import com.shatrughna.drivemate.greeting.GreetingControllerImpl
import com.shatrughna.drivemate.greeting.GreetingGeneratorImpl
import com.shatrughna.drivemate.greeting.GreetingTtsManager
import com.shatrughna.drivemate.greeting.VoiceInfo
import com.shatrughna.drivemate.location.DeviceLocation
import com.shatrughna.drivemate.location.DeviceLocationProvider
import com.shatrughna.drivemate.weather.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidAutoReliabilityTest {

    // --- Fakes ---

    private class FakeCarConnectionManager : CarConnectionManager {
        val stateFlow = MutableStateFlow<CarConnectionState>(CarConnectionState.Disconnected())
        override val connectionState: StateFlow<CarConnectionState> = stateFlow

        override fun startMonitoring() {}
        override fun stopMonitoring() {}
        override fun onBluetoothConnected(deviceName: String) {}
        override fun onBluetoothDisconnected(deviceName: String) {}
        override fun setSimulatedConnection(connected: Boolean) {}
    }

    private class FakeGreetingTtsManager : GreetingTtsManager {
        private val _isInitialized = MutableStateFlow(true)
        override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

        private val _isSpeaking = MutableStateFlow(false)
        override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

        var shouldInitSucceed = true
        var shouldAudioFocusSucceed = true
        var shouldSpeakSucceed = true
        var speakDelayMs = 0L

        var speakCallCount = 0
        var stopCallCount = 0
        var lastSpokenText: String? = null

        override suspend fun initialize(): Result<Unit> {
            return if (shouldInitSucceed) {
                _isInitialized.value = true
                Result.success(Unit)
            } else {
                _isInitialized.value = false
                Result.failure(IllegalStateException("TTS Init Failed"))
            }
        }

        override suspend fun speak(text: String): Result<Unit> {
            speakCallCount++
            if (!shouldInitSucceed) {
                return Result.failure(IllegalStateException("TTS not initialized"))
            }
            if (!shouldAudioFocusSucceed) {
                return Result.failure(IllegalStateException("Audio focus denied"))
            }
            _isSpeaking.value = true
            if (speakDelayMs > 0) {
                delay(speakDelayMs)
            }
            if (!shouldSpeakSucceed) {
                _isSpeaking.value = false
                return Result.failure(IllegalStateException("TTS playback failed with error"))
            }
            _isSpeaking.value = false
            lastSpokenText = text
            return Result.success(Unit)
        }

        override fun stop() {
            stopCallCount++
            _isSpeaking.value = false
        }

        override fun shutdown() {
            stop()
            _isInitialized.value = false
        }

        override fun setSpeechRate(rate: Float) {}
        override fun setPitch(pitch: Float) {}
        override fun setLanguage(locale: Locale): Boolean = true
        override fun setVoice(voiceName: String?): Boolean = true
        override fun getAvailableVoices(): List<VoiceInfo> = emptyList()
    }

    private class FakeWeatherRepository : WeatherRepository {
        var weatherDelayMs = 0L
        var shouldFail = false
        var callCount = 0

        override suspend fun getCurrentWeather(
            cityName: String,
            latitude: Double,
            longitude: Double,
            forceRefresh: Boolean
        ): WeatherInfo {
            callCount++
            if (weatherDelayMs > 0) {
                delay(weatherDelayMs)
            }
            if (shouldFail) {
                throw RuntimeException("Weather API timeout or unreachable")
            }
            return WeatherInfo(
                temperatureCelsius = 28.0f,
                weatherCode = 1,
                conditionText = "Sunny",
                cityName = cityName,
                isFetchedFromNetwork = true
            )
        }

        override fun mapWmoCodeToCondition(code: Int): String = "Clear"
    }

    private class FakePreferencesRepository(
        initialSettings: DriveMateSettings = DriveMateSettings()
    ) : DriveMatePreferencesRepository {
        val settings = MutableStateFlow(initialSettings)
        override val settingsFlow: Flow<DriveMateSettings> = settings
        override val tripStatsFlow: Flow<TripStats> = MutableStateFlow(TripStats())

        override suspend fun updateDriverName(name: String) {}
        override suspend fun updateVehicle(brand: String, model: String, variant: String) {}
        override suspend fun updateGreetingEnabled(enabled: Boolean) {}
        override suspend fun updateGreetingStyle(style: GreetingStyle) {}
        override suspend fun updateCustomGreetingTemplate(template: String) {}
        override suspend fun updateSpeechRate(rate: Float) {}
        override suspend fun updatePitch(pitch: Float) {}
        override suspend fun updateLanguageTag(tag: String) {}
        override suspend fun updateVoiceName(voice: String?) {}
        override suspend fun updateAutoMonitorBluetooth(enabled: Boolean) {}
        override suspend fun updateTargetBluetoothName(name: String) {}
        override suspend fun updateWeatherSettings(includeInGreeting: Boolean, cityName: String, lat: Double, lon: Double) {}
        override suspend fun updateAutoDetectLocation(enabled: Boolean) {}
        override suspend fun updateVehicleCare(odometerKm: Double, nextServiceKm: Int, fuelReminder: Boolean) {}
        override suspend fun updateVehicleCare(odometerKm: Int, nextServiceKm: Int, fuelReminder: Boolean) {}
        override suspend fun updateVehicleRegistration(regNumber: String) {}
        override suspend fun updateVehiclePhotoUri(uriString: String?) {}
        override suspend fun updateFavoriteAddresses(home: String, office: String) {}
        override suspend fun recordCompletedTrip(distanceKm: Float, durationMinutes: Long) {}
        override suspend fun updateLastParkedLocation(lat: Double, lon: Double, address: String?) {}
        override suspend fun updateDriverFatigueAlert(enabled: Boolean) {}
        override suspend fun updatePreferredMusicApp(app: String) {}
        override suspend fun updateVoiceAssistantEnabled(enabled: Boolean) {}
        override suspend fun updateHeyDriveMateEnabled(enabled: Boolean) {}
        override suspend fun updateWakeWordSensitivity(sensitivity: Float) {}
        override suspend fun resetToDefaults() {}
    }

    private class FakeDeviceLocationProvider : DeviceLocationProvider {
        var hasPermission = true
        var mockLocation: DeviceLocation? = null

        override fun hasLocationPermission(): Boolean = hasPermission
        override suspend fun getCurrentLocation(): DeviceLocation? = mockLocation
    }

    // --- Test Fixtures ---

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeCarConnectionManager: FakeCarConnectionManager
    private lateinit var fakeTtsManager: FakeGreetingTtsManager
    private lateinit var fakeWeatherRepository: FakeWeatherRepository
    private lateinit var fakePreferencesRepository: FakePreferencesRepository
    private lateinit var fakeLocationProvider: FakeDeviceLocationProvider
    private lateinit var sessionManager: DrivingSessionManagerImpl
    private lateinit var greetingController: GreetingControllerImpl

    @Before
    fun setUp() {
        fakeCarConnectionManager = FakeCarConnectionManager()
        fakeTtsManager = FakeGreetingTtsManager()
        fakeWeatherRepository = FakeWeatherRepository()
        fakePreferencesRepository = FakePreferencesRepository()
        fakeLocationProvider = FakeDeviceLocationProvider()

        sessionManager = DrivingSessionManagerImpl(
            carConnectionManager = fakeCarConnectionManager,
            scope = CoroutineScope(testDispatcher)
        )

        greetingController = GreetingControllerImpl(
            preferencesRepository = fakePreferencesRepository,
            sessionManager = sessionManager,
            greetingGenerator = GreetingGeneratorImpl(),
            ttsManager = fakeTtsManager,
            weatherRepository = fakeWeatherRepository,
            vehicleCareManager = VehicleCareManagerImpl(),
            locationProvider = fakeLocationProvider,
            scope = CoroutineScope(testDispatcher)
        )

        greetingController.start()
        testDispatcher.scheduler.advanceUntilIdle()
    }

    // 1. Bluetooth-only connects → NO session, NO greeting
    @Test
    fun testScenario1_bluetoothOnly_doesNotTriggerGreeting() = runTest(testDispatcher) {
        val btState = CarConnectionState.Connected(
            connectionType = CarConnectionType.BLUETOOTH_ONLY,
            deviceOrVehicleName = "Tata Nexon BT Audio",
            isBluetoothConnected = true,
            activeBluetoothDeviceName = "Tata Nexon BT Audio"
        )
        fakeCarConnectionManager.stateFlow.value = btState
        advanceUntilIdle()

        assertFalse(sessionManager.isSessionActive.value)
        assertNull(sessionManager.currentSessionId.value)
        assertFalse(sessionManager.hasGreetingPlayed.value)
        assertEquals(0, fakeTtsManager.speakCallCount)
        assertNull(fakeTtsManager.lastSpokenText)
    }

    // 2. Android Auto connects while Bluetooth is already connected → starts session and triggers greeting
    @Test
    fun testScenario2_androidAutoWhileBluetoothConnected_triggersGreeting() = runTest(testDispatcher) {
        // Step A: Phone connects to BT
        val btState = CarConnectionState.Connected(
            connectionType = CarConnectionType.BLUETOOTH_ONLY,
            deviceOrVehicleName = "Tata Nexon BT Audio",
            isBluetoothConnected = true,
            activeBluetoothDeviceName = "Tata Nexon BT Audio"
        )
        fakeCarConnectionManager.stateFlow.value = btState
        advanceUntilIdle()
        assertEquals(0, fakeTtsManager.speakCallCount)

        // Step B: Android Auto projection connects
        val aaState = CarConnectionState.Connected(
            connectionType = CarConnectionType.ANDROID_AUTO_PROJECTION,
            deviceOrVehicleName = "Tata Nexon (Android Auto)",
            isBluetoothConnected = true,
            activeBluetoothDeviceName = "Tata Nexon BT Audio"
        )
        fakeCarConnectionManager.stateFlow.value = aaState
        advanceUntilIdle()

        assertTrue(sessionManager.isSessionActive.value)
        assertNotNull(sessionManager.currentSessionId.value)
        assertTrue(sessionManager.hasGreetingPlayed.value)
        assertEquals(1, fakeTtsManager.speakCallCount)
        assertNotNull(fakeTtsManager.lastSpokenText)
    }

    // 3. Android Auto emits multiple connection events in rapid succession → greeting plays only once
    @Test
    fun testScenario3_rapidDuplicateConnectionEvents_deduplicated() = runTest(testDispatcher) {
        val aaState = CarConnectionState.Connected(
            connectionType = CarConnectionType.ANDROID_AUTO_PROJECTION,
            deviceOrVehicleName = "Tata Nexon (Android Auto)"
        )

        // Multiple repeated triggers
        fakeCarConnectionManager.stateFlow.value = aaState
        fakeCarConnectionManager.stateFlow.value = aaState
        fakeCarConnectionManager.stateFlow.value = aaState
        advanceUntilIdle()

        assertEquals(1, fakeTtsManager.speakCallCount)
        assertTrue(sessionManager.hasGreetingPlayed.value)
    }

    // 4. Car disconnects while greeting preparation is running → greeting cancelled, not played
    @Test
    fun testScenario4_disconnectDuringGreetingPreparation_cancelsGreeting() = runTest(testDispatcher) {
        // Weather API simulates a 1000ms delay during prep
        fakeLocationProvider.mockLocation = DeviceLocation(18.5204, 73.8567, "Pune")
        fakeWeatherRepository.weatherDelayMs = 1000L

        val aaState = CarConnectionState.Connected(
            connectionType = CarConnectionType.ANDROID_AUTO_PROJECTION,
            deviceOrVehicleName = "Tata Nexon (Android Auto)"
        )
        fakeCarConnectionManager.stateFlow.value = aaState
        advanceTimeBy(500L) // Halfway through weather fetch

        // Car disconnects before weather finishes
        fakeCarConnectionManager.stateFlow.value = CarConnectionState.Disconnected()
        advanceUntilIdle()

        // TTS should never have been invoked because session ended during prep
        assertEquals(0, fakeTtsManager.speakCallCount)
        assertFalse(sessionManager.hasGreetingPlayed.value)
    }

    // 5. Car disconnects while TTS is speaking → TTS stopped immediately, greeting NOT marked played
    @Test
    fun testScenario5_disconnectDuringTtsPlayback_stopsTtsAndDoesNotMarkPlayed() = runTest(testDispatcher) {
        fakeTtsManager.speakDelayMs = 2000L

        val aaState = CarConnectionState.Connected(
            connectionType = CarConnectionType.ANDROID_AUTO_PROJECTION,
            deviceOrVehicleName = "Tata Nexon (Android Auto)"
        )
        fakeCarConnectionManager.stateFlow.value = aaState
        advanceTimeBy(500L) // TTS is actively speaking

        assertTrue(fakeTtsManager.isSpeaking.value)

        // Car disconnects mid-speech
        fakeCarConnectionManager.stateFlow.value = CarConnectionState.Disconnected()
        advanceUntilIdle()

        // TTS must have been stopped
        assertTrue(fakeTtsManager.stopCallCount >= 1)
        assertFalse(sessionManager.isSessionActive.value)
        assertFalse(sessionManager.hasGreetingPlayed.value)
    }

    // 6. TTS initialization fails → greeting fails, NOT marked as played, retried up to limit
    @Test
    fun testScenario6_ttsInitFailure_retriesBoundedAndDoesNotMarkPlayed() = runTest(testDispatcher) {
        fakeTtsManager.shouldInitSucceed = false

        val aaState = CarConnectionState.Connected(
            connectionType = CarConnectionType.ANDROID_AUTO_PROJECTION,
            deviceOrVehicleName = "Tata Nexon (Android Auto)"
        )
        fakeCarConnectionManager.stateFlow.value = aaState
        advanceUntilIdle()

        // Bounded retries: 3 attempts total (initial + 2 retries)
        assertEquals(3, fakeTtsManager.speakCallCount)
        assertFalse(sessionManager.hasGreetingPlayed.value)
    }

    // 7. TTS playback fails mid-speech → greeting marked as failed, NOT marked as played
    @Test
    fun testScenario7_ttsPlaybackFailure_doesNotMarkPlayed() = runTest(testDispatcher) {
        fakeTtsManager.shouldSpeakSucceed = false

        val aaState = CarConnectionState.Connected(
            connectionType = CarConnectionType.ANDROID_AUTO_PROJECTION,
            deviceOrVehicleName = "Tata Nexon (Android Auto)"
        )
        fakeCarConnectionManager.stateFlow.value = aaState
        advanceUntilIdle()

        assertEquals(3, fakeTtsManager.speakCallCount)
        assertFalse(sessionManager.hasGreetingPlayed.value)
    }

    // 8. TTS completes successfully → greeting marked as played for current sessionId
    @Test
    fun testScenario8_ttsSuccess_marksGreetingPlayedForCurrentSession() = runTest(testDispatcher) {
        fakeTtsManager.shouldSpeakSucceed = true

        val aaState = CarConnectionState.Connected(
            connectionType = CarConnectionType.ANDROID_AUTO_PROJECTION,
            deviceOrVehicleName = "Tata Nexon (Android Auto)"
        )
        fakeCarConnectionManager.stateFlow.value = aaState
        advanceUntilIdle()

        assertEquals(1, fakeTtsManager.speakCallCount)
        assertTrue(sessionManager.hasGreetingPlayed.value)
        assertNotNull(sessionManager.currentSessionId.value)
    }

    // 9. Audio focus denied → greeting speech aborted, NOT marked as played
    @Test
    fun testScenario9_audioFocusDenied_abortsGreetingSpeech() = runTest(testDispatcher) {
        fakeTtsManager.shouldAudioFocusSucceed = false

        val aaState = CarConnectionState.Connected(
            connectionType = CarConnectionType.ANDROID_AUTO_PROJECTION,
            deviceOrVehicleName = "Tata Nexon (Android Auto)"
        )
        fakeCarConnectionManager.stateFlow.value = aaState
        advanceUntilIdle()

        assertFalse(sessionManager.hasGreetingPlayed.value)
    }

    // 10. Stale utterance callback does not affect active session
    @Test
    fun testScenario10_staleSessionCannotMarkGreetingPlayed() = runTest(testDispatcher) {
        val aaState = CarConnectionState.Connected(
            connectionType = CarConnectionType.ANDROID_AUTO_PROJECTION,
            deviceOrVehicleName = "Tata Nexon (Android Auto)"
        )
        fakeCarConnectionManager.stateFlow.value = aaState
        advanceUntilIdle()

        val activeSessionId = sessionManager.currentSessionId.value
        assertNotNull(activeSessionId)

        // Attempting to mark with an invalid or expired sessionId fails
        val result = sessionManager.markGreetingPlayed("old-expired-utterance-id")
        assertFalse(result)
    }

    // 11. Rapid disconnect-reconnect cycle → old session cancelled, new session gets new unique sessionId, new greeting triggers
    @Test
    fun testScenario11_rapidDisconnectReconnect_generatesNewSessionAndNewGreeting() = runTest(testDispatcher) {
        // Drive 1
        val aaState = CarConnectionState.Connected(
            connectionType = CarConnectionType.ANDROID_AUTO_PROJECTION,
            deviceOrVehicleName = "Tata Nexon (Android Auto)",
            timestampMillis = 1000L
        )
        fakeCarConnectionManager.stateFlow.value = aaState
        advanceUntilIdle()

        val firstSessionId = sessionManager.currentSessionId.value
        assertNotNull(firstSessionId)
        assertEquals(1, fakeTtsManager.speakCallCount)
        assertTrue(sessionManager.hasGreetingPlayed.value)

        // Car unplugs
        fakeCarConnectionManager.stateFlow.value = CarConnectionState.Disconnected()
        advanceUntilIdle()
        assertFalse(sessionManager.isSessionActive.value)
        assertNull(sessionManager.currentSessionId.value)
        assertFalse(sessionManager.hasGreetingPlayed.value)

        // Drive 2 (Re-plug)
        val aaState2 = CarConnectionState.Connected(
            connectionType = CarConnectionType.ANDROID_AUTO_PROJECTION,
            deviceOrVehicleName = "Tata Nexon (Android Auto)",
            timestampMillis = 2000L
        )
        fakeCarConnectionManager.stateFlow.value = aaState2
        advanceUntilIdle()

        val secondSessionId = sessionManager.currentSessionId.value
        assertNotNull(secondSessionId)
        assertNotEquals(firstSessionId, secondSessionId)
        assertEquals(2, fakeTtsManager.speakCallCount)
        assertTrue(sessionManager.hasGreetingPlayed.value)
    }

    // 12. Weather API takes longer than 2.0s or fails → basic greeting plays without weather
    @Test
    fun testScenario12_weatherTimeout_proceedsWithBasicGreeting() = runTest(testDispatcher) {
        // Weather takes 5 seconds (exceeding 2s timeout)
        fakeLocationProvider.mockLocation = DeviceLocation(18.5204, 73.8567, "Pune")
        fakeWeatherRepository.weatherDelayMs = 5000L

        val aaState = CarConnectionState.Connected(
            connectionType = CarConnectionType.ANDROID_AUTO_PROJECTION,
            deviceOrVehicleName = "Tata Nexon (Android Auto)"
        )
        fakeCarConnectionManager.stateFlow.value = aaState

        // Advance 2.5s (past the 2.0s timeout)
        advanceTimeBy(2500L)
        advanceUntilIdle()

        // TTS should have spoken without waiting 5 seconds!
        assertEquals(1, fakeTtsManager.speakCallCount)
        assertTrue(sessionManager.hasGreetingPlayed.value)
        val spokenText = fakeTtsManager.lastSpokenText
        assertNotNull(spokenText)
        // Weather details ("Sunny" / "28") should NOT be included because it timed out
        assertFalse(spokenText!!.contains("Sunny"))
    }

    // 13. Single greeting per driving session even with repeated state evaluations
    @Test
    fun testScenario13_singleGreetingPerDrivingSession() = runTest(testDispatcher) {
        val aaState = CarConnectionState.Connected(
            connectionType = CarConnectionType.ANDROID_AUTO_PROJECTION,
            deviceOrVehicleName = "Tata Nexon (Android Auto)"
        )
        fakeCarConnectionManager.stateFlow.value = aaState
        advanceUntilIdle()

        assertEquals(1, fakeTtsManager.speakCallCount)

        // Call playWelcomeGreeting manually again (simulating repeated trigger)
        val secondResult = greetingController.playWelcomeGreeting()
        advanceUntilIdle()

        assertEquals("Greeting already played", secondResult.getOrNull())
        assertEquals(1, fakeTtsManager.speakCallCount)
    }

    // 14. Dynamic Device Location Detected (e.g. Mumbai) -> Greeting reflects detected city and coordinates
    @Test
    fun testScenario14_dynamicDeviceLocationDetected_greetingReflectsCurrentCity() = runTest(testDispatcher) {
        fakePreferencesRepository.settings.value = DriveMateSettings(
            greetingStyle = GreetingStyle.DETAILED
        )
        fakeLocationProvider.hasPermission = true
        fakeLocationProvider.mockLocation = DeviceLocation(
            latitude = 19.0760,
            longitude = 72.8777,
            cityName = "Mumbai"
        )

        val aaState = CarConnectionState.Connected(
            connectionType = CarConnectionType.ANDROID_AUTO_PROJECTION,
            deviceOrVehicleName = "Tata Nexon (Android Auto)"
        )
        fakeCarConnectionManager.stateFlow.value = aaState
        advanceUntilIdle()

        assertEquals(1, fakeTtsManager.speakCallCount)
        val spoken = fakeTtsManager.lastSpokenText
        assertNotNull(spoken)
        assertTrue(spoken!!.contains("Mumbai"))
    }

    // 15. Location Permission Denied -> Gracefully falls back to configured settings city without failing
    @Test
    fun testScenario15_locationPermissionDenied_fallsBackToSettingsCity() = runTest(testDispatcher) {
        fakePreferencesRepository.settings.value = DriveMateSettings(
            greetingStyle = GreetingStyle.DETAILED,
            weatherCityName = "Pune"
        )
        fakeLocationProvider.hasPermission = false
        fakeLocationProvider.mockLocation = null

        val aaState = CarConnectionState.Connected(
            connectionType = CarConnectionType.ANDROID_AUTO_PROJECTION,
            deviceOrVehicleName = "Tata Nexon (Android Auto)"
        )
        fakeCarConnectionManager.stateFlow.value = aaState
        advanceUntilIdle()

        assertEquals(1, fakeTtsManager.speakCallCount)
        val spoken = fakeTtsManager.lastSpokenText
        assertNotNull(spoken)
        // Falls back to settings city "Pune"
        assertTrue(spoken!!.contains("Pune"))
    }
}
