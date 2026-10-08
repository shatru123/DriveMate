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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Android Auto Screen displaying strictly separated and truthful metrics:
 * 1. LIVE VEHICLE (Speed & Authoritative Odometer)
 * 2. CURRENT DRIVE (GPS Distance & Duration)
 * 3. TODAY (Daily GPS total & trip count)
 * 4. SERVICE SCHEDULE (Next service distance)
 */
class CarTripStatusScreen(carContext: CarContext) : Screen(carContext) {

    private val app = carContext.applicationContext as DriveMateApplication
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var currentTelemetry: VehicleTelemetry = VehicleTelemetry()
    private var currentSettings: DriveMateSettings = DriveMateSettings()

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                AppLogger.d(AppLogger.TAG_ANDROID_AUTO, "CarTripStatusScreen onDestroy - cancelling scope")
                scope.cancel()
            }
        })

        scope.launch {
            try {
                currentSettings = app.preferencesRepository.settingsFlow.first()
            } catch (e: Exception) {
                AppLogger.w(AppLogger.TAG_ANDROID_AUTO, "Error reading settings: ${e.message}")
            }
        }

        scope.launch {
            app.vehicleTelemetryRepository.telemetry.collectLatest { telemetry ->
                currentTelemetry = telemetry
                invalidate()
            }
        }
    }

    override fun onGetTemplate(): Template {
        val stats = app.tripTracker.tripStats.value
        val durationMins = stats.activeTripDurationSeconds / 60
        val distKm = stats.activeTripDistanceKm
        val hasActiveDrive = stats.activeTripDurationSeconds > 0L || distKm > 0.05f
        val distText = if (hasActiveDrive) String.format("%.1f km", distKm) else "No active drive"

        val hours = (stats.activeMovingDurationSeconds / 3600f).coerceAtLeast(0.01f)
        val avgSpeed = if (distKm > 0.05f && stats.activeMovingDurationSeconds > 0L) (distKm / hours).toInt() else null

        // 1. LIVE VEHICLE
        val speedStr = when {
            currentTelemetry.speedAvailability == TelemetryAvailability.LIVE && currentTelemetry.speedKmh != null ->
                "${currentTelemetry.speedKmh!!.toInt()} km/h • ${currentTelemetry.speedSource.displayName}"
            currentTelemetry.speedAvailability == TelemetryAvailability.STALE && currentTelemetry.speedKmh != null ->
                "${currentTelemetry.speedKmh!!.toInt()} km/h • DATA STALE"
            else -> "Speed unavailable"
        }
        val odoStr = when {
            currentTelemetry.isAuthoritativeOdometer && currentTelemetry.vehicleOdometerKm != null ->
                "${String.format("%,.1f", currentTelemetry.vehicleOdometerKm)} km (Live Vehicle)"
            currentTelemetry.manualOdometerKm != null ->
                "${String.format("%,.1f", currentTelemetry.manualOdometerKm)} km (Manual)"
            else -> "Odometer unavailable"
        }
        val liveVehicleText = "Speed: $speedStr • Odometer: $odoStr"

        // 2. CURRENT DRIVE
        val currentDriveText = if (hasActiveDrive) {
            "$distText (GPS) • $durationMins min • Avg ${avgSpeed?.let { "$it km/h" } ?: "unavailable"}"
        } else "No active drive"

        // 3. TODAY
        val todayText = if (stats.todayTripsCount > 0) {
            "${String.format("%.1f", stats.todayTotalDistanceKm)} km GPS distance across ${stats.todayTripsCount} trips"
        } else "No trips today"

        // 4. SERVICE SCHEDULE
        val serviceText = currentTelemetry.effectiveOdometerKm?.takeIf { currentSettings.serviceTargetConfigured }?.let {
            val remainingService = (currentSettings.nextServiceKm - it).coerceAtLeast(0.0)
            "${String.format("%,.0f", remainingService)} km remaining (Next: ${currentSettings.nextServiceKm} km)"
        } ?: "Service distance unavailable"

        val pane = Pane.Builder().apply {
            addRow(
                Row.Builder()
                    .setTitle("🚗 LIVE VEHICLE")
                    .addText(liveVehicleText)
                    .build()
            )
            addRow(
                Row.Builder()
                    .setTitle("⏱️ CURRENT DRIVE")
                    .addText(currentDriveText)
                    .build()
            )
            addRow(
                Row.Builder()
                    .setTitle("📅 TODAY")
                    .addText(todayText)
                    .build()
            )
            addRow(
                Row.Builder()
                    .setTitle("🔧 SERVICE SCHEDULE")
                    .addText(serviceText)
                    .build()
            )
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
            .setTitle("Vehicle & Trip Telemetry")
            .setStartHeaderAction(Action.BACK)
            .build()

        return PaneTemplate.Builder(pane)
            .setHeader(header)
            .build()
    }
}
