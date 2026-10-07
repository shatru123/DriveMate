package com.shatrughna.drivemate.car.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.shatrughna.drivemate.data.model.WeatherInfo

/**
 * Android Auto Screen showing localized live weather conditions.
 */
class CarWeatherScreen(
    carContext: CarContext,
    private val weather: WeatherInfo
) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val pane = Pane.Builder().apply {
            if (weather.isAvailable) {
                addRow(
                    Row.Builder()
                        .setTitle("Temperature")
                        .addText(weather.displayTemperature)
                        .build()
                )
                addRow(
                    Row.Builder()
                        .setTitle("Condition")
                        .addText(weather.conditionText)
                        .build()
                )
                addRow(
                    Row.Builder()
                        .setTitle("Location")
                        .addText(weather.cityName.ifBlank { "Detected Location" })
                        .build()
                )
            } else {
                addRow(
                    Row.Builder()
                        .setTitle("Status")
                        .addText("Live weather data is currently unavailable.")
                        .build()
                )
            }
            addAction(
                Action.Builder()
                    .setTitle("Back")
                    .setOnClickListener {
                        screenManager.pop()
                    }
                    .build()
            )
        }.build()

        val header = Header.Builder()
            .setTitle("Weather Conditions")
            .setStartHeaderAction(Action.BACK)
            .build()

        return PaneTemplate.Builder(pane)
            .setHeader(header)
            .build()
    }
}
