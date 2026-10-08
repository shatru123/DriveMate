package com.shatrughna.drivemate.car.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.shatrughna.drivemate.DriveMateApplication
import com.shatrughna.drivemate.core.telemetry.TelemetryAvailability
import com.shatrughna.drivemate.core.telemetry.VehicleTelemetry
import com.shatrughna.drivemate.data.model.DriveMateSettings
import com.shatrughna.drivemate.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

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
        val stats = app.tripTracker.tripStats.value
        val vehicleName = currentSettings.fullVehicleName
            .takeUnless { it == "Connected vehicle" }
            ?: "Vehicle"

        // 1. Projection and telemetry are separate states. Android Auto being
        // connected never implies that the vehicle exposes live telemetry.
        val tripMins = stats.activeTripDurationSeconds / 60
        val hasActiveDrive = stats.activeTripDurationSeconds > 0L || stats.activeTripDistanceKm > 0.05f
        val tripText = if (hasActiveDrive) {
            "${String.format(Locale.US, "%.1f", stats.activeTripDistanceKm)} km • $tripMins min • GPS"
        } else {
            "No active drive"
        }
        val vehicleDataText = listOf(
            odometerText(),
            fuelText()
        ).joinToString(" • ")

        val pane = Pane.Builder()
            .addRow(
                Row.Builder()
                    .setTitle(vehicleName)
                    .addText(connectionSummary())
                    .setOnClickListener { screenManager.push(CarVehicleStatusScreen(carContext)) }
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Speed")
                    .addText(speedText())
                    .setOnClickListener { screenManager.push(CarVehicleStatusScreen(carContext)) }
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Current drive")
                    .addText(tripText)
                    .setOnClickListener { screenManager.push(CarTripStatusScreen(carContext)) }
                    .build()
            )
            .addRow(
                Row.Builder()
                    .setTitle("Vehicle data")
                    .addText(vehicleDataText)
                    .setOnClickListener { screenManager.push(CarVehicleStatusScreen(carContext)) }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Ask DriveMate")
                    .setOnClickListener { screenManager.push(CarVoiceAssistantScreen(carContext)) }
                    .build()
            )
            .addAction(
                Action.Builder()
                    .setTitle("Navigate")
                    .setOnClickListener { screenManager.push(CarDestinationSearchScreen(carContext)) }
                    .build()
            )
            .build()

        val header = Header.Builder()
            .setTitle("DriveMate")
            .setStartHeaderAction(Action.APP_ICON)
            .build()

        return PaneTemplate.Builder(pane)
            .setHeader(header)
            .build()
    }

    private fun speedText(): String = when {
        currentTelemetry.speedKmh != null && currentTelemetry.speedAvailability == TelemetryAvailability.LIVE ->
            "${currentTelemetry.speedKmh!!.toInt()} km/h • LIVE • ${currentTelemetry.speedSource.displayName}"
        currentTelemetry.speedKmh != null && currentTelemetry.speedAvailability == TelemetryAvailability.STALE ->
            "${currentTelemetry.speedKmh!!.toInt()} km/h • STALE"
        else -> "-- km/h • ${currentTelemetry.speedAvailability.label}"
    }

    private fun odometerText(): String = when {
        currentTelemetry.isAuthoritativeOdometer && currentTelemetry.vehicleOdometerKm != null ->
            "Odo ${String.format(Locale.US, "%,.1f", currentTelemetry.vehicleOdometerKm)} km • Vehicle"
        currentTelemetry.manualOdometerKm != null ->
            "Odo ${String.format(Locale.US, "%,.1f", currentTelemetry.manualOdometerKm)} km • Manual"
        else -> "Odo unavailable"
    }

    private fun fuelText(): String {
        val fuel = if (currentTelemetry.fuelAvailability == TelemetryAvailability.LIVE || currentTelemetry.fuelAvailability == TelemetryAvailability.STALE) {
            currentTelemetry.fuelLevelPercent?.let { "Fuel ${it.toInt()}%" } ?: "Fuel unavailable"
        } else "Fuel ${currentTelemetry.fuelAvailability.label.lowercase()}"
        val range = if (currentTelemetry.rangeAvailability == TelemetryAvailability.LIVE || currentTelemetry.rangeAvailability == TelemetryAvailability.STALE) {
            currentTelemetry.rangeRemainingKm?.let { "Range ${it.toInt()} km" } ?: "Range unavailable"
        } else "Range ${currentTelemetry.rangeAvailability.label.lowercase()}"
        return "$fuel • $range"
    }

    private fun connectionSummary(): String = when {
        currentTelemetry.speedAvailability == TelemetryAvailability.LIVE &&
                currentTelemetry.vehicleTelemetryConnected -> "Vehicle data live"
        currentTelemetry.androidAutoConnected && currentTelemetry.vehicleTelemetryConnected -> "Vehicle data limited"
        currentTelemetry.androidAutoConnected -> "Android Auto connected • vehicle data unavailable"
        else -> "Vehicle data unavailable"
    }
}
