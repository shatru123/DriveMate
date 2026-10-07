package com.shatrughna.drivemate.voice

import com.shatrughna.drivemate.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * State representing audio ownership and microphone access state machine:
 * IDLE -> WAKE_WORD_LISTENING -> WAKE_WORD_DETECTED -> TTS_RESPONSE -> COMMAND_LISTENING -> PROCESSING -> TTS_RESPONSE -> WAKE_WORD_LISTENING
 */
enum class AudioOwnerState {
    IDLE,
    WAKE_WORD_LISTENING,
    WAKE_WORD_DETECTED,
    TTS_RESPONSE,
    COMMAND_LISTENING,
    PROCESSING,
    ERROR
}

/**
 * Active owner holding the hardware microphone or audio focus.
 */
enum class AudioResourceOwner {
    NONE,
    WAKE_WORD_ENGINE,
    VOICE_ASSISTANT_COMMAND,
    TTS_PLAYBACK
}

/**
 * Centralized coordinator ensuring strictly mutual exclusion for microphone and audio output.
 * Ensures that:
 * 1. Only ONE speech recognition engine holds the microphone at a time.
 * 2. Speech recognition is suspended while TTS is speaking (prevents acoustic feedback).
 * 3. All resources are immediately released on disconnect.
 */
interface AudioInputCoordinator {
    val state: StateFlow<AudioOwnerState>
    val activeOwner: StateFlow<AudioResourceOwner>

    suspend fun requestWakeWordListening(): Boolean
    suspend fun onWakeWordDetected()
    suspend fun onTtsStarted()
    suspend fun onTtsCompleted()
    suspend fun requestCommandListening(): Boolean
    suspend fun onCommandListeningFinished()
    fun setProcessing()
    fun setError(message: String)
    suspend fun releaseAll()
}

class AudioInputCoordinatorImpl : AudioInputCoordinator {

    private val mutex = Mutex()

    private val _state = MutableStateFlow(AudioOwnerState.IDLE)
    override val state: StateFlow<AudioOwnerState> = _state.asStateFlow()

    private val _activeOwner = MutableStateFlow(AudioResourceOwner.NONE)
    override val activeOwner: StateFlow<AudioResourceOwner> = _activeOwner.asStateFlow()

    override suspend fun requestWakeWordListening(): Boolean = mutex.withLock {
        // Strictly reject wake word listening while TTS is actively speaking
        if (_activeOwner.value == AudioResourceOwner.TTS_PLAYBACK) {
            AppLogger.w(AppLogger.Tag.APP, "AudioCoordinator: Denied wake word listening because TTS_PLAYBACK is active")
            return false
        }

        // Only grant wake word listening if idle, none, error or coming from processing
        if (_activeOwner.value == AudioResourceOwner.NONE ||
            _state.value == AudioOwnerState.PROCESSING ||
            _state.value == AudioOwnerState.IDLE ||
            _state.value == AudioOwnerState.ERROR
        ) {
            _activeOwner.value = AudioResourceOwner.WAKE_WORD_ENGINE
            _state.value = AudioOwnerState.WAKE_WORD_LISTENING
            AppLogger.d(AppLogger.Tag.APP, "AudioCoordinator: Granted microphone to WAKE_WORD_ENGINE")
            return true
        }
        AppLogger.w(AppLogger.Tag.APP, "AudioCoordinator: Denied wake word listening. Active owner: ${_activeOwner.value}")
        return false
    }

    override suspend fun onWakeWordDetected() = mutex.withLock {
        _activeOwner.value = AudioResourceOwner.NONE
        _state.value = AudioOwnerState.WAKE_WORD_DETECTED
        AppLogger.i(AppLogger.Tag.APP, "AudioCoordinator: Wake word detected. Released microphone from WakeWordEngine.")
    }

    override suspend fun onTtsStarted() = mutex.withLock {
        _activeOwner.value = AudioResourceOwner.TTS_PLAYBACK
        _state.value = AudioOwnerState.TTS_RESPONSE
        AppLogger.d(AppLogger.Tag.APP, "AudioCoordinator: TTS playback started. Microphone locked.")
    }

    override suspend fun onTtsCompleted() = mutex.withLock {
        if (_activeOwner.value == AudioResourceOwner.TTS_PLAYBACK) {
            _activeOwner.value = AudioResourceOwner.NONE
        }
        AppLogger.d(AppLogger.Tag.APP, "AudioCoordinator: TTS playback completed. Audio channel cleared.")
    }

    override suspend fun requestCommandListening(): Boolean = mutex.withLock {
        // Strictly reject command listening while TTS is actively playing audio to prevent acoustic feedback
        if (_activeOwner.value == AudioResourceOwner.TTS_PLAYBACK) {
            AppLogger.w(AppLogger.Tag.APP, "AudioCoordinator: Denied command listening because TTS_PLAYBACK is active")
            return false
        }

        // Command listening allowed if wake word was detected or manually requested while idle/none/error
        if (_activeOwner.value == AudioResourceOwner.NONE ||
            _state.value == AudioOwnerState.WAKE_WORD_DETECTED ||
            _state.value == AudioOwnerState.IDLE ||
            _state.value == AudioOwnerState.ERROR
        ) {
            _activeOwner.value = AudioResourceOwner.VOICE_ASSISTANT_COMMAND
            _state.value = AudioOwnerState.COMMAND_LISTENING
            AppLogger.d(AppLogger.Tag.APP, "AudioCoordinator: Granted microphone to VOICE_ASSISTANT_COMMAND")
            return true
        }
        AppLogger.w(AppLogger.Tag.APP, "AudioCoordinator: Denied command listening. Active owner: ${_activeOwner.value}")
        return false
    }

    override suspend fun onCommandListeningFinished() = mutex.withLock {
        if (_activeOwner.value == AudioResourceOwner.VOICE_ASSISTANT_COMMAND) {
            _activeOwner.value = AudioResourceOwner.NONE
        }
        AppLogger.d(AppLogger.Tag.APP, "AudioCoordinator: Command listening finished. Microphone released.")
    }

    override fun setProcessing() {
        _activeOwner.value = AudioResourceOwner.NONE
        _state.value = AudioOwnerState.PROCESSING
        AppLogger.d(AppLogger.Tag.APP, "AudioCoordinator: Set state to PROCESSING")
    }

    override fun setError(message: String) {
        _activeOwner.value = AudioResourceOwner.NONE
        _state.value = AudioOwnerState.ERROR
        AppLogger.w(AppLogger.Tag.APP, "AudioCoordinator: Set state to ERROR: $message")
    }

    override suspend fun releaseAll() = mutex.withLock {
        _activeOwner.value = AudioResourceOwner.NONE
        _state.value = AudioOwnerState.IDLE
        AppLogger.i(AppLogger.Tag.APP, "AudioCoordinator: Released all audio resources. State -> IDLE")
    }
}
