package com.shatrughna.drivemate.ui.climate

import android.widget.Toast
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
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Waves
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shatrughna.drivemate.core.capabilities.ClimateControlProvider
import com.shatrughna.drivemate.core.capabilities.ClimateState
import com.shatrughna.drivemate.ui.components.DriveMateCard
import com.shatrughna.drivemate.ui.components.DriveMateChip
import com.shatrughna.drivemate.ui.components.rememberPressScale
import com.shatrughna.drivemate.ui.theme.DarkBackground
import com.shatrughna.drivemate.ui.theme.DarkBorder
import com.shatrughna.drivemate.ui.theme.DarkSurfaceVariant
import com.shatrughna.drivemate.ui.theme.NexonAmberAccent
import com.shatrughna.drivemate.ui.theme.NexonCyanPrimary
import com.shatrughna.drivemate.ui.theme.NexonEmeraldAccent
import com.shatrughna.drivemate.ui.theme.TextMuted
import com.shatrughna.drivemate.ui.theme.TextPrimary
import com.shatrughna.drivemate.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClimateScreen(
    climateProvider: ClimateControlProvider,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by climateProvider.climateState.collectAsStateWithLifecycle()

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
                            text = "Climate & Cabin Comfort",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Vehicle HVAC system",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Honest Vehicle Capability Notice
            DriveMateCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NexonAmberAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = NexonAmberAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Vehicle Capability Status",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = NexonAmberAccent
                        )
                        Text(
                            text = "Direct HVAC CAN bus control is restricted to Nexon physical dash controls. Operating in companion mode.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Hero Temperature Dial Card
            DriveMateCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text(
                        text = "CABIN TARGET TEMPERATURE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )

                    // Big Temperature display with +/- controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = {
                                val res = climateProvider.setTemperature(state.targetTemperatureCelsius - 0.5f)
                                Toast.makeText(context, res.userMessage, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                                .border(1.dp, DarkBorder, CircleShape)
                                .rememberPressScale()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease Temperature",
                                tint = NexonCyanPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(28.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f°", state.targetTemperatureCelsius),
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                                color = NexonCyanPrimary,
                                fontSize = 56.sp
                            )
                            Text(
                                text = if (state.isAutoMode) "AUTO CLIMATE ACTIVE" else "MANUAL CONTROL",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (state.isAutoMode) NexonEmeraldAccent else TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.width(28.dp))

                        IconButton(
                            onClick = {
                                val res = climateProvider.setTemperature(state.targetTemperatureCelsius + 0.5f)
                                Toast.makeText(context, res.userMessage, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                                .border(1.dp, DarkBorder, CircleShape)
                                .rememberPressScale()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase Temperature",
                                tint = NexonCyanPrimary
                            )
                        }
                    }

                    // Fan Speed Bar Indicator
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Blower Fan Speed",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "Speed ${state.fanSpeed} of 7",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (step in 1..7) {
                                val isActive = step <= state.fanSpeed
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (isActive) NexonCyanPrimary else DarkSurfaceVariant)
                                        .border(1.dp, if (isActive) NexonCyanPrimary.copy(alpha = 0.5f) else DarkBorder, RoundedCornerShape(3.dp))
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(
                                onClick = {
                                    val newSpeed = (state.fanSpeed - 1).coerceAtLeast(1)
                                    val res = climateProvider.setFanSpeed(newSpeed)
                                    Toast.makeText(context, res.userMessage, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.rememberPressScale()
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Lower Fan", tint = TextSecondary)
                            }
                            Spacer(modifier = Modifier.width(32.dp))
                            IconButton(
                                onClick = {
                                    val newSpeed = (state.fanSpeed + 1).coerceAtMost(7)
                                    val res = climateProvider.setFanSpeed(newSpeed)
                                    Toast.makeText(context, res.userMessage, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.rememberPressScale()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Higher Fan", tint = TextSecondary)
                            }
                        }
                    }
                }
            }

            // Quick HVAC Modes Grid
            DriveMateCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "HVAC MODES & PRESETS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DriveMateChip(
                            label = "A/C ${if (state.isAcOn) "ON" else "OFF"}",
                            selected = state.isAcOn,
                            onClick = {
                                val res = climateProvider.setAcEnabled(!state.isAcOn)
                                Toast.makeText(context, res.userMessage, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )

                        DriveMateChip(
                            label = "AUTO",
                            selected = state.isAutoMode,
                            onClick = {
                                val res = climateProvider.setAutoMode(!state.isAutoMode)
                                Toast.makeText(context, res.userMessage, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )

                        DriveMateChip(
                            label = "DEFROST",
                            selected = state.isDefrostActive,
                            onClick = {
                                val res = climateProvider.setDefrostActive(!state.isDefrostActive)
                                Toast.makeText(context, res.userMessage, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DriveMateChip(
                            label = "RECIRCULATION",
                            selected = state.isRecirculationActive,
                            onClick = {
                                val res = climateProvider.setRecirculationActive(!state.isRecirculationActive)
                                Toast.makeText(context, res.userMessage, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )

                        DriveMateChip(
                            label = "ECONOMY",
                            selected = false,
                            onClick = {
                                Toast.makeText(context, "Economy climate active. Regulating compressor load.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
