package com.shatrughna.drivemate.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shatrughna.drivemate.ui.theme.CardGradientEnd
import com.shatrughna.drivemate.ui.theme.CardGradientStart
import com.shatrughna.drivemate.ui.theme.DarkBorder
import com.shatrughna.drivemate.ui.theme.DarkSurface
import com.shatrughna.drivemate.ui.theme.DarkSurfaceVariant
import com.shatrughna.drivemate.ui.theme.NexonAmberAccent
import com.shatrughna.drivemate.ui.theme.NexonCyanGlow
import com.shatrughna.drivemate.ui.theme.NexonCyanPrimary
import com.shatrughna.drivemate.ui.theme.NexonEmeraldAccent
import com.shatrughna.drivemate.ui.theme.NexonRedAccent
import com.shatrughna.drivemate.ui.theme.TextMuted
import com.shatrughna.drivemate.ui.theme.TextPrimary
import com.shatrughna.drivemate.ui.theme.TextSecondary

/**
 * DriveMate Automotive Gradient Presets.
 */
object DriveMateGradient {
    val cardSurface = Brush.verticalGradient(
        colors = listOf(CardGradientStart, CardGradientEnd)
    )

    val heroVehicle = Brush.verticalGradient(
        colors = listOf(
            CardGradientStart,
            Color(0xFF10192A),
            CardGradientEnd
        )
    )

    val cyanGlow = Brush.radialGradient(
        colors = listOf(NexonCyanPrimary.copy(alpha = 0.25f), Color.Transparent)
    )

    val emeraldGlow = Brush.radialGradient(
        colors = listOf(NexonEmeraldAccent.copy(alpha = 0.20f), Color.Transparent)
    )

    val accentBorder = Brush.horizontalGradient(
        colors = listOf(NexonCyanPrimary.copy(alpha = 0.6f), DarkBorder)
    )
}

/**
 * Micro-interaction helper: Creates an organic, tactile spring press scale (1.0 -> 0.96 -> 1.0).
 */
@Composable
fun Modifier.rememberPressScale(
    pressedScale: Float = 0.96f
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "press_scale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Premium Glassmorphic / Dark Automotive Card Container.
 */
@Composable
fun DriveMateCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    border: BorderStroke? = BorderStroke(1.dp, DarkBorder),
    useGradient: Boolean = false,
    containerColor: Color = DarkSurface,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 0.985f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "card_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (useGradient) Color.Transparent else containerColor
        ),
        border = border
    ) {
        if (useGradient) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DriveMateGradient.cardSurface)
            ) {
                content()
            }
        } else {
            content()
        }
    }
}

/**
 * Standardized Section Header with crisp tracking and accent highlights.
 */
@Composable
fun DriveMateSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = NexonCyanPrimary,
                letterSpacing = 1.1.sp
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        trailingContent?.invoke()
    }
}

/**
 * Button Style Variants.
 */
enum class DriveMateButtonVariant {
    PRIMARY,
    SECONDARY,
    OUTLINED,
    DANGER
}

/**
 * Automotive tactile button with built-in spring press micro-interaction and loading indicator.
 */
@Composable
fun DriveMateButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: DriveMateButtonVariant = DriveMateButtonVariant.PRIMARY,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "button_scale"
    )

    when (variant) {
        DriveMateButtonVariant.PRIMARY -> {
            Button(
                onClick = onClick,
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NexonCyanPrimary,
                    contentColor = Color.Black,
                    disabledContainerColor = DarkSurfaceVariant,
                    disabledContentColor = TextMuted
                ),
                modifier = modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            ) {
                ButtonInnerContent(text = text, icon = icon, isLoading = isLoading, iconTint = Color.Black)
            }
        }
        DriveMateButtonVariant.SECONDARY -> {
            Button(
                onClick = onClick,
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkSurfaceVariant,
                    contentColor = TextPrimary,
                    disabledContainerColor = DarkSurfaceVariant.copy(alpha = 0.5f),
                    disabledContentColor = TextMuted
                ),
                modifier = modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            ) {
                ButtonInnerContent(text = text, icon = icon, isLoading = isLoading, iconTint = NexonCyanPrimary)
            }
        }
        DriveMateButtonVariant.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                shape = shape,
                border = BorderStroke(1.dp, if (enabled) NexonCyanPrimary.copy(alpha = 0.6f) else DarkBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NexonCyanPrimary,
                    disabledContentColor = TextMuted
                ),
                modifier = modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            ) {
                ButtonInnerContent(text = text, icon = icon, isLoading = isLoading, iconTint = NexonCyanPrimary)
            }
        }
        DriveMateButtonVariant.DANGER -> {
            OutlinedButton(
                onClick = onClick,
                enabled = enabled && !isLoading,
                interactionSource = interactionSource,
                shape = shape,
                border = BorderStroke(1.dp, if (enabled) NexonRedAccent.copy(alpha = 0.6f) else DarkBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = NexonRedAccent,
                    disabledContentColor = TextMuted
                ),
                modifier = modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            ) {
                ButtonInnerContent(text = text, icon = icon, isLoading = isLoading, iconTint = NexonRedAccent)
            }
        }
    }
}

@Composable
private fun ButtonInnerContent(
    text: String,
    icon: ImageVector?,
    isLoading: Boolean,
    iconTint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = iconTint,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

/**
 * Tactile Icon Button with spring press scale.
 */
@Composable
fun DriveMateIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = TextSecondary,
    backgroundColor: Color = Color.Transparent,
    size: Dp = 40.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "icon_btn_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size((size.value * 0.55f).dp)
        )
    }
}

/**
 * Automotive Filter / Action Chip.
 */
@Composable
fun DriveMateChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "chip_scale"
    )

    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) NexonCyanGlow else DarkSurfaceVariant,
        border = BorderStroke(
            1.dp,
            if (selected) NexonCyanPrimary.copy(alpha = 0.5f) else DarkBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) NexonCyanPrimary else TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) NexonCyanPrimary else TextPrimary,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

/**
 * Standardized Automotive Metric Display.
 */
@Composable
fun DriveMateMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    icon: ImageVector? = null,
    accentColor: Color = NexonCyanPrimary,
    valueFontSize: TextUnit = 22.sp
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = valueFontSize,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                letterSpacing = (-0.5).sp
            )
            if (!unit.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Connection & State Badge with pulsing dot.
 */
@Composable
fun DriveMateStatusBadge(
    text: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = NexonEmeraldAccent,
    inactiveColor: Color = TextMuted,
    pulse: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "badgePulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val currentDotColor = if (isActive) activeColor else inactiveColor

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = (if (isActive) activeColor else inactiveColor).copy(alpha = 0.12f),
        border = BorderStroke(0.8.dp, (if (isActive) activeColor else inactiveColor).copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(currentDotColor)
                    .then(if (isActive && pulse) Modifier.alpha(alphaAnim) else Modifier)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) activeColor else TextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Human-friendly Empty State.
 */
@Composable
fun DriveMateEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Info,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.4f))
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NexonCyanPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            if (actionText != null && onActionClick != null) {
                Spacer(modifier = Modifier.height(14.dp))
                DriveMateButton(
                    text = actionText,
                    onClick = onActionClick,
                    variant = DriveMateButtonVariant.OUTLINED
                )
            }
        }
    }
}

/**
 * Calm Automotive Loading State.
 */
@Composable
fun DriveMateLoadingState(
    message: String = "Loading automotive telemetry...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = NexonCyanPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

/**
 * Calm Human-Friendly Error State.
 */
@Composable
fun DriveMateErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryLabel: String = "Retry"
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, NexonRedAccent.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = NexonAmberAccent,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            DriveMateButton(
                text = retryLabel,
                onClick = onRetry,
                variant = DriveMateButtonVariant.SECONDARY,
                icon = Icons.Default.Refresh
            )
        }
    }
}
