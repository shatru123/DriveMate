package com.shatrughna.drivemate.car.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.shatrughna.drivemate.DriveMateApplication
import com.shatrughna.drivemate.util.AppLogger

/**
 * Android Auto screen displaying recent destination history (capped at 6 items for driver safety).
 */
class CarRecentDestinationsScreen(carContext: CarContext) : Screen(carContext) {

    private val app = carContext.applicationContext as DriveMateApplication

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()
        val recents = app.recentDestinationRepository.recentDestinations.value.take(6)

        if (recents.isEmpty()) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("No recent destinations")
                    .addText("Your recent drives will appear here")
                    .build()
            )
        } else {
            recents.forEach { dest ->
                val subtitle = dest.address ?: dest.query ?: "Recent drive"
                listBuilder.addItem(
                    Row.Builder()
                        .setTitle(dest.name)
                        .addText(subtitle)
                        .setOnClickListener {
                            AppLogger.i(AppLogger.Tag.APP, "Car screen launching a recent destination")
                            app.destinationManager.launchNavigation(carContext, dest)
                            screenManager.pop()
                        }
                        .build()
                )
            }
        }

        val header = Header.Builder()
            .setTitle("Recent Destinations")
            .setStartHeaderAction(Action.BACK)
            .build()

        return ListTemplate.Builder()
            .setSingleList(listBuilder.build())
            .setHeader(header)
            .build()
    }
}
