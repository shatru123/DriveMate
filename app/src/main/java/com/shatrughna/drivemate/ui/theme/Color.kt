package com.shatrughna.drivemate.ui.theme

import androidx.compose.ui.graphics.Color

// Deep-space automotive palette: calm enough for daily use, with restrained
// luminous accents that keep important states easy to scan while driving.
val NexonCyanPrimary = Color(0xFF8BE6DE)
val NexonCyanDark = Color(0xFF58C2BF)
val NexonCyanGlow = Color(0x268BE6DE)

val NexonAmberAccent = Color(0xFFF4C46E)
val NexonEmeraldAccent = Color(0xFF72D6AA)
val NexonRedAccent = Color(0xFFFF8585)

val DarkBackground = Color(0xFF080C14)
val DarkSurface = Color(0xFF101722)
val DarkSurfaceVariant = Color(0xFF182333)
val DarkBorder = Color(0xFF29384D)

val TextPrimary = Color(0xFFF4F7FB)
val TextSecondary = Color(0xFFA5B1C2)
val TextMuted = Color(0xFF6F7C8E)

val CardGradientStart = Color(0xFF1A2A3C)
val CardGradientEnd = Color(0xFF101722)

/** Semantic palette entry point for new UI surfaces. Legacy aliases above are retained for existing screens. */
object DriveMateColors {
    val background = DarkBackground
    val surface = DarkSurface
    val surfaceElevated = DarkSurfaceVariant
    val border = DarkBorder
    val primary = TextPrimary
    val secondary = TextSecondary
    val muted = TextMuted
    val accent = NexonCyanPrimary
    val success = NexonEmeraldAccent
    val warning = NexonAmberAccent
    val critical = NexonRedAccent
}
