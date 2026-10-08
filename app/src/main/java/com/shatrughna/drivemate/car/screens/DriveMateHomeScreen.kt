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

        // 1. Projection and telemetry are separate states. Android Auto being
        // connected never implies that the vehicle exposes live telemetry.
        listBuilder.addItem(
            Row.Builder()
                .setTitle("DriveMate")
                .addText(connectionSummary())
                .setOnClickListener {
                    screenManager.push(CarVehicleStatusScreen(carContext))
                }
                .build()
        )

        // 2. Real Vehicle Telemetry: speed
        val speedStr = when {
            currentTelemetry.speedKmh != null && currentTelemetry.speedAvailability == TelemetryAvailability.LIVE ->
                "${currentTelemetry.speedKmh!!.toInt()} km/h • ${currentTelemetry.speedSource.displayName}"
            currentTelemetry.speedKmh != null && currentTelemetry.speedAvailability == TelemetryAvailability.STALE ->
                "${currentTelemetry.speedKmh!!.toInt()} km/h • DATA STALE"
            else -> "Speed unavailable"
        }
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Speed")
                .addText(speedStr)
                .setOnClickListener {
                    screenManager.push(CarVehicleStatusScreen(carContext))
                }
                .build()
        )

        // 3. Real Vehicle Telemetry: odometer with explicit source.
        val odoStr = when {
            currentTelemetry.isAuthoritativeOdometer && currentTelemetry.vehicleOdometerKm != null ->
                "${String.format("%,.1f", currentTelemetry.vehicleOdometerKm)} km • Vehicle"
            currentTelemetry.manualOdometerKm != null ->
                "${String.format("%,.1f", currentTelemetry.manualOdometerKm)} km • Manual"
            else -> "Odometer unavailable"
        }
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Odometer")
                .addText(odoStr)
                .setOnClickListener {
                    screenManager.push(CarVehicleStatusScreen(carContext))
                }
                .build()
        )

        // 4. Voice Assistant Quick Action Item (truthful push-to-talk, no continuous wake-word)
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Ask DriveMate")
                .addText("Push-to-talk • Tap to speak")
                .setOnClickListener {
                    screenManager.push(CarVoiceAssistantScreen(carContext))
                }
                .build()
        )

        // 5. Search Destination (Categories)
        listBuilder.addItem(
            Row.Builder()
                .setTitle("📍 Search Destination")
                .addText("Fuel, service, airport, food")
                .setOnClickListener {
                    screenManager.push(CarDestinationSearchScreen(carContext))
                }
                .build()
        )

        // 6. Recent Destinations
        listBuilder.addItem(
            Row.Builder()
                .setTitle("🕘 Recent Destinations")
                .addText("Quickly resume recent drives")
                .setOnClickListener {
                    screenManager.push(CarRecentDestinationsScreen(carContext))
                }
                .build()
        )

        // 7. Trip Statistics & Maintenance
        val tripMins = stats.activeTripDurationSeconds / 60
        val hasActiveDrive = stats.activeTripDurationSeconds > 0L || stats.activeTripDistanceKm > 0.05f
        val tripDist = stats.activeTripDistanceKm.takeIf { hasActiveDrive }?.let { String.format("%.1f", it) }
        val remainingService = currentTelemetry.effectiveOdometerKm?.takeIf { currentSettings.serviceTargetConfigured }?.let {
            (currentSettings.nextServiceKm - it).coerceAtLeast(0.0)
        }
        val serviceText = remainingService?.let { "${String.format("%,.0f", it)} km to service" } ?: "Service distance unavailable"
        listBuilder.addItem(
            Row.Builder()
                .setTitle(tripDist?.let { "Current Drive: $it km" } ?: "Current Drive")
                .addText(if (hasActiveDrive) "$tripMins min active (GPS) • $serviceText" else serviceText)
                .setOnClickListener {
                    screenManager.push(CarTripStatusScreen(carContext))
                }
                .build()
        )

        // 8. Live Weather Row
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
            .setTitle("DriveMate • ${currentSettings.fullVehicleName}")
            .setStartHeaderAction(Action.APP_ICON)
            .build()

        return ListTemplate.Builder()
            .setSingleList(listBuilder.build())
            .setHeader(header)
            .build()
    }

    private fun connectionSummary(): String = when {
        currentTelemetry.speedAvailability == TelemetryAvailability.LIVE &&
                currentTelemetry.vehicleTelemetryConnected -> "Vehicle data live"
        currentTelemetry.androidAutoConnected && currentTelemetry.vehicleTelemetryConnected -> "Vehicle data limited"
        currentTelemetry.androidAutoConnected -> "Android Auto connected • vehicle data unavailable"
        else -> "Vehicle data unavailable"
    }
}
