package com.shatrughna.drivemate.ui.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shatrughna.drivemate.core.telemetry.TelemetryAvailability
import com.shatrughna.drivemate.core.telemetry.VehicleTelemetry
import com.shatrughna.drivemate.ui.components.DriveMateCard
import com.shatrughna.drivemate.ui.components.rememberPressScale
import com.shatrughna.drivemate.ui.theme.DarkBackground
import com.shatrughna.drivemate.ui.theme.DarkBorder
import com.shatrughna.drivemate.ui.theme.DarkSurfaceVariant
import com.shatrughna.drivemate.ui.theme.NexonAmberAccent
import com.shatrughna.drivemate.ui.theme.NexonCyanPrimary
import com.shatrughna.drivemate.ui.theme.NexonEmeraldAccent
import com.shatrughna.drivemate.ui.theme.NexonRedAccent
import com.shatrughna.drivemate.ui.theme.TextMuted
import com.shatrughna.drivemate.ui.theme.TextPrimary
import com.shatrughna.drivemate.ui.theme.TextSecondary
import com.shatrughna.drivemate.voice.AudioOwnerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleConnectionDiagnosticsScreen(
    telemetry: VehicleTelemetry,
    isCarConnected: Boolean,
    audioState: AudioOwnerState,
    onNavigateBack: () -> Unit,
    onRefresh: () -> Unit,
    onCopyDiagnostics: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.rememberPressScale()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Connection Diagnostics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Hardware APIs • Telemetry • Audio Stability",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onCopyDiagnostics,
                        modifier = Modifier.rememberPressScale()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy diagnostics", tint = NexonCyanPrimary)
                    }
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.rememberPressScale()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Diagnostics",
                            tint = NexonCyanPrimary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // 1. Android Auto Connection & Release Blocker Guard Card
            DriveMateCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isCarConnected) NexonEmeraldAccent else TextMuted)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ANDROID AUTO PROJECTION",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                        }
                        StatusPill(
                            text = if (isCarConnected) "CONNECTED" else "STANDBY",
                            color = if (isCarConnected) NexonEmeraldAccent else TextMuted
                        )
                    }

                    DiagnosticRow(
                        label = "Connection Mode",
                        value = if (isCarConnected) "Passive Car Companion" else "Phone Standalone",
                        detail = "No auto-mic hijacking • Zero audio focus grabs on connect"
                    )

                    DiagnosticRow(
                        label = "Wake-Word Policy",
                        value = if (isCarConnected) "DisabledWakeWordEngine" else "Phone Wake-Word",
                        detail = if (isCarConnected) "Continuous mic strictly suppressed to protect Spotify" else "Listening for \"Hey DriveMate\""
                    )
                }
            }

            // 2. Real Vehicle Telemetry Streams
            DriveMateCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = NexonCyanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "VEHICLE TELEMETRY (CAR HARDWARE)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                        }
                        StatusPill(
                            text = if (telemetry.vehicleTelemetryConnected) "AVAILABLE" else "UNAVAILABLE",
                            color = if (telemetry.vehicleTelemetryConnected) NexonCyanPrimary else TextMuted
                        )
                    }

                    DiagnosticRow(
                        label = "Live Speed",
                        value = if (telemetry.speedAvailability == TelemetryAvailability.LIVE || telemetry.speedAvailability == TelemetryAvailability.STALE) {
                            telemetry.speedKmh?.let { String.format("%.0f km/h", it) } ?: "Unavailable"
                        } else "Unavailable",
                        detail = "Source: ${telemetry.speedSource.displayName} • ${telemetry.speedAvailability.label} • ${telemetry.speedTimestampMillis?.let { "updated ${((System.currentTimeMillis() - it) / 1000).coerceAtLeast(0)}s ago" } ?: "no update"}"
                    )

                    DiagnosticRow(
                        label = "Authoritative Odometer",
                        value = if (telemetry.odometerAvailability == TelemetryAvailability.LIVE || telemetry.odometerAvailability == TelemetryAvailability.STALE) {
                            telemetry.vehicleOdometerKm?.let { String.format("%,.1f km", it) } ?: "Unavailable"
                        } else "Unavailable",
                        detail = "Source: ${telemetry.odometerSource.displayName} • ${telemetry.odometerAvailability.label}"
                    )

                    DiagnosticRow(
                        label = "Calibrated Odometer Fallback",
                        value = telemetry.manualOdometerKm?.let { String.format("%,.1f km", it) } ?: "Not configured",
                        detail = "Manual calibration from settings"
                    )

                    DiagnosticRow(
                        label = "Current Trip (GPS)",
                        value = telemetry.tripGpsDistanceKm.takeIf { it > 0.0 }?.let { String.format("%.2f km", it) } ?: "No active trip",
                        detail = "Isolated GPS distance • Never alters vehicle odometer"
                    )

                    DiagnosticRow(
                        label = "Fuel / Range",
                        value = "${telemetry.fuelLevelPercent?.takeIf { telemetry.fuelAvailability == TelemetryAvailability.LIVE }?.let { String.format("%.0f%%", it) } ?: "Fuel unavailable"} • " +
                            "${telemetry.rangeRemainingKm?.takeIf { telemetry.rangeAvailability == TelemetryAvailability.LIVE }?.let { String.format("%.0f km", it) } ?: "Range unavailable"}",
                        detail = "Fuel: ${telemetry.fuelSource.displayName} (${telemetry.fuelAvailability.label}) • Range: ${telemetry.fuelSource.displayName} (${telemetry.rangeAvailability.label})"
                    )
                }
            }

            // 3. Sensor & Subsystem Honesty Card
            DriveMateCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "SENSOR SUBSYSTEM HONESTY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )

                    DiagnosticRow(
                        label = "TPMS (Tire Pressure)",
                        value = "Unavailable through Android Auto",
                        detail = "Factory wheel sensors display on the instrument cluster. No fake PSI is shown."
                    )

                    DiagnosticRow(
                        label = "Ultrasonic Parking Radar",
                        value = "OEM Infotainment Native",
                        detail = "Direct bumper sensor proximity arcs are restricted by Android Auto. Zero fake arcs."
                    )

                    DiagnosticRow(
                        label = "OBD2 Diagnostics",
                        value = "Not Connected (Standby)",
                        detail = "Ready for BLE / Wi-Fi ELM327 scanner adapter."
                    )

                    DiagnosticRow(
                        label = "Climate (HVAC) Actuation",
                        value = "Read-Only Companion",
                        detail = "Direct CAN bus write restricted. Physical console dials retain vehicle control."
                    )
                }
            }

            // 4. Audio Focus State Machine Card
            DriveMateCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = NexonAmberAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AUDIO FOCUS STATE MACHINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                        }
                        StatusPill(
                            text = audioState.name,
                            color = when (audioState) {
                                AudioOwnerState.IDLE -> NexonEmeraldAccent
                                AudioOwnerState.RECORDING -> NexonRedAccent
                                AudioOwnerState.TTS -> NexonCyanPrimary
                                else -> NexonAmberAccent
                            }
                        )
                    }

                    DiagnosticRow(
                        label = "Mutual Exclusion",
                        value = if (audioState == AudioOwnerState.IDLE) "Focus Released to Media" else "Transient Active (${audioState.name})",
                        detail = "Spotify / YouTube Music retain uninterrupted playback during car connection"
                    )

                    DiagnosticRow(
                        label = "TTS Audio Stream",
                        value = "USAGE_ASSISTANCE_NAVIGATION_GUIDANCE",
                        detail = "Transient ducking only • Focus abandoned immediately upon utterance completion"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DiagnosticRow(
    label: String,
    value: String,
    detail: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = NexonCyanPrimary
            )
        }
        Text(
            text = detail,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }
}

@Composable
private fun StatusPill(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.8.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
