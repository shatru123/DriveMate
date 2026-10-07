package com.shatrughna.drivemate.car.screens

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.shatrughna.drivemate.DriveMateApplication

/**
 * Android Auto Screen displaying verified active trip statistics.
 */
class CarTripStatusScreen(carContext: CarContext) : Screen(carContext) {

    private val app = carContext.applicationContext as DriveMateApplication

    override fun onGetTemplate(): Template {
        val stats = app.tripTracker.tripStats.value
        val durationMins = stats.activeTripDurationSeconds / 60
        val distKm = stats.activeTripDistanceKm
        val distText = if (distKm > 0.05f) String.format("%.1f km", distKm) else "0.0 km"

        val hours = (stats.activeTripDurationSeconds / 3600f).coerceAtLeast(0.01f)
        val avgSpeed = if (distKm > 0.05f) (distKm / hours).toInt() else 0

        val pane = Pane.Builder().apply {
            addRow(
                Row.Builder()
                    .setTitle("Trip Duration")
                    .addText("$durationMins minutes active driving")
                    .build()
            )
            addRow(
                Row.Builder()
                    .setTitle("GPS Distance")
                    .addText(distText)
                    .build()
            )
            addRow(
                Row.Builder()
                    .setTitle("Average Speed")
                    .addText("$avgSpeed km/h")
                    .build()
            )
            addRow(
                Row.Builder()
                    .setTitle("Today's Total Distance")
                    .addText("${String.format("%.1f", stats.todayTotalDistanceKm)} km (${stats.todayTripsCount} trips)")
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
            .setTitle("Trip Statistics")
            .setStartHeaderAction(Action.BACK)
            .build()

        return PaneTemplate.Builder(pane)
            .setHeader(header)
            .build()
    }
}
