package com.shatrughna.drivemate.ui.home

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shatrughna.drivemate.ui.components.ActiveTripTickerCard
import com.shatrughna.drivemate.ui.components.ConnectionStatusCard
import com.shatrughna.drivemate.ui.components.CreatorCard
import com.shatrughna.drivemate.ui.components.DailyDrivingStatsCard
import com.shatrughna.drivemate.ui.components.GreetingStatusCard
import com.shatrughna.drivemate.ui.components.SmartDestinationRow
import com.shatrughna.drivemate.ui.components.VehicleCareSummaryCard
import com.shatrughna.drivemate.ui.components.VehicleHeaderCard
import com.shatrughna.drivemate.ui.components.WeatherSummaryCard
import com.shatrughna.drivemate.ui.theme.DarkBackground
import com.shatrughna.drivemate.ui.theme.NexonCyanPrimary
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.shatrughna.drivemate.ui.theme.NexonEmeraldAccent
import com.shatrughna.drivemate.ui.theme.TextMuted
import com.shatrughna.drivemate.ui.theme.TextPrimary
import com.shatrughna.drivemate.ui.theme.TextSecondary
import com.shatrughna.drivemate.ui.voice.VoiceAssistantSheet
import com.shatrughna.drivemate.ui.trip.TripReportCard
import com.shatrughna.drivemate.ui.parking.FindMyCarCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val isSessionActive by viewModel.isSessionActive.collectAsStateWithLifecycle()
    val hasGreetingPlayed by viewModel.hasGreetingPlayed.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val currentGreetingText by viewModel.currentGreetingText.collectAsStateWithLifecycle()
    val isSimulating by viewModel.isSimulating.collectAsStateWithLifecycle()
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val tripStats by viewModel.tripStats.collectAsStateWithLifecycle()
    val destinations by viewModel.suggestedDestinations.collectAsStateWithLifecycle()
    val careInfo by viewModel.vehicleCareInfo.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceAssistantState.collectAsStateWithLifecycle()
    val latestTrip by viewModel.latestTrip.collectAsStateWithLifecycle()

    var showVoiceSheet by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.refreshWeather(forceRefresh = true)
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startVoiceAssistant()
        }
    }

    LaunchedEffect(settings.autoDetectLocation) {
        if (settings.autoDetectLocation && !viewModel.hasLocationPermission()) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
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
                },
                containerColor = Color(0xFF3B82F6),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "DriveMate Voice Assistant",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                ),
                title = {
                    Column {
                        Text(
                            text = "DriveMate",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = NexonCyanPrimary
                        )
                        Text(
                            text = "Your Personal Driving Companion",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Driver Greeting banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Welcome, ${settings.driverName} 👋",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            // Active Driving Session Banner / Ticker
            AnimatedVisibility(visible = isSessionActive) {
                ActiveTripTickerCard(tripStats = tripStats)
            }

            // 1. Vehicle info card
            VehicleHeaderCard(settings = settings)

            // 2. Weather card (V2)
            WeatherSummaryCard(
                weather = weather,
                onRefreshWeather = {
                    if (settings.autoDetectLocation && !viewModel.hasLocationPermission()) {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    } else {
                        viewModel.refreshWeather(forceRefresh = true)
                    }
                }
            )

            // 3. Android Auto / Car Connection card
            ConnectionStatusCard(
                connectionState = connectionState,
                isSimulating = isSimulating,
                onToggleSimulation = viewModel::toggleSimulation
            )

            // 4. Smart Suggested Destinations (V2)
            if (destinations.isNotEmpty()) {
                SmartDestinationRow(
                    destinations = destinations,
                    onSelectDestination = { dest -> viewModel.launchDestination(context, dest) }
                )
            }

            // 5. Greeting card with active quote & preview
            GreetingStatusCard(
                settings = settings,
                currentGreetingText = currentGreetingText,
                isSpeaking = isSpeaking,
                onToggleGreetingEnabled = viewModel::toggleGreetingEnabled,
                onPreviewGreeting = viewModel::previewGreeting,
                onStopGreeting = viewModel::stopSpeaking
            )

            // 6. Today's drive live tracking card (V2)
            DailyDrivingStatsCard(tripStats = tripStats)

            // 7. Recent Completed Trip & Route Report
            latestTrip?.let { trip ->
                TripReportCard(tripReport = trip)
            }

            // 8. Find My Car (Parking Location)
            if (settings.hasParkedLocation) {
                FindMyCarCard(
                    settings = settings,
                    onNavigateToCar = { viewModel.navigateToParkedCar(context) }
                )
            }

            // 9. Vehicle Care & Service status card (V2)
            VehicleCareSummaryCard(careInfo = careInfo)

            // 10. Creator Profile & Contact
            CreatorCard()

            Spacer(modifier = Modifier.height(80.dp))
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
