package com.shatrughna.drivemate

import com.shatrughna.drivemate.core.telemetry.TelemetryArbitration
import com.shatrughna.drivemate.core.telemetry.TelemetryAvailability
import com.shatrughna.drivemate.core.telemetry.TelemetrySource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryArbitrationTest {
    private val now = 100_000L

    @Test
    fun liveAndroidAutoWinsOverComparableLiveGps() {
        assertFalse(
            TelemetryArbitration.shouldPreferIncoming(
                incomingAvailability = TelemetryAvailability.LIVE,
                incomingTimestampMillis = now,
                incomingSource = TelemetrySource.PHONE_GPS,
                incomingHasValue = true,
                currentAvailability = TelemetryAvailability.LIVE,
                currentTimestampMillis = now,
                currentSource = TelemetrySource.ANDROID_AUTO_CAR_HARDWARE,
                currentHasValue = true,
                nowMillis = now
            )
        )
    }

    @Test
    fun liveGpsWinsOverStaleAndroidAuto() {
        assertTrue(
            TelemetryArbitration.shouldPreferIncoming(
                incomingAvailability = TelemetryAvailability.LIVE,
                incomingTimestampMillis = now,
                incomingSource = TelemetrySource.PHONE_GPS,
                incomingHasValue = true,
                currentAvailability = TelemetryAvailability.STALE,
                currentTimestampMillis = now - 500L,
                currentSource = TelemetrySource.ANDROID_AUTO_CAR_HARDWARE,
                currentHasValue = true,
                nowMillis = now
            )
        )
    }

    @Test
    fun liveGpsWinsWhenAndroidAutoIsUnavailable() {
        assertTrue(
            TelemetryArbitration.shouldPreferIncoming(
                incomingAvailability = TelemetryAvailability.LIVE,
                incomingTimestampMillis = now,
                incomingSource = TelemetrySource.PHONE_GPS,
                incomingHasValue = true,
                currentAvailability = TelemetryAvailability.UNAVAILABLE,
                currentTimestampMillis = null,
                currentSource = TelemetrySource.ANDROID_AUTO_CAR_HARDWARE,
                currentHasValue = false,
                nowMillis = now
            )
        )
    }

    @Test
    fun freshLowerPrioritySourceCanBeatOldLiveHigherPrioritySource() {
        assertTrue(
            TelemetryArbitration.shouldPreferIncoming(
                incomingAvailability = TelemetryAvailability.LIVE,
                incomingTimestampMillis = now,
                incomingSource = TelemetrySource.PHONE_GPS,
                incomingHasValue = true,
                currentAvailability = TelemetryAvailability.LIVE,
                currentTimestampMillis = now - 5_000L,
                currentSource = TelemetrySource.ANDROID_AUTO_CAR_HARDWARE,
                currentHasValue = true,
                nowMillis = now
            )
        )
    }

    @Test
    fun staleValuesExpireAfterTheConfiguredWindow() {
        assertEquals(
            TelemetryAvailability.STALE,
            TelemetryArbitration.expireAvailability(
                availability = TelemetryAvailability.LIVE,
                timestampMillis = now - 2_500L,
                nowMillis = now,
                staleAfterMillis = 2_000L
            )
        )
        assertEquals(
            TelemetryAvailability.UNAVAILABLE,
            TelemetryArbitration.expireAvailability(
                availability = TelemetryAvailability.LIVE,
                timestampMillis = now - 7_000L,
                nowMillis = now,
                staleAfterMillis = 2_000L
            )
        )
    }
}
