package com.shatrughna.drivemate.destination

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.shatrughna.drivemate.data.model.Destination
import com.shatrughna.drivemate.data.model.DestinationCategory
import com.shatrughna.drivemate.data.model.DestinationSource
import com.shatrughna.drivemate.data.model.DriveMateSettings
import com.shatrughna.drivemate.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.UUID

interface DestinationManager {
    val recentDestinations: StateFlow<List<Destination>>
    fun getSuggestedDestinations(settings: DriveMateSettings): List<Destination>
    fun launchNavigation(context: Context, destination: Destination): Boolean
    fun launchNavigationQuery(context: Context, destinationQuery: String): Boolean
    fun clearRecentDestinations()
}

class DestinationManagerImpl(
    private val recentDestinationRepository: RecentDestinationRepository? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : DestinationManager {

    override val recentDestinations: StateFlow<List<Destination>>
        get() = recentDestinationRepository?.recentDestinations
            ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())

    override fun getSuggestedDestinations(settings: DriveMateSettings): List<Destination> {
        val now = LocalTime.now()
        val hour = now.hour
        val suggestions = mutableListOf<Destination>()

        // 1. Vehicle Care context
        val remainingServiceKm = settings.nextServiceKm - settings.odometerKm
        if (remainingServiceKm <= 1000) {
            suggestions.add(
                Destination(
                    id = "service_tata",
                    name = "Tata Service Center",
                    address = "Authorized Tata Motors Workshop",
                    source = DestinationSource.SUGGESTED,
                    query = "Tata Motors authorized service center",
                    category = DestinationCategory.SERVICE_CENTER
                )
            )
        }

        if (settings.fuelReminderEnabled) {
            suggestions.add(
                Destination(
                    id = "fuel_pump",
                    name = "Nearby Petrol Pump",
                    address = "Fuel Station",
                    source = DestinationSource.SUGGESTED,
                    query = "petrol pump near me",
                    category = DestinationCategory.FUEL
                )
            )
        }

        // 2. Saved Home & Office Shortcuts
        val homeDest = Destination(
            id = "saved_home",
            name = "Home",
            address = settings.homeAddress.ifBlank { "Home" },
            source = DestinationSource.SAVED,
            query = settings.homeAddress.ifBlank { "Home" },
            category = DestinationCategory.HOME
        )

        val officeDest = Destination(
            id = "saved_office",
            name = "Office",
            address = settings.officeAddress.ifBlank { "Office" },
            source = DestinationSource.SAVED,
            query = settings.officeAddress.ifBlank { "Office" },
            category = DestinationCategory.OFFICE
        )

        // 3. Contextual Time-of-Day Suggestions
        when (hour) {
            in 6..11 -> {
                suggestions.add(officeDest)
                suggestions.add(
                    Destination(
                        id = "suggest_coffee",
                        name = "Coffee & Breakfast",
                        address = "Morning Stop",
                        source = DestinationSource.SUGGESTED,
                        query = "coffee shop near me",
                        category = DestinationCategory.FOOD
                    )
                )
                suggestions.add(homeDest)
            }
            in 16..22 -> {
                suggestions.add(homeDest)
                suggestions.add(
                    Destination(
                        id = "suggest_dinner",
                        name = "Restaurants",
                        address = "Evening Dining",
                        source = DestinationSource.SUGGESTED,
                        query = "restaurants near me",
                        category = DestinationCategory.FOOD
                    )
                )
                suggestions.add(officeDest)
            }
            else -> {
                suggestions.add(homeDest)
                suggestions.add(officeDest)
                suggestions.add(
                    Destination(
                        id = "suggest_fuel",
                        name = "Petrol Pump",
                        address = "Fuel & Air",
                        source = DestinationSource.SUGGESTED,
                        query = "petrol pump near me",
                        category = DestinationCategory.FUEL
                    )
                )
            }
        }

        // Return deduplicated list
        return suggestions.distinctBy { it.name }
    }

    override fun launchNavigation(context: Context, destination: Destination): Boolean {
        val success = launchNavigationQuery(context, destination.searchQueryOrAddress)
        if (success && recentDestinationRepository != null) {
            scope.launch {
                recentDestinationRepository.addDestination(destination)
            }
        }
        return success
    }

    override fun launchNavigationQuery(context: Context, destinationQuery: String): Boolean {
        try {
            val query = Uri.encode(destinationQuery)
            val gmmIntentUri = Uri.parse("google.navigation:q=$query")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                setPackage("com.google.android.apps.maps")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
                AppLogger.i(AppLogger.Tag.APP, "Launched Google Navigation for $destinationQuery")
                recordQueryToRecents(destinationQuery)
                return true
            } else {
                val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                if (geoIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(geoIntent)
                    AppLogger.i(AppLogger.Tag.APP, "Launched Geo Intent for $destinationQuery")
                    recordQueryToRecents(destinationQuery)
                    return true
                } else {
                    // Fallback to web maps URL so it never crashes
                    val webIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/search/?api=1&query=$query")
                    ).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(webIntent)
                    AppLogger.i(AppLogger.Tag.APP, "Launched Web Maps URL fallback for $destinationQuery")
                    recordQueryToRecents(destinationQuery)
                    return true
                }
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.APP, "Failed to launch navigation for $destinationQuery", e)
            return false
        }
    }

    override fun clearRecentDestinations() {
        if (recentDestinationRepository != null) {
            scope.launch {
                recentDestinationRepository.clearHistory()
            }
        }
    }

    private fun recordQueryToRecents(query: String) {
        if (recentDestinationRepository != null) {
            scope.launch {
                recentDestinationRepository.addDestination(
                    Destination(
                        id = UUID.randomUUID().toString(),
                        name = query.replaceFirstChar { it.uppercase() },
                        query = query,
                        source = DestinationSource.MAP_SEARCH,
                        category = DestinationCategory.CUSTOM
                    )
                )
            }
        }
    }
}
