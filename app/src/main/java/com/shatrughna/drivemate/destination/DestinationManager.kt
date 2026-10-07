package com.shatrughna.drivemate.destination

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.shatrughna.drivemate.data.model.Destination
import com.shatrughna.drivemate.data.model.DestinationType
import com.shatrughna.drivemate.data.model.DriveMateSettings
import com.shatrughna.drivemate.util.AppLogger
import java.time.LocalTime

interface DestinationManager {
    fun getSuggestedDestinations(settings: DriveMateSettings): List<Destination>
    fun launchNavigation(context: Context, destination: Destination): Boolean
    fun launchNavigationQuery(context: Context, destinationQuery: String): Boolean
}

class DestinationManagerImpl : DestinationManager {

    override fun getSuggestedDestinations(settings: DriveMateSettings): List<Destination> {
        val now = LocalTime.now()
        val hour = now.hour

        val homeDest = Destination(
            id = "home",
            title = "Home",
            subtitle = settings.homeAddress.ifBlank { "Home" },
            searchQueryOrAddress = settings.homeAddress.ifBlank { "Home" },
            iconType = DestinationType.HOME
        )

        val officeDest = Destination(
            id = "office",
            title = "Office",
            subtitle = settings.officeAddress.ifBlank { "Office" },
            searchQueryOrAddress = settings.officeAddress.ifBlank { "Office" },
            iconType = DestinationType.OFFICE
        )

        return when (hour) {
            in 6..11 -> listOf(officeDest, homeDest)
            in 16..22 -> listOf(homeDest, officeDest)
            else -> listOf(homeDest, officeDest)
        }
    }

    override fun launchNavigation(context: Context, destination: Destination): Boolean {
        return launchNavigationQuery(context, destination.searchQueryOrAddress)
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
                return true
            } else {
                val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(geoIntent)
                return true
            }
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.APP, "Failed to launch navigation for $destinationQuery", e)
            return false
        }
    }
}
