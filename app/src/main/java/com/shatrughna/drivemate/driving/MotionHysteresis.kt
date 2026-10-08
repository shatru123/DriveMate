package com.shatrughna.drivemate.driving

enum class MotionState {
    MOVING,
    STOPPED,
    UNKNOWN
}

/** Debounces noisy speed samples before changing the driving-session state. */
class MotionHysteresis(
    private val startSpeedKmh: Float = 5f,
    private val stopSpeedKmh: Float = 2f,
    private val movingSamplesRequired: Int = 2,
    private val stoppedSamplesRequired: Int = 3
) {
    private var moving = false
    private var movingSamples = 0
    private var stoppedSamples = 0

    fun update(speedKmh: Float?): MotionState {
        val speed = speedKmh?.takeIf { it.isFinite() && it >= 0f } ?: run {
            movingSamples = 0
            stoppedSamples = 0
            return MotionState.UNKNOWN
        }

        if (!moving) {
            stoppedSamples = 0
            if (speed >= startSpeedKmh) {
                movingSamples += 1
                if (movingSamples >= movingSamplesRequired) {
                    moving = true
                    movingSamples = 0
                    return MotionState.MOVING
                }
            } else {
                movingSamples = 0
            }
            return MotionState.UNKNOWN
        }

        movingSamples = 0
        if (speed <= stopSpeedKmh) {
            stoppedSamples += 1
            if (stoppedSamples >= stoppedSamplesRequired) {
                moving = false
                stoppedSamples = 0
                return MotionState.STOPPED
            }
        } else {
            stoppedSamples = 0
        }
        return MotionState.MOVING
    }

    fun reset() {
        moving = false
        movingSamples = 0
        stoppedSamples = 0
    }
}
