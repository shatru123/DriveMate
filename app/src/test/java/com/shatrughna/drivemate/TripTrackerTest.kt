package com.shatrughna.drivemate

import com.shatrughna.drivemate.data.model.TripStats
import org.junit.Assert.assertEquals
import org.junit.Test

class TripTrackerTest {

    @Test
    fun testTripStatsFormatting() {
        val stats = TripStats(
            activeTripDurationSeconds = 125L, // 2m 5s
            activeTripDistanceKm = 1.35f,
            todayTripsCount = 3,
            todayTotalDistanceKm = 24.8f,
            todayTotalDurationMinutes = 45L
        )

        assertEquals("02:05", stats.formattedActiveDuration)
        assertEquals("1.4 km", stats.formattedActiveDistance)
        assertEquals("24.8 km", stats.formattedTodayDistance)
        assertEquals(3, stats.todayTripsCount)
        assertEquals(45L, stats.todayTotalDurationMinutes)
    }

    @Test
    fun testZeroTripStatsDefaults() {
        val zeroStats = TripStats()
        assertEquals("00:00", zeroStats.formattedActiveDuration)
        assertEquals("0.0 km", zeroStats.formattedActiveDistance)
        assertEquals("0.0 km", zeroStats.formattedTodayDistance)
        assertEquals(0, zeroStats.todayTripsCount)
    }
}
