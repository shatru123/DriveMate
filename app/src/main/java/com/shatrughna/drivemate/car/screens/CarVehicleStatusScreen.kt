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
import com.shatrughna.drivemate.core.telemetry.TelemetryAvailability
import com.shatrughna.drivemate.core.telemetry.VehicleTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Authoritative, glanceable vehicle-status screen. */
class CarVehicleStatusScreen(carContext: CarContext) : Screen(carContext) {
    private val app = carContext.applicationContext as DriveMateApplication
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var telemetryJob: Job? = null
    private var telemetry = VehicleTelemetry()

    init {
        lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) {
                telemetryJob?.cancel()
                scope.cancel()
            }
        })
        telemetryJob = scope.launch {
            app.vehicleTelemetryRepository.telemetry.collectLatest {
                telemetry = it
                invalidate()
            }
        }
    }

    override fun onGetTemplate(): Template {
        val items = ItemList.Builder()
            .addItem(Row.Builder().setTitle("Connection").addText(if (telemetry.androidAutoConnected) "Android Auto • Connected" else "Android Auto • Disconnected").build())
            .addItem(Row.Builder().setTitle("Vehicle telemetry").addText(if (telemetry.vehicleTelemetryConnected) "Available" else "Waiting for vehicle data").build())
            .addItem(Row.Builder().setTitle("Speed").addText(speedText()).build())
            .addItem(Row.Builder().setTitle("Odometer").addText(odometerText()).build())
            .addItem(Row.Builder().setTitle("Fuel / range").addText(fuelText()).build())
            .addItem(Row.Builder().setTitle("TPMS").addText("Not available • Check instrument cluster").build())
            .addItem(Row.Builder().setTitle("OBD2").addText("Not connected").build())
            .build()
        return ListTemplate.Builder()
            .setSingleList(items)
            .setHeader(Header.Builder().setTitle("Vehicle").setStartHeaderAction(Action.BACK).build())
            .build()
    }

    private fun speedText(): String = when (telemetry.speedAvailability) {
        TelemetryAvailability.LIVE -> telemetry.speedKmh?.let { "${it.toInt()} km/h • ${telemetry.speedSource.displayName}" } ?: "Unavailable"
        TelemetryAvailability.STALE -> telemetry.speedKmh?.let { "${it.toInt()} km/h • DATA STALE" } ?: "Unavailable"
        else -> "Unavailable"
    }

    private fun odometerText(): String = when {
        telemetry.isAuthoritativeOdometer && telemetry.vehicleOdometerKm != null -> "${String.format("%,.1f", telemetry.vehicleOdometerKm)} km • LIVE VEHICLE"
        telemetry.manualOdometerKm != null -> "${String.format("%,.1f", telemetry.manualOdometerKm)} km • MANUAL"
        else -> "Unavailable"
    }

    private fun fuelText(): String {
        val fuel = when (telemetry.fuelAvailability) {
            TelemetryAvailability.LIVE -> telemetry.fuelLevelPercent?.let { "${it.toInt()}%" } ?: "Unavailable"
            TelemetryAvailability.STALE -> "Fuel data stale"
            else -> "Fuel unavailable"
        }
        val range = when (telemetry.rangeAvailability) {
            TelemetryAvailability.LIVE -> telemetry.rangeRemainingKm?.let { "${it.toInt()} km range" } ?: "Range unavailable"
            TelemetryAvailability.STALE -> "Range data stale"
            else -> "Range unavailable"
        }
        return "$fuel • $range"
    }
}
