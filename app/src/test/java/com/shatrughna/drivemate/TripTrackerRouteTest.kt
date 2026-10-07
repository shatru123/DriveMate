package com.shatrughna.drivemate

import com.shatrughna.drivemate.data.model.DriveMateSettings
import com.shatrughna.drivemate.data.model.GreetingStyle
import com.shatrughna.drivemate.data.model.TripReport
import com.shatrughna.drivemate.data.model.TripStats
import com.shatrughna.drivemate.data.preferences.DriveMatePreferencesRepository
import com.shatrughna.drivemate.driving.DrivingSessionManager
import com.shatrughna.drivemate.driving.TripHistoryRepository
import com.shatrughna.drivemate.driving.TripTrackerImpl
import com.shatrughna.drivemate.greeting.GreetingTtsManager
import com.shatrughna.drivemate.greeting.VoiceInfo
import com.shatrughna.drivemate.location.DeviceLocation
import com.shatrughna.drivemate.location.DeviceLocationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

import com.shatrughna.drivemate.car.CarConnectionState
import com.shatrughna.drivemate.driving.DrivingSession
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

@OptIn(ExperimentalCoroutinesApi::class)
class TripTrackerRouteTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeSessionManager : DrivingSessionManager {
        val activeFlow = MutableStateFlow(false)
        override val isSessionActive: StateFlow<Boolean> = activeFlow
        override val currentSessionId: StateFlow<String?> = MutableStateFlow(null)
        override val hasGreetingPlayed: StateFlow<Boolean> = MutableStateFlow(false)
        override val sessionStartTime: StateFlow<Long?> = MutableStateFlow(null)
        override val greetingTriggerEvents: SharedFlow<DrivingSession> = MutableSharedFlow()

        override fun startSessionMonitoring() {}
        override fun markGreetingPlayed(sessionId: String): Boolean = true
        override fun resetSession() {}
        override suspend fun onConnectionStateChanged(state: CarConnectionState) {}
    }

    private class FakeLocationProvider : DeviceLocationProvider {
        var hasPermission: Boolean = true
        private var sampleCount = 0
        override fun hasLocationPermission(): Boolean = hasPermission
        override suspend fun getCurrentLocation(): DeviceLocation? {
            val lat = 18.5204 + (sampleCount * 0.0003)
            sampleCount++
            return DeviceLocation(lat, 73.8567, "Pune")
        }
    }

    private class FakeTripHistoryRepo : TripHistoryRepository {
        val trips = mutableListOf<TripReport>()
        private val _recentTrips = MutableStateFlow<List<TripReport>>(emptyList())
        override val recentTrips: StateFlow<List<TripReport>> = _recentTrips
        private val _latestTrip = MutableStateFlow<TripReport?>(null)
        override val latestTrip: StateFlow<TripReport?> = _latestTrip

        override suspend fun saveTrip(trip: TripReport) {
            trips.add(trip)
            _latestTrip.value = trip
            _recentTrips.value = trips
        }

        override suspend fun clearHistory() {
            trips.clear()
            _latestTrip.value = null
            _recentTrips.value = emptyList()
        }
    }

    private class FakeTtsManager : GreetingTtsManager {
        val spokenList = mutableListOf<String>()
        override val isInitialized: StateFlow<Boolean> = MutableStateFlow(true)
        override val isSpeaking: StateFlow<Boolean> = MutableStateFlow(false)

        override suspend fun initialize(): Result<Unit> = Result.success(Unit)
        override suspend fun speak(text: String): Result<Unit> {
            spokenList.add(text)
            return Result.success(Unit)
        }
        override fun stop() {}
        override fun shutdown() {}
        override fun setSpeechRate(rate: Float) {}
        override fun setPitch(pitch: Float) {}
        override fun setLanguage(locale: Locale): Boolean = true
        override fun setVoice(voiceName: String?): Boolean = true
        override fun getAvailableVoices(): List<VoiceInfo> = emptyList()
    }

    private class FakePreferencesRepository : DriveMatePreferencesRepository {
        val settingsFlowInternal = MutableStateFlow(DriveMateSettings())
        override val settingsFlow: Flow<DriveMateSettings> = settingsFlowInternal
        override val tripStatsFlow: Flow<TripStats> = MutableStateFlow(TripStats())

        var recordedDistance: Float = 0f
        var recordedDuration: Long = 0L
        var lastParkedLat: Double? = null
        var lastParkedLon: Double? = null
        var lastParkedAddress: String? = null

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
        override suspend fun updateVehicleCare(odometerKm: Int, nextServiceKm: Int, fuelReminder: Boolean) {}
        override suspend fun updateFavoriteAddresses(home: String, office: String) {}

        override suspend fun recordCompletedTrip(distanceKm: Float, durationMinutes: Long) {
            recordedDistance += distanceKm
            recordedDuration += durationMinutes
        }

        override suspend fun updateLastParkedLocation(lat: Double, lon: Double, address: String?) {
            lastParkedLat = lat
            lastParkedLon = lon
            lastParkedAddress = address
        }

        override suspend fun updateDriverFatigueAlert(enabled: Boolean) {}
        override suspend fun updatePreferredMusicApp(app: String) {}
        override suspend fun updateVoiceAssistantEnabled(enabled: Boolean) {}
        override suspend fun updateHeyDriveMateEnabled(enabled: Boolean) {}
        override suspend fun updateWakeWordSensitivity(sensitivity: Float) {}
        override suspend fun resetToDefaults() {}
    }

    private lateinit var fakeSessionManager: FakeSessionManager
    private lateinit var fakePreferencesRepository: FakePreferencesRepository
    private lateinit var fakeLocationProvider: FakeLocationProvider
    private lateinit var fakeTripHistoryRepo: FakeTripHistoryRepo
    private lateinit var fakeTtsManager: FakeTtsManager
    private lateinit var tripTracker: TripTrackerImpl

    @Before
    fun setUp() {
        fakeSessionManager = FakeSessionManager()
        fakePreferencesRepository = FakePreferencesRepository()
        fakeLocationProvider = FakeLocationProvider()
        fakeTripHistoryRepo = FakeTripHistoryRepo()
        fakeTtsManager = FakeTtsManager()

        tripTracker = TripTrackerImpl(
            sessionManager = fakeSessionManager,
            preferencesRepository = fakePreferencesRepository,
            locationProvider = fakeLocationProvider,
            tripHistoryRepository = fakeTripHistoryRepo,
            ttsManager = fakeTtsManager,
            scope = kotlinx.coroutines.CoroutineScope(testDispatcher)
        )
        tripTracker.start()
    }

    @Test
    fun testActiveDriveAccumulationAndReportGeneration() = runTest(testDispatcher) {
        // Start driving session
        fakeSessionManager.activeFlow.value = true
        // Simulate 21 seconds of driving (advancing time without advanceUntilIdle while loop is running)
        advanceTimeBy(21000L)

        val activeStats = tripTracker.tripStats.value
        assertTrue(activeStats.activeTripDurationSeconds >= 20L)
        assertTrue(activeStats.activeTripDistanceKm > 0.1f)

        // Car parks / disconnects
        fakeSessionManager.activeFlow.value = false
        advanceUntilIdle()

        // Verifications
        assertEquals(1, fakeTripHistoryRepo.trips.size)
        val report = fakeTripHistoryRepo.trips.first()
        assertTrue(report.distanceKm > 0.1f)
        assertNotNull(report.endLocationName)

        // Verify parking spot saved
        assertEquals(18.5204, fakePreferencesRepository.lastParkedLat ?: 0.0, 0.01)
        assertEquals(73.8567, fakePreferencesRepository.lastParkedLon ?: 0.0, 0.001)

        // Verify post-drive TTS voice debrief spoke
        assertEquals(1, fakeTtsManager.spokenList.size)
        assertTrue(fakeTtsManager.spokenList.first().contains("Trip complete"))
    }
}
