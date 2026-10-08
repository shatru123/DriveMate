package com.shatrughna.drivemate.ui.mycar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shatrughna.drivemate.core.capabilities.CapabilityStatus
import com.shatrughna.drivemate.core.capabilities.VehicleCapabilitiesState
import com.shatrughna.drivemate.core.insights.AiCarInsight
import com.shatrughna.drivemate.core.insights.InsightPriority
import com.shatrughna.drivemate.data.model.DriveMateSettings
import com.shatrughna.drivemate.ui.components.DriveMateCard
import com.shatrughna.drivemate.ui.components.DriveMateSectionHeader
import com.shatrughna.drivemate.ui.components.ThreeDimensionalVehicleCard
import com.shatrughna.drivemate.ui.components.rememberPressScale
import com.shatrughna.drivemate.ui.theme.DarkBackground
import com.shatrughna.drivemate.ui.theme.DarkBorder
import com.shatrughna.drivemate.ui.theme.DarkSurfaceVariant
import com.shatrughna.drivemate.ui.theme.NexonAmberAccent
import com.shatrughna.drivemate.ui.theme.NexonCyanPrimary
import com.shatrughna.drivemate.ui.theme.NexonEmeraldAccent
import com.shatrughna.drivemate.ui.theme.NexonRedAccent
import com.shatrughna.drivemate.ui.theme.TextMuted
import com.shatrughna.drivemate.ui.theme.TextPrimary
import com.shatrughna.drivemate.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCarScreen(
    settings: DriveMateSettings,
    capabilities: VehicleCapabilitiesState,
    topInsight: AiCarInsight?,
    documentCount: Int,
    nextServiceDueKm: Double?,
    totalExpenses: Double,
    onNavigateToDocuments: () -> Unit,
    onNavigateToMaintenance: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToClimate: () -> Unit,
    onNavigateToParking: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToTimeline: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                            text = "My Car • Command Center",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${settings.vehicleBrand} ${settings.vehicleModel} (${settings.vehicleRegistrationNumber})",
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

            // 1. Hero 3D Vehicle Card
            ThreeDimensionalVehicleCard(settings = settings)

            // 2. Active AI Car Insight Banner (if present)
            if (topInsight != null) {
                val insightColor = when (topInsight.priority) {
                    InsightPriority.HIGH -> NexonAmberAccent
                    InsightPriority.MEDIUM -> NexonCyanPrimary
                    InsightPriority.LOW -> NexonEmeraldAccent
                }

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
                                .background(insightColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = insightColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI INSIGHT: ${topInsight.title}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = insightColor,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = topInsight.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // 3. Central Subsystems Grid (2 columns)
            DriveMateSectionHeader(title = "VEHICLE OPERATING SYSTEM")

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Row 1: Document Vault & Maintenance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HubActionTile(
                        title = "Document Vault",
                        subtitle = "$documentCount Stored • Biometric",
                        icon = Icons.Default.Description,
                        accentColor = NexonCyanPrimary,
                        onClick = onNavigateToDocuments,
                        modifier = Modifier.weight(1f)
                    )

                    HubActionTile(
                        title = "Service & Care",
                        subtitle = nextServiceDueKm?.let { "Due at ${it.toInt()} km" } ?: "Service interval unavailable",
                        icon = Icons.Default.Build,
                        accentColor = NexonAmberAccent,
                        onClick = onNavigateToMaintenance,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Expense Manager & Climate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HubActionTile(
                        title = "Expenses",
                        subtitle = "₹${totalExpenses.toInt()} Logged",
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = NexonEmeraldAccent,
                        onClick = onNavigateToExpenses,
                        modifier = Modifier.weight(1f)
                    )

                    HubActionTile(
                        title = "Climate (HVAC)",
                        subtitle = "22°C • Companion",
                        icon = Icons.Default.AcUnit,
                        accentColor = NexonCyanPrimary,
                        onClick = onNavigateToClimate,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3: Parking Mode & Analytics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HubActionTile(
                        title = "Parking Mode",
                        subtitle = "Surround Radar & Spot",
                        icon = Icons.Default.LocalParking,
                        accentColor = NexonEmeraldAccent,
                        onClick = onNavigateToParking,
                        modifier = Modifier.weight(1f)
                    )

                    HubActionTile(
                        title = "Trip Analytics",
                        subtitle = "Monthly Stats & Chart",
                        icon = Icons.Default.TrendingUp,
                        accentColor = NexonCyanPrimary,
                        onClick = onNavigateToAnalytics,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 4: Unified Timeline (Full width)
                HubActionTile(
                    title = "Vehicle Timeline",
                    subtitle = "Unified stream of drives, fuel, service & reminders",
                    icon = Icons.Default.Timeline,
                    accentColor = NexonCyanPrimary,
                    onClick = onNavigateToTimeline,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 4. Vehicle Capability Diagnostics & Transparency
            DriveMateSectionHeader(title = "HARDWARE CAPABILITY MATRIX")

            DriveMateCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    capabilities.allCapabilities.forEach { cap ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cap.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = cap.detailMessage,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }

                            val badgeColor = when (cap.status) {
                                CapabilityStatus.SUPPORTED -> NexonEmeraldAccent
                                CapabilityStatus.COMING_SOON -> NexonCyanPrimary
                                else -> TextMuted
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(badgeColor.copy(alpha = 0.12f))
                                    .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = cap.status.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun HubActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DriveMateCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}
