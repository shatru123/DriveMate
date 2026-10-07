package com.shatrughna.drivemate.car.screens

import android.content.Intent
import android.net.Uri
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.shatrughna.drivemate.data.model.DriveMateSettings
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Android Auto Screen displaying verified parked location and directions.
 */
class CarFindMyCarScreen(
    carContext: CarContext,
    private val settings: DriveMateSettings
) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val lat = settings.lastParkedLatitude
        val lon = settings.lastParkedLongitude
        val address = settings.lastParkedAddress ?: "Recorded GPS coordinates"
        val timeMillis = settings.lastParkedTimestampMillis

        val formattedTime = if (timeMillis != null && timeMillis > 0L) {
            val instant = Instant.ofEpochMilli(timeMillis)
            DateTimeFormatter.ofPattern("hh:mm a, dd MMM")
                .withZone(ZoneId.systemDefault())
                .format(instant)
        } else {
            "Recent drive"
        }

        val pane = Pane.Builder().apply {
            addRow(
                Row.Builder()
                    .setTitle("Parked Location")
                    .addText(address)
                    .build()
            )
            addRow(
                Row.Builder()
                    .setTitle("Parked Since")
                    .addText(formattedTime)
                    .build()
            )

            if (lat != null && lon != null) {
                addAction(
                    Action.Builder()
                        .setTitle("Walking Directions")
                        .setOnClickListener {
                            try {
                                val uri = Uri.parse("google.navigation:q=$lat,$lon&mode=w")
                                val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                carContext.startActivity(mapIntent)
                            } catch (_: Exception) {}
                        }
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
            .setTitle("Find My Car")
            .setStartHeaderAction(Action.BACK)
            .build()

        return PaneTemplate.Builder(pane)
            .setHeader(header)
            .build()
    }
}
