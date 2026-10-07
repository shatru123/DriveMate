package com.shatrughna.drivemate.voice.wakeword

import com.shatrughna.drivemate.util.AppLogger

/**
 * WakeWordEngine implementation for dedicated vehicle hardware or DSP hotword detectors.
 * When not supported by the vehicle head unit, operates in inactive state.
 */
class HardwareWakeWordEngine : WakeWordEngine {

    override val isRunning: Boolean = false

    override fun start() {
        AppLogger.d(AppLogger.Tag.CAR_HARDWARE, "HardwareWakeWordEngine: Vehicle DSP hotword detector unavailable.")
    }

    override fun stop() {
        // No-op
    }

    override fun setListener(listener: WakeWordListener?) {
        // No-op
    }
}
