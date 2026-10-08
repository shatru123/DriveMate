package com.shatrughna.drivemate.greeting

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.shatrughna.drivemate.util.AppLogger
import com.shatrughna.drivemate.voice.AudioInputCoordinator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.Locale
import java.util.UUID

data class VoiceInfo(
    val name: String,
    val locale: Locale,
    val isNetworkConnectionRequired: Boolean
)

interface GreetingTtsManager {
    val isInitialized: StateFlow<Boolean>
    val isSpeaking: StateFlow<Boolean>

    suspend fun initialize(): Result<Unit>
    suspend fun speak(text: String, locale: Locale? = null): Result<Unit>
    fun isLanguageAvailable(locale: Locale): Boolean
    fun stop()
    fun shutdown()

    fun setSpeechRate(rate: Float)
    fun setPitch(pitch: Float)
    fun setLanguage(locale: Locale): Boolean
    fun setVoice(voiceName: String?): Boolean
    fun getAvailableVoices(): List<VoiceInfo>
}

class GreetingTtsManagerImpl(
    private val context: Context,
    private val audioCoordinator: AudioInputCoordinator? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) : GreetingTtsManager, TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private val initMutex = Mutex()
    private var initDeferred: CompletableDeferred<Boolean>? = null

    private val _isInitialized = MutableStateFlow(false)
    override val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    override val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    // Track active speaking completion per utterance ID
    private val pendingUtterances = java.util.concurrent.ConcurrentHashMap<String, CompletableDeferred<Result<Unit>>>()

    private var speechRate: Float = 1.0f
    private var speechPitch: Float = 1.0f
    private var targetLocale: Locale = Locale.forLanguageTag("en-IN")
    private var targetVoiceName: String? = null

    private val utteranceProgressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            AppLogger.i(AppLogger.Tag.TTS, "[TTS_STARTED] Speech playback started for utteranceId: $utteranceId")
            if (utteranceId != null && pendingUtterances.containsKey(utteranceId)) {
                _isSpeaking.value = true
            }
        }

        override fun onDone(utteranceId: String?) {
            AppLogger.i(AppLogger.Tag.TTS, "[TTS_COMPLETED] Speech playback completed for utteranceId: $utteranceId")
            val def = if (utteranceId != null) pendingUtterances.remove(utteranceId) else null
            if (def != null) {
                _isSpeaking.value = false
                def.complete(Result.success(Unit))
            } else {
                AppLogger.d(AppLogger.Tag.TTS, "Ignored onDone for unmanaged or stale utteranceId: $utteranceId")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            onError(utteranceId, TextToSpeech.ERROR)
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            AppLogger.e(AppLogger.Tag.TTS, "TTS utterance error: $errorCode for utteranceId: $utteranceId")
            val def = if (utteranceId != null) pendingUtterances.remove(utteranceId) else null
            if (def != null) {
                _isSpeaking.value = false
                def.complete(
                    Result.failure(IllegalStateException("TTS playback failed with error code: $errorCode"))
                )
            } else {
                AppLogger.d(AppLogger.Tag.TTS, "Ignored onError for unmanaged or stale utteranceId: $utteranceId")
            }
        }
    }

    override suspend fun initialize(): Result<Unit> = initMutex.withLock {
        if (_isInitialized.value && tts != null) {
            return Result.success(Unit)
        }

        initDeferred = CompletableDeferred()
        withContext(Dispatchers.Main) {
            try {
                AppLogger.i(AppLogger.Tag.TTS, "Initializing TextToSpeech engine...")
                tts = TextToSpeech(context.applicationContext, this@GreetingTtsManagerImpl)
            } catch (e: Exception) {
                AppLogger.e(AppLogger.Tag.TTS, "Failed to instantiate TextToSpeech", e)
                initDeferred?.complete(false)
            }
        }

        val success = initDeferred?.await() ?: false
        return if (success) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalStateException("TextToSpeech initialization failed on device"))
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            AppLogger.i(AppLogger.Tag.TTS, "TextToSpeech engine initialized successfully")
            tts?.setOnUtteranceProgressListener(utteranceProgressListener)
            applySettingsInternal()
            _isInitialized.value = true
            initDeferred?.complete(true)
        } else {
            AppLogger.e(AppLogger.Tag.TTS, "TextToSpeech initialization returned error status: $status")
            _isInitialized.value = false
            initDeferred?.complete(false)
        }
    }

    private fun applySettingsInternal() {
        val engine = tts ?: return
        try {
            engine.setSpeechRate(speechRate)
            engine.setPitch(speechPitch)

            val langResult = engine.setLanguage(targetLocale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                AppLogger.w(AppLogger.Tag.TTS, "Language $targetLocale not supported, falling back to US English")
                engine.setLanguage(Locale.US)
            }

            if (targetVoiceName != null) {
                val matchingVoice = engine.voices?.firstOrNull { it.name == targetVoiceName }
                if (matchingVoice != null) {
                    engine.voice = matchingVoice
                }
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.TTS, "Error applying initial TTS settings", e)
        }
    }

    override fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(speechRate)
    }

    override fun setPitch(pitch: Float) {
        speechPitch = pitch.coerceIn(0.5f, 2.0f)
        tts?.setPitch(speechPitch)
    }

    override fun setLanguage(locale: Locale): Boolean {
        targetLocale = locale
        val engine = tts ?: return true
        val result = engine.setLanguage(locale)
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
    }

    override fun setVoice(voiceName: String?): Boolean {
        targetVoiceName = voiceName
        val engine = tts ?: return true
        if (voiceName == null) return true
        val voice = engine.voices?.firstOrNull { it.name == voiceName } ?: return false
        val result = engine.setVoice(voice)
        return result == TextToSpeech.SUCCESS
    }

    override fun getAvailableVoices(): List<VoiceInfo> {
        val engine = tts ?: return emptyList()
        return try {
            engine.voices?.map { voice ->
                VoiceInfo(
                    name = voice.name,
                    locale = voice.locale,
                    isNetworkConnectionRequired = voice.isNetworkConnectionRequired
                )
            } ?: emptyList()
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tag.TTS, "Could not query available voices", e)
            emptyList()
        }
    }

    override fun isLanguageAvailable(locale: Locale): Boolean {
        val engine = tts ?: return true
        val result = engine.isLanguageAvailable(locale)
        return result >= TextToSpeech.LANG_AVAILABLE
    }

    override suspend fun speak(text: String, locale: Locale?): Result<Unit> {
        if (text.isBlank()) {
            return Result.success(Unit)
        }

        if (!_isInitialized.value) {
            val initResult = initialize()
            if (initResult.isFailure) {
                return initResult
            }
        }

        val engine = tts ?: return Result.failure(IllegalStateException("TTS not available"))

        if (locale != null) {
            val available = engine.isLanguageAvailable(locale)
            if (available >= TextToSpeech.LANG_AVAILABLE) {
                engine.setLanguage(locale)
            } else {
                AppLogger.w(AppLogger.Tag.TTS, "TTS Locale $locale not available (code $available), falling back to US English")
                engine.setLanguage(Locale.US)
            }
        }

        // Stop any current utterance
        stop()
        audioCoordinator?.releaseAll()

        val utteranceId = "drivemate_greeting_${UUID.randomUUID()}"
        val deferred = CompletableDeferred<Result<Unit>>()
        pendingUtterances[utteranceId] = deferred

        // Request audio focus so media ducks in car
        val focusGranted = audioCoordinator?.onTtsStarted() ?: true
        if (!focusGranted) {
            pendingUtterances.remove(utteranceId)
            AppLogger.w(AppLogger.Tag.AUDIO_FOCUS, "[AUDIO_FOCUS_DENIED] Audio focus was not granted. Aborting TTS playback.")
            return Result.failure(IllegalStateException("Audio focus request denied"))
        }

        // Apply modern automotive navigation guidance audio attributes
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        engine.setAudioAttributes(audioAttributes)

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        val queueResult = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (queueResult != TextToSpeech.SUCCESS) {
            pendingUtterances.remove(utteranceId)
            audioCoordinator?.onTtsCompleted()
            _isSpeaking.value = false
            AppLogger.e(AppLogger.Tag.TTS, "[TTS_CANCELLED] Failed to queue TTS utterance: $queueResult")
            return Result.failure(IllegalStateException("Failed to queue TTS speech: $queueResult"))
        }

        return try {
            withTimeout(15000L) {
                deferred.await()
            }
        } catch (e: TimeoutCancellationException) {
            AppLogger.e(AppLogger.Tag.TTS, "[TTS_CANCELLED] TTS utterance $utteranceId timed out after 15s")
            stop()
            Result.failure(e)
        } catch (e: CancellationException) {
            AppLogger.w(AppLogger.Tag.TTS, "[TTS_CANCELLED] TTS utterance $utteranceId was cancelled")
            stop()
            throw e
        } finally {
            pendingUtterances.remove(utteranceId)
            audioCoordinator?.onTtsCompleted()
        }
    }

    override fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tag.TTS, "Error stopping TTS", e)
        }
        _isSpeaking.value = false
        audioCoordinator?.abandonAudioFocus()
        val remaining = pendingUtterances.values.toList()
        pendingUtterances.clear()
        remaining.forEach {
            it.complete(Result.failure(CancellationException("Playback stopped")))
        }
    }

    override fun shutdown() {
        stop()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tag.TTS, "Error shutting down TTS", e)
        }
        tts = null
        _isInitialized.value = false
    }

}
