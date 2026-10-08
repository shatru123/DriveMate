package com.shatrughna.drivemate

import com.shatrughna.drivemate.data.analytics.DrivingAnalyticsEngine
import com.shatrughna.drivemate.data.model.TripReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DrivingAnalyticsEngineV5Test {

    @Test
    fun testEmptyTripsReturnsCleanZeroSummary() {
        val summary = DrivingAnalyticsEngine.computeMonthlySummary(emptyList())

        assertEquals(0, summary.totalTrips)
        assertEquals(0f, summary.totalDistanceKm, 0.001f)
        assertEquals(0L, summary.totalDurationMinutes)
        assertNull(summary.avgSpeedKmh)
        assertNull(summary.avgEcoScore)
        assertNull(summary.estimatedFuelConsumedLiters)
        assertTrue(summary.weeklyMetrics.isEmpty())
    }

    @Test
    fun testDistanceWeightedAverageSpeed() {
        val now = System.currentTimeMillis()
        val trip1 = TripReport(
            id = "t1",
            startTimeMillis = now - 3600_000,
            endTimeMillis = now,
            distanceKm = 10f,
            avgSpeedKmh = 100f,
            maxSpeedKmh = 110f,
            ecoScore = 90
        )
        val trip2 = TripReport(
            id = "t2",
            startTimeMillis = now - 7200_000,
            endTimeMillis = now - 3600_000,
            distanceKm = 90f,
            avgSpeedKmh = 50f,
            maxSpeedKmh = 70f,
            ecoScore = 80
        )

        val summary = DrivingAnalyticsEngine.computeMonthlySummary(listOf(trip1, trip2))

        assertEquals(2, summary.totalTrips)
        assertEquals(100f, summary.totalDistanceKm, 0.001f)
        // Distance weighted: (10 * 100 + 90 * 50) / 100 = 55.0 km/h
        assertEquals(55f, summary.avgSpeedKmh!!, 0.01f)
    }

    @Test
    fun testWeeklyMetricsGrouping() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 3)
        val earlyMonthTime = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, 18)
        val midMonthTime = cal.timeInMillis

        val trip1 = TripReport(
            id = "t1",
            startTimeMillis = earlyMonthTime,
            endTimeMillis = earlyMonthTime + 1800_000,
            distanceKm = 25f,
            avgSpeedKmh = 45f,
            maxSpeedKmh = 60f,
            ecoScore = 85
        )

        val trip2 = TripReport(
            id = "t2",
            startTimeMillis = midMonthTime,
            endTimeMillis = midMonthTime + 1800_000,
            distanceKm = 40f,
            avgSpeedKmh = 55f,
            maxSpeedKmh = 75f,
            ecoScore = 92
        )

        val summary = DrivingAnalyticsEngine.computeMonthlySummary(listOf(trip1, trip2))

        assertEquals(2, summary.totalTrips)
        assertEquals(65f, summary.totalDistanceKm, 0.001f)
        assertTrue(summary.weeklyMetrics.isNotEmpty())
    }
}
