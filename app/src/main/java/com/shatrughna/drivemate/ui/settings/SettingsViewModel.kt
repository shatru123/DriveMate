package com.shatrughna.drivemate.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shatrughna.drivemate.data.model.DriveMateSettings
import com.shatrughna.drivemate.data.model.GreetingStyle
import com.shatrughna.drivemate.data.preferences.DriveMatePreferencesRepository
import com.shatrughna.drivemate.destination.DestinationManager
import com.shatrughna.drivemate.greeting.GreetingController
import com.shatrughna.drivemate.greeting.GreetingGenerator
import com.shatrughna.drivemate.greeting.GreetingTtsManager
import com.shatrughna.drivemate.greeting.TemplateValidationResult
import com.shatrughna.drivemate.greeting.VoiceInfo
import com.shatrughna.drivemate.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class SettingsViewModel(
    private val preferencesRepository: DriveMatePreferencesRepository,
    private val greetingController: GreetingController,
    private val greetingGenerator: GreetingGenerator,
    private val ttsManager: GreetingTtsManager,
    private val destinationManager: DestinationManager? = null
) : ViewModel() {

    val settings: StateFlow<DriveMateSettings> = preferencesRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DriveMateSettings()
        )

    val isSpeaking: StateFlow<Boolean> = greetingController.isSpeaking

    private val _templateValidation = MutableStateFlow<TemplateValidationResult>(TemplateValidationResult.Valid)
    val templateValidation: StateFlow<TemplateValidationResult> = _templateValidation.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<VoiceInfo>>(emptyList())
    val availableVoices: StateFlow<List<VoiceInfo>> = _availableVoices.asStateFlow()

    init {
        loadAvailableVoices()
    }

    private fun loadAvailableVoices() {
        viewModelScope.launch {
            _availableVoices.value = ttsManager.getAvailableVoices()
        }
    }

    fun updateDriverName(name: String) {
        viewModelScope.launch {
            preferencesRepository.updateDriverName(name)
        }
    }

    fun updateVehicle(brand: String, model: String, variant: String) {
        viewModelScope.launch {
            preferencesRepository.updateVehicle(brand, model, variant)
        }
    }

    fun updateVehicleRegistration(regNumber: String) {
        viewModelScope.launch {
            preferencesRepository.updateVehicleRegistration(regNumber)
        }
    }

    fun saveVehiclePhoto(context: Context, sourceUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val destinationFile = File(context.filesDir, "vehicle_profile_photo.jpg")
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    destinationFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                preferencesRepository.updateVehiclePhotoUri(destinationFile.absolutePath)
                AppLogger.i(AppLogger.Tag.SETTINGS, "Saved vehicle profile photo to ${destinationFile.absolutePath}")
            } catch (e: Exception) {
                AppLogger.e(AppLogger.Tag.SETTINGS, "Failed to save vehicle photo", e)
            }
        }
    }

    fun removeVehiclePhoto(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, "vehicle_profile_photo.jpg")
                if (file.exists()) {
                    file.delete()
                }
                preferencesRepository.updateVehiclePhotoUri(null)
                AppLogger.i(AppLogger.Tag.SETTINGS, "Removed vehicle profile photo")
            } catch (e: Exception) {
                AppLogger.e(AppLogger.Tag.SETTINGS, "Failed to remove vehicle photo", e)
            }
        }
    }

    fun clearRecentDestinations() {
        destinationManager?.clearRecentDestinations()
    }

    fun updateGreetingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateGreetingEnabled(enabled)
        }
    }

    fun updateGreetingStyle(style: GreetingStyle) {
        viewModelScope.launch {
            preferencesRepository.updateGreetingStyle(style)
        }
    }

    fun updateCustomTemplate(template: String) {
        _templateValidation.value = greetingGenerator.validateTemplate(template)
        viewModelScope.launch {
            preferencesRepository.updateCustomGreetingTemplate(template)
        }
    }

    fun updateSpeechRate(rate: Float) {
        viewModelScope.launch {
            preferencesRepository.updateSpeechRate(rate)
            ttsManager.setSpeechRate(rate)
        }
    }

    fun updatePitch(pitch: Float) {
        viewModelScope.launch {
            preferencesRepository.updatePitch(pitch)
            ttsManager.setPitch(pitch)
        }
    }

    fun updateLanguageTag(tag: String) {
        viewModelScope.launch {
            preferencesRepository.updateLanguageTag(tag)
        }
    }

    fun updateVoiceName(voiceName: String?) {
        viewModelScope.launch {
            preferencesRepository.updateVoiceName(voiceName)
            ttsManager.setVoice(voiceName)
        }
    }

    fun updateWeatherSettings(includeInGreeting: Boolean, cityName: String, lat: Double, lon: Double) {
        viewModelScope.launch {
            preferencesRepository.updateWeatherSettings(includeInGreeting, cityName, lat, lon)
        }
    }

    fun updateAutoDetectLocation(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateAutoDetectLocation(enabled)
        }
    }

    fun updateVehicleCare(odometerKm: Double, nextServiceKm: Int, fuelReminder: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateVehicleCare(odometerKm, nextServiceKm, fuelReminder)
        }
    }

    fun updateVehicleCare(odometerKm: Int, nextServiceKm: Int, fuelReminder: Boolean) {
        updateVehicleCare(odometerKm.toDouble(), nextServiceKm, fuelReminder)
    }

    fun updateFavoriteAddresses(home: String, office: String) {
        viewModelScope.launch {
            preferencesRepository.updateFavoriteAddresses(home, office)
        }
    }

    fun updateDriverFatigueAlert(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateDriverFatigueAlert(enabled)
        }
    }

    fun updatePreferredMusicApp(app: String) {
        viewModelScope.launch {
            preferencesRepository.updatePreferredMusicApp(app)
        }
    }

    fun updateVoiceAssistantEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateVoiceAssistantEnabled(enabled)
        }
    }

    fun updateHeyDriveMateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateHeyDriveMateEnabled(enabled)
        }
    }

    fun previewGreeting() {
        viewModelScope.launch {
            greetingController.previewGreeting(settings.value)
        }
    }

    fun stopSpeaking() {
        greetingController.stopSpeaking()
    }

    fun updateDemoModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateDemoModeEnabled(enabled)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            preferencesRepository.resetToDefaults()
        }
    }

    class Factory(
        private val preferencesRepository: DriveMatePreferencesRepository,
        private val greetingController: GreetingController,
        private val greetingGenerator: GreetingGenerator,
        private val ttsManager: GreetingTtsManager,
        private val destinationManager: DestinationManager? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                preferencesRepository,
                greetingController,
                greetingGenerator,
                ttsManager,
                destinationManager
            ) as T
        }
    }
}
