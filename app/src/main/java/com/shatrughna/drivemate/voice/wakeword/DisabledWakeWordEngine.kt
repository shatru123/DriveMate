package com.shatrughna.drivemate.voice.wakeword

import com.shatrughna.drivemate.util.AppLogger

/**
 * Passive / Disabled implementation of WakeWordEngine.
 * Used by default when connected to Android Auto to prevent continuous
 * microphone contention and audio focus hijacking that disrupts music playback (e.g. Spotify).
 */
class DisabledWakeWordEngine : WakeWordEngine {

    override val isRunning: Boolean = false

    override fun start() {
        AppLogger.d(AppLogger.Tag.AUDIO, "DisabledWakeWordEngine: Passive mode active. Continuous listening disabled for car audio stability.")
    }

    override fun stop() {
        // No-op
    }

    override fun setListener(listener: WakeWordListener?) {
        // No-op
    }
}
