package com.shatrughna.drivemate.car.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.shatrughna.drivemate.DriveMateApplication
import com.shatrughna.drivemate.core.telemetry.TelemetryAvailability
import com.shatrughna.drivemate.core.telemetry.VehicleTelemetry
import com.shatrughna.drivemate.data.model.DriveMateSettings
import com.shatrughna.drivemate.data.model.WeatherInfo
import com.shatrughna.drivemate.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Primary Android Auto Dashboard Screen.
 * Driver-safe, glanceable, and push-to-talk interface built with official AndroidX Car App Library templates.
 * Binds lifecycles strictly to Screen.lifecycle to avoid background leaks.
 */
class DriveMateHomeScreen(carContext: CarContext) : Screen(carContext) {

    private val app = carContext.applicationContext as DriveMateApplication
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var dataLoadJob: Job? = null
    private var telemetryJob: Job? = null

    private var currentSettings: DriveMateSettings = DriveMateSettings()
    private var currentWeather: WeatherInfo = WeatherInfo.unavailable()
    private var currentTelemetry: VehicleTelemetry = VehicleTelemetry()

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                AppLogger.d(AppLogger.TAG_ANDROID_AUTO, "DriveMateHomeScreen onDestroy - cancelling scope")
                scope.cancel()
            }
        })

        loadData()
        observeTelemetry()
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
                AppLogger.w(AppLogger.TAG_ANDROID_AUTO, "Error loading car dashboard data: ${e.message}")
            } finally {
                invalidate()
            }
        }
    }

    private fun observeTelemetry() {
        telemetryJob?.cancel()
        telemetryJob = scope.launch {
            app.vehicleTelemetryRepository.telemetry.collectLatest { telemetry ->
                currentTelemetry = telemetry
                invalidate()
            }
        }
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()
        val stats = app.tripTracker.tripStats.value

        // 1. Voice Assistant Quick Action Item (Truthful push-to-talk, no continuous wake-word)
        listBuilder.addItem(
            Row.Builder()
                .setTitle("🎙️ Ask DriveMate")
                .addText("Push-to-talk • Tap to speak")
                .setOnClickListener {
                    screenManager.push(CarVoiceAssistantScreen(carContext))
                }
                .build()
        )

        // 2. Real Vehicle Telemetry: Speed & Authoritative Odometer
        val speedStr = when {
            currentTelemetry.speedAvailability == TelemetryAvailability.LIVE && currentTelemetry.speedKmh != null ->
                "${currentTelemetry.speedKmh?.toInt()} km/h (${currentTelemetry.speedSource.displayName})"
            currentTelemetry.speedKmh != null ->
                "${currentTelemetry.speedKmh?.toInt()} km/h"
            else -> "Vehicle Idle"
        }
        val odoStr = when {
            currentTelemetry.isAuthoritativeOdometer ->
                "${String.format("%,.1f", currentTelemetry.vehicleOdometerKm)} km (Car Odometer)"
            else ->
                "${String.format("%,.1f", currentTelemetry.manualOdometerKm)} km (Calibrated)"
        }
        listBuilder.addItem(
            Row.Builder()
                .setTitle("⚡ Speed & Odometer")
                .addText("$speedStr • $odoStr")
                .setOnClickListener {
                    screenManager.push(CarTripStatusScreen(carContext))
                }
                .build()
        )

        // 3. Search Destination (Categories)
        listBuilder.addItem(
            Row.Builder()
                .setTitle("📍 Search Destination")
                .addText("Petrol pump, Tata service, Airport, Food")
                .setOnClickListener {
                    screenManager.push(CarDestinationSearchScreen(carContext))
                }
                .build()
        )

        // 4. Recent Destinations
        listBuilder.addItem(
            Row.Builder()
                .setTitle("🕘 Recent Destinations")
                .addText("Quickly resume recent drives")
                .setOnClickListener {
                    screenManager.push(CarRecentDestinationsScreen(carContext))
                }
                .build()
        )

        // 5. Trip Statistics & Maintenance
        val tripMins = stats.activeTripDurationSeconds / 60
        val tripDist = String.format("%.1f", stats.activeTripDistanceKm)
        val remainingService = (currentSettings.nextServiceKm - currentTelemetry.effectiveOdometerKm).coerceAtLeast(0.0)
        val serviceText = "${String.format("%,.0f", remainingService)} km to service"
        listBuilder.addItem(
            Row.Builder()
                .setTitle("🚗 Current Drive: $tripDist km")
                .addText("$tripMins min active (GPS) • $serviceText")
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
