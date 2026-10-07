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
 * Driver-safe category destination search screen for Android Auto head unit.
 */
class CarDestinationSearchScreen(carContext: CarContext) : Screen(carContext) {

    private val app = carContext.applicationContext as DriveMateApplication

    private val searchCategories = listOf(
        "Nearby Petrol Pump" to "Find nearest fuel station",
        "Tata Motors Service Center" to "Authorized service workshop",
        "Coffee & Cafes" to "Quick morning refreshments",
        "Restaurants & Food" to "Dining and takeaways near me",
        "Airport" to "Fastest route to nearest airport",
        "Nearest Hospital" to "Emergency medical care"
    )

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        for ((category, description) in searchCategories) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle(category)
                    .addText(description)
                    .setOnClickListener {
                        AppLogger.i(AppLogger.Tag.APP, "Car screen selected category: $category")
                        app.destinationManager.launchNavigationQuery(carContext, category)
                        screenManager.pop()
                    }
                    .build()
            )
        }

        val header = Header.Builder()
            .setTitle("Search Destination")
            .setStartHeaderAction(Action.BACK)
            .build()

        return ListTemplate.Builder()
            .setSingleList(listBuilder.build())
            .setHeader(header)
            .build()
    }
}
