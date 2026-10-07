package com.shatrughna.drivemate.voice.wakeword

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.shatrughna.drivemate.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Implementation of WakeWordEngine using Android's native SpeechRecognizer.
 * Strictly checks for deliberate wake phrases ("Hey DriveMate") and rejects false positives.
 * Automatically extracts trailing commands if spoken in one breath.
 */
class SpeechRecognizerWakeWordEngine(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) : WakeWordEngine {

    companion object {
        // Regex strictly matching "Hey DriveMate", "Hey Drive Mate", "OK DriveMate"
        private val WAKE_PHRASE_REGEX = Regex("^(hey|ok)\\s+drive\\s*mate[.,!?:;]*\\s*", RegexOption.IGNORE_CASE)
        // High sensitivity direct fallback
        private val STANDALONE_DRIVEMATE_REGEX = Regex("^drive\\s*mate[.,!?:;]*\\s*", RegexOption.IGNORE_CASE)

        /**
         * Checks whether raw speech contains the deliberate wake phrase.
         * Returns Pair(isWakeWord, trailingCommand).
         */
        fun parseWakePhrase(input: String, allowStandalone: Boolean = false): Pair<Boolean, String?> {
            val clean = input.trim().lowercase(Locale.getDefault())
            if (clean.isBlank()) return Pair(false, null)

            // Strict rejection of common false positives
            if (clean == "hey" || clean == "drive" || clean == "mate" ||
                clean == "hey drive" || clean == "hey driver" || clean.startsWith("hey driver ")
            ) {
                return Pair(false, null)
            }

            // Check primary "Hey DriveMate" / "OK DriveMate"
            val match = WAKE_PHRASE_REGEX.find(clean)
            if (match != null) {
                val trailing = clean.substring(match.range.last + 1).trim()
                return Pair(true, trailing.ifBlank { null })
            }

            // Check optional standalone "DriveMate" if enabled
            if (allowStandalone) {
                val standaloneMatch = STANDALONE_DRIVEMATE_REGEX.find(clean)
                if (standaloneMatch != null) {
                    val trailing = clean.substring(standaloneMatch.range.last + 1).trim()
                    return Pair(true, trailing.ifBlank { null })
                }
            }

            return Pair(false, null)
        }
    }

    private var listener: WakeWordListener? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var restartJob: Job? = null
    private var _isRunning = false
    override val isRunning: Boolean get() = _isRunning

    override fun setListener(listener: WakeWordListener?) {
        this.listener = listener
    }

    override fun start() {
        if (_isRunning) return
        val micPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!micPermission) {
            AppLogger.w(AppLogger.Tag.APP, "WakeWordEngine: Microphone permission not granted. Cannot start.")
            listener?.onError("Microphone permission required")
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            AppLogger.w(AppLogger.Tag.APP, "WakeWordEngine: Speech recognition not available on device.")
            listener?.onError("Speech recognition not available")
            return
        }

        _isRunning = true
        AppLogger.i(AppLogger.Tag.APP, "WakeWordEngine: Started listening for \"Hey DriveMate\" hotword.")
        scheduleNextListeningCycle(delayMillis = 0L)
    }

    override fun stop() {
        if (!_isRunning) return
        _isRunning = false
        AppLogger.i(AppLogger.Tag.APP, "WakeWordEngine: Stopped wake word engine. Releasing microphone.")

        restartJob?.cancel()
        restartJob = null

        val recognizer = speechRecognizer
        speechRecognizer = null
        if (recognizer != null) {
            try {
                recognizer.stopListening()
                recognizer.cancel()
                recognizer.destroy()
            } catch (e: Exception) {
                AppLogger.w(AppLogger.Tag.APP, "WakeWordEngine: Error destroying recognizer: ${e.message}")
            }
        }
    }

    private fun scheduleNextListeningCycle(delayMillis: Long) {
        if (!_isRunning) return
        restartJob?.cancel()
        restartJob = scope.launch {
            if (delayMillis > 0) {
                delay(delayMillis)
            }
            if (_isRunning) {
                startListeningCycle()
            }
        }
    }

    private fun startListeningCycle() {
        if (!_isRunning) return

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createCycleListener())
                }
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.APP, "WakeWordEngine: Failed to start listening cycle: ${e.message}", e)
            scheduleNextListeningCycle(delayMillis = 1500L)
        }
    }

    private fun createCycleListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                // If recognizer timed out or heard no speech, cycle quietly
                if (_isRunning) {
                    val backoff = when (error) {
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 1000L
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            _isRunning = false
                            listener?.onError("Permissions lost")
                            return
                        }
                        else -> 300L
                    }
                    scheduleNextListeningCycle(backoff)
                }
            }

            override fun onResults(results: Bundle?) {
                if (!_isRunning) return

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: emptyList()
                var detected = false

                for (text in matches) {
                    val (isWake, trailing) = parseWakePhrase(text)
                    if (isWake) {
                        AppLogger.i(AppLogger.Tag.APP, "WakeWordEngine: Detected wake phrase in: \"$text\" (trailing: $trailing)")
                        detected = true
                        listener?.onWakeWordDetected("Hey DriveMate", trailing)
                        break
                    }
                }

                if (!detected && _isRunning) {
                    scheduleNextListeningCycle(delayMillis = 200L)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (!_isRunning) return

                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: emptyList()
                for (text in matches) {
                    val (isWake, trailing) = parseWakePhrase(text)
                    if (isWake) {
                        AppLogger.i(AppLogger.Tag.APP, "WakeWordEngine: Detected wake phrase via partial results: \"$text\"")
                        // Stop current listening to hand off control
                        try {
                            speechRecognizer?.stopListening()
                        } catch (_: Exception) {}
                        listener?.onWakeWordDetected("Hey DriveMate", trailing)
                        break
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}
