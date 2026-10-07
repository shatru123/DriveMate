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
import com.shatrughna.drivemate.data.model.WeatherInfo
import com.shatrughna.drivemate.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Primary Android Auto Dashboard Screen.
 * Driver-safe, glanceable, and voice-first interface built with official AndroidX Car App Library templates.
 */
class DriveMateHomeScreen(carContext: CarContext) : Screen(carContext) {

    private val app = carContext.applicationContext as DriveMateApplication
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var dataLoadJob: Job? = null

    private var currentSettings: DriveMateSettings = DriveMateSettings()
    private var currentWeather: WeatherInfo = WeatherInfo.unavailable()
    private var isDataLoaded = false

    init {
        loadData()
    }

    private fun loadData() {
        dataLoadJob?.cancel()
        dataLoadJob = scope.launch {
            try {
                currentSettings = app.preferencesRepository.settingsFlow.first()
                val resolvedLoc = app.locationResolver.resolveLocation(currentSettings)
                if (resolvedLoc.isAvailable) {
                    currentWeather = app.weatherRepository.getCurrentWeather(
                        cityName = resolvedLoc.displayName ?: "",
                        latitude = resolvedLoc.latitude,
                        longitude = resolvedLoc.longitude
                    )
                }
            } catch (e: Exception) {
                AppLogger.w(AppLogger.Tag.APP, "Error loading car dashboard data: ${e.message}")
            } finally {
                isDataLoaded = true
                invalidate()
            }
        }
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()
        val stats = app.tripTracker.tripStats.value

        // 1. Voice Assistant Quick Action Item
        listBuilder.addItem(
            Row.Builder()
                .setTitle("🎙️ Ask DriveMate")
                .addText("Tap to speak or say \"Hey DriveMate\"")
                .setOnClickListener {
                    screenManager.push(CarVoiceAssistantScreen(carContext))
                }
                .build()
        )

        // 2. Search Destination (Categories)
        listBuilder.addItem(
            Row.Builder()
                .setTitle("📍 Search Destination")
                .addText("Petrol pump, Tata service, Airport, Food")
                .setOnClickListener {
                    screenManager.push(CarDestinationSearchScreen(carContext))
                }
                .build()
        )

        // 3. Recent Destinations
        listBuilder.addItem(
            Row.Builder()
                .setTitle("🕘 Recent Destinations")
                .addText("Quickly resume recent drives")
                .setOnClickListener {
                    screenManager.push(CarRecentDestinationsScreen(carContext))
                }
                .build()
        )

        // 4. Suggested For You
        listBuilder.addItem(
            Row.Builder()
                .setTitle("⭐ Suggested For You")
                .addText("Contextual suggestions & saved shortcuts")
                .setOnClickListener {
                    screenManager.push(CarSuggestedDestinationsScreen(carContext, currentSettings))
                }
                .build()
        )

        // 5. Trip Statistics Row
        val tripMins = stats.activeTripDurationSeconds / 60
        val tripDist = String.format("%.1f", stats.activeTripDistanceKm)
        listBuilder.addItem(
            Row.Builder()
                .setTitle("🚗 Trip & Vehicle Status")
                .addText("$tripDist km driven • $tripMins min active")
                .setOnClickListener {
                    screenManager.push(CarTripStatusScreen(carContext))
                }
                .build()
        )

        // 6. Live Weather Row
        val weatherText = if (currentWeather.isAvailable) {
            "${currentWeather.displayTemperature} • ${currentWeather.conditionText} in ${currentWeather.cityName.ifBlank { "Current Location" }}"
        } else {
            "Weather unavailable"
        }
        listBuilder.addItem(
            Row.Builder()
                .setTitle("🌤️ Live Weather")
                .addText(weatherText)
                .setOnClickListener {
                    screenManager.push(CarWeatherScreen(carContext, currentWeather))
                }
                .build()
        )

        val header = Header.Builder()
            .setTitle("DriveMate • Tata Nexon")
            .setStartHeaderAction(Action.APP_ICON)
            .build()

        return ListTemplate.Builder()
            .setSingleList(listBuilder.build())
            .setHeader(header)
            .build()
    }
}
