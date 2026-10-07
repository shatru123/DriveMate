package com.shatrughna.drivemate.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shatrughna.drivemate.data.model.GreetingStyle
import com.shatrughna.drivemate.greeting.GreetingGeneratorImpl
import com.shatrughna.drivemate.greeting.TemplateValidationResult
import com.shatrughna.drivemate.ui.components.CreatorCard
import com.shatrughna.drivemate.ui.theme.DarkBackground
import com.shatrughna.drivemate.ui.theme.DarkBorder
import com.shatrughna.drivemate.ui.theme.DarkSurface
import com.shatrughna.drivemate.ui.theme.DarkSurfaceVariant
import com.shatrughna.drivemate.ui.theme.NexonAmberAccent
import com.shatrughna.drivemate.ui.theme.NexonCyanPrimary
import com.shatrughna.drivemate.ui.theme.NexonRedAccent
import com.shatrughna.drivemate.ui.theme.TextMuted
import com.shatrughna.drivemate.ui.theme.TextPrimary
import com.shatrughna.drivemate.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val templateValidation by viewModel.templateValidation.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.saveVehiclePhoto(context, uri)
        }
    }

    var driverNameInput by remember(settings.driverName) { mutableStateOf(settings.driverName) }
    var vehicleBrandInput by remember(settings.vehicleBrand) { mutableStateOf(settings.vehicleBrand) }
    var vehicleModelInput by remember(settings.vehicleModel) { mutableStateOf(settings.vehicleModel) }
    var vehicleVariantInput by remember(settings.vehicleVariant) { mutableStateOf(settings.vehicleVariant) }
    var registrationInput by remember(settings.vehicleRegistrationNumber) { mutableStateOf(settings.vehicleRegistrationNumber) }
    var customTemplateInput by remember(settings.customGreetingTemplate) { mutableStateOf(settings.customGreetingTemplate) }

    // V2 inputs
    var weatherCityInput by remember(settings.weatherCityName) { mutableStateOf(settings.weatherCityName) }
    var odometerInput by remember(settings.odometerKm) { mutableStateOf(settings.odometerKm.toString()) }
    var nextServiceInput by remember(settings.nextServiceKm) { mutableStateOf(settings.nextServiceKm.toString()) }
    var homeAddressInput by remember(settings.homeAddress) { mutableStateOf(settings.homeAddress) }
    var officeAddressInput by remember(settings.officeAddress) { mutableStateOf(settings.officeAddress) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = TextPrimary
                ),
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NexonCyanPrimary
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Driver Section
            SettingsSectionHeader(title = "Driver Profile")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = driverNameInput,
                        onValueChange = {
                            driverNameInput = it
                            viewModel.updateDriverName(it)
                        },
                        label = { Text("Driver Name") },
                        singleLine = true,
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2. Vehicle Section
            SettingsSectionHeader(title = "Vehicle Details")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = vehicleBrandInput,
                        onValueChange = {
                            vehicleBrandInput = it
                            viewModel.updateVehicle(it, vehicleModelInput, vehicleVariantInput)
                        },
                        label = { Text("Vehicle Brand (e.g. TATA)") },
                        singleLine = true,
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = vehicleModelInput,
                        onValueChange = {
                            vehicleModelInput = it
                            viewModel.updateVehicle(vehicleBrandInput, it, vehicleVariantInput)
                        },
                        label = { Text("Vehicle Model (e.g. Nexon)") },
                        singleLine = true,
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = vehicleVariantInput,
                        onValueChange = {
                            vehicleVariantInput = it
                            viewModel.updateVehicle(vehicleBrandInput, vehicleModelInput, it)
                        },
                        label = { Text("Vehicle Variant (e.g. Creative+ S)") },
                        singleLine = true,
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = registrationInput,
                        onValueChange = {
                            registrationInput = it
                            viewModel.updateVehicleRegistration(it)
                        },
                        label = { Text("Registration Plate (e.g. MH 28 BW 1624)") },
                        singleLine = true,
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = DarkBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Vehicle Profile Photo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (settings.vehiclePhotoUri != null) "Custom photo uploaded" else "Using stylized 3D card fallback",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (settings.vehiclePhotoUri != null) {
                                OutlinedButton(
                                    onClick = { viewModel.removeVehiclePhoto(context) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NexonRedAccent),
                                    border = BorderStroke(1.dp, NexonRedAccent.copy(alpha = 0.5f))
                                ) {
                                    Text("Remove")
                                }
                            }
                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NexonCyanPrimary)
                            ) {
                                Text(
                                    text = if (settings.vehiclePhotoUri != null) "Change Photo" else "Upload Photo",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 3. Weather & Environmental Integration (V2)
            SettingsSectionHeader(title = "Weather & Environment (V2)")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Include Weather in Greeting",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Speaks temperature & conditions on connection",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = settings.includeWeatherInGreeting,
                            onCheckedChange = {
                                viewModel.updateWeatherSettings(
                                    includeInGreeting = it,
                                    cityName = weatherCityInput,
                                    lat = settings.weatherLatitude,
                                    lon = settings.weatherLongitude
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NexonCyanPrimary,
                                checkedTrackColor = NexonCyanPrimary.copy(alpha = 0.3f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Detect Current Location",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Uses GPS/Network to fetch weather wherever you drive",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = settings.autoDetectLocation,
                            onCheckedChange = viewModel::updateAutoDetectLocation,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NexonCyanPrimary,
                                checkedTrackColor = NexonCyanPrimary.copy(alpha = 0.3f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = weatherCityInput,
                        onValueChange = {
                            weatherCityInput = it
                            viewModel.updateWeatherSettings(
                                includeInGreeting = settings.includeWeatherInGreeting,
                                cityName = it,
                                lat = settings.weatherLatitude,
                                lon = settings.weatherLongitude
                            )
                        },
                        label = { Text(if (settings.autoDetectLocation) "Fallback City Name (e.g. Pune)" else "City Name (e.g. Pune)") },
                        supportingText = {
                            Text(if (settings.autoDetectLocation) "Used if location is unavailable or denied" else "Fixed custom city")
                        },
                        singleLine = true,
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 4. Vehicle Care & Service (V2)
            SettingsSectionHeader(title = "Vehicle Care & Maintenance (V2)")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = odometerInput,
                        onValueChange = {
                            odometerInput = it
                            val parsedOdo = it.toDoubleOrNull() ?: settings.odometerKm
                            viewModel.updateVehicleCare(parsedOdo, settings.nextServiceKm, settings.fuelReminderEnabled)
                        },
                        label = { Text("Current Odometer (km)") },
                        singleLine = true,
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = nextServiceInput,
                        onValueChange = {
                            nextServiceInput = it
                            val parsedNext = it.toIntOrNull() ?: settings.nextServiceKm
                            viewModel.updateVehicleCare(settings.odometerKm, parsedNext, settings.fuelReminderEnabled)
                        },
                        label = { Text("Next Service Target (km, e.g. 15000)") },
                        singleLine = true,
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Fuel & Range Check Alert",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                        Switch(
                            checked = settings.fuelReminderEnabled,
                            onCheckedChange = {
                                viewModel.updateVehicleCare(settings.odometerKm, settings.nextServiceKm, it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NexonCyanPrimary,
                                checkedTrackColor = NexonCyanPrimary.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }

            // 5. Favorite Destinations (V2)
            SettingsSectionHeader(title = "Favorite Destinations (V2)")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = homeAddressInput,
                        onValueChange = {
                            homeAddressInput = it
                            viewModel.updateFavoriteAddresses(it, officeAddressInput)
                        },
                        label = { Text("Home Address / Landmark") },
                        singleLine = true,
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = officeAddressInput,
                        onValueChange = {
                            officeAddressInput = it
                            viewModel.updateFavoriteAddresses(homeAddressInput, it)
                        },
                        label = { Text("Office Address / Workplace") },
                        singleLine = true,
                        colors = outlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = DarkBorder)

                    OutlinedButton(
                        onClick = { viewModel.clearRecentDestinations() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NexonAmberAccent),
                        border = BorderStroke(1.dp, NexonAmberAccent.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Recent Destinations History")
                    }
                }
            }

            // Smart Assistant & Safety
            SettingsSectionHeader(title = "Voice Assistant & Driving Safety")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Voice Assistant",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Enable hands-free voice commands and queries",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = settings.voiceAssistantEnabled,
                            onCheckedChange = viewModel::updateVoiceAssistantEnabled
                        )
                    }

                    HorizontalDivider(color = DarkBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hey DriveMate Wake Word",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Say \"Hey DriveMate\" to activate assistant while driving",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = settings.heyDriveMateEnabled,
                            onCheckedChange = viewModel::updateHeyDriveMateEnabled
                        )
                    }

                    HorizontalDivider(color = DarkBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Driver Fatigue Alert",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Announce rest reminder after 2 hours of continuous driving",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = settings.driverFatigueAlertEnabled,
                            onCheckedChange = viewModel::updateDriverFatigueAlert
                        )
                    }

                    HorizontalDivider(color = DarkBorder)

                    Column {
                        Text(
                            text = "Preferred Music Player for Voice",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Target app when saying 'Play [song]'",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Spotify", "YouTube Music").forEach { app ->
                                val selected = settings.preferredMusicApp.equals(app, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.updatePreferredMusicApp(app) },
                                    label = { Text(app) }
                                )
                            }
                        }
                    }
                }
            }

            // 6. Greeting Section
            SettingsSectionHeader(title = "Welcome Greeting Experience")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Enable Welcome Greeting",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Automatically announce when car connects",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = settings.greetingEnabled,
                            onCheckedChange = viewModel::updateGreetingEnabled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NexonCyanPrimary,
                                checkedTrackColor = NexonCyanPrimary.copy(alpha = 0.3f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Greeting Style",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    GreetingStyle.entries.forEach { style ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.updateGreetingStyle(style) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (settings.greetingStyle == style),
                                onClick = { viewModel.updateGreetingStyle(style) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = NexonCyanPrimary,
                                    unselectedColor = TextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = style.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = style.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Custom Template Editor
                    AnimatedVisibility(visible = settings.greetingStyle == GreetingStyle.CUSTOM) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Text(
                                text = "Custom Template",
                                style = MaterialTheme.typography.labelLarge,
                                color = NexonAmberAccent
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = customTemplateInput,
                                onValueChange = {
                                    customTemplateInput = it
                                    viewModel.updateCustomTemplate(it)
                                },
                                placeholder = { Text("e.g. Good {timeOfDay}, {name}. It's {weather}. Welcome to your {brand} {model}.") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = outlinedTextFieldColors(),
                                minLines = 2,
                                maxLines = 4
                            )

                            // Placeholder token chips
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap tokens to insert:",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                GreetingGeneratorImpl.SUPPORTED_PLACEHOLDERS.forEach { token ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(DarkSurfaceVariant)
                                            .border(1.dp, NexonCyanPrimary.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                            .clickable {
                                                customTemplateInput = "$customTemplateInput $token".trim()
                                                viewModel.updateCustomTemplate(customTemplateInput)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = token,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NexonCyanPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // Validation message
                            if (templateValidation is TemplateValidationResult.Invalid) {
                                val invalid = templateValidation as TemplateValidationResult.Invalid
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = NexonRedAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = invalid.reason,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NexonRedAccent
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 7. Voice & Speech Controls
            SettingsSectionHeader(title = "Voice & Speech Engine")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Speech Rate Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Speech Rate",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "${String.format("%.2f", settings.speechRate)}x",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NexonCyanPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = settings.speechRate,
                        onValueChange = viewModel::updateSpeechRate,
                        valueRange = 0.5f..2.0f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = NexonCyanPrimary,
                            activeTrackColor = NexonCyanPrimary,
                            inactiveTrackColor = DarkSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Pitch Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Voice Pitch",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "${String.format("%.2f", settings.pitch)}x",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NexonCyanPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = settings.pitch,
                        onValueChange = viewModel::updatePitch,
                        valueRange = 0.5f..2.0f,
                        steps = 14,
                        colors = SliderDefaults.colors(
                            thumbColor = NexonCyanPrimary,
                            activeTrackColor = NexonCyanPrimary,
                            inactiveTrackColor = DarkSurfaceVariant
                        )
                    )
                }
            }

            // 8. Testing & Verification
            SettingsSectionHeader(title = "Test & Preview")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Listen to your configured greeting directly through the phone speakers before driving.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = viewModel::previewGreeting,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NexonCyanPrimary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isSpeaking
                        ) {
                            if (isSpeaking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Playing...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Preview Greeting", fontWeight = FontWeight.Bold)
                            }
                        }

                        AnimatedVisibility(visible = isSpeaking) {
                            OutlinedButton(
                                onClick = viewModel::stopSpeaking,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = NexonAmberAccent),
                                border = BorderStroke(1.dp, NexonAmberAccent),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Stop")
                            }
                        }
                    }
                }
            }

            // 9. Platform Limitations & Android Auto Compatibility
            SettingsSectionHeader(title = "Android Auto & Platform Safety")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, NexonCyanPrimary.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = NexonCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Official Platform Integration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = NexonCyanPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• Official API: DriveMate integrates with androidx.car.app.connection.CarConnection to detect Android Auto projection (wired and wireless).\n\n" +
                                "• Audio Focus: Audio is routed cleanly through car speakers using Android Audio Focus (Assistance Navigation stream with transient ducking), so in-car media smoothly ducks.\n\n" +
                                "• Safety Guidelines: Android Auto strictly restricts unprompted background audio unless the app is in the foreground or executing an active driving session. DriveMate adheres 100% to Google safety standards without unsupported hacks or accessibility workarounds.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                }
            }

            // 10. About & Reset
            SettingsSectionHeader(title = "About DriveMate")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DriveMate v2.0.0",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Personal Driving Companion for Tata Nexon Creative+ S",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "100% on-device. No telemetry, no background network tracking, and minimal permissions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = viewModel::resetToDefaults,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NexonRedAccent),
                        border = BorderStroke(1.dp, NexonRedAccent.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset All Settings to Defaults")
                    }
                }
            }

            // About Developer & Creator
            SettingsSectionHeader(title = "About DriveMate & Developer")
            CreatorCard()

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = NexonCyanPrimary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NexonCyanPrimary,
    unfocusedBorderColor = DarkBorder,
    focusedLabelColor = NexonCyanPrimary,
    unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = NexonCyanPrimary
)
