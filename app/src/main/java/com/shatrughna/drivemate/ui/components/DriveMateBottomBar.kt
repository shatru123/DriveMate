package com.shatrughna.drivemate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shatrughna.drivemate.ui.theme.DarkBorder
import com.shatrughna.drivemate.ui.theme.DarkSurface
import com.shatrughna.drivemate.ui.theme.NexonCyanPrimary
import com.shatrughna.drivemate.ui.theme.TextMuted
import com.shatrughna.drivemate.ui.theme.TextPrimary

enum class DriveMateTopLevelDestination(
    val label: String,
    val icon: ImageVector
) {
    HOME("Home", Icons.Default.Home),
    DRIVE("Drive", Icons.Default.Route),
    VEHICLE("Vehicle", Icons.Default.DirectionsCar),
    ASSISTANT("Assistant", Icons.Default.AutoAwesome),
    MORE("More", Icons.Default.MoreHoriz)
}

@Composable
fun DriveMateBottomBar(
    selected: DriveMateTopLevelDestination,
    onSelected: (DriveMateTopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp),
        color = DarkSurface.copy(alpha = 0.97f),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DriveMateTopLevelDestination.entries.forEach { destination ->
                val isSelected = destination == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) NexonCyanPrimary.copy(alpha = 0.13f)
                            else Color.Transparent
                        )
                        .clickable(
                            onClickLabel = "Open ${destination.label}",
                            onClick = { onSelected(destination) }
                        )
                        .padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label,
                        tint = if (isSelected) NexonCyanPrimary else TextMuted,
                        modifier = Modifier.size(21.dp)
                    )
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) TextPrimary else TextMuted,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
