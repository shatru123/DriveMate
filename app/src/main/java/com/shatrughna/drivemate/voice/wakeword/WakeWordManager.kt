package com.shatrughna.drivemate.voice.wakeword

import com.shatrughna.drivemate.data.preferences.DriveMatePreferencesRepository
import com.shatrughna.drivemate.driving.DrivingSessionManager
import com.shatrughna.drivemate.greeting.GreetingController
import com.shatrughna.drivemate.greeting.GreetingTtsManager
import com.shatrughna.drivemate.util.AppLogger
import com.shatrughna.drivemate.voice.AudioInputCoordinator
import com.shatrughna.drivemate.voice.VoiceAssistantManager
import com.shatrughna.drivemate.voice.VoiceAssistantState
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

/**
 * State machine representing the lifecycle and interaction state of hands-free voice activation.
 */
enum class WakeWordState {
    STOPPED,
    IDLE,
    LISTENING_FOR_WAKE_WORD,
    WAKE_WORD_DETECTED,
    LISTENING_FOR_COMMAND,
    PROCESSING,
    RESPONDING,
    ERROR
}

interface WakeWordManager {
    val state: StateFlow<WakeWordState>
    fun start()
    fun stop()
}

class WakeWordManagerImpl(
    private val wakeWordEngine: WakeWordEngine,
    private val voiceAssistantManager: VoiceAssistantManager,
    private val greetingController: GreetingController,
    private val ttsManager: GreetingTtsManager,
    private val sessionManager: DrivingSessionManager,
    private val preferencesRepository: DriveMatePreferencesRepository,
    private val audioCoordinator: AudioInputCoordinator? = null,
    private val carConnectionManager: com.shatrughna.drivemate.car.CarConnectionManager? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) : WakeWordManager, WakeWordListener {

    private val _state = MutableStateFlow(WakeWordState.STOPPED)
    override val state: StateFlow<WakeWordState> = _state.asStateFlow()

    private val mutex = Mutex()
    private var commandTimeoutJob: Job? = null

    init {
        wakeWordEngine.setListener(this)

        // Bind lifecycle to verified driving sessions
        scope.launch {
            sessionManager.isSessionActive.collectLatest { isActive ->
                if (isActive) {
                    val isAndroidAuto = carConnectionManager?.connectionState?.value?.isAndroidAutoConnected == true
                    if (isAndroidAuto) {
                        AppLogger.i(
                            AppLogger.Tag.AUDIO,
                            "Android Auto active: Continuous hotword suppressed to preserve Spotify/car media playback. Explicit push-to-talk active."
                        )
                        stop()
                        return@collectLatest
                    }

                    val settings = preferencesRepository.settingsFlow.first()
                    if (settings.heyDriveMateEnabled) {
                        AppLogger.i(AppLogger.Tag.APP, "Driving session active: Starting \"Hey DriveMate\" engine.")
                        start()
                    }
                } else {
                    AppLogger.i(AppLogger.Tag.APP, "Driving session ended: Stopping \"Hey DriveMate\" engine.")
                    stop()
                }
            }
        }

        // Monitor VoiceAssistantManager state to sync and resume listening
        scope.launch {
            voiceAssistantManager.state.collectLatest { vaState ->
                when (vaState) {
                    is VoiceAssistantState.Processing -> {
                        commandTimeoutJob?.cancel()
                        _state.value = WakeWordState.PROCESSING
                    }
                    is VoiceAssistantState.Responding -> {
                        commandTimeoutJob?.cancel()
                        _state.value = WakeWordState.RESPONDING
                    }
                    is VoiceAssistantState.Idle -> {
                        if (_state.value == WakeWordState.PROCESSING || _state.value == WakeWordState.RESPONDING) {
                            resumeWakeWordListening()
                        }
                    }
                    is VoiceAssistantState.Error -> {
                        commandTimeoutJob?.cancel()
                        _state.value = WakeWordState.ERROR
                        delay(2000L)
                        resumeWakeWordListening()
                    }
                    VoiceAssistantState.Listening -> {
                        // Handled when command listening is requested
                    }
                }
            }
        }
    }

    override fun start() {
        scope.launch {
            mutex.withLock {
                val isAndroidAuto = carConnectionManager?.connectionState?.value?.isAndroidAutoConnected == true
                if (isAndroidAuto) {
                    AppLogger.d(AppLogger.Tag.AUDIO, "Android Auto projection active: continuous hotword recognition suppressed.")
                    _state.value = WakeWordState.STOPPED
                    return@withLock
                }

                val settings = preferencesRepository.settingsFlow.first()
                if (!settings.heyDriveMateEnabled) {
                    AppLogger.d(AppLogger.Tag.APP, "Hey DriveMate is disabled in settings. Skipping start.")
                    _state.value = WakeWordState.STOPPED
                    return@withLock
                }

                if (audioCoordinator != null && !audioCoordinator.requestWakeWordListening()) {
                    AppLogger.w(AppLogger.Tag.APP, "AudioCoordinator denied microphone for WakeWordEngine.")
                    return@withLock
                }

                if (!wakeWordEngine.isRunning) {
                    wakeWordEngine.start()
                    _state.value = WakeWordState.LISTENING_FOR_WAKE_WORD
                }
            }
        }
    }

    override fun stop() {
        scope.launch {
            mutex.withLock {
                commandTimeoutJob?.cancel()
                commandTimeoutJob = null
                wakeWordEngine.stop()
                audioCoordinator?.releaseAll()
                _state.value = WakeWordState.STOPPED
            }
        }
    }

    override fun onWakeWordDetected(phrase: String, trailingCommand: String?) {
        scope.launch {
            AppLogger.i(AppLogger.Tag.APP, "WakeWordManager: Handshake detected! phrase=\"$phrase\", trailing=\"$trailingCommand\"")

            // Priority coordination: immediately halt greeting speech so driver's command takes precedence
            greetingController.stopSpeaking()

            // Synchronously halt hotword recognition engine during interaction
            wakeWordEngine.stop()
            audioCoordinator?.onWakeWordDetected()

            if (!trailingCommand.isNullOrBlank()) {
                // Compound command in one breath ("Hey DriveMate, navigate to Pune airport")
                _state.value = WakeWordState.PROCESSING
                audioCoordinator?.setProcessing()
                voiceAssistantManager.processTextCommand(trailingCommand)
            } else {
                // Direct standalone activation -> Respond "Yes?" and await command
                _state.value = WakeWordState.WAKE_WORD_DETECTED
                audioCoordinator?.onTtsStarted()
                ttsManager.speak("Yes?")
                audioCoordinator?.onTtsCompleted()

                if (audioCoordinator != null && !audioCoordinator.requestCommandListening()) {
                    AppLogger.w(AppLogger.Tag.APP, "AudioCoordinator denied command listening.")
                    resumeWakeWordListening()
                    return@launch
                }

                _state.value = WakeWordState.LISTENING_FOR_COMMAND
                voiceAssistantManager.startListening()

                // Arm 6-second timeout: if no command received, return to hotword detection
                commandTimeoutJob?.cancel()
                commandTimeoutJob = scope.launch {
                    delay(6000L)
                    if (_state.value == WakeWordState.LISTENING_FOR_COMMAND) {
                        AppLogger.d(AppLogger.Tag.APP, "WakeWordManager: Command window timed out after 6s. Resuming wake word listening.")
                        voiceAssistantManager.stopListening()
                        resumeWakeWordListening()
                    }
                }
            }
        }
    }

    override fun onError(error: String) {
        AppLogger.w(AppLogger.Tag.APP, "WakeWordManager: Engine error: $error")
        _state.value = WakeWordState.ERROR
        audioCoordinator?.setError(error)
    }

    private fun resumeWakeWordListening() {
        scope.launch {
            if (carConnectionManager?.connectionState?.value?.isAndroidAutoConnected == true) {
                _state.value = WakeWordState.STOPPED
                return@launch
            }
            if (sessionManager.isSessionActive.value) {
                val settings = preferencesRepository.settingsFlow.first()
                if (settings.heyDriveMateEnabled) {
                    delay(400L) // Brief delay to let audio channel clear
                    if (audioCoordinator == null || audioCoordinator.requestWakeWordListening()) {
                        wakeWordEngine.start()
                        _state.value = WakeWordState.LISTENING_FOR_WAKE_WORD
                    }
                }
            } else {
                _state.value = WakeWordState.STOPPED
                audioCoordinator?.releaseAll()
            }
        }
    }
}
