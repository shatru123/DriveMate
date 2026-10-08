package com.shatrughna.drivemate.core.insights

import com.shatrughna.drivemate.data.analytics.MonthlyDrivingSummary
import com.shatrughna.drivemate.data.model.DocumentExpiryStatus
import com.shatrughna.drivemate.data.model.ExpenseSummary
import com.shatrughna.drivemate.data.model.ServiceSchedule
import com.shatrughna.drivemate.data.model.ServiceUrgency
import com.shatrughna.drivemate.data.model.VehicleDocument
import java.util.Locale

enum class InsightPriority {
    HIGH,
    MEDIUM,
    LOW
}

enum class InsightCategory {
    SAFETY_COMPLIANCE,
    MAINTENANCE,
    EXPENSE_EFFICIENCY,
    DRIVING_HABITS
}

data class AiCarInsight(
    val id: String,
    val title: String,
    val message: String,
    val category: InsightCategory,
    val priority: InsightPriority,
    val targetScreen: String? = null
)

object AiCarInsightsEngine {

    fun generateInsights(
        currentOdometerKm: Double,
        documents: List<VehicleDocument>,
        serviceSchedule: ServiceSchedule,
        expenseSummary: ExpenseSummary,
        drivingSummary: MonthlyDrivingSummary
    ): List<AiCarInsight> {
        val insights = mutableListOf<AiCarInsight>()

        // 1. Check Document Expiries
        val expiringDocs = documents.filter { it.status() == DocumentExpiryStatus.EXPIRING_SOON }
        val expiredDocs = documents.filter { it.status() == DocumentExpiryStatus.EXPIRED }

        for (doc in expiredDocs) {
            insights.add(
                AiCarInsight(
                    id = "insight_doc_expired_${doc.id}",
                    title = "${doc.title} Expired",
                    message = "${doc.title} (${doc.documentNumber}) has expired. Renew immediately to avoid legal penalties.",
                    category = InsightCategory.SAFETY_COMPLIANCE,
                    priority = InsightPriority.HIGH,
                    targetScreen = "DocumentVault"
                )
            )
        }

        for (doc in expiringDocs) {
            val days = doc.daysUntilExpiry() ?: 30
            insights.add(
                AiCarInsight(
                    id = "insight_doc_expiring_${doc.id}",
                    title = "${doc.title} Expiring Soon",
                    message = "${doc.title} expires in $days days (${doc.documentNumber}). Schedule renewal soon.",
                    category = InsightCategory.SAFETY_COMPLIANCE,
                    priority = if (days <= 10) InsightPriority.HIGH else InsightPriority.MEDIUM,
                    targetScreen = "DocumentVault"
                )
            )
        }

        // 2. Check Service Urgency
        val serviceUrgency = if (serviceSchedule.isConfigured) serviceSchedule.urgency(currentOdometerKm) else null
        val kmLeft = if (serviceSchedule.isConfigured) serviceSchedule.kmRemaining(currentOdometerKm) else null
        val daysLeft = if (serviceSchedule.isConfigured) serviceSchedule.daysRemaining() else null

        when (serviceUrgency) {
            ServiceUrgency.OVERDUE -> {
                insights.add(
                    AiCarInsight(
                        id = "insight_service_overdue",
                        title = "Service Overdue",
                        message = "Your scheduled service at ${serviceSchedule.nextServiceOdometerKm.toInt()} km is overdue. Visit your service provider.",
                        category = InsightCategory.MAINTENANCE,
                        priority = InsightPriority.HIGH,
                        targetScreen = "Maintenance"
                    )
                )
            }
            ServiceUrgency.DUE_NOW -> {
                insights.add(
                    AiCarInsight(
                        id = "insight_service_due_now",
                        title = "Periodic Service Due Soon",
                        message = "Next service due in ${kmLeft!!.toInt()} km or $daysLeft days.",
                        category = InsightCategory.MAINTENANCE,
                        priority = InsightPriority.HIGH,
                        targetScreen = "Maintenance"
                    )
                )
            }
            ServiceUrgency.DUE_SOON -> {
                insights.add(
                    AiCarInsight(
                        id = "insight_service_due_soon",
                        title = "Service Approaching",
                        message = "${kmLeft!!.toInt()} km remaining until the next service.",
                        category = InsightCategory.MAINTENANCE,
                        priority = InsightPriority.MEDIUM,
                        targetScreen = "Maintenance"
                    )
                )
            }
            ServiceUrgency.ON_SCHEDULE -> {
                // Good health
            }
            null -> {
                // No service data is available.
            }
        }

        // 3. Expense & Cost-Per-Km Insights
        if (expenseSummary.costPerKm > 0) {
            insights.add(
                AiCarInsight(
                    id = "insight_running_cost",
                    title = "Running Cost Analysis",
                    message = String.format(
                        Locale.getDefault(),
                        "Average vehicle running cost is ₹%.2f per km. This month's total spend is ₹%.0f.",
                        expenseSummary.costPerKm,
                        expenseSummary.currentMonthSpent
                    ),
                    category = InsightCategory.EXPENSE_EFFICIENCY,
                    priority = InsightPriority.LOW,
                    targetScreen = "Expenses"
                )
            )
        }

        // 4. Driving Habit & Eco Score Insights
        if (drivingSummary.totalTrips > 0 && drivingSummary.avgEcoScore?.let { it >= 85 } == true) {
            insights.add(
                AiCarInsight(
                    id = "insight_eco_driving",
                    title = "Efficient Driving Pattern",
                    message = "Excellent throttle control and smooth braking! Average eco rating is ${drivingSummary.avgEcoScore}/100 across ${drivingSummary.totalTrips} drives.",
                    category = InsightCategory.DRIVING_HABITS,
                    priority = InsightPriority.LOW,
                    targetScreen = "Analytics"
                )
            )
        }

        return insights.sortedBy { it.priority }
    }
}
