package com.shatrughna.drivemate.ui.more

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shatrughna.drivemate.ui.components.DriveMateCard
import com.shatrughna.drivemate.ui.components.DriveMateSectionHeader
import com.shatrughna.drivemate.ui.theme.DarkBackground
import com.shatrughna.drivemate.ui.theme.DarkSurfaceVariant
import com.shatrughna.drivemate.ui.theme.NexonCyanPrimary
import com.shatrughna.drivemate.ui.theme.TextMuted
import com.shatrughna.drivemate.ui.theme.TextPrimary
import com.shatrughna.drivemate.ui.theme.TextSecondary

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.shatrughna.drivemate.auth.model.UserProfile
import com.shatrughna.drivemate.ui.components.ZoomablePhotoViewerDialog
import com.shatrughna.drivemate.ui.theme.DarkBorder
import com.shatrughna.drivemate.ui.theme.DarkSurface
import com.shatrughna.drivemate.ui.theme.NexonCyanGlow
import com.shatrughna.drivemate.ui.theme.NexonRedAccent
import com.shatrughna.drivemate.util.VehiclePhotoLoader
import com.shatrughna.drivemate.vehicle.model.VehicleProfile

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    user: UserProfile? = null,
    vehicle: VehicleProfile? = null,
    onLogout: () -> Unit = {},
    onUploadProfilePhoto: ((android.net.Uri) -> Unit)? = null,
    onRemoveProfilePhoto: (() -> Unit)? = null,
    onDocuments: () -> Unit,
    onMaintenance: () -> Unit,
    onExpenses: () -> Unit,
    onParking: () -> Unit,
    onDiagnostics: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Lightbox zoom viewer state
    var zoomPhotoTitle by remember { mutableStateOf<String?>(null) }
    var zoomPhotoSubtitle by remember { mutableStateOf<String?>(null) }
    var zoomPhotoBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var zoomPhotoUri by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUploadProfilePhoto?.invoke(uri)
        }
    }

    val profileThumbnail by produceState<ImageBitmap?>(initialValue = null, user?.profileImageUrl) {
        value = VehiclePhotoLoader.loadOptimizedBitmap(context, user?.profileImageUrl, maxDimension = 512)
    }
    val vehicleThumbnail by produceState<ImageBitmap?>(initialValue = null, vehicle?.photoUri) {
        value = VehiclePhotoLoader.loadOptimizedBitmap(context, vehicle?.photoUri, maxDimension = 512)
    }

    if (zoomPhotoTitle != null) {
        ZoomablePhotoViewerDialog(
            title = zoomPhotoTitle ?: "Photo",
            subtitle = zoomPhotoSubtitle,
            imageBitmap = zoomPhotoBitmap,
            imageUri = zoomPhotoUri,
            onDismiss = {
                zoomPhotoTitle = null
                zoomPhotoSubtitle = null
                zoomPhotoBitmap = null
                zoomPhotoUri = null
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Log Out",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to log out of DriveMate? Your vehicle data will remain securely saved for your next login.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text(
                        text = "Log Out",
                        color = NexonRedAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(text = "Cancel", color = TextPrimary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "More",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Your DriveMate toolkit",
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
                .background(DarkBackground)
                .padding(innerPadding)
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // User Account Card
            if (user != null) {
                DriveMateCard(containerColor = DarkSurfaceVariant) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar Box with camera badge and zoom viewer
                            Box(
                                modifier = Modifier.size(54.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(NexonCyanGlow)
                                        .border(2.dp, NexonCyanPrimary, CircleShape)
                                        .clickable {
                                            if (!user.profileImageUrl.isNullOrBlank()) {
                                                zoomPhotoTitle = user.name
                                                zoomPhotoSubtitle = "Profile Photo • ${user.email}"
                                                zoomPhotoBitmap = profileThumbnail
                                                zoomPhotoUri = user.profileImageUrl
                                            } else {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (profileThumbnail != null) {
                                        Image(
                                            bitmap = profileThumbnail!!,
                                            contentDescription = "User Avatar",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = NexonCyanPrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                // Upload / Change Camera Badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(NexonCyanPrimary)
                                        .border(1.5.dp, DarkSurface, CircleShape)
                                        .clickable {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = "Change profile photo",
                                        tint = Color.Black,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = user.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            OutlinedButton(
                                onClick = { showLogoutDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NexonRedAccent.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NexonRedAccent),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Log Out",
                                    tint = NexonRedAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Log Out",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NexonRedAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (vehicle != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkBackground.copy(alpha = 0.5f))
                                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                                    .clickable(enabled = vehicle.photoUri != null) {
                                        zoomPhotoTitle = vehicle.fullDisplayName
                                        zoomPhotoSubtitle = "Registered Vehicle • ${vehicle.registrationNumber}"
                                        zoomPhotoBitmap = vehicleThumbnail
                                        zoomPhotoUri = vehicle.photoUri
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (vehicleThumbnail != null) {
                                    Image(
                                        bitmap = vehicleThumbnail!!,
                                        contentDescription = "Vehicle Photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .border(1.dp, NexonCyanPrimary.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        tint = NexonCyanPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = vehicle.fullDisplayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (vehicle.registrationNumber.isNotBlank()) {
                                    Text(
                                        text = vehicle.registrationNumber,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NexonCyanPrimary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                if (vehicle.photoUri != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.ZoomIn,
                                        contentDescription = "View vehicle photo",
                                        tint = NexonCyanPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            DriveMateSectionHeader(
                title = "TOOLS",
                subtitle = "Secondary features stay one tap away"
            )
            MoreAction("Documents", "Keep your vehicle records private and organized", Icons.Default.Description, onDocuments)
            MoreAction("Vehicle care", "Service schedule and maintenance history", Icons.Default.Build, onMaintenance)
            MoreAction("Expenses", "Fuel, service and running costs", Icons.Default.AccountBalanceWallet, onExpenses)
            MoreAction("Find my car", "Open your last saved parking location", Icons.Default.LocalParking, onParking)
            MoreAction("Diagnostics", "Inspect connections, sources and audio state", Icons.Default.Tune, onDiagnostics)
            MoreAction("Settings", "Vehicle identity, voice and privacy controls", Icons.Default.Settings, onSettings)
            MoreAction("About DriveMate", "Developer details, philosophy & architecture", Icons.Default.Info, onAbout)
            Spacer(modifier = Modifier.height(92.dp))
        }
    }
}

@Composable
private fun MoreAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    DriveMateCard(onClick = onClick, containerColor = DarkSurfaceVariant) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = NexonCyanPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
