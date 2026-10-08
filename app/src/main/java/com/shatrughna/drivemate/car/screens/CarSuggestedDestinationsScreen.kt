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
import com.shatrughna.drivemate.data.model.DriveMateSettings
import com.shatrughna.drivemate.util.AppLogger

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Android Auto screen presenting contextual destination suggestions based on time of day,
 * fuel status, vehicle service interval, and saved shortcuts.
 */
class CarSuggestedDestinationsScreen(
    carContext: CarContext,
    private val settings: DriveMateSettings? = null
) : Screen(carContext) {

    private val app = carContext.applicationContext as DriveMateApplication

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()
        val effectiveSettings = settings ?: runCatching {
            runBlocking { app.preferencesRepository.settingsFlow.first() }
        }.getOrDefault(DriveMateSettings())
        val suggestions = app.destinationManager.getSuggestedDestinations(effectiveSettings).take(6)

        if (suggestions.isEmpty()) {
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("No suggestions available")
                    .addText("DriveMate learns your routine as you drive")
                    .build()
            )
        } else {
            suggestions.forEach { dest ->
                val subtitle = dest.address ?: dest.query ?: "Contextual recommendation"
                listBuilder.addItem(
                    Row.Builder()
                        .setTitle(dest.name)
                        .addText(subtitle)
                        .setOnClickListener {
                            AppLogger.i(AppLogger.Tag.APP, "Car screen selected a suggested destination")
                            app.destinationManager.launchNavigation(carContext, dest)
                            screenManager.pop()
                        }
                        .build()
                )
            }
        }

        val header = Header.Builder()
            .setTitle("Suggested For You")
            .setStartHeaderAction(Action.BACK)
            .build()

        return ListTemplate.Builder()
            .setSingleList(listBuilder.build())
            .setHeader(header)
            .build()
    }
}
