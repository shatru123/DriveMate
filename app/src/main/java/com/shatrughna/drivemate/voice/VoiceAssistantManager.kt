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
import com.shatrughna.drivemate.core.telemetry.TelemetryAvailability
import com.shatrughna.drivemate.core.telemetry.TelemetrySource
import com.shatrughna.drivemate.core.telemetry.VehicleDataCoordinator
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
    private val vehicleDataCoordinator: VehicleDataCoordinator? = null,
    private val responseProvider: VoiceResponseProvider = VoiceResponseProvider(),
    private val questionFallback: VoiceQuestionFallback? = null,
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

                val langPref = settings.voiceAssistantLanguage
                val targetLocale = when (langPref.lowercase()) {
                    "hi", "hindi" -> Locale("hi", "IN")
                    "mr", "marathi" -> Locale("mr", "IN")
                    "en", "english" -> Locale.forLanguageTag("en-IN")
                    else -> Locale.getDefault()
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, targetLocale)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, targetLocale.toLanguageTag())
                    if (langPref == "auto") {
                        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "hi-IN", "mr-IN"))
                    }
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
        ttsManager.stop()
        scope.launch {
            audioCoordinator?.onCommandListeningFinished()
        }
        _state.value = VoiceAssistantState.Idle
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
            val command = parser.parse(commandText)
            executeCommand(command, commandText)
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
                        executeCommand(command, recognizedText)
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

    private suspend fun executeCommand(command: VoiceCommand, rawInput: String = "") {
        val settings = preferencesRepository.settingsFlow.first()
        val language = VoiceLanguageClassifier.resolveResponseLanguage(rawInput, settings.voiceAssistantLanguage)

        when (command) {
            is VoiceCommand.CheckSpeed -> {
                val telemetry = vehicleDataCoordinator?.telemetry?.value
                val speech = responseProvider.speed(
                    language = language,
                    speedKmh = telemetry?.speedKmh,
                    source = telemetry?.speedSource ?: TelemetrySource.NONE,
                    availability = telemetry?.speedAvailability ?: TelemetryAvailability.UNAVAILABLE
                )
                respondWithVoice(speech, language)
            }

            is VoiceCommand.CheckOdometer -> {
                val telemetry = vehicleDataCoordinator?.telemetry?.value
                val odo = telemetry?.effectiveOdometerKm ?: settings.effectiveOdometerKm
                val isAuth = telemetry?.isAuthoritativeOdometer ?: (settings.vehicleOdometerKm != null)
                val speech = responseProvider.odometer(
                    language = language,
                    odometerKm = odo,
                    isAuthoritative = isAuth
                )
                respondWithVoice(speech, language)
            }

            is VoiceCommand.CheckFuel -> {
                val telemetry = vehicleDataCoordinator?.telemetry?.value
                val speech = responseProvider.fuel(
                    language = language,
                    fuelPercent = telemetry?.fuelLevelPercent,
                    availability = telemetry?.fuelAvailability ?: TelemetryAvailability.UNAVAILABLE
                )
                respondWithVoice(speech, language)
            }

            is VoiceCommand.CheckRange -> {
                val telemetry = vehicleDataCoordinator?.telemetry?.value
                val speech = responseProvider.range(
                    language = language,
                    rangeKm = telemetry?.rangeRemainingKm,
                    availability = telemetry?.rangeAvailability ?: TelemetryAvailability.UNAVAILABLE
                )
                respondWithVoice(speech, language)
            }

            is VoiceCommand.CheckVehicleStatus -> {
                val telemetry = vehicleDataCoordinator?.telemetry?.value
                val isConnected = carConnectionManager.connectionState.value.isAndroidAutoConnected || (telemetry?.androidAutoConnected == true)
                val hasTelemetry = telemetry?.vehicleTelemetryConnected == true
                val speech = responseProvider.vehicleStatus(
                    language = language,
                    isCarConnected = isConnected,
                    hasTelemetry = hasTelemetry
                )
                respondWithVoice(speech, language)
            }

            is VoiceCommand.CheckAverageSpeed -> {
                val stats = tripTracker.tripStats.value
                val activeSecs = stats.activeMovingDurationSeconds.takeIf { it > 0 } ?: stats.activeTripDurationSeconds
                val avgSpeed = if (activeSecs > 10 && stats.activeTripDistanceKm > 0.05f) {
                    stats.activeTripDistanceKm / (activeSecs / 3600.0f)
                } else null
                val speech = responseProvider.averageSpeed(
                    language = language,
                    avgSpeedKmh = avgSpeed
                )
                respondWithVoice(speech, language)
            }

            is VoiceCommand.CheckTodayDriving -> {
                val stats = tripTracker.tripStats.value
                val speech = responseProvider.todayDriving(
                    language = language,
                    todayDistanceKm = stats.todayTotalDistanceKm,
                    todayTripsCount = stats.todayTripsCount,
                    todayDurationMinutes = stats.todayTotalDurationMinutes
                )
                respondWithVoice(speech, language)
            }

            is VoiceCommand.CheckTripStats -> {
                val stats = tripTracker.tripStats.value
                val speech = responseProvider.tripStats(
                    language = language,
                    activeDurationSeconds = stats.activeTripDurationSeconds,
                    activeDistanceKm = stats.activeTripDistanceKm,
                    todayDistanceKm = stats.todayTotalDistanceKm,
                    todayTripsCount = stats.todayTripsCount
                )
                respondWithVoice(speech, language)
            }

            is VoiceCommand.CheckWeather -> {
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

                val speech = responseProvider.weather(
                    language = language,
                    tempText = weather?.displayTemperature,
                    conditionText = weather?.conditionText?.lowercase(),
                    cityName = weather?.cityName
                )
                respondWithVoice(speech, language)
            }

            is VoiceCommand.FindCar -> {
                val address = if (settings.hasParkedLocation) {
                    settings.lastParkedAddress ?: String.format(Locale.US, "%.4f, %.4f", settings.lastParkedLatitude ?: 0.0, settings.lastParkedLongitude ?: 0.0)
                } else null
                val speech = responseProvider.findCar(
                    language = language,
                    address = address
                )
                respondWithVoice(speech, language)
            }

            is VoiceCommand.SaveParking -> {
                val details = if (settings.hasParkedLocation) {
                    String.format(Locale.US, "%.4f, %.4f", settings.lastParkedLatitude ?: 0.0, settings.lastParkedLongitude ?: 0.0)
                } else null
                val speech = responseProvider.saveParking(
                    language = language,
                    locationDetails = details
                )
                respondWithVoice(speech, language)
            }

            is VoiceCommand.CheckMaintenance -> {
                val speech = responseProvider.maintenance(
                    language = language,
                    remainingKm = settings.remainingServiceKm,
                    nextServiceKm = settings.nextServiceKm,
                    isConfigured = settings.serviceTargetConfigured
                )
                respondWithVoice(speech, language)
            }

            is VoiceCommand.CheckVehicleCare -> {
                val speech = responseProvider.vehicleCare(
                    language = language,
                    effectiveOdoKm = settings.effectiveOdometerKm,
                    remainingKm = settings.remainingServiceKm,
                    nextServiceKm = settings.nextServiceKm,
                    isConfigured = settings.serviceTargetConfigured
                )
                respondWithVoice(speech, language)
            }

            is VoiceCommand.CheckDocument -> {
                val isAa = carConnectionManager.connectionState.value.isAndroidAutoConnected
                val docs = documentVaultRepository?.documents?.value ?: emptyList()
                val expiring = docs.firstOrNull { it.daysUntilExpiry()?.let { d -> d in 0..30 } == true }
                val speech = responseProvider.documents(
                    language = language,
                    isAndroidAuto = isAa,
                    expiringTitle = expiring?.title,
                    daysLeft = expiring?.daysUntilExpiry(),
                    totalDocsCount = docs.size
                )
                respondWithVoice(speech, language)
            }

            is VoiceCommand.CheckExpenses -> {
                val summary = settings.odometerKm?.let { expenseRepository?.getSummary(it) }
                val speech = responseProvider.expenses(
                    language = language,
                    currentMonthSpent = summary?.currentMonthSpent,
                    costPerKm = summary?.costPerKm
                )
                respondWithVoice(speech, language)
            }

            is VoiceCommand.PlayMusic -> {
                val appToUse = command.appName ?: settings.preferredMusicApp
                val androidAuto = carConnectionManager.connectionState.value.isAndroidAutoConnected
                if (androidAuto) {
                    val speech = when (language) {
                        VoiceLanguage.HINDI -> "आगे बढ़ने के लिए $appToUse खोलें। ड्राइवमेट इस कार स्क्रीन से मीडिया नियंत्रित नहीं कर सकता।"
                        VoiceLanguage.MARATHI -> "पुढे सुरू ठेवण्यासाठी $appToUse उघडा. ड्राईव्हमेट या कार स्क्रीनवरून मीडिया नियंत्रित करू शकत नाही."
                        else -> "Open $appToUse to continue. DriveMate cannot safely control that media app from this car screen."
                    }
                    respondWithVoice(speech, language)
                } else {
                    val launched = dispatchMusicIntent(command.query, appToUse)
                    val speech = if (launched) {
                        when (language) {
                            VoiceLanguage.HINDI -> "${command.query} के लिए $appToUse खोला गया।"
                            VoiceLanguage.MARATHI -> "${command.query} साठी $appToUse उघडले."
                            else -> "Opened $appToUse for ${command.query}."
                        }
                    } else {
                        when (language) {
                            VoiceLanguage.HINDI -> "मैं अभी $appToUse नहीं खोल सका।"
                            VoiceLanguage.MARATHI -> "मी आता $appToUse उघडू शकलो नाही."
                            else -> "I couldn't open $appToUse right now."
                        }
                    }
                    respondWithVoice(speech, language)
                }
            }

            is VoiceCommand.Navigate -> {
                val androidAuto = carConnectionManager.connectionState.value.isAndroidAutoConnected
                if (androidAuto) {
                    val speech = when (language) {
                        VoiceLanguage.HINDI -> "कार स्क्रीन पर अपने नेविगेशन ऐप से ${command.destination} सर्च करें।"
                        VoiceLanguage.MARATHI -> "कार स्क्रीनवरील नेव्हिगेशन ॲपमध्ये ${command.destination} शोधा."
                        else -> "Use your navigation app on the car screen to search for ${command.destination}."
                    }
                    respondWithVoice(speech, language)
                } else {
                    val launched = destinationManager.launchNavigationQuery(context, command.destination)
                    val speech = if (launched) {
                        when (language) {
                            VoiceLanguage.HINDI -> "${command.destination} के लिए नेविगेशन शुरू किया गया।"
                            VoiceLanguage.MARATHI -> "${command.destination} साठी नेव्हिगेशन सुरू केले."
                            else -> "Navigation opened for ${command.destination}."
                        }
                    } else {
                        when (language) {
                            VoiceLanguage.HINDI -> "नेविगेशन अभी उपलब्ध नहीं है।"
                            VoiceLanguage.MARATHI -> "नेव्हिगेशन सध्या उपलब्ध नाही."
                            else -> "Navigation is unavailable right now."
                        }
                    }
                    respondWithVoice(speech, language)
                }
            }

            is VoiceCommand.ControlClimate -> {
                val actionResult = capabilityManager?.evaluateAction("climate_control", "climate control")
                val speech = actionResult?.userMessage ?: when (language) {
                    VoiceLanguage.HINDI -> "आपका वाहन अभी ड्राइवमेट को एसी नियंत्रण की अनुमति नहीं देता। कृपया वाहन कंसोल का उपयोग करें।"
                    VoiceLanguage.MARATHI -> "तुमचे वाहन सध्या ड्राईव्हमेटला एसी नियंत्रणाची परवानगी देत नाही. कृपया वाहन कन्सोल वापरा."
                    else -> "Your vehicle doesn't currently provide AC control access to DriveMate. Adjust temperature on the vehicle console."
                }
                respondWithVoice(speech, language)
            }

            is VoiceCommand.ViewCamera -> {
                val actionResult = capabilityManager?.evaluateAction("camera_360", "${command.cameraType} camera")
                val speech = actionResult?.userMessage ?: when (language) {
                    VoiceLanguage.HINDI -> "सुरक्षा के लिए कैमरा फीड वाहन स्क्रीन तक सीमित है।"
                    VoiceLanguage.MARATHI -> "ड्रायव्हिंग सुरक्षेसाठी कॅमेरा फीड इन्फोटेनमेंट स्क्रीनपुरती मर्यादित आहे."
                    else -> "OEM camera feeds are restricted to the vehicle infotainment screen while driving."
                }
                respondWithVoice(speech, language)
            }

            is VoiceCommand.WatchVideo -> {
                val isCarConnected = carConnectionManager.connectionState.value.isVerifiedCarSession
                if (isCarConnected) {
                    val speech = when (language) {
                        VoiceLanguage.HINDI -> "ड्राइविंग सुरक्षा के लिए कार स्क्रीन पर वीडियो उपलब्ध नहीं है।"
                        VoiceLanguage.MARATHI -> "ड्रायव्हिंग सुरक्षेसाठी कार स्क्रीनवर व्हिडिओ उपलब्ध नाही."
                        else -> "For driving safety, video is unavailable on the car screen. Open a compatible media app for audio."
                    }
                    respondWithVoice(speech, language)
                } else {
                    val launched = dispatchYouTubeIntent(command.query)
                    val speech = if (launched) {
                        when (language) {
                            VoiceLanguage.HINDI -> "फोन पर ${command.query} के लिए यूट्यूब खोला गया।"
                            VoiceLanguage.MARATHI -> "फोनवर ${command.query} साठी यूट्यूब उघडले."
                            else -> "Opened YouTube for ${command.query} on your phone."
                        }
                    } else {
                        when (language) {
                            VoiceLanguage.HINDI -> "यूट्यूब अभी नहीं खोला जा सका।"
                            VoiceLanguage.MARATHI -> "यूट्यूब आता उघडता आले नाही."
                            else -> "I couldn't open YouTube right now."
                        }
                    }
                    respondWithVoice(speech, language)
                }
            }

            is VoiceCommand.Help -> {
                respondWithVoice(responseProvider.help(language), language)
            }

            is VoiceCommand.AboutAssistant -> {
                respondWithVoice(responseProvider.about(language), language)
            }

            is VoiceCommand.AssistantStatus -> {
                respondWithVoice(responseProvider.assistantStatus(language), language)
            }

            is VoiceCommand.Greeting -> {
                respondWithVoice(responseProvider.greeting(language), language)
            }

            is VoiceCommand.ThankYou -> {
                respondWithVoice(responseProvider.thankYou(language), language)
            }

            is VoiceCommand.StopAssistant -> {
                ttsManager.stop()
                audioCoordinator?.releaseAll()
                _state.value = VoiceAssistantState.Idle
            }

            is VoiceCommand.Unknown -> {
                val fallbackResponse = questionFallback?.answer(command.rawQuery, language)
                val speech = fallbackResponse ?: responseProvider.unknown(language)
                respondWithVoice(speech, language)
            }
        }
    }

    private suspend fun respondWithVoice(speechText: String, language: VoiceLanguage) {
        _state.value = VoiceAssistantState.Responding(speechText)
        try {
            val targetLocale = language.locale
            val isSupported = if (language == VoiceLanguage.MARATHI || language == VoiceLanguage.HINDI) {
                ttsManager.isLanguageAvailable(targetLocale)
            } else true

            val (finalSpeech, finalLocale) = if (!isSupported) {
                AppLogger.w(AppLogger.Tag.TTS, "TTS voice not available on device for ${language.displayName}, falling back to English")
                "${responseProvider.unsupportedTtsFallback(language)} $speechText" to Locale.forLanguageTag("en-IN")
            } else {
                speechText to targetLocale
            }

            withContext(Dispatchers.Default) {
                ttsManager.speak(finalSpeech, finalLocale)
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
