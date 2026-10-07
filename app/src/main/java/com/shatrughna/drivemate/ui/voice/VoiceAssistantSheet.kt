package com.shatrughna.drivemate.ui.voice

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shatrughna.drivemate.voice.VoiceAssistantState

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
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF131722),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DriveMate Voice Assistant",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Animated Microphone / Status Visualizer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(110.dp)
            ) {
                if (state is VoiceAssistantState.Listening) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(pulseScale)
                            .background(Color(0xFF3B82F6).copy(alpha = 0.25f), CircleShape)
                    )
                }

                val micColor = when (state) {
                    is VoiceAssistantState.Listening -> Color(0xFF3B82F6)
                    is VoiceAssistantState.Responding -> Color(0xFF10B981)
                    is VoiceAssistantState.Error -> Color(0xFFEF4444)
                    else -> Color(0xFF1E293B)
                }

                Surface(
                    shape = CircleShape,
                    color = micColor,
                    modifier = Modifier
                        .size(76.dp)
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
                            is VoiceAssistantState.Responding -> {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Speaking",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Microphone",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // State Description / Transcript
            when (state) {
                VoiceAssistantState.Idle -> {
                    Text(
                        text = "Tap the microphone to speak or type a command below",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                }
                VoiceAssistantState.Listening -> {
                    Text(
                        text = "Listening for your voice...",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF60A5FA),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                is VoiceAssistantState.Processing -> {
                    Text(
                        text = "\"${state.recognizedText}\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
                is VoiceAssistantState.Responding -> {
                    Text(
                        text = state.speechText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF34D399),
                        textAlign = TextAlign.Center
                    )
                }
                is VoiceAssistantState.Error -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFF87171),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Text Input fallback
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Ask something (e.g. Play Believer)", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
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
                        .background(Color(0xFF3B82F6), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Suggestions Chips
            Text(
                text = "Try saying:",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(8.dp))

            val suggestions = listOf(
                "🎵 Play Believer on Spotify",
                "🧭 Directions to Home",
                "⛽ Nearest petrol pump",
                "📊 Trip status",
                "🌦️ Weather report",
                "🚗 Where did I park?",
                "🛠️ When is next service?"
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestions.forEach { prompt ->
                    SuggestionChip(text = prompt) {
                        val clean = prompt.substringAfter(" ")
                        onProcessTextCommand(clean)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E293B),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFCBD5E1),
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
