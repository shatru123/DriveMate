package com.shatrughna.drivemate.core.telemetry

/**
 * Chooses between telemetry channels without allowing a stale authoritative
 * source to hide a fresh fallback. Availability and freshness win first;
 * source priority is only used when the values are comparably fresh.
 */
object TelemetryArbitration {
    private const val COMPARABLE_FRESHNESS_WINDOW_MILLIS = 2_500L

    fun shouldPreferIncoming(
        incomingAvailability: TelemetryAvailability,
        incomingTimestampMillis: Long?,
        incomingSource: TelemetrySource,
        incomingHasValue: Boolean,
        currentAvailability: TelemetryAvailability,
        currentTimestampMillis: Long?,
        currentSource: TelemetrySource,
        currentHasValue: Boolean,
        nowMillis: Long
    ): Boolean {
        // A source without a value must never replace a usable value from another source.
        if (!incomingHasValue && currentHasValue) return incomingSource == currentSource
        if (!currentHasValue && incomingHasValue) return true

        val incomingAvailabilityRank = availabilityRank(incomingAvailability)
        val currentAvailabilityRank = availabilityRank(currentAvailability)
        if (incomingAvailabilityRank != currentAvailabilityRank) {
            return incomingAvailabilityRank > currentAvailabilityRank
        }

        val incomingAge = freshnessAge(incomingTimestampMillis, nowMillis)
        val currentAge = freshnessAge(currentTimestampMillis, nowMillis)
        val ageDelta = currentAge - incomingAge
        if (ageDelta > COMPARABLE_FRESHNESS_WINDOW_MILLIS) return true
        if (ageDelta < -COMPARABLE_FRESHNESS_WINDOW_MILLIS) return false

        return sourceRank(incomingSource) < sourceRank(currentSource)
    }

    fun expireAvailability(
        availability: TelemetryAvailability,
        timestampMillis: Long?,
        nowMillis: Long,
        staleAfterMillis: Long
    ): TelemetryAvailability {
        if (timestampMillis == null) return availability
        val age = (nowMillis - timestampMillis).coerceAtLeast(0L)
        return when {
            age <= staleAfterMillis -> availability
            age <= staleAfterMillis * 3 -> {
                if (availability == TelemetryAvailability.LIVE) TelemetryAvailability.STALE else availability
            }
            else -> TelemetryAvailability.UNAVAILABLE
        }
    }

    private fun freshnessAge(timestampMillis: Long?, nowMillis: Long): Long =
        timestampMillis?.let { (nowMillis - it).coerceAtLeast(0L) } ?: Long.MAX_VALUE

    private fun availabilityRank(availability: TelemetryAvailability): Int = when (availability) {
        TelemetryAvailability.LIVE -> 3
        TelemetryAvailability.STALE -> 2
        TelemetryAvailability.UNAVAILABLE,
        TelemetryAvailability.NOT_CONNECTED,
        TelemetryAvailability.NOT_AUTHORIZED,
        TelemetryAvailability.NOT_SUPPORTED,
        TelemetryAvailability.COMING_SOON -> 1
    }

    private fun sourceRank(source: TelemetrySource): Int = when (source) {
        TelemetrySource.ANDROID_AUTO_CAR_HARDWARE -> 0
        TelemetrySource.OBD2_BLE, TelemetrySource.OBD2_WIFI -> 1
        TelemetrySource.PHONE_GPS -> 2
        TelemetrySource.MANUAL -> 3
        TelemetrySource.NONE -> 4
    }
}
