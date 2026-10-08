package com.shatrughna.drivemate.ui.components

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shatrughna.drivemate.ui.theme.DarkBorder
import com.shatrughna.drivemate.ui.theme.DarkSurface
import com.shatrughna.drivemate.ui.theme.NexonCyanPrimary
import com.shatrughna.drivemate.ui.theme.TextMuted
import com.shatrughna.drivemate.ui.theme.TextPrimary
import com.shatrughna.drivemate.util.VehiclePhotoLoader

/**
 * High-fidelity fullscreen zoomable photo lightbox with:
 * - Pinch-to-zoom gesture (1x - 5x)
 * - 2D drag panning with soft boundaries
 * - Double-tap to toggle between 1x and 2.5x zoom
 * - On-screen quick zoom controls (+, -, reset) and scale percentage indicator
 * - Header with title, subtitle and close button
 * - Back button dismissal support
 */
@Composable
fun ZoomablePhotoViewerDialog(
    title: String,
    subtitle: String? = null,
    imageBitmap: ImageBitmap? = null,
    @DrawableRes drawableRes: Int? = null,
    imageUri: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Resolve ImageBitmap if imageUri is provided and imageBitmap is null
    val loadedBitmap by produceState<ImageBitmap?>(initialValue = imageBitmap, imageUri) {
        if (imageBitmap != null) {
            value = imageBitmap
        } else if (!imageUri.isNullOrBlank()) {
            value = VehiclePhotoLoader.loadOptimizedBitmap(context, imageUri, maxDimension = 1920)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        BackHandler(onBack = onDismiss)

        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
        val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

        fun updateScale(newScale: Float) {
            val clamped = newScale.coerceIn(1f, 5f)
            scale = clamped
            if (clamped == 1f) {
                offsetX = 0f
                offsetY = 0f
            } else {
                val maxOffsetX = (screenWidthPx * (clamped - 1f)) / 2f
                val maxOffsetY = (screenHeightPx * (clamped - 1f)) / 2f
                offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
        ) {
            // Main Zoomable Image Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1.05f) {
                                    updateScale(1f)
                                } else {
                                    updateScale(2.5f)
                                }
                            },
                            onTap = {
                                if (scale <= 1.05f) {
                                    onDismiss()
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale

                            if (newScale > 1f) {
                                val maxOffsetX = (screenWidthPx * (newScale - 1f)) / 2f
                                val maxOffsetY = (screenHeightPx * (newScale - 1f)) / 2f
                                offsetX = (offsetX + pan.x * newScale).coerceIn(-maxOffsetX, maxOffsetX)
                                offsetY = (offsetY + pan.y * newScale).coerceIn(-maxOffsetY, maxOffsetY)
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                val imageModifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )

                when {
                    loadedBitmap != null -> {
                        Image(
                            bitmap = loadedBitmap!!,
                            contentDescription = title,
                            contentScale = ContentScale.Fit,
                            modifier = imageModifier
                        )
                    }
                    drawableRes != null -> {
                        Image(
                            painter = painterResource(id = drawableRes),
                            contentDescription = title,
                            contentScale = ContentScale.Fit,
                            modifier = imageModifier
                        )
                    }
                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = NexonCyanPrimary,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Loading photo...",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Top App Bar / Header
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = NexonCyanPrimary
                            )
                        }
                    }

                    // Close Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .background(DarkSurface.copy(alpha = 0.85f), CircleShape)
                            .border(1.dp, DarkBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close photo viewer",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Bottom Controls Bar (Zoom In, Zoom Out, Reset, Scale Pill)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkSurface.copy(alpha = 0.90f),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Zoom Out Button
                    IconButton(
                        onClick = { updateScale(scale - 0.5f) },
                        enabled = scale > 1.05f,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Zoom Out",
                            tint = if (scale > 1.05f) TextPrimary else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Scale Percentage Pill
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NexonCyanPrimary.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NexonCyanPrimary.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "${(scale * 100).toInt()}%",
                            color = NexonCyanPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    // Zoom In Button
                    IconButton(
                        onClick = { updateScale(scale + 0.5f) },
                        enabled = scale < 4.95f,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Zoom In",
                            tint = if (scale < 4.95f) TextPrimary else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Reset Button (if zoomed)
                    if (scale > 1.05f) {
                        IconButton(
                            onClick = { updateScale(1f) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = "Reset Zoom",
                                tint = NexonCyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
