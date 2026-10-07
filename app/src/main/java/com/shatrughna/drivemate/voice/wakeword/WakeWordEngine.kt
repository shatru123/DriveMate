package com.shatrughna.drivemate.voice.wakeword

/**
 * Listener callback for wake-word detection events.
 */
interface WakeWordListener {
    /**
     * Triggered when the deliberate wake phrase is detected.
     * @param phrase The exact detected hotword.
     * @param trailingCommand Optional command spoken in the same breath (e.g. "navigate to office").
     */
    fun onWakeWordDetected(phrase: String, trailingCommand: String?)

    fun onError(error: String)
}

/**
 * Pluggable contract for wake-word engines.
 * Decouples DriveMate from underlying hotword implementations (native speech recognizer,
 * Porcupine, Snowboy, or on-device ML models).
 */
interface WakeWordEngine {
    val isRunning: Boolean
    fun start()
    fun stop()
    fun setListener(listener: WakeWordListener?)
}
