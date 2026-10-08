package com.shatrughna.drivemate.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import com.shatrughna.drivemate.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Strict centralized audio ownership and microphone access state machine:
 * IDLE -> USER_REQUESTED_VOICE -> REQUESTING_AUDIO_FOCUS -> RECORDING -> PROCESSING -> TTS -> RELEASE_AUDIO_FOCUS -> IDLE
 */
enum class AudioOwnerState {
    IDLE,
    USER_REQUESTED_VOICE,
    REQUESTING_AUDIO_FOCUS,
    RECORDING,
    PROCESSING,
    TTS,
    RELEASE_AUDIO_FOCUS,
    ERROR,

    // Legacy/compatibility states preserved for existing callers & tests
    @Deprecated("Use RECORDING or USER_REQUESTED_VOICE")
    WAKE_WORD_LISTENING,
    @Deprecated("Use USER_REQUESTED_VOICE")
    WAKE_WORD_DETECTED,
    @Deprecated("Use TTS")
    TTS_RESPONSE,
    @Deprecated("Use RECORDING")
    COMMAND_LISTENING
}

/**
 * Active owner holding the hardware microphone or audio focus.
 */
enum class AudioResourceOwner {
    NONE,
    WAKE_WORD_ENGINE,
    VOICE_ASSISTANT_COMMAND,
    TTS_PLAYBACK,
    MICROPHONE_RECORDING
}

/**
 * Centralized coordinator ensuring strict mutual exclusion for microphone and audio output.
 * Ensures that:
 * 1. Only ONE voice engine / speech recognizer holds the microphone at any time.
 * 2. Speech recognition is halted while TTS is speaking (prevents acoustic feedback and locks).
 * 3. Audio focus is requested transiently and abandoned immediately on completion, error, or disconnect.
 * 4. Music media playback (Spotify / YouTube Music) is never disrupted by background connection loops.
 */
interface AudioInputCoordinator {
    val state: StateFlow<AudioOwnerState>
    val activeOwner: StateFlow<AudioResourceOwner>

    suspend fun requestVoiceInput(): Boolean
    suspend fun onRecordingStarted()
    suspend fun onRecordingFinished()
    suspend fun onTtsStarted(): Boolean
    suspend fun onTtsCompleted()
    fun setProcessing()
    fun setError(message: String)
    fun abandonAudioFocus()
    suspend fun releaseAll()

    // Backward compatibility methods for wake word and command listening
    suspend fun requestWakeWordListening(): Boolean
    suspend fun onWakeWordDetected()
    suspend fun requestCommandListening(): Boolean
    suspend fun onCommandListeningFinished()
}

class AudioInputCoordinatorImpl(context: Context? = null) : AudioInputCoordinator {

    private val mutex = Mutex()

    private val _state = MutableStateFlow(AudioOwnerState.IDLE)
    override val state: StateFlow<AudioOwnerState> = _state.asStateFlow()

    private val _activeOwner = MutableStateFlow(AudioResourceOwner.NONE)
    override val activeOwner: StateFlow<AudioResourceOwner> = _activeOwner.asStateFlow()

    private val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    override suspend fun requestVoiceInput(): Boolean = mutex.withLock {
        if (_activeOwner.value == AudioResourceOwner.TTS_PLAYBACK || _state.value == AudioOwnerState.TTS) {
            AppLogger.w(AppLogger.Tag.AUDIO_FOCUS, "AudioCoordinator: Denied voice input because TTS is actively speaking.")
            return false
        }

        if (_activeOwner.value == AudioResourceOwner.MICROPHONE_RECORDING ||
            _activeOwner.value == AudioResourceOwner.VOICE_ASSISTANT_COMMAND
        ) {
            AppLogger.w(AppLogger.Tag.AUDIO_FOCUS, "AudioCoordinator: Voice input already active.")
            return false
        }

        _state.value = AudioOwnerState.USER_REQUESTED_VOICE
        AppLogger.i(AppLogger.Tag.AUDIO_FOCUS, "[AUDIO_FOCUS] State -> USER_REQUESTED_VOICE")

        _state.value = AudioOwnerState.REQUESTING_AUDIO_FOCUS
        AppLogger.i(AppLogger.Tag.AUDIO_FOCUS, "[AUDIO_FOCUS] State -> REQUESTING_AUDIO_FOCUS")

        return requestAudioFocusLocked(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
    }

    override suspend fun onRecordingStarted() = mutex.withLock {
        _activeOwner.value = AudioResourceOwner.MICROPHONE_RECORDING
        _state.value = AudioOwnerState.RECORDING
        AppLogger.i(AppLogger.Tag.MICROPHONE, "[MIC_STARTED] Microphone opened for user command. State -> RECORDING")
    }

    override suspend fun onRecordingFinished() = mutex.withLock {
        if (_activeOwner.value == AudioResourceOwner.MICROPHONE_RECORDING ||
            _activeOwner.value == AudioResourceOwner.VOICE_ASSISTANT_COMMAND
        ) {
            _activeOwner.value = AudioResourceOwner.NONE
        }
        _state.value = AudioOwnerState.PROCESSING
        AppLogger.i(AppLogger.Tag.MICROPHONE, "[MIC_STOPPED] Microphone released immediately. State -> PROCESSING")
    }

    override suspend fun onTtsStarted(): Boolean = mutex.withLock {
        if (_activeOwner.value == AudioResourceOwner.MICROPHONE_RECORDING ||
            _activeOwner.value == AudioResourceOwner.VOICE_ASSISTANT_COMMAND
        ) {
            AppLogger.w(AppLogger.Tag.AUDIO, "AudioCoordinator: Forcing microphone release before TTS playback.")
            _activeOwner.value = AudioResourceOwner.NONE
        }
        _activeOwner.value = AudioResourceOwner.TTS_PLAYBACK
        _state.value = AudioOwnerState.TTS
        val granted = requestAudioFocusLocked(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        if (!granted) {
            _activeOwner.value = AudioResourceOwner.NONE
            _state.value = AudioOwnerState.ERROR
        }
        AppLogger.i(AppLogger.Tag.TTS, "[TTS_STARTED] State -> TTS (focusGranted=$granted)")
        granted
    }

    override suspend fun onTtsCompleted() = mutex.withLock {
        if (_activeOwner.value == AudioResourceOwner.TTS_PLAYBACK) {
            _activeOwner.value = AudioResourceOwner.NONE
        }
        _state.value = AudioOwnerState.RELEASE_AUDIO_FOCUS
        AppLogger.i(AppLogger.Tag.TTS, "[TTS_COMPLETED] State -> RELEASE_AUDIO_FOCUS")

        releaseAudioFocusLocked()
        _state.value = AudioOwnerState.IDLE
        AppLogger.i(AppLogger.Tag.AUDIO_FOCUS, "[AUDIO_FOCUS_ABANDONED] Audio focus released. State -> IDLE")
    }

    override fun setProcessing() {
        if (_activeOwner.value != AudioResourceOwner.TTS_PLAYBACK) {
            _activeOwner.value = AudioResourceOwner.NONE
        }
        _state.value = AudioOwnerState.PROCESSING
        AppLogger.d(AppLogger.Tag.AUDIO, "AudioCoordinator: State -> PROCESSING")
    }

    override fun setError(message: String) {
        _activeOwner.value = AudioResourceOwner.NONE
        releaseAudioFocusLocked()
        _state.value = AudioOwnerState.ERROR
        AppLogger.w(AppLogger.Tag.AUDIO, "AudioCoordinator: State -> ERROR: $message")
    }

    override fun abandonAudioFocus() {
        _activeOwner.value = AudioResourceOwner.NONE
        releaseAudioFocusLocked()
        _state.value = AudioOwnerState.IDLE
        AppLogger.i(AppLogger.Tag.AUDIO_FOCUS, "[AUDIO_FOCUS_ABANDONED] Complete audio focus release. State -> IDLE")
    }

    override suspend fun releaseAll() = mutex.withLock {
        abandonAudioFocus()
    }

    // --- Backward Compatibility Implementations ---

    override suspend fun requestWakeWordListening(): Boolean = mutex.withLock {
        if (_activeOwner.value == AudioResourceOwner.TTS_PLAYBACK) {
            AppLogger.w(AppLogger.Tag.AUDIO, "AudioCoordinator: Denied wake word listening because TTS_PLAYBACK is active")
            return false
        }
        if (_activeOwner.value == AudioResourceOwner.VOICE_ASSISTANT_COMMAND ||
            _activeOwner.value == AudioResourceOwner.MICROPHONE_RECORDING
        ) {
            AppLogger.w(AppLogger.Tag.AUDIO, "AudioCoordinator: Denied wake word listening because command recording is active")
            return false
        }

        if (_activeOwner.value == AudioResourceOwner.NONE ||
            _state.value == AudioOwnerState.PROCESSING ||
            _state.value == AudioOwnerState.IDLE ||
            _state.value == AudioOwnerState.ERROR
        ) {
            _activeOwner.value = AudioResourceOwner.WAKE_WORD_ENGINE
            _state.value = AudioOwnerState.WAKE_WORD_LISTENING
            AppLogger.d(AppLogger.Tag.AUDIO, "AudioCoordinator: Granted microphone to WAKE_WORD_ENGINE")
            return true
        }
        return false
    }

    override suspend fun onWakeWordDetected() = mutex.withLock {
        _activeOwner.value = AudioResourceOwner.NONE
        _state.value = AudioOwnerState.WAKE_WORD_DETECTED
        AppLogger.i(AppLogger.Tag.AUDIO, "AudioCoordinator: Wake word detected. Released microphone from WakeWordEngine.")
    }

    override suspend fun requestCommandListening(): Boolean = mutex.withLock {
        if (_activeOwner.value == AudioResourceOwner.TTS_PLAYBACK) {
            AppLogger.w(AppLogger.Tag.AUDIO, "AudioCoordinator: Denied command listening because TTS_PLAYBACK is active")
            return false
        }

        if (_activeOwner.value == AudioResourceOwner.NONE ||
            _activeOwner.value == AudioResourceOwner.WAKE_WORD_ENGINE ||
            _state.value == AudioOwnerState.WAKE_WORD_DETECTED ||
            _state.value == AudioOwnerState.USER_REQUESTED_VOICE ||
            _state.value == AudioOwnerState.REQUESTING_AUDIO_FOCUS ||
            _state.value == AudioOwnerState.IDLE ||
            _state.value == AudioOwnerState.ERROR
        ) {
            if (!requestAudioFocusLocked(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)) return false
            _activeOwner.value = AudioResourceOwner.VOICE_ASSISTANT_COMMAND
            _state.value = AudioOwnerState.COMMAND_LISTENING
            AppLogger.d(AppLogger.Tag.AUDIO, "AudioCoordinator: Granted microphone to VOICE_ASSISTANT_COMMAND")
            return true
        }
        return false
    }

    override suspend fun onCommandListeningFinished() = mutex.withLock {
        if (_activeOwner.value == AudioResourceOwner.VOICE_ASSISTANT_COMMAND ||
            _activeOwner.value == AudioResourceOwner.MICROPHONE_RECORDING
        ) {
            _activeOwner.value = AudioResourceOwner.NONE
        }
        _state.value = AudioOwnerState.PROCESSING
        AppLogger.d(AppLogger.Tag.AUDIO, "AudioCoordinator: Command listening finished. State -> PROCESSING")
    }

    private fun requestAudioFocusLocked(focusGain: Int): Boolean {
        val manager = audioManager ?: return true
        return try {
            releaseAudioFocusLocked()
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val request = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AudioFocusRequest.Builder(focusGain)
                    .setAudioAttributes(attributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { change ->
                        when (change) {
                            AudioManager.AUDIOFOCUS_LOSS,
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                                AppLogger.w(AppLogger.Tag.AUDIO_FOCUS, "Audio focus lost; releasing DriveMate audio resources")
                                abandonAudioFocus()
                            }
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                                AppLogger.d(AppLogger.Tag.AUDIO_FOCUS, "Audio focus may duck; keeping the active DriveMate utterance alive")
                            }
                        }
                    }
                    .build()
            } else null
            val result = if (request != null) {
                audioFocusRequest = request
                manager.requestAudioFocus(request)
            } else {
                @Suppress("DEPRECATION")
                manager.requestAudioFocus({ change -> if (change == AudioManager.AUDIOFOCUS_LOSS) abandonAudioFocus() }, AudioManager.STREAM_MUSIC, focusGain)
            }
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tag.AUDIO_FOCUS, "Unable to acquire transient audio focus", e)
            false
        }
    }

    private fun releaseAudioFocusLocked() {
        val manager = audioManager ?: return
        try {
            audioFocusRequest?.let { manager.abandonAudioFocusRequest(it) }
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tag.AUDIO_FOCUS, "Unable to abandon audio focus", e)
        } finally {
            audioFocusRequest = null
        }
    }
}
