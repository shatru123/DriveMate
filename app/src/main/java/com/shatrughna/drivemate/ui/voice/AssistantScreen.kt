package com.shatrughna.drivemate.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shatrughna.drivemate.ui.components.DriveMateAssistantOrb
import com.shatrughna.drivemate.ui.components.DriveMateCard
import com.shatrughna.drivemate.ui.components.DriveMateButton
import com.shatrughna.drivemate.ui.components.DriveMateButtonVariant
import com.shatrughna.drivemate.ui.home.MainViewModel
import com.shatrughna.drivemate.ui.theme.DarkBackground
import com.shatrughna.drivemate.ui.theme.DarkSurface
import com.shatrughna.drivemate.ui.theme.NexonCyanPrimary
import com.shatrughna.drivemate.ui.theme.TextMuted
import com.shatrughna.drivemate.ui.theme.TextPrimary
import com.shatrughna.drivemate.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.voiceAssistantState.collectAsStateWithLifecycle()
    var showVoiceSheet by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startVoiceAssistant()
        }
    }

    fun startExplicitVoiceInteraction() {
        showVoiceSheet = true
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.startVoiceAssistant()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Ask DriveMate",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Your AI co-driver",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(DarkBackground, DarkSurface, DarkBackground))
                )
                .padding(innerPadding)
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(14.dp))
            DriveMateAssistantOrb(
                state = state,
                onClick = ::startExplicitVoiceInteraction,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = when (state) {
                    com.shatrughna.drivemate.voice.VoiceAssistantState.Idle -> "How can I help you?"
                    com.shatrughna.drivemate.voice.VoiceAssistantState.Listening -> "Listening..."
                    is com.shatrughna.drivemate.voice.VoiceAssistantState.Processing -> "Thinking..."
                    is com.shatrughna.drivemate.voice.VoiceAssistantState.Responding -> "Speaking..."
                    is com.shatrughna.drivemate.voice.VoiceAssistantState.Error -> "Something went wrong"
                },
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Tap the assistant to speak, or choose a safe shortcut below.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            val suggestions = listOf(
                "Navigate to home",
                "Find nearest petrol pump",
                "Show my last trip",
                "How is my vehicle data?"
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                suggestions.forEach { suggestion ->
                    DriveMateCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.processVoiceTextCommand(suggestion) },
                        containerColor = DarkSurface
                    ) {
                        Text(
                            text = suggestion,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                    }
                }
            }

            DriveMateButton(
                text = "Tap to speak",
                onClick = ::startExplicitVoiceInteraction,
                icon = Icons.Default.AutoAwesome,
                modifier = Modifier.fillMaxWidth(),
                variant = DriveMateButtonVariant.PRIMARY
            )
            Spacer(modifier = Modifier.height(92.dp))
        }

        if (showVoiceSheet) {
            VoiceAssistantSheet(
                state = state,
                onStartListening = ::startExplicitVoiceInteraction,
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
