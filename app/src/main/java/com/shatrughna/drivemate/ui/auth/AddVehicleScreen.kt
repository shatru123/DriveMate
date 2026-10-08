package com.shatrughna.drivemate.ui.auth

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shatrughna.drivemate.ui.theme.DarkBackground
import com.shatrughna.drivemate.ui.theme.DarkBorder
import com.shatrughna.drivemate.ui.theme.DarkSurface
import com.shatrughna.drivemate.ui.theme.DarkSurfaceVariant
import com.shatrughna.drivemate.ui.theme.NexonCyanGlow
import com.shatrughna.drivemate.ui.theme.NexonCyanPrimary
import com.shatrughna.drivemate.ui.theme.NexonRedAccent
import com.shatrughna.drivemate.ui.theme.TextMuted
import com.shatrughna.drivemate.ui.theme.TextPrimary
import com.shatrughna.drivemate.ui.theme.TextSecondary

private val POPULAR_BRANDS = listOf("Tata", "Hyundai", "Maruti Suzuki", "Mahindra", "Kia", "Toyota", "Honda", "Volkswagen", "Skoda", "MG")
private val FUEL_TYPES = listOf("Petrol", "Diesel", "Electric", "Hybrid", "CNG")
private val TRANSMISSION_TYPES = listOf("Manual", "Automatic")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleScreen(
    viewModel: AuthViewModel,
    onVehicleSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val make by viewModel.vehicleMake.collectAsStateWithLifecycle()
    val model by viewModel.vehicleModel.collectAsStateWithLifecycle()
    val variant by viewModel.vehicleVariant.collectAsStateWithLifecycle()
    val year by viewModel.vehicleYear.collectAsStateWithLifecycle()
    val reg by viewModel.vehicleReg.collectAsStateWithLifecycle()
    val fuel by viewModel.vehicleFuel.collectAsStateWithLifecycle()
    val trans by viewModel.vehicleTransmission.collectAsStateWithLifecycle()
    val odometer by viewModel.vehicleOdometer.collectAsStateWithLifecycle()
    val nickname by viewModel.vehicleNickname.collectAsStateWithLifecycle()
    val isLoading by viewModel.vehicleLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.vehicleError.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Add Your Vehicle",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Register your car to connect your vehicle cockpit, maintenance schedules, and voice controls.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!errorMessage.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NexonRedAccent.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, NexonRedAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NexonRedAccent
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Quick Brand Selection Chips
            Text(
                text = "POPULAR BRANDS",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(POPULAR_BRANDS) { brand ->
                    val isSelected = make.equals(brand, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) NexonCyanPrimary else DarkSurfaceVariant)
                            .border(1.dp, if (isSelected) NexonCyanPrimary else DarkBorder, RoundedCornerShape(12.dp))
                            .clickable { viewModel.vehicleMake.value = brand }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = brand,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) DarkBackground else TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Vehicle Make Field
            OutlinedTextField(
                value = make,
                onValueChange = { viewModel.vehicleMake.value = it },
                label = { Text("Brand / Make *") },
                placeholder = { Text("e.g. Tata, Hyundai") },
                leadingIcon = {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = NexonCyanPrimary)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                colors = textFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Vehicle Model Field
            OutlinedTextField(
                value = model,
                onValueChange = { viewModel.vehicleModel.value = it },
                label = { Text("Model *") },
                placeholder = { Text("e.g. Nexon, Creta, Swift") },
                leadingIcon = {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = NexonCyanPrimary)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                colors = textFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Variant & Year Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = variant,
                    onValueChange = { viewModel.vehicleVariant.value = it },
                    label = { Text("Variant") },
                    placeholder = { Text("Creative+ S") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1.4f)
                )

                OutlinedTextField(
                    value = year,
                    onValueChange = { viewModel.vehicleYear.value = it },
                    label = { Text("Year") },
                    placeholder = { Text("2024") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    colors = textFieldColors(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Registration Number Field
            OutlinedTextField(
                value = reg,
                onValueChange = { viewModel.vehicleReg.value = it.uppercase() },
                label = { Text("Registration Number") },
                placeholder = { Text("MH12AB1234") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Next),
                colors = textFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Fuel Type Selection
            Text(
                text = "FUEL TYPE",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FUEL_TYPES) { f ->
                    val isSelected = fuel == f
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) NexonCyanPrimary else DarkSurfaceVariant)
                            .border(1.dp, if (isSelected) NexonCyanPrimary else DarkBorder, RoundedCornerShape(12.dp))
                            .clickable { viewModel.vehicleFuel.value = f }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = f,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) DarkBackground else TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transmission Selection
            Text(
                text = "TRANSMISSION",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TRANSMISSION_TYPES.forEach { t ->
                    val isSelected = trans == t
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) NexonCyanPrimary else DarkSurfaceVariant)
                            .border(1.dp, if (isSelected) NexonCyanPrimary else DarkBorder, RoundedCornerShape(12.dp))
                            .clickable { viewModel.vehicleTransmission.value = t }
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = t,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) DarkBackground else TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Initial Odometer Field
            OutlinedTextField(
                value = odometer,
                onValueChange = { viewModel.vehicleOdometer.value = it },
                label = { Text("Initial Odometer (Km)") },
                placeholder = { Text("e.g. 12500") },
                leadingIcon = {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = NexonCyanPrimary)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                colors = textFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Nickname Field (Optional)
            OutlinedTextField(
                value = nickname,
                onValueChange = { viewModel.vehicleNickname.value = it },
                label = { Text("Vehicle Nickname (Optional)") },
                placeholder = { Text("e.g. Beast, Silver Arrow") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                colors = textFieldColors(),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Save Vehicle Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.saveVehicle(onSuccess = onVehicleSaved)
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NexonCyanPrimary,
                    disabledContainerColor = NexonCyanPrimary.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = DarkBackground,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "Save & Go to Dashboard",
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkBackground,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun textFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = DarkSurfaceVariant,
    unfocusedContainerColor = DarkSurface,
    disabledContainerColor = DarkSurface,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedIndicatorColor = NexonCyanPrimary,
    unfocusedIndicatorColor = DarkBorder,
    focusedLabelColor = NexonCyanPrimary,
    unfocusedLabelColor = TextMuted
)
