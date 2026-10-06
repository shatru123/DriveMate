package com.shatrughna.drivemate.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shatrughna.drivemate.data.model.DriveMateSettings
import com.shatrughna.drivemate.data.model.GreetingStyle
import com.shatrughna.drivemate.util.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "drivemate_settings")

/**
 * Repository interface for managing persistent application settings.
 */
interface DriveMatePreferencesRepository {
    val settingsFlow: Flow<DriveMateSettings>
    suspend fun updateDriverName(name: String)
    suspend fun updateVehicle(brand: String, model: String, variant: String)
    suspend fun updateGreetingEnabled(enabled: Boolean)
    suspend fun updateGreetingStyle(style: GreetingStyle)
    suspend fun updateCustomGreetingTemplate(template: String)
    suspend fun updateSpeechRate(rate: Float)
    suspend fun updatePitch(pitch: Float)
    suspend fun updateLanguageTag(tag: String)
    suspend fun updateVoiceName(voice: String?)
    suspend fun updateAutoMonitorBluetooth(enabled: Boolean)
    suspend fun updateTargetBluetoothName(name: String)
    suspend fun resetToDefaults()
}

class DriveMatePreferencesRepositoryImpl(
    private val context: Context
) : DriveMatePreferencesRepository {

    private object PreferencesKeys {
        val DRIVER_NAME = stringPreferencesKey("driver_name")
        val VEHICLE_BRAND = stringPreferencesKey("vehicle_brand")
        val VEHICLE_MODEL = stringPreferencesKey("vehicle_model")
        val VEHICLE_VARIANT = stringPreferencesKey("vehicle_variant")
        val GREETING_ENABLED = booleanPreferencesKey("greeting_enabled")
        val GREETING_STYLE = stringPreferencesKey("greeting_style")
        val CUSTOM_GREETING_TEMPLATE = stringPreferencesKey("custom_greeting_template")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val PITCH = floatPreferencesKey("pitch")
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")
        val VOICE_NAME = stringPreferencesKey("voice_name")
        val AUTO_MONITOR_BT = booleanPreferencesKey("auto_monitor_bluetooth")
        val TARGET_BT_NAME = stringPreferencesKey("target_bluetooth_name")
    }

    override val settingsFlow: Flow<DriveMateSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                AppLogger.e(AppLogger.Tag.SETTINGS, "Error reading DataStore preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            DriveMateSettings(
                driverName = preferences[PreferencesKeys.DRIVER_NAME] ?: "Shatrughna",
                vehicleBrand = preferences[PreferencesKeys.VEHICLE_BRAND] ?: "TATA",
                vehicleModel = preferences[PreferencesKeys.VEHICLE_MODEL] ?: "Nexon",
                vehicleVariant = preferences[PreferencesKeys.VEHICLE_VARIANT] ?: "Creative+ S",
                greetingEnabled = preferences[PreferencesKeys.GREETING_ENABLED] ?: true,
                greetingStyle = GreetingStyle.fromName(preferences[PreferencesKeys.GREETING_STYLE]),
                customGreetingTemplate = preferences[PreferencesKeys.CUSTOM_GREETING_TEMPLATE]
                    ?: "Good {timeOfDay}, {name}. Welcome to your {brand} {model}.",
                speechRate = preferences[PreferencesKeys.SPEECH_RATE] ?: 1.0f,
                pitch = preferences[PreferencesKeys.PITCH] ?: 1.0f,
                languageTag = preferences[PreferencesKeys.LANGUAGE_TAG] ?: "en-IN",
                voiceName = preferences[PreferencesKeys.VOICE_NAME],
                autoMonitorBluetooth = preferences[PreferencesKeys.AUTO_MONITOR_BT] ?: true,
                targetBluetoothName = preferences[PreferencesKeys.TARGET_BT_NAME] ?: "Tata Nexon"
            )
        }

    override suspend fun updateDriverName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DRIVER_NAME] = name.trim()
        }
    }

    override suspend fun updateVehicle(brand: String, model: String, variant: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VEHICLE_BRAND] = brand.trim()
            preferences[PreferencesKeys.VEHICLE_MODEL] = model.trim()
            preferences[PreferencesKeys.VEHICLE_VARIANT] = variant.trim()
        }
    }

    override suspend fun updateGreetingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GREETING_ENABLED] = enabled
        }
    }

    override suspend fun updateGreetingStyle(style: GreetingStyle) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GREETING_STYLE] = style.name
        }
    }

    override suspend fun updateCustomGreetingTemplate(template: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_GREETING_TEMPLATE] = template
        }
    }

    override suspend fun updateSpeechRate(rate: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SPEECH_RATE] = rate.coerceIn(0.5f, 2.0f)
        }
    }

    override suspend fun updatePitch(pitch: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PITCH] = pitch.coerceIn(0.5f, 2.0f)
        }
    }

    override suspend fun updateLanguageTag(tag: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE_TAG] = tag
        }
    }

    override suspend fun updateVoiceName(voice: String?) {
        context.dataStore.edit { preferences ->
            if (voice != null) {
                preferences[PreferencesKeys.VOICE_NAME] = voice
            } else {
                preferences.remove(PreferencesKeys.VOICE_NAME)
            }
        }
    }

    override suspend fun updateAutoMonitorBluetooth(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_MONITOR_BT] = enabled
        }
    }

    override suspend fun updateTargetBluetoothName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TARGET_BT_NAME] = name.trim()
        }
    }

    override suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
