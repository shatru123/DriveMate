package com.shatrughna.drivemate.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shatrughna.drivemate.core.telemetry.TelemetryAvailability
import com.shatrughna.drivemate.core.telemetry.TelemetrySource
import com.shatrughna.drivemate.core.telemetry.VehicleTelemetry
import com.shatrughna.drivemate.data.model.DriveMateSettings
import com.shatrughna.drivemate.data.model.TripStats
import com.shatrughna.drivemate.ui.theme.CardGradientEnd
import com.shatrughna.drivemate.ui.theme.CardGradientStart
import com.shatrughna.drivemate.ui.theme.DarkBorder
import com.shatrughna.drivemate.ui.theme.DarkSurface
import com.shatrughna.drivemate.ui.theme.DarkSurfaceVariant
import com.shatrughna.drivemate.ui.theme.NexonAmberAccent
import com.shatrughna.drivemate.ui.theme.NexonCyanPrimary
import com.shatrughna.drivemate.ui.theme.NexonEmeraldAccent
import com.shatrughna.drivemate.ui.theme.NexonRedAccent
import com.shatrughna.drivemate.ui.theme.TextMuted
import com.shatrughna.drivemate.ui.theme.TextPrimary
import com.shatrughna.drivemate.ui.theme.TextSecondary
import com.shatrughna.drivemate.voice.VoiceAssistantState
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The phone-first cockpit hero. It deliberately consumes the real telemetry
 * model instead of manufacturing a visually convenient dashboard value.
 */
@Composable
fun DriveMateCockpit(
    settings: DriveMateSettings,
    telemetry: VehicleTelemetry,
    tripStats: TripStats,
    isSessionActive: Boolean,
    voiceState: VoiceAssistantState,
    onAssistantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vehicleName = settings.fullVehicleName
        .takeUnless { it == "Connected vehicle" }
        ?: "MY VEHICLE"

    val connectionLabel = when {
        telemetry.speedAvailability == TelemetryAvailability.LIVE &&
                telemetry.vehicleTelemetryConnected -> "VEHICLE DATA LIVE"
        telemetry.androidAutoConnected && telemetry.vehicleTelemetryConnected -> "VEHICLE DATA LIMITED"
        telemetry.androidAutoConnected -> "ANDROID AUTO CONNECTED"
        telemetry.speedSource == TelemetrySource.PHONE_GPS -> "PHONE GPS ACTIVE"
        else -> "VEHICLE DATA UNAVAILABLE"
    }
    val connectionColor = when {
        telemetry.speedAvailability == TelemetryAvailability.LIVE -> NexonEmeraldAccent
        telemetry.androidAutoConnected || telemetry.speedSource == TelemetrySource.PHONE_GPS -> NexonAmberAccent
        else -> TextMuted
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Brush.horizontalGradient(
                    listOf(NexonCyanPrimary.copy(alpha = 0.42f), DarkBorder, DarkBorder)
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(CardGradientStart, DarkSurface, CardGradientEnd)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DRIVEMATE",
                            style = MaterialTheme.typography.labelSmall,
                            color = NexonCyanPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.8.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = vehicleName,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    CockpitConnectionIndicator(
                        label = connectionLabel,
                        color = connectionColor,
                        isActive = telemetry.speedAvailability == TelemetryAvailability.LIVE
                    )
                }

                DriveMateSpeedometer(
                    speedKmh = telemetry.speedKmh,
                    availability = telemetry.speedAvailability,
                    source = telemetry.speedSource
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryModule(
                        modifier = Modifier.weight(1f),
                        title = "ODOMETER",
                        value = odometerValue(settings, telemetry),
                        detail = odometerDetail(settings, telemetry),
                        icon = Icons.Default.Speed,
                        state = odometerState(settings, telemetry)
                    )
                    TelemetryModule(
                        modifier = Modifier.weight(1f),
                        title = "FUEL",
                        value = telemetry.fuelLevelPercent?.let { "${it.roundToInt()}%" } ?: "--",
                        detail = availabilityDetail(telemetry.fuelAvailability),
                        icon = Icons.Default.LocalGasStation,
                        state = telemetry.fuelAvailability
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryModule(
                        modifier = Modifier.weight(1f),
                        title = "RANGE",
                        value = telemetry.rangeRemainingKm?.let { "${it.roundToInt()} km" } ?: "--",
                        detail = availabilityDetail(telemetry.rangeAvailability),
                        icon = Icons.Default.Navigation,
                        state = telemetry.rangeAvailability
                    )
                    TelemetryModule(
                        modifier = Modifier.weight(1f),
                        title = "TRIP",
                        value = if (isSessionActive) {
                            String.format(Locale.US, "%.1f km", tripStats.activeTripDistanceKm)
                        } else "--",
                        detail = if (isSessionActive) tripStats.formattedActiveDuration else "No active drive",
                        icon = Icons.Default.Route,
                        state = if (isSessionActive) TelemetryAvailability.LIVE else TelemetryAvailability.UNAVAILABLE
                    )
                }
            }
        }

        DriveMateAssistantOrb(
            state = voiceState,
            onClick = onAssistantClick
        )
    }
}

@Composable
fun DriveMateSpeedometer(
    speedKmh: Float?,
    availability: TelemetryAvailability,
    source: TelemetrySource,
    modifier: Modifier = Modifier
) {
    val validSpeed = speedKmh?.takeIf { it.isFinite() && it >= 0f }
    val animatedSpeed by animateFloatAsState(
        targetValue = validSpeed ?: 0f,
        animationSpec = tween(DriveMateAnimations.valueDurationMillis, easing = FastOutSlowInEasing),
        label = "cockpit_speed"
    )
    val accent = when (availability) {
        TelemetryAvailability.LIVE -> NexonCyanPrimary
        TelemetryAvailability.STALE -> NexonAmberAccent
        else -> TextMuted
    }
    val progress = (animatedSpeed / 180f).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(214.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(204.dp)) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2f
            val diameter = size.minDimension - stroke
            val arcTopLeft = Offset(inset, inset)
            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)
            drawArc(
                color = DarkSurfaceVariant,
                startAngle = 134f,
                sweepAngle = 272f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (progress > 0f) {
                drawArc(
                    color = accent,
                    startAngle = 134f,
                    sweepAngle = 272f * progress,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "SPEED",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                letterSpacing = 2.2.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = validSpeed?.let { animatedSpeed.roundToInt().toString() } ?: "--",
                color = if (validSpeed == null) TextSecondary else TextPrimary,
                fontSize = 58.sp,
                lineHeight = 62.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-2).sp
            )
            Text(
                text = if (validSpeed == null) "Speed unavailable" else "km/h",
                style = MaterialTheme.typography.bodyMedium,
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(7.dp))
            CockpitStateLabel(
                text = if (validSpeed == null) availabilityDetail(availability) else speedSourceLabel(availability, source),
                color = accent
            )
        }
    }
}

@Composable
private fun TelemetryModule(
    title: String,
    value: String,
    detail: String,
    icon: ImageVector,
    state: TelemetryAvailability,
    modifier: Modifier = Modifier
) {
    val stateColor = stateColor(state)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface.copy(alpha = 0.72f),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder.copy(alpha = 0.72f))
    ) {
        Column(
            modifier = Modifier.padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = stateColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = if (value == "--") TextSecondary else TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = stateColor,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CockpitConnectionIndicator(
    label: String,
    color: Color,
    isActive: Boolean
) {
    val transition = rememberInfiniteTransition(label = "connection_glow")
    val alpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(DriveMateAnimations.pulseDurationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "connection_alpha"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = if (isActive) alpha else 1f))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun CockpitStateLabel(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DriveMateAssistantOrb(
    state: VoiceAssistantState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = state !is VoiceAssistantState.Idle
    val transition = rememberInfiniteTransition(label = "assistant_orb")
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "assistant_pulse"
    )
    val orbColor = when (state) {
        VoiceAssistantState.Listening -> NexonCyanPrimary
        is VoiceAssistantState.Processing -> NexonAmberAccent
        is VoiceAssistantState.Responding -> NexonEmeraldAccent
        is VoiceAssistantState.Error -> NexonRedAccent
        VoiceAssistantState.Idle -> NexonCyanPrimary
    }
    val stateText = when (state) {
        VoiceAssistantState.Idle -> "Tap to speak"
        VoiceAssistantState.Listening -> "Listening..."
        is VoiceAssistantState.Processing -> "Thinking..."
        is VoiceAssistantState.Responding -> "Speaking..."
        is VoiceAssistantState.Error -> "Try again"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, orbColor.copy(alpha = 0.30f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .scale(if (active) pulse else 1f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(orbColor.copy(alpha = 0.36f), orbColor.copy(alpha = 0.08f), Color.Transparent)
                        )
                    )
                    .border(1.dp, orbColor.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Ask DriveMate",
                    tint = orbColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ASK DRIVEMATE",
                    style = MaterialTheme.typography.labelSmall,
                    color = orbColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
            Text(
                text = "Speak",
                style = MaterialTheme.typography.labelLarge,
                color = orbColor
            )
        }
    }
}

@Composable
fun CockpitActionRail(
    onNavigate: () -> Unit,
    onVehicle: () -> Unit,
    onTrips: () -> Unit,
    onAssistant: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        CockpitAction("Navigate", Icons.Default.Navigation, onNavigate, Modifier.weight(1f))
        CockpitAction("Vehicle", Icons.Default.DirectionsCar, onVehicle, Modifier.weight(1f))
        CockpitAction("Trips", Icons.Default.Route, onTrips, Modifier.weight(1f))
        CockpitAction("Assistant", Icons.Default.AutoAwesome, onAssistant, Modifier.weight(1f))
    }
}

/** A premium, data-honest vehicle profile surface without a fabricated car render. */
@Composable
fun DriveMateVehicleIdentityPanel(
    settings: DriveMateSettings,
    modifier: Modifier = Modifier
) {
    val configured = settings.vehicleBrand.isNotBlank() || settings.vehicleModel.isNotBlank()
    val title = settings.fullVehicleName.takeUnless { it == "Connected vehicle" } ?: "MY VEHICLE"
    val subtitle = when {
        settings.vehicleRegistrationNumber.isNotBlank() -> settings.normalizedRegistrationNumber
        configured -> "Vehicle profile"
        else -> "Configure your vehicle profile"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .background(Brush.horizontalGradient(listOf(CardGradientStart, CardGradientEnd)))
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(NexonCyanPrimary.copy(alpha = 0.13f))
                    .border(1.dp, NexonCyanPrimary.copy(alpha = 0.30f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "Vehicle profile",
                    tint = NexonCyanPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "VEHICLE PROFILE",
                    style = MaterialTheme.typography.labelSmall,
                    color = NexonCyanPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.1.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            CockpitStateLabel(
                text = if (configured) "READY" else "SET UP",
                color = if (configured) NexonEmeraldAccent else NexonAmberAccent
            )
        }
    }
}

@Composable
private fun CockpitAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = DarkSurfaceVariant.copy(alpha = 0.82f),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 11.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = NexonCyanPrimary,
                modifier = Modifier.size(23.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun availabilityDetail(availability: TelemetryAvailability): String = when (availability) {
    TelemetryAvailability.LIVE -> "Live"
    TelemetryAvailability.STALE -> "Stale"
    TelemetryAvailability.NOT_SUPPORTED -> "Not supported"
    TelemetryAvailability.NOT_AUTHORIZED -> "Permission needed"
    TelemetryAvailability.NOT_CONNECTED -> "Not connected"
    TelemetryAvailability.COMING_SOON -> "Coming soon"
    TelemetryAvailability.UNAVAILABLE -> "Unavailable"
}

private fun speedSourceLabel(availability: TelemetryAvailability, source: TelemetrySource): String = when {
    availability == TelemetryAvailability.STALE -> "Stale • ${source.displayName}"
    source == TelemetrySource.PHONE_GPS -> "GPS"
    source == TelemetrySource.ANDROID_AUTO_CAR_HARDWARE -> "Vehicle"
    source == TelemetrySource.OBD2_BLE || source == TelemetrySource.OBD2_WIFI -> "OBD2"
    else -> availabilityDetail(availability)
}

private fun stateColor(availability: TelemetryAvailability): Color = when (availability) {
    TelemetryAvailability.LIVE -> NexonEmeraldAccent
    TelemetryAvailability.STALE -> NexonAmberAccent
    TelemetryAvailability.COMING_SOON -> NexonCyanPrimary
    else -> TextMuted
}

private fun odometerValue(settings: DriveMateSettings, telemetry: VehicleTelemetry): String {
    val value = when {
        telemetry.isAuthoritativeOdometer -> telemetry.vehicleOdometerKm
        else -> telemetry.manualOdometerKm ?: settings.manualOdometerKm ?: settings.odometerKm
    }
    return value?.let { String.format(Locale.US, "%,.0f km", it) } ?: "--"
}

private fun odometerDetail(settings: DriveMateSettings, telemetry: VehicleTelemetry): String = when {
    telemetry.isAuthoritativeOdometer -> if (telemetry.odometerAvailability == TelemetryAvailability.STALE) "Vehicle • stale" else "Vehicle"
    telemetry.manualOdometerKm != null || settings.manualOdometerKm != null || settings.odometerKm != null -> "Manual"
    else -> "Unavailable"
}

private fun odometerState(settings: DriveMateSettings, telemetry: VehicleTelemetry): TelemetryAvailability = when {
    telemetry.isAuthoritativeOdometer -> telemetry.odometerAvailability
    telemetry.manualOdometerKm != null || settings.manualOdometerKm != null || settings.odometerKm != null -> TelemetryAvailability.UNAVAILABLE
    else -> TelemetryAvailability.UNAVAILABLE
}
