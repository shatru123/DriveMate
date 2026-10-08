package com.shatrughna.drivemate.voice

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.shatrughna.drivemate.car.CarConnectionManager
import com.shatrughna.drivemate.data.preferences.DriveMatePreferencesRepository
import com.shatrughna.drivemate.destination.DestinationManager
import com.shatrughna.drivemate.driving.TripTracker
import com.shatrughna.drivemate.greeting.GreetingTtsManager
import com.shatrughna.drivemate.location.WeatherLocationResolver
import com.shatrughna.drivemate.location.WeatherLocationResolverImpl
import com.shatrughna.drivemate.util.AppLogger
import com.shatrughna.drivemate.weather.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale

sealed class VoiceAssistantState {
    data object Idle : VoiceAssistantState()
    data object Listening : VoiceAssistantState()
    data class Processing(val recognizedText: String) : VoiceAssistantState()
    data class Responding(val speechText: String) : VoiceAssistantState()
    data class Error(val message: String) : VoiceAssistantState()
}

interface VoiceAssistantManager {
    val state: StateFlow<VoiceAssistantState>
    fun startListening()
    fun stopListening()
    fun processTextCommand(commandText: String)
    fun release()
}

class VoiceAssistantManagerImpl(
    private val context: Context,
    private val ttsManager: GreetingTtsManager,
    private val preferencesRepository: DriveMatePreferencesRepository,
    private val weatherRepository: WeatherRepository,
    private val destinationManager: DestinationManager,
    private val tripTracker: TripTracker,
    private val carConnectionManager: CarConnectionManager,
    private val locationResolver: WeatherLocationResolver? = null,
    private val audioCoordinator: AudioInputCoordinator? = null,
    private val parser: VoiceCommandParser = VoiceCommandParser(),
    private val capabilityManager: com.shatrughna.drivemate.core.capabilities.VehicleCapabilityManager? = null,
    private val documentVaultRepository: com.shatrughna.drivemate.data.repository.DocumentVaultRepository? = null,
    private val maintenanceRepository: com.shatrughna.drivemate.data.repository.MaintenanceRepository? = null,
    private val expenseRepository: com.shatrughna.drivemate.data.repository.ExpenseRepository? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) : VoiceAssistantManager {

    private val _state = MutableStateFlow<VoiceAssistantState>(VoiceAssistantState.Idle)
    override val state: StateFlow<VoiceAssistantState> = _state.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private val recognitionMutex = Mutex()
    private var recognitionGeneration = 0L
    private var activeRecognitionGeneration = 0L

    private fun createRecognizer(generation: Long): SpeechRecognizer? {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            AppLogger.w(AppLogger.Tag.APP, "Speech recognition is not available on this device.")
            _state.value = VoiceAssistantState.Error("Speech recognition not available")
            return null
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createListener(generation))
        }
        return speechRecognizer
    }

    override fun startListening() {
        scope.launch {
            recognitionMutex.withLock {
                if (_state.value is VoiceAssistantState.Listening ||
                    _state.value is VoiceAssistantState.Processing ||
                    _state.value is VoiceAssistantState.Responding
                ) return@withLock
                val generation = ++recognitionGeneration
                activeRecognitionGeneration = generation
                val settings = preferencesRepository.settingsFlow.first()
                if (!settings.voiceAssistantEnabled) {
                    AppLogger.d(AppLogger.Tag.APP, "Voice Assistant is disabled in settings.")
                    _state.value = VoiceAssistantState.Error("Voice Assistant is disabled")
                    audioCoordinator?.setError("Voice Assistant is disabled")
                    return@withLock
                }

                if (audioCoordinator != null && !audioCoordinator.requestCommandListening()) {
                    AppLogger.w(AppLogger.Tag.APP, "AudioCoordinator denied microphone for VoiceAssistant.")
                    return@withLock
                }

                val recognizer = createRecognizer(generation)
                if (recognizer == null) {
                    audioCoordinator?.setError("Speech recognition not available")
                    return@withLock
                }
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                try {
                    _state.value = VoiceAssistantState.Listening
                    recognizer.startListening(intent)
                } catch (e: Exception) {
                    AppLogger.e(AppLogger.Tag.APP, "Failed to start speech recognizer", e)
                    audioCoordinator?.setError("Voice input is unavailable")
                    _state.value = VoiceAssistantState.Error("Voice input is unavailable. Try again.")
                }
            }
        }
    }

    override fun stopListening() {
        activeRecognitionGeneration = ++recognitionGeneration
        destroyRecognizer()
        scope.launch {
            audioCoordinator?.onCommandListeningFinished()
        }
        if (_state.value is VoiceAssistantState.Listening) {
            _state.value = VoiceAssistantState.Idle
        }
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tag.APP, "Error stopping SpeechRecognizer: ${e.message}")
        }
        speechRecognizer = null
    }

    override fun release() {
        stopListening()
        scope.launch {
            audioCoordinator?.releaseAll()
        }
        _state.value = VoiceAssistantState.Idle
    }

    override fun processTextCommand(commandText: String) {
        scope.launch {
            audioCoordinator?.setProcessing()
            executeCommand(parser.parse(commandText))
        }
    }

    private fun createListener(generation: Long): RecognitionListener {
        fun isCurrent(): Boolean = generation == activeRecognitionGeneration
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                if (!isCurrent()) return
                if (_state.value is VoiceAssistantState.Listening) {
                    audioCoordinator?.setProcessing()
                    _state.value = VoiceAssistantState.Processing("Analyzing...")
                }
            }

            override fun onError(error: Int) {
                if (!isCurrent()) return
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please try again."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out."
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                    else -> "Speech recognition failed. Please try again."
                }
                AppLogger.w(AppLogger.Tag.APP, "Speech recognizer error: $errorMsg")
                destroyRecognizer()
                audioCoordinator?.setError(errorMsg)
                _state.value = VoiceAssistantState.Error(errorMsg)
            }

            override fun onResults(results: Bundle?) {
                if (!isCurrent()) return
                scope.launch {
                    audioCoordinator?.onCommandListeningFinished()
                }
                destroyRecognizer()
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull() ?: ""
                if (recognizedText.isNotBlank()) {
                    _state.value = VoiceAssistantState.Processing(recognizedText)
                    val command = parser.parse(recognizedText)
                    scope.launch {
                        executeCommand(command)
                    }
                } else {
                    _state.value = VoiceAssistantState.Idle
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (!isCurrent()) return
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull()
                if (!partial.isNullOrBlank()) {
                    _state.value = VoiceAssistantState.Processing(partial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private suspend fun executeCommand(command: VoiceCommand) {
        val settings = preferencesRepository.settingsFlow.first()

        when (command) {
            is VoiceCommand.PlayMusic -> {
                val appToUse = command.appName ?: settings.preferredMusicApp
                val androidAuto = carConnectionManager.connectionState.value.isAndroidAutoConnected
                if (androidAuto) {
                    respondWithVoice("Open $appToUse to continue. DriveMate cannot safely control that media app from this car screen.")
                } else {
                    val launched = dispatchMusicIntent(command.query, appToUse)
                    respondWithVoice(
                        if (launched) "Opened $appToUse for ${command.query}."
                        else "I couldn't open $appToUse right now."
                    )
                }
            }

            is VoiceCommand.Navigate -> {
                val androidAuto = carConnectionManager.connectionState.value.isAndroidAutoConnected
                if (androidAuto) {
                    respondWithVoice("Use your navigation app on the car screen to search for ${command.destination}.")
                } else {
                    val launched = destinationManager.launchNavigationQuery(context, command.destination)
                    respondWithVoice(if (launched) "Navigation opened for ${command.destination}." else "Navigation is unavailable right now.")
                }
            }

            VoiceCommand.CheckWeather -> {
                val resolvedLoc = locationResolver?.resolveLocation(settings)
                    ?: WeatherLocationResolverImpl(null).resolveLocation(settings)

                val weather = if (resolvedLoc.isAvailable) {
                    try {
                        weatherRepository.getCurrentWeather(
                            cityName = resolvedLoc.displayName ?: "",
                            latitude = resolvedLoc.latitude,
                            longitude = resolvedLoc.longitude
                        )
                    } catch (e: Exception) {
                        null
                    }
                } else null

                val speech = if (weather != null && weather.isAvailable) {
                    "It is currently ${weather.displayTemperature} and ${weather.conditionText.lowercase()} in ${weather.cityName}."
                } else {
                    "Weather is currently unavailable."
                }
                respondWithVoice(speech)
            }

            VoiceCommand.CheckTripStats -> {
                val stats = tripTracker.tripStats.value
                val activeMins = (stats.activeTripDurationSeconds / 60)
                val speech = if (activeMins > 0 || stats.activeTripDistanceKm > 0) {
                    "You have been driving for $activeMins minutes, covering ${String.format("%.1f", stats.activeTripDistanceKm)} kilometers."
                } else {
                    "Today's total driving distance is ${String.format("%.1f", stats.todayTotalDistanceKm)} kilometers across ${stats.todayTripsCount} trips."
                }
                respondWithVoice(speech)
            }

            VoiceCommand.CheckVehicleCare -> {
                val speech = if (settings.effectiveOdometerKm != null && settings.serviceTargetConfigured) {
                    val remainingKm = settings.remainingServiceKm?.toInt() ?: 0
                    val odoText = String.format(Locale.US, "%,.1f", settings.effectiveOdometerKm)
                    "Your vehicle has covered $odoText kilometers. Next service is due at ${settings.nextServiceKm} kilometers, which is in $remainingKm kilometers."
                } else "Odometer data is unavailable, so service distance cannot be calculated."
                respondWithVoice(speech)
            }

            VoiceCommand.FindCar -> {
                val speech = if (settings.hasParkedLocation) {
                    val address = settings.lastParkedAddress ?: "your last recorded parking coordinates"
                    "Your vehicle is parked at $address."
                } else {
                    "No saved parking location found. DriveMate will automatically save your spot when you park."
                }
                respondWithVoice(speech)
            }

            VoiceCommand.SaveParking -> {
                val speech = if (settings.hasParkedLocation) {
                    "Your parking spot is saved at coordinates ${String.format(Locale.getDefault(), "%.4f, %.4f", settings.lastParkedLatitude ?: 0.0, settings.lastParkedLongitude ?: 0.0)}."
                } else {
                    "Parking spot saved at your current vehicle location."
                }
                respondWithVoice(speech)
            }

            is VoiceCommand.ControlClimate -> {
                val actionResult = capabilityManager?.evaluateAction("climate_control", "climate control")
                val speech = actionResult?.userMessage
                    ?: "Your vehicle doesn't currently provide AC control access to DriveMate. Adjust temperature on the vehicle console."
                respondWithVoice(speech)
            }

            is VoiceCommand.ViewCamera -> {
                val actionResult = capabilityManager?.evaluateAction("camera_360", "${command.cameraType} camera")
                val speech = actionResult?.userMessage
                    ?: "OEM camera feeds are restricted to the vehicle infotainment screen while driving."
                respondWithVoice(speech)
            }

            is VoiceCommand.CheckDocument -> {
                if (carConnectionManager.connectionState.value.isAndroidAutoConnected) {
                    respondWithVoice("Vehicle documents are available only on your phone for privacy.")
                    return
                }
                val docs = documentVaultRepository?.documents?.value ?: emptyList()
                val expiring = docs.filter { it.daysUntilExpiry()?.let { d -> d in 0..30 } == true }
                val speech = if (expiring.isNotEmpty()) {
                    val first = expiring.first()
                    "Attention: Your ${first.title} expires in ${first.daysUntilExpiry()} days. Please renew soon."
                } else if (docs.isNotEmpty()) {
                    "All your ${docs.size} stored vehicle documents are currently valid."
                } else {
                    "Your vehicle documents are saved in the phone-only document vault."
                }
                respondWithVoice(speech)
            }

            VoiceCommand.CheckMaintenance -> {
                val speech = if (settings.remainingServiceKm != null && settings.serviceTargetConfigured) {
                    "Next periodic service is due in ${settings.remainingServiceKm!!.toInt()} kilometers at ${settings.nextServiceKm} kilometers."
                } else "Service interval is unavailable until an odometer and service target are configured."
                respondWithVoice(speech)
            }

            is VoiceCommand.CheckExpenses -> {
                val summary = settings.odometerKm?.let { expenseRepository?.getSummary(it) }
                val speech = if (summary != null) {
                    "You have spent ${summary.currentMonthSpent.toInt()} rupees this month. Average running cost is ${String.format(Locale.getDefault(), "%.1f", summary.costPerKm)} rupees per kilometer."
                } else {
                    "Vehicle running costs and fuel logs can be viewed in your Expense Manager."
                }
                respondWithVoice(speech)
            }

            is VoiceCommand.WatchVideo -> {
                val isCarConnected = carConnectionManager.connectionState.value.isVerifiedCarSession
                if (isCarConnected) {
                    respondWithVoice("For driving safety, video is unavailable on the car screen. Open a compatible media app for audio.")
                } else {
                    val launched = dispatchYouTubeIntent(command.query)
                    respondWithVoice(
                        if (launched) "Opened YouTube for ${command.query} on your phone."
                        else "I couldn't open YouTube right now."
                    )
                }
            }

            is VoiceCommand.Unknown -> {
                val speech = "I didn't quite catch that. You can ask me to play a song, navigate, check the weather, or check trip status."
                respondWithVoice(speech)
            }
        }
    }

    private suspend fun respondWithVoice(speechText: String) {
        _state.value = VoiceAssistantState.Responding(speechText)
        try {
            withContext(Dispatchers.Default) {
                ttsManager.speak(speechText)
            }
        } finally {
            audioCoordinator?.releaseAll()
        }
        delay(400L)
        _state.value = VoiceAssistantState.Idle
    }

    private fun dispatchMusicIntent(query: String, preferredApp: String): Boolean {
        try {
            val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(SearchManager.QUERY, query)
                putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
            }

            when (preferredApp.lowercase()) {
                "spotify" -> {
                    intent.setPackage("com.spotify.music")
                }
                "youtube music", "yt music" -> {
                    intent.setPackage("com.google.android.apps.youtube.music")
                }
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return true
            } else {
                val uriIntent = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:${Uri.encode(query)}")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (uriIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(uriIntent)
                    return true
                } else {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/search/${Uri.encode(query)}")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(webIntent)
                    return true
                }
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.APP, "Failed to dispatch music intent: ${e.message}", e)
        }
        return false
    }

    private fun dispatchYouTubeIntent(query: String): Boolean {
        try {
            val encoded = Uri.encode(query)
            val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$encoded")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (appIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(appIntent)
                return true
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                return true
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.APP, "Failed to launch YouTube: ${e.message}", e)
        }
        return false
    }
}
