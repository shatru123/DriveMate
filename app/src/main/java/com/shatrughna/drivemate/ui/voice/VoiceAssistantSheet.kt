package com.shatrughna.drivemate.ui.voice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shatrughna.drivemate.ui.components.DriveMateButton
import com.shatrughna.drivemate.ui.components.DriveMateButtonVariant
import com.shatrughna.drivemate.ui.components.rememberPressScale
import com.shatrughna.drivemate.ui.theme.DarkBorder
import com.shatrughna.drivemate.ui.theme.DarkSurface
import com.shatrughna.drivemate.ui.theme.DarkSurfaceVariant
import com.shatrughna.drivemate.ui.theme.NexonCyanGlow
import com.shatrughna.drivemate.ui.theme.NexonCyanPrimary
import com.shatrughna.drivemate.ui.theme.NexonEmeraldAccent
import com.shatrughna.drivemate.ui.theme.NexonRedAccent
import com.shatrughna.drivemate.ui.theme.TextMuted
import com.shatrughna.drivemate.ui.theme.TextPrimary
import com.shatrughna.drivemate.ui.theme.TextSecondary
import com.shatrughna.drivemate.voice.VoiceAssistantState

/**
 * Premium Automotive Voice Assistant Bottom Sheet.
 *
 * Features:
 * 1. Multi-tier animated audio waveform visualizer for acoustic listening state.
 * 2. Distinct states: Idle, Listening, Processing, Responding, Error.
 * 3. Tactile suggestion chips with quick actions.
 * 4. Text command input fallback with micro-interactions.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VoiceAssistantSheet(
    state: VoiceAssistantState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onProcessTextCommand: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var textInput by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F1522),
        contentColor = TextPrimary,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DriveMate Voice Assistant",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Hands-free automotive speech engine",
                        style = MaterialTheme.typography.labelSmall,
                        color = NexonCyanPrimary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.rememberPressScale()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Animated Microphone & Multi-tier Acoustic Waveform Visualizer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(130.dp)
            ) {
                // Ambient Glow Aura
                if (state is VoiceAssistantState.Listening) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(pulseScale)
                            .background(NexonCyanGlow, CircleShape)
                    )
                } else if (state is VoiceAssistantState.Responding) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(pulseScale)
                            .background(NexonEmeraldAccent.copy(alpha = 0.20f), CircleShape)
                    )
                }

                val micBg = when (state) {
                    is VoiceAssistantState.Listening -> NexonCyanPrimary
                    is VoiceAssistantState.Responding -> NexonEmeraldAccent
                    is VoiceAssistantState.Error -> NexonRedAccent
                    is VoiceAssistantState.Processing -> Color(0xFF2563EB)
                    else -> DarkSurfaceVariant
                }

                val micContentColor = when (state) {
                    is VoiceAssistantState.Listening -> Color.Black
                    is VoiceAssistantState.Responding -> Color.Black
                    else -> Color.White
                }

                Surface(
                    shape = CircleShape,
                    color = micBg,
                    modifier = Modifier
                        .size(80.dp)
                        .rememberPressScale()
                        .clickable {
                            if (state is VoiceAssistantState.Listening) {
                                onStopListening()
                            } else {
                                onStartListening()
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        when (state) {
                            is VoiceAssistantState.Processing -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(34.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                            }
                            is VoiceAssistantState.Responding -> {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Speaking",
                                    tint = micContentColor,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Microphone",
                                    tint = micContentColor,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Waveform Audio Pulse Bars (Active when Listening)
            AnimatedVisibility(visible = state is VoiceAssistantState.Listening) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(10.dp))
                    AudioWaveformPulseBars()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // State Description / Transcript
            when (state) {
                VoiceAssistantState.Idle -> {
                    Text(
                        text = "Tap the microphone to speak or type a command below",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
                VoiceAssistantState.Listening -> {
                    Text(
                        text = "Listening for your voice...",
                        style = MaterialTheme.typography.titleMedium,
                        color = NexonCyanPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                is VoiceAssistantState.Processing -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Processing command...",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"${state.recognizedText}\"",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is VoiceAssistantState.Responding -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, NexonEmeraldAccent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = NexonEmeraldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DriveMate Response",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NexonEmeraldAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = state.speechText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }
                }
                is VoiceAssistantState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Voice input is unavailable. Check microphone permission and try again.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NexonRedAccent,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        DriveMateButton(
                            text = "Try Again",
                            onClick = onStartListening,
                            variant = DriveMateButtonVariant.SECONDARY,
                            icon = Icons.Default.Refresh
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Text Input Fallback
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Ask something (e.g. Navigate to airport)", color = TextMuted) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NexonCyanPrimary,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        cursorColor = NexonCyanPrimary
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            onProcessTextCommand(textInput)
                            textInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(NexonCyanPrimary, CircleShape)
                        .rememberPressScale()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Suggestions Chips
            Text(
                text = "TRY ASKING:",
                style = MaterialTheme.typography.labelSmall,
                color = NexonCyanPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            val suggestions = listOf(
                "🎵 Play Believer on Spotify",
                "🧭 Directions to Home",
                "⛽ Nearest petrol pump",
                "📊 Trip status",
                "🌤️ Weather report",
                "🚗 Where did I park?",
                "🛠️ When is next service?"
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestions.forEach { prompt ->
                    VoiceSuggestionChip(text = prompt) {
                        val clean = prompt.substringAfter(" ")
                        onProcessTextCommand(clean)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 5-bar animated acoustic waveform reacting in real-time.
 */
@Composable
private fun AudioWaveformPulseBars() {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    val bar1 by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 26f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 14f,
        targetValue = 34f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(tween(280, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar3"
    )
    val bar4 by infiniteTransition.animateFloat(
        initialValue = 16f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(tween(460, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar4"
    )
    val bar5 by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(tween(320, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar5"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(44.dp)
    ) {
        listOf(bar1, bar2, bar3, bar4, bar5).forEach { barHeight ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(barHeight.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(NexonCyanPrimary, NexonCyanPrimary.copy(alpha = 0.5f))
                        )
                    )
            )
        }
    }
}

@Composable
private fun VoiceSuggestionChip(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
            .rememberPressScale()
            .clickable { onClick() }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
