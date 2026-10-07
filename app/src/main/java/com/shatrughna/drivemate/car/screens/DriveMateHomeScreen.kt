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

        // 2. Driver & Vehicle Greeting Row
        val driverName = currentSettings.driverName.ifBlank { "Driver" }
        val vehicleName = currentSettings.fullVehicleName.ifBlank { "Tata Nexon" }
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Welcome, $driverName")
                .addText("$vehicleName • Active Companion")
                .build()
        )

        // 3. Live Weather Row
        val weatherText = if (currentWeather.isAvailable) {
            "${currentWeather.displayTemperature} • ${currentWeather.conditionText} in ${currentWeather.cityName.ifBlank { "Local" }}"
        } else {
            "Weather unavailable"
        }
        listBuilder.addItem(
            Row.Builder()
                .setTitle("🌤️ Weather")
                .addText(weatherText)
                .setOnClickListener {
                    screenManager.push(CarWeatherScreen(carContext, currentWeather))
                }
                .build()
        )

        // 4. Trip Statistics Row
        val tripMins = stats.activeTripDurationSeconds / 60
        val tripDist = String.format("%.1f", stats.activeTripDistanceKm)
        listBuilder.addItem(
            Row.Builder()
                .setTitle("🚗 Active Trip")
                .addText("$tripDist km driven • $tripMins min")
                .setOnClickListener {
                    screenManager.push(CarTripStatusScreen(carContext))
                }
                .build()
        )

        // 5. Quick Navigation: Home
        val homeAddr = currentSettings.homeAddress.ifBlank { "Home" }
        listBuilder.addItem(
            Row.Builder()
                .setTitle("🏠 Navigate to Home")
                .addText(homeAddr)
                .setOnClickListener {
                    app.destinationManager.launchNavigationQuery(carContext, homeAddr)
                }
                .build()
        )

        // 6. Quick Navigation: Office
        val officeAddr = currentSettings.officeAddress.ifBlank { "Office" }
        listBuilder.addItem(
            Row.Builder()
                .setTitle("🏢 Navigate to Office")
                .addText(officeAddr)
                .setOnClickListener {
                    app.destinationManager.launchNavigationQuery(carContext, officeAddr)
                }
                .build()
        )

        // If parked location exists, offer Find My Car row
        if (currentSettings.hasParkedLocation) {
            val parkedAddr = currentSettings.lastParkedAddress ?: "Saved parking coordinates"
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("📍 Find My Car")
                    .addText(parkedAddr)
                    .setOnClickListener {
                        screenManager.push(CarFindMyCarScreen(carContext, currentSettings))
                    }
                    .build()
            )
        }

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
