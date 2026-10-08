package com.shatrughna.drivemate.data.timeline

import com.shatrughna.drivemate.data.model.DocumentExpiryStatus
import com.shatrughna.drivemate.data.model.ExpenseCategory
import com.shatrughna.drivemate.data.repository.DocumentVaultRepository
import com.shatrughna.drivemate.data.repository.ExpenseRepository
import com.shatrughna.drivemate.data.repository.MaintenanceRepository
import com.shatrughna.drivemate.driving.TripHistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

interface VehicleTimelineRepository {
    val timelineItems: StateFlow<List<VehicleTimelineItem>>
}

class VehicleTimelineRepositoryImpl(
    tripHistoryRepository: TripHistoryRepository,
    expenseRepository: ExpenseRepository,
    maintenanceRepository: MaintenanceRepository,
    documentVaultRepository: DocumentVaultRepository,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : VehicleTimelineRepository {

    private val _timelineItems = MutableStateFlow<List<VehicleTimelineItem>>(emptyList())
    override val timelineItems: StateFlow<List<VehicleTimelineItem>> = _timelineItems.asStateFlow()

    init {
        scope.launch {
            combine(
                tripHistoryRepository.recentTrips,
                expenseRepository.expenses,
                maintenanceRepository.serviceRecords,
                documentVaultRepository.documents
            ) { trips, expenses, services, docs ->
                val list = mutableListOf<VehicleTimelineItem>()

                // 1. Add Trips
                for (trip in trips) {
                    list.add(
                        VehicleTimelineItem(
                            id = "timeline_trip_${trip.id}",
                            category = TimelineCategory.DRIVE,
                            title = "Drive: ${trip.startLocationName} → ${trip.endLocationName}",
                            subtitle = "${trip.formattedDuration} • Avg ${trip.formattedAvgSpeed}",
                            timestampMillis = trip.startTimeMillis,
                            metricText = String.format("+%.1f km", trip.distanceKm),
                            details = trip.ecoScore?.let { "Eco Score: $it/100" } ?: "Eco score unavailable"
                        )
                    )
                }

                // 2. Add Expenses
                for (exp in expenses) {
                    val isFuel = exp.category == ExpenseCategory.FUEL
                    list.add(
                        VehicleTimelineItem(
                            id = "timeline_exp_${exp.id}",
                            category = if (isFuel) TimelineCategory.FUEL else TimelineCategory.EXPENSE,
                            title = if (isFuel) "Fuel Fill-up" else exp.category.displayName,
                            subtitle = exp.location.ifBlank { "Vehicle expense recorded" },
                            timestampMillis = exp.dateMillis,
                            metricText = "₹${exp.amount.toInt()}",
                            details = if (isFuel && exp.fuelLiters != null) "${exp.fuelLiters} Liters" else exp.notes
                        )
                    )
                }

                // 3. Add Service records
                for (rec in services) {
                    list.add(
                        VehicleTimelineItem(
                            id = "timeline_service_${rec.id}",
                            category = TimelineCategory.MAINTENANCE,
                            title = rec.title,
                            subtitle = "${rec.odometerKm.toInt()} km • ${rec.workshopName}",
                            timestampMillis = rec.dateMillis,
                            metricText = if (rec.cost > 0) "₹${rec.cost.toInt()}" else "Free",
                            details = rec.notes
                        )
                    )
                }

                // 4. Add Document expiry reminders
                for (doc in docs) {
                    val status = doc.status()
                    if (status == DocumentExpiryStatus.EXPIRING_SOON || status == DocumentExpiryStatus.EXPIRED) {
                        list.add(
                            VehicleTimelineItem(
                                id = "timeline_doc_${doc.id}",
                                category = TimelineCategory.DOCUMENT_EXPIRY,
                                title = "Renewal Notice: ${doc.title}",
                                subtitle = doc.documentNumber,
                                timestampMillis = doc.expiryDateMillis ?: doc.updatedAt,
                                metricText = doc.expiryBadgeText(),
                                isAlert = true,
                                details = "Expires soon. Tap documents to view or renew."
                            )
                        )
                    }
                }

                list.sortedByDescending { it.timestampMillis }
            }.collect { merged ->
                _timelineItems.value = merged
            }
        }
    }
}
