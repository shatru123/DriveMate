package com.shatrughna.drivemate.car.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.Row
import androidx.car.app.model.SearchTemplate
import androidx.car.app.model.Template
import com.shatrughna.drivemate.DriveMateApplication
import com.shatrughna.drivemate.util.AppLogger

/**
 * Driver-safe arbitrary destination search and category discovery screen for Android Auto head unit.
 * Uses official SearchTemplate with voice and on-screen keyboard support.
 */
class CarDestinationSearchScreen(carContext: CarContext) : Screen(carContext) {

    private val app = carContext.applicationContext as DriveMateApplication
    private var searchQuery: String = ""

    private val searchCategories = listOf(
        "Nearby Petrol Pump" to "Find nearest fuel station",
        "Vehicle Service Center" to "Nearby service workshop",
        "Coffee & Cafes" to "Quick morning refreshments",
        "Restaurants & Food" to "Dining and takeaways near me",
        "Airport" to "Fastest route to nearest airport",
        "Nearest Hospital" to "Emergency medical care"
    )

    private val searchCallback = object : SearchTemplate.SearchCallback {
        override fun onSearchSubmitted(searchText: String) {
            val query = searchText.trim()
            if (query.isNotBlank()) {
                AppLogger.i(AppLogger.Tag.APP, "Car screen submitted arbitrary search: $query")
                app.destinationManager.launchNavigationQuery(carContext, query)
                screenManager.pop()
            }
        }

        override fun onSearchTextChanged(searchText: String) {
            searchQuery = searchText
            invalidate()
        }
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        val query = searchQuery.trim()
        if (query.isNotBlank()) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("Navigate to \"$query\"")
                    .addText("Search destination via Google Maps")
                    .setOnClickListener {
                        AppLogger.i(AppLogger.Tag.APP, "Car screen selected search item: $query")
                        app.destinationManager.launchNavigationQuery(carContext, query)
                        screenManager.pop()
                    }
                    .build()
            )
        }

        val filteredCategories = if (query.isBlank()) {
            searchCategories
        } else {
            searchCategories.filter { (cat, desc) ->
                cat.contains(query, ignoreCase = true) || desc.contains(query, ignoreCase = true)
            }
        }

        for ((category, description) in filteredCategories) {
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

        return SearchTemplate.Builder(searchCallback)
            .setHeaderAction(Action.BACK)
            .setSearchHint("Search destination or category...")
            .setInitialSearchText(searchQuery)
            .setItemList(listBuilder.build())
            .build()
    }
}
