package com.shatrughna.drivemate.greeting

import com.shatrughna.drivemate.care.VehicleCareManager
import com.shatrughna.drivemate.data.model.DriveMateSettings
import com.shatrughna.drivemate.data.model.WeatherInfo
import com.shatrughna.drivemate.data.preferences.DriveMatePreferencesRepository
import com.shatrughna.drivemate.driving.DrivingSessionManager
import com.shatrughna.drivemate.util.AppLogger
import com.shatrughna.drivemate.weather.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Orchestrator between driving session detection, weather, vehicle care, greeting generation, and TTS audio playback.
 */
interface GreetingController {
    val lastSpokenGreeting: StateFlow<String>
    val isSpeaking: StateFlow<Boolean>

    fun start()
    suspend fun playWelcomeGreeting(): Result<String>
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

    private var isStarted = false

    override fun start() {
        if (isStarted) return
        isStarted = true

        AppLogger.i(AppLogger.Tag.GREETING, "Starting GreetingController event listener...")
        sessionManager.startSessionMonitoring()

        scope.launch {
            sessionManager.greetingTriggerEvents.collectLatest { connectionEvent ->
                AppLogger.i(
                    AppLogger.Tag.GREETING,
                    "Greeting event triggered from session for: ${connectionEvent.deviceOrVehicleName}"
                )
                playWelcomeGreeting()
            }
        }
    }

    override suspend fun playWelcomeGreeting(): Result<String> {
        val settings = preferencesRepository.settingsFlow.first()
        if (!settings.greetingEnabled) {
            AppLogger.i(AppLogger.Tag.GREETING, "Welcome greetings are disabled in settings. Skipping speech.")
            return Result.success("Greeting disabled")
        }

        if (sessionManager.hasGreetingPlayed.value) {
            AppLogger.d(AppLogger.Tag.GREETING, "Greeting has already been played for this drive. Suppressing.")
            return Result.success("Greeting already played")
        }

        val weatherInfo: WeatherInfo? = if (settings.includeWeatherInGreeting) {
            try {
                weatherRepository.getCurrentWeather(
                    cityName = settings.weatherCityName,
                    latitude = settings.weatherLatitude,
                    longitude = settings.weatherLongitude
                )
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
        AppLogger.i(AppLogger.Tag.GREETING, "Speaking greeting: \"$greetingText\"")

        // Configure TTS parameters
        ttsManager.setSpeechRate(settings.speechRate)
        ttsManager.setPitch(settings.pitch)
        if (settings.languageTag.isNotBlank()) {
            ttsManager.setLanguage(Locale.forLanguageTag(settings.languageTag))
        }
        if (settings.voiceName != null) {
            ttsManager.setVoice(settings.voiceName)
        }

        // Mark as played to prevent duplicate trigger during playback
        sessionManager.markGreetingPlayed()

        val speakResult = ttsManager.speak(greetingText)
        return if (speakResult.isSuccess) {
            Result.success(greetingText)
        } else {
            AppLogger.e(AppLogger.Tag.GREETING, "TTS speech failed: ${speakResult.exceptionOrNull()?.message}")
            Result.failure(speakResult.exceptionOrNull() ?: IllegalStateException("TTS playback failed"))
        }
    }

    override suspend fun previewGreeting(customSettings: DriveMateSettings?): Result<String> {
        val settings = customSettings ?: preferencesRepository.settingsFlow.first()

        val weatherInfo: WeatherInfo? = if (settings.includeWeatherInGreeting) {
            try {
                weatherRepository.getCurrentWeather(
                    cityName = settings.weatherCityName,
                    latitude = settings.weatherLatitude,
                    longitude = settings.weatherLongitude
                )
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
        ttsManager.stop()
    }
}
