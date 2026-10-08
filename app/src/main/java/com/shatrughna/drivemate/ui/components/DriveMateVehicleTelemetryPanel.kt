package com.shatrughna.drivemate.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shatrughna.drivemate.core.telemetry.TelemetryAvailability
import com.shatrughna.drivemate.core.telemetry.VehicleTelemetry
import com.shatrughna.drivemate.ui.theme.NexonAmberAccent
import com.shatrughna.drivemate.ui.theme.NexonCyanPrimary
import com.shatrughna.drivemate.ui.theme.NexonEmeraldAccent
import com.shatrughna.drivemate.ui.theme.TextMuted
import com.shatrughna.drivemate.ui.theme.TextPrimary
import com.shatrughna.drivemate.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun DriveMateVehicleTelemetryPanel(
    telemetry: VehicleTelemetry,
    modifier: Modifier = Modifier
) {
    DriveMateCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            DriveMateSectionHeader(
                title = "VEHICLE TELEMETRY",
                subtitle = "Live values only • source-aware status"
            )
            TelemetryStatusRow(
                title = "Speed",
                value = telemetry.speedKmh
                    ?.takeIf { telemetry.speedAvailability.isUsable() }
                    ?.let { String.format(Locale.US, "%.0f km/h", it) }
                    ?: "Unavailable",
                detail = sourceDetail(telemetry.speedAvailability, telemetry.speedSource.displayName),
                icon = Icons.Default.Speed,
                availability = telemetry.speedAvailability
            )
            TelemetryStatusRow(
                title = "Odometer",
                value = telemetry.vehicleOdometerKm
                    ?.takeIf { telemetry.odometerAvailability.isUsable() }
                    ?.let { String.format(Locale.US, "%,.1f km", it) }
                    ?: telemetry.manualOdometerKm
                        ?.let { String.format(Locale.US, "%,.1f km", it) }
                        ?: "Unavailable",
                detail = when {
                    telemetry.odometerAvailability.isUsable() -> sourceDetail(telemetry.odometerAvailability, telemetry.odometerSource.displayName)
                    telemetry.manualOdometerKm != null -> "Manual calibration"
                    else -> availabilityLabel(telemetry.odometerAvailability)
                },
                icon = Icons.Default.DirectionsCar,
                availability = if (telemetry.odometerAvailability.isUsable()) telemetry.odometerAvailability else TelemetryAvailability.UNAVAILABLE
            )
            TelemetryStatusRow(
                title = "Fuel",
                value = telemetry.fuelLevelPercent
                    ?.takeIf { telemetry.fuelAvailability.isUsable() }
                    ?.let { String.format(Locale.US, "%.0f%%", it) }
                    ?: "Unavailable",
                detail = sourceDetail(telemetry.fuelAvailability, telemetry.fuelSource.displayName),
                icon = Icons.Default.LocalGasStation,
                availability = telemetry.fuelAvailability
            )
            TelemetryStatusRow(
                title = "Range",
                value = telemetry.rangeRemainingKm
                    ?.takeIf { telemetry.rangeAvailability.isUsable() }
                    ?.let { String.format(Locale.US, "%.0f km", it) }
                    ?: "Unavailable",
                detail = availabilityLabel(telemetry.rangeAvailability),
                icon = Icons.Default.Navigation,
                availability = telemetry.rangeAvailability
            )
            TelemetryStatusRow(
                title = "Engine / battery",
                value = engineBatteryValue(telemetry),
                detail = vehicleDetail(telemetry),
                icon = Icons.Default.Build,
                availability = vehicleAvailability(telemetry)
            )
            TelemetryStatusRow(
                title = "TPMS",
                value = if (telemetry.tpms.availability.isUsable()) "Sensor data available" else "Unavailable",
                detail = telemetry.tpms.notice,
                icon = Icons.Default.DirectionsCar,
                availability = telemetry.tpms.availability
            )
        }
    }
}

@Composable
private fun TelemetryStatusRow(
    title: String,
    value: String,
    detail: String,
    icon: ImageVector,
    availability: TelemetryAvailability
) {
    val accent = availabilityColor(availability)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                maxLines = 1
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (value == "Unavailable") TextSecondary else TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun TelemetryAvailability.isUsable(): Boolean =
    this == TelemetryAvailability.LIVE || this == TelemetryAvailability.STALE

private fun sourceDetail(availability: TelemetryAvailability, source: String): String = when {
    availability == TelemetryAvailability.STALE -> "Stale • $source"
    availability == TelemetryAvailability.LIVE -> "Live • $source"
    else -> availabilityLabel(availability)
}

private fun availabilityLabel(availability: TelemetryAvailability): String = when (availability) {
    TelemetryAvailability.LIVE -> "Live"
    TelemetryAvailability.STALE -> "Stale"
    TelemetryAvailability.NOT_CONNECTED -> "Not connected"
    TelemetryAvailability.NOT_SUPPORTED -> "Not supported by vehicle"
    TelemetryAvailability.NOT_AUTHORIZED -> "Permission needed"
    TelemetryAvailability.COMING_SOON -> "Coming soon"
    TelemetryAvailability.UNAVAILABLE -> "Unavailable"
}

private fun engineBatteryValue(telemetry: VehicleTelemetry): String = buildList {
    telemetry.engineRpm?.let { add("$it RPM") }
    telemetry.coolantTempCelsius?.let { add("$it°C") }
    telemetry.batteryVoltage?.let { add(String.format(Locale.US, "%.1f V", it)) }
}.joinToString(" • ").ifBlank { "Unavailable" }

private fun vehicleDetail(telemetry: VehicleTelemetry): String =
    if (telemetry.vehicleTelemetryConnected) "Live vehicle source" else "Not exposed by vehicle"

private fun vehicleAvailability(telemetry: VehicleTelemetry): TelemetryAvailability =
    if (telemetry.vehicleTelemetryConnected && (telemetry.engineRpm != null || telemetry.coolantTempCelsius != null || telemetry.batteryVoltage != null)) {
        TelemetryAvailability.LIVE
    } else {
        TelemetryAvailability.UNAVAILABLE
    }

private fun availabilityColor(availability: TelemetryAvailability) = when (availability) {
    TelemetryAvailability.LIVE -> NexonEmeraldAccent
    TelemetryAvailability.STALE -> NexonAmberAccent
    else -> TextMuted
}
