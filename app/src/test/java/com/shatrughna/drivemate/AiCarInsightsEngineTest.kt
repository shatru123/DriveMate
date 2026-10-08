package com.shatrughna.drivemate

import com.shatrughna.drivemate.core.insights.AiCarInsightsEngine
import com.shatrughna.drivemate.core.insights.InsightCategory
import com.shatrughna.drivemate.core.insights.InsightPriority
import com.shatrughna.drivemate.data.analytics.MonthlyDrivingSummary
import com.shatrughna.drivemate.data.model.DocumentType
import com.shatrughna.drivemate.data.model.ExpenseCategory
import com.shatrughna.drivemate.data.model.ExpenseSummary
import com.shatrughna.drivemate.data.model.ServiceSchedule
import com.shatrughna.drivemate.data.model.VehicleDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class AiCarInsightsEngineTest {

    @Test
    fun testGenerateInsightsForExpiringDocumentAndDueService() {
        val now = System.currentTimeMillis()
        val docs = listOf(
            VehicleDocument(
                title = "PUC Certificate",
                type = DocumentType.PUC,
                documentNumber = "MH28-PUC-123",
                expiryDateMillis = now + TimeUnit.DAYS.toMillis(8) // Expiring soon (<10d -> HIGH priority)
            )
        )

        val schedule = ServiceSchedule(
            intervalKm = 15000,
            intervalMonths = 12,
            lastServiceOdometerKm = 15000.0,
            lastServiceDateMillis = now - TimeUnit.DAYS.toMillis(180)
        )

        val expenseSummary = ExpenseSummary(
            totalSpent = 12500.0,
            currentMonthSpent = 4500.0,
            categoryTotals = mapOf(ExpenseCategory.FUEL to 4500.0),
            costPerKm = 6.2
        )

        val drivingSummary = MonthlyDrivingSummary(
            monthName = "October 2026",
            totalDistanceKm = 520f,
            totalTrips = 20,
            totalDurationMinutes = 680L,
            avgSpeedKmh = 48f,
            estimatedFuelConsumedLiters = 32f,
            avgEcoScore = 92,
            weeklyMetrics = emptyList()
        )

        val currentOdo = 29200.0 // 800 km left -> Service DUE_NOW (HIGH priority)
        val insights = AiCarInsightsEngine.generateInsights(
            currentOdometerKm = currentOdo,
            documents = docs,
            serviceSchedule = schedule,
            expenseSummary = expenseSummary,
            drivingSummary = drivingSummary
        )

        assertTrue(insights.isNotEmpty())
        val topInsight = insights.first()
        assertEquals(InsightPriority.HIGH, topInsight.priority)

        val docInsight = insights.find { it.category == InsightCategory.SAFETY_COMPLIANCE }
        assertNotNull(docInsight)
        assertTrue(docInsight!!.title.contains("Expiring Soon"))

        val serviceInsight = insights.find { it.category == InsightCategory.MAINTENANCE }
        assertNotNull(serviceInsight)
        assertTrue(serviceInsight!!.message.contains("service", ignoreCase = true) || serviceInsight.message.contains("due", ignoreCase = true))
    }
}
