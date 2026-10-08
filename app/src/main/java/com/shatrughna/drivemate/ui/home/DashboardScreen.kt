package com.shatrughna.drivemate.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shatrughna.drivemate.ui.components.CockpitActionRail
import com.shatrughna.drivemate.ui.components.DriveMateCockpit
import com.shatrughna.drivemate.ui.components.DriveMateCurrentDriveSummary
import com.shatrughna.drivemate.ui.components.DriveMateStatusBadge
import com.shatrughna.drivemate.ui.components.DynamicDestinationSearchCard
import com.shatrughna.drivemate.ui.components.rememberPressScale
import com.shatrughna.drivemate.ui.theme.DarkBackground
import com.shatrughna.drivemate.ui.theme.DarkSurface
import com.shatrughna.drivemate.ui.theme.NexonEmeraldAccent
import com.shatrughna.drivemate.ui.theme.NexonCyanPrimary
import com.shatrughna.drivemate.ui.theme.TextMuted
import com.shatrughna.drivemate.ui.theme.TextPrimary
import com.shatrughna.drivemate.ui.theme.TextSecondary
import com.shatrughna.drivemate.ui.voice.VoiceAssistantSheet

/**
 * Automotive Command Center Phone Dashboard.
 *
 * Visual hierarchy:
 * 1. Driver context and connection state
 * 2. Truthful telemetry cockpit
 * 3. Explicit assistant and driver actions
 * 4. Navigation discovery
 * 5. Current-drive journal summary
 * Secondary vehicle utilities remain behind Vehicle / My Car.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToMyCar: () -> Unit = {},
    onNavigateToDocuments: () -> Unit = {},
    onNavigateToMaintenance: () -> Unit = {},
    onNavigateToExpenses: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToParking: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {},
    onNavigateToAssistant: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val isSessionActive by viewModel.isSessionActive.collectAsStateWithLifecycle()
    val tripStats by viewModel.tripStats.collectAsStateWithLifecycle()
    val destinations by viewModel.suggestedDestinations.collectAsStateWithLifecycle()
    val recentDestinations by viewModel.recentDestinations.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceAssistantState.collectAsStateWithLifecycle()

    var showVoiceSheet by remember { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startVoiceAssistant()
        }
    }

    fun launchVoiceAssistant() {
        showVoiceSheet = true
        val hasMicPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasMicPermission) {
            viewModel.startVoiceAssistant()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { launchVoiceAssistant() },
                containerColor = NexonCyanPrimary,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.rememberPressScale()
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "DriveMate Voice Assistant",
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Ask DriveMate",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = TextPrimary
                ),
                title = {
                    Column {
                        Text(
                            text = "DriveMate",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = NexonCyanPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "Your driving companion • ${settings.fullVehicleName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.rememberPressScale()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkBackground,
                            DarkSurface,
                            DarkBackground
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(NexonCyanPrimary.copy(alpha = 0.12f), Color.Transparent)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Driver Greeting banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (settings.driverName.isBlank()) "Welcome back" else "Welcome back, ${settings.driverName}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (settings.fullVehicleName == "Connected vehicle") "Set up your vehicle to get started" else "Everything ready for ${settings.fullVehicleName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    DriveMateStatusBadge(
                        text = when {
                            isSessionActive -> "On the road"
                            connectionState.isConnected -> "Connected"
                            else -> "Ready"
                        },
                        isActive = isSessionActive || connectionState.isConnected,
                        activeColor = NexonEmeraldAccent,
                        pulse = isSessionActive
                    )
                }

                // 1. Truthful telemetry-first cockpit hero
                DriveMateCockpit(
                    settings = settings,
                    telemetry = telemetry,
                    tripStats = tripStats,
                    isSessionActive = isSessionActive,
                    voiceState = voiceState,
                    onAssistantClick = { launchVoiceAssistant() },
                    onDiagnosticsClick = onNavigateToDiagnostics
                )

                // 2. Focused driver actions
                CockpitActionRail(
                    onNavigate = {
                        viewModel.searchAndLaunchDestination(context, "Nearby Petrol Pump")
                    },
                    onVehicle = onNavigateToMyCar,
                    onTrips = onNavigateToAnalytics,
                    onAssistant = onNavigateToAssistant
                )

                // 3. Navigation stays available without overwhelming the cockpit.
                DynamicDestinationSearchCard(
                    suggestedDestinations = destinations,
                    recentDestinations = recentDestinations,
                    onSearchDestination = { query -> viewModel.searchAndLaunchDestination(context, query) },
                    onSelectDestination = { dest -> viewModel.launchDestination(context, dest) }
                )

                // 4. Current drive summary is the single live journal module on Home.
                DriveMateCurrentDriveSummary(
                    tripStats = tripStats,
                    isSessionActive = isSessionActive
                )

                Spacer(modifier = Modifier.height(92.dp))
            }
        }

        if (showVoiceSheet) {
            VoiceAssistantSheet(
                state = voiceState,
                onStartListening = {
                    val hasMicPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasMicPermission) {
                        viewModel.startVoiceAssistant()
                    } else {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopListening = viewModel::stopVoiceAssistant,
                onProcessTextCommand = viewModel::processVoiceTextCommand,
                onDismiss = {
                    viewModel.stopVoiceAssistant()
                    showVoiceSheet = false
                }
            )
        }
    }
}
