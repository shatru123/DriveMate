package com.shatrughna.drivemate

import com.shatrughna.drivemate.driving.MotionHysteresis
import com.shatrughna.drivemate.driving.MotionState
import org.junit.Assert.assertEquals
import org.junit.Test

class MotionHysteresisTest {
    @Test
    fun requiresConsecutiveMovingSamples() {
        val motion = MotionHysteresis()

        assertEquals(MotionState.UNKNOWN, motion.update(5.5f))
        assertEquals(MotionState.MOVING, motion.update(6.0f))
    }

    @Test
    fun requiresConsecutiveStoppedSamplesAndUsesLowerStopThreshold() {
        val motion = MotionHysteresis()
        motion.update(8f)
        motion.update(8f)

        assertEquals(MotionState.MOVING, motion.update(1.5f))
        assertEquals(MotionState.MOVING, motion.update(1.5f))
        assertEquals(MotionState.STOPPED, motion.update(1.5f))
    }

    @Test
    fun invalidSamplesDoNotCreateAStoppedTransition() {
        val motion = MotionHysteresis()
        motion.update(8f)
        motion.update(8f)

        assertEquals(MotionState.UNKNOWN, motion.update(null))
        assertEquals(MotionState.MOVING, motion.update(8f))
    }
}
