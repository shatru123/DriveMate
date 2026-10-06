package com.shatrughna.drivemate.greeting

import com.shatrughna.drivemate.care.VehicleCareManager
import com.shatrughna.drivemate.data.model.DriveMateSettings
import com.shatrughna.drivemate.data.model.WeatherInfo
import com.shatrughna.drivemate.data.preferences.DriveMatePreferencesRepository
import com.shatrughna.drivemate.driving.DrivingSession
import com.shatrughna.drivemate.driving.DrivingSessionManager
import com.shatrughna.drivemate.util.AppLogger
import com.shatrughna.drivemate.weather.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * Orchestrator between driving session detection, weather, vehicle care, greeting generation, and TTS audio playback.
 */
interface GreetingController {
    val lastSpokenGreeting: StateFlow<String>
    val isSpeaking: StateFlow<Boolean>

    fun start()
    suspend fun playWelcomeGreeting(sessionId: String? = null): Result<String>
    suspend fun previewGreeting(customSettings: DriveMateSettings? = null): Result<String>
    fun stopSpeaking()
}

class GreetingControllerImpl(
    private val preferencesRepository: DriveMatePreferencesRepository,
    private val sessionManager: DrivingSessionManager,
    private val greetingGenerator: GreetingGenerator,
    private val ttsManager: GreetingTtsManager,
    private val weatherRepository: WeatherRepository,
    private val vehicleCareManager: VehicleCareManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : GreetingController {

    private val _lastSpokenGreeting = MutableStateFlow("")
    override val lastSpokenGreeting: StateFlow<String> = _lastSpokenGreeting.asStateFlow()

    override val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking

    private val greetingMutex = Mutex()
    private var activeGreetingJob: Job? = null
    private var isStarted = false

    override fun start() {
        if (isStarted) return
        isStarted = true

        AppLogger.i(AppLogger.Tag.GREETING, "Starting GreetingController event listener...")
        sessionManager.startSessionMonitoring()

        // Listen for new verified driving sessions
        scope.launch {
            sessionManager.greetingTriggerEvents.collectLatest { session ->
                AppLogger.i(
                    AppLogger.Tag.GREETING,
                    "Greeting event triggered from session [${session.sessionId}] for: ${session.connectionState.deviceOrVehicleName}"
                )
                activeGreetingJob?.cancel()
                activeGreetingJob = scope.launch {
                    playWelcomeGreeting(session.sessionId)
                }
            }
        }

        // Cancel greeting if session ends / car disconnects
        scope.launch {
            var wasActive = false
            sessionManager.isSessionActive.collectLatest { isActive ->
                if (wasActive && !isActive) {
                    AppLogger.d(AppLogger.Tag.GREETING, "Session inactive, cancelling active greeting job and stopping TTS")
                    activeGreetingJob?.cancel()
                    ttsManager.stop()
                }
                wasActive = isActive
            }
        }
    }

    override suspend fun playWelcomeGreeting(sessionId: String?): Result<String> = greetingMutex.withLock {
        val targetSessionId = sessionId ?: sessionManager.currentSessionId.value

        val settings = preferencesRepository.settingsFlow.first()
        if (!settings.greetingEnabled) {
            AppLogger.i(AppLogger.Tag.GREETING, "Welcome greetings are disabled in settings. Skipping speech.")
            return Result.success("Greeting disabled")
        }

        if (sessionManager.hasGreetingPlayed.value) {
            AppLogger.d(AppLogger.Tag.GREETING, "Greeting has already been played for this drive. Suppressing.")
            return Result.success("Greeting already played")
        }

        if (targetSessionId != null && sessionManager.currentSessionId.value != targetSessionId) {
            AppLogger.w(AppLogger.Tag.GREETING, "Session $targetSessionId is no longer active. Aborting greeting.")
            return Result.failure(IllegalStateException("Driving session ended before greeting"))
        }

        val weatherInfo: WeatherInfo? = if (settings.includeWeatherInGreeting) {
            try {
                withTimeoutOrNull(2000L) {
                    weatherRepository.getCurrentWeather(
                        cityName = settings.weatherCityName,
                        latitude = settings.weatherLatitude,
                        longitude = settings.weatherLongitude
                    )
                }
            } catch (e: Exception) {
                AppLogger.w(AppLogger.Tag.GREETING, "Weather fetch timed out or failed: ${e.message}")
                null
            }
        } else {
            null
        }

        val careReminder = vehicleCareManager.generateCareReminderPhrase(settings)

        val greetingText = greetingGenerator.generateGreeting(
            driverName = settings.driverName,
            vehicleBrand = settings.vehicleBrand,
            vehicleModel = settings.vehicleModel,
            vehicleVariant = settings.vehicleVariant,
            style = settings.greetingStyle,
            customTemplate = settings.customGreetingTemplate,
            weatherInfo = weatherInfo,
            careReminder = careReminder
        )

        _lastSpokenGreeting.value = greetingText
        AppLogger.i(AppLogger.Tag.GREETING, "Prepared greeting text: \"$greetingText\"")

        // Configure TTS parameters
        ttsManager.setSpeechRate(settings.speechRate)
        ttsManager.setPitch(settings.pitch)
        if (settings.languageTag.isNotBlank()) {
            ttsManager.setLanguage(Locale.forLanguageTag(settings.languageTag))
        }
        if (settings.voiceName != null) {
            ttsManager.setVoice(settings.voiceName)
        }

        // Bounded retry loop: max 2 retries (3 total attempts)
        val maxAttempts = 3
        var lastError: Throwable? = null

        for (attempt in 1..maxAttempts) {
            // Check session validity before each attempt
            if (targetSessionId != null && sessionManager.currentSessionId.value != targetSessionId) {
                AppLogger.w(AppLogger.Tag.GREETING, "Session invalidated before attempt $attempt. Aborting.")
                return Result.failure(IllegalStateException("Session disconnected during greeting attempt"))
            }

            AppLogger.i(AppLogger.Tag.GREETING, "Speaking greeting (Attempt $attempt of $maxAttempts)...")
            val speakResult = ttsManager.speak(greetingText)

            if (speakResult.isSuccess) {
                // MARK AS PLAYED ONLY UPON SUCCESSFUL TTS COMPLETION
                if (targetSessionId != null) {
                    val marked = sessionManager.markGreetingPlayed(targetSessionId)
                    if (marked) {
                        AppLogger.i(AppLogger.Tag.GREETING, "Greeting played and marked as played for session: $targetSessionId")
                    } else {
                        AppLogger.w(AppLogger.Tag.GREETING, "TTS completed, but session $targetSessionId was no longer current.")
                    }
                } else {
                    sessionManager.currentSessionId.value?.let { sessionManager.markGreetingPlayed(it) }
                }
                return Result.success(greetingText)
            } else {
                lastError = speakResult.exceptionOrNull()
                AppLogger.w(
                    AppLogger.Tag.GREETING,
                    "TTS attempt $attempt failed: ${lastError?.message}."
                )
                if (attempt < maxAttempts) {
                    delay(1000L)
                }
            }
        }

        AppLogger.e(AppLogger.Tag.GREETING, "All $maxAttempts TTS attempts failed. Greeting NOT marked as played.")
        return Result.failure(lastError ?: IllegalStateException("TTS playback failed after retries"))
    }

    override suspend fun previewGreeting(customSettings: DriveMateSettings?): Result<String> {
        val settings = customSettings ?: preferencesRepository.settingsFlow.first()

        val weatherInfo: WeatherInfo? = if (settings.includeWeatherInGreeting) {
            try {
                withTimeoutOrNull(2000L) {
                    weatherRepository.getCurrentWeather(
                        cityName = settings.weatherCityName,
                        latitude = settings.weatherLatitude,
                        longitude = settings.weatherLongitude
                    )
                }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }

        val careReminder = vehicleCareManager.generateCareReminderPhrase(settings)

        val greetingText = greetingGenerator.generateGreeting(
            driverName = settings.driverName,
            vehicleBrand = settings.vehicleBrand,
            vehicleModel = settings.vehicleModel,
            vehicleVariant = settings.vehicleVariant,
            style = settings.greetingStyle,
            customTemplate = settings.customGreetingTemplate,
            weatherInfo = weatherInfo,
            careReminder = careReminder
        )

        _lastSpokenGreeting.value = greetingText
        AppLogger.i(AppLogger.Tag.GREETING, "Previewing greeting: \"$greetingText\"")

        ttsManager.setSpeechRate(settings.speechRate)
        ttsManager.setPitch(settings.pitch)
        if (settings.languageTag.isNotBlank()) {
            ttsManager.setLanguage(Locale.forLanguageTag(settings.languageTag))
        }
        if (settings.voiceName != null) {
            ttsManager.setVoice(settings.voiceName)
        }

        val speakResult = ttsManager.speak(greetingText)
        return if (speakResult.isSuccess) {
            Result.success(greetingText)
        } else {
            Result.failure(speakResult.exceptionOrNull() ?: IllegalStateException("TTS preview failed"))
        }
    }

    override fun stopSpeaking() {
        activeGreetingJob?.cancel()
        ttsManager.stop()
    }
}
