package com.shatrughna.drivemate.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shatrughna.drivemate.data.model.DriveMateSettings
import com.shatrughna.drivemate.data.model.GreetingStyle
import com.shatrughna.drivemate.data.model.TripStats
import com.shatrughna.drivemate.util.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "drivemate_settings")

/**
 * Repository interface for managing persistent application settings and driving statistics.
 */
interface DriveMatePreferencesRepository {
    val settingsFlow: Flow<DriveMateSettings>
    val tripStatsFlow: Flow<TripStats>

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

    // V2 Methods
    suspend fun updateWeatherSettings(includeInGreeting: Boolean, cityName: String, lat: Double, lon: Double)
    suspend fun updateAutoDetectLocation(enabled: Boolean)
    suspend fun updateVehicleCare(odometerKm: Double, nextServiceKm: Int, fuelReminder: Boolean)
    suspend fun updateVehicleCare(odometerKm: Int, nextServiceKm: Int, fuelReminder: Boolean)
    suspend fun updateVehicleRegistration(regNumber: String)
    suspend fun updateVehiclePhotoUri(uriString: String?)
    suspend fun updateFavoriteAddresses(home: String, office: String)
    suspend fun recordCompletedTrip(distanceKm: Float, durationMinutes: Long)
    suspend fun updateLastParkedLocation(lat: Double, lon: Double, address: String?)
    suspend fun updateDriverFatigueAlert(enabled: Boolean)
    suspend fun updatePreferredMusicApp(app: String)
    suspend fun updateVoiceAssistantEnabled(enabled: Boolean)
    suspend fun updateVoiceAssistantLanguage(language: String)
    suspend fun updateHeyDriveMateEnabled(enabled: Boolean)
    suspend fun updateWakeWordSensitivity(sensitivity: Float)
    suspend fun updateDemoModeEnabled(enabled: Boolean)
    suspend fun updateAutoGreetingOnAndroidAuto(enabled: Boolean)
    suspend fun syncActiveProfile(
        driverName: String,
        vehicleBrand: String,
        vehicleModel: String,
        vehicleVariant: String,
        registrationNumber: String,
        odometerKm: Double?,
        photoUri: String?
    ) {}
    suspend fun clearActiveUserSession() {}
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
        val AUTO_GREETING_AA = booleanPreferencesKey("auto_greeting_on_android_auto")

        // V2 Keys
        val INCLUDE_WEATHER = booleanPreferencesKey("include_weather_in_greeting")
        val AUTO_DETECT_LOCATION = booleanPreferencesKey("auto_detect_location")
        val WEATHER_CITY = stringPreferencesKey("weather_city_name")
        val WEATHER_LAT = doublePreferencesKey("weather_latitude")
        val WEATHER_LON = doublePreferencesKey("weather_longitude")
        val VEHICLE_REGISTRATION = stringPreferencesKey("vehicle_registration_number")
        val VEHICLE_PHOTO_URI = stringPreferencesKey("vehicle_photo_uri")
        val ODOMETER_DOUBLE = doublePreferencesKey("odometer_km_double")
        val ODOMETER_KM = intPreferencesKey("odometer_km")
        val NEXT_SERVICE_KM = intPreferencesKey("next_service_km")
        val SERVICE_TARGET_CONFIGURED = booleanPreferencesKey("service_target_configured")
        val FUEL_REMINDER = booleanPreferencesKey("fuel_reminder_enabled")
        val HOME_ADDRESS = stringPreferencesKey("home_address")
        val OFFICE_ADDRESS = stringPreferencesKey("office_address")
        val LAST_PARKED_LAT = doublePreferencesKey("last_parked_latitude")
        val LAST_PARKED_LON = doublePreferencesKey("last_parked_longitude")
        val LAST_PARKED_ADDRESS = stringPreferencesKey("last_parked_address")
        val LAST_PARKED_TIMESTAMP = longPreferencesKey("last_parked_timestamp_millis")
        val DRIVER_FATIGUE_ALERT = booleanPreferencesKey("driver_fatigue_alert_enabled")
        val PREFERRED_MUSIC_APP = stringPreferencesKey("preferred_music_app")
        val VOICE_ASSISTANT_ENABLED = booleanPreferencesKey("voice_assistant_enabled")
        val VOICE_ASSISTANT_LANGUAGE = stringPreferencesKey("voice_assistant_language")
        val HEY_DRIVEMATE_ENABLED = booleanPreferencesKey("hey_drivemate_enabled")
        val WAKE_WORD_SENSITIVITY = floatPreferencesKey("wake_word_sensitivity")
        val DEMO_MODE_ENABLED = booleanPreferencesKey("demo_mode_enabled")

        // Trip stats keys
        val TODAY_TRIPS = intPreferencesKey("today_trips_count")
        val TODAY_DISTANCE = floatPreferencesKey("today_total_distance_km")
        val TODAY_DURATION = longPreferencesKey("today_total_duration_minutes")
        val LAST_TRIP_DATE = stringPreferencesKey("last_trip_date")
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
                driverName = preferences[PreferencesKeys.DRIVER_NAME] ?: "",
                vehicleBrand = preferences[PreferencesKeys.VEHICLE_BRAND] ?: "",
                vehicleModel = preferences[PreferencesKeys.VEHICLE_MODEL] ?: "",
                vehicleVariant = preferences[PreferencesKeys.VEHICLE_VARIANT] ?: "",
                vehicleRegistrationNumber = preferences[PreferencesKeys.VEHICLE_REGISTRATION] ?: "",
                vehiclePhotoUri = preferences[PreferencesKeys.VEHICLE_PHOTO_URI],
                greetingEnabled = preferences[PreferencesKeys.GREETING_ENABLED] ?: true,
                autoGreetingOnAndroidAuto = preferences[PreferencesKeys.AUTO_GREETING_AA] ?: false,
                greetingStyle = GreetingStyle.fromName(preferences[PreferencesKeys.GREETING_STYLE]),
                customGreetingTemplate = preferences[PreferencesKeys.CUSTOM_GREETING_TEMPLATE]
                    ?: "Good {timeOfDay}, {name}. Welcome to your vehicle.",
                speechRate = preferences[PreferencesKeys.SPEECH_RATE] ?: 1.0f,
                pitch = preferences[PreferencesKeys.PITCH] ?: 1.0f,
                languageTag = preferences[PreferencesKeys.LANGUAGE_TAG] ?: "en-IN",
                voiceName = preferences[PreferencesKeys.VOICE_NAME],
                autoMonitorBluetooth = preferences[PreferencesKeys.AUTO_MONITOR_BT] ?: true,
                targetBluetoothName = preferences[PreferencesKeys.TARGET_BT_NAME] ?: "",
                includeWeatherInGreeting = preferences[PreferencesKeys.INCLUDE_WEATHER] ?: true,
                autoDetectLocation = preferences[PreferencesKeys.AUTO_DETECT_LOCATION] ?: true,
                weatherCityName = preferences[PreferencesKeys.WEATHER_CITY] ?: "",
                weatherLatitude = preferences[PreferencesKeys.WEATHER_LAT] ?: 0.0,
                weatherLongitude = preferences[PreferencesKeys.WEATHER_LON] ?: 0.0,
                manualOdometerKm = preferences[PreferencesKeys.ODOMETER_DOUBLE]
                    ?: preferences[PreferencesKeys.ODOMETER_KM]?.toDouble(),
                odometerKm = preferences[PreferencesKeys.ODOMETER_DOUBLE]
                    ?: preferences[PreferencesKeys.ODOMETER_KM]?.toDouble(),
                nextServiceKm = preferences[PreferencesKeys.NEXT_SERVICE_KM] ?: 15000,
                serviceTargetConfigured = preferences[PreferencesKeys.SERVICE_TARGET_CONFIGURED] ?: false,
                fuelReminderEnabled = preferences[PreferencesKeys.FUEL_REMINDER] ?: false,
                homeAddress = preferences[PreferencesKeys.HOME_ADDRESS] ?: "",
                officeAddress = preferences[PreferencesKeys.OFFICE_ADDRESS] ?: "",
                lastParkedLatitude = preferences[PreferencesKeys.LAST_PARKED_LAT],
                lastParkedLongitude = preferences[PreferencesKeys.LAST_PARKED_LON],
                lastParkedAddress = preferences[PreferencesKeys.LAST_PARKED_ADDRESS],
                lastParkedTimestampMillis = preferences[PreferencesKeys.LAST_PARKED_TIMESTAMP],
                driverFatigueAlertEnabled = preferences[PreferencesKeys.DRIVER_FATIGUE_ALERT] ?: true,
                preferredMusicApp = preferences[PreferencesKeys.PREFERRED_MUSIC_APP] ?: "Spotify",
                voiceAssistantEnabled = preferences[PreferencesKeys.VOICE_ASSISTANT_ENABLED] ?: true,
                voiceAssistantLanguage = preferences[PreferencesKeys.VOICE_ASSISTANT_LANGUAGE] ?: "auto",
                heyDriveMateEnabled = preferences[PreferencesKeys.HEY_DRIVEMATE_ENABLED] ?: false,
                wakeWordSensitivity = preferences[PreferencesKeys.WAKE_WORD_SENSITIVITY] ?: 0.5f,
                isDemoModeEnabled = preferences[PreferencesKeys.DEMO_MODE_ENABLED] ?: false
            )
        }

    override val tripStatsFlow: Flow<TripStats> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val todayStr = LocalDate.now().toString()
            val savedDate = preferences[PreferencesKeys.LAST_TRIP_DATE]

            // If new day, reset today's stats representation
            if (savedDate != todayStr) {
                TripStats(todayTripsCount = 0, todayTotalDistanceKm = 0.0f, todayTotalDurationMinutes = 0L)
            } else {
                TripStats(
                    todayTripsCount = preferences[PreferencesKeys.TODAY_TRIPS] ?: 0,
                    todayTotalDistanceKm = preferences[PreferencesKeys.TODAY_DISTANCE] ?: 0.0f,
                    todayTotalDurationMinutes = preferences[PreferencesKeys.TODAY_DURATION] ?: 0L
                )
            }
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

    override suspend fun updateWeatherSettings(
        includeInGreeting: Boolean,
        cityName: String,
        lat: Double,
        lon: Double
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.INCLUDE_WEATHER] = includeInGreeting
            preferences[PreferencesKeys.WEATHER_CITY] = cityName.trim()
            preferences[PreferencesKeys.WEATHER_LAT] = lat
            preferences[PreferencesKeys.WEATHER_LON] = lon
        }
    }

    override suspend fun updateAutoDetectLocation(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_DETECT_LOCATION] = enabled
        }
    }

    override suspend fun updateVehicleCare(odometerKm: Double, nextServiceKm: Int, fuelReminder: Boolean) {
        context.dataStore.edit { preferences ->
            val currentOdometer = preferences[PreferencesKeys.ODOMETER_DOUBLE]
                ?: preferences[PreferencesKeys.ODOMETER_KM]?.toDouble()
            if (currentOdometer != null && odometerKm < currentOdometer) {
                AppLogger.w(
                    AppLogger.Tag.SETTINGS,
                    "Vehicle care odometer update lower than current value (proposed: $odometerKm, current: $currentOdometer)"
                )
            }
            preferences[PreferencesKeys.ODOMETER_DOUBLE] = odometerKm
            preferences[PreferencesKeys.ODOMETER_KM] = odometerKm.toInt()
            preferences[PreferencesKeys.NEXT_SERVICE_KM] = nextServiceKm
            preferences[PreferencesKeys.SERVICE_TARGET_CONFIGURED] = true
            preferences[PreferencesKeys.FUEL_REMINDER] = fuelReminder
        }
    }

    override suspend fun updateVehicleCare(odometerKm: Int, nextServiceKm: Int, fuelReminder: Boolean) {
        updateVehicleCare(odometerKm.toDouble(), nextServiceKm, fuelReminder)
    }

    override suspend fun updateDemoModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEMO_MODE_ENABLED] = enabled
        }
    }

    override suspend fun updateAutoGreetingOnAndroidAuto(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_GREETING_AA] = enabled
        }
    }

    override suspend fun updateVehicleRegistration(regNumber: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VEHICLE_REGISTRATION] = DriveMateSettings.normalizeRegistration(regNumber)
        }
    }

    override suspend fun updateVehiclePhotoUri(uriString: String?) {
        context.dataStore.edit { preferences ->
            if (uriString != null) {
                preferences[PreferencesKeys.VEHICLE_PHOTO_URI] = uriString
            } else {
                preferences.remove(PreferencesKeys.VEHICLE_PHOTO_URI)
            }
        }
    }

    override suspend fun updateFavoriteAddresses(home: String, office: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HOME_ADDRESS] = home.trim()
            preferences[PreferencesKeys.OFFICE_ADDRESS] = office.trim()
        }
    }

    override suspend fun recordCompletedTrip(distanceKm: Float, durationMinutes: Long) {
        context.dataStore.edit { preferences ->
            val todayStr = LocalDate.now().toString()
            val savedDate = preferences[PreferencesKeys.LAST_TRIP_DATE]

            val (currentTrips, currentDistance, currentDuration) = if (savedDate == todayStr) {
                Triple(
                    preferences[PreferencesKeys.TODAY_TRIPS] ?: 0,
                    preferences[PreferencesKeys.TODAY_DISTANCE] ?: 0.0f,
                    preferences[PreferencesKeys.TODAY_DURATION] ?: 0L
                )
            } else {
                Triple(0, 0.0f, 0L)
            }

            preferences[PreferencesKeys.LAST_TRIP_DATE] = todayStr
            preferences[PreferencesKeys.TODAY_TRIPS] = currentTrips + 1
            preferences[PreferencesKeys.TODAY_DISTANCE] = currentDistance + distanceKm
            preferences[PreferencesKeys.TODAY_DURATION] = currentDuration + durationMinutes

            // Strict Data Honesty: GPS distance accumulates in trip stats only, never modifying the vehicle odometer
        }
    }

    override suspend fun updateLastParkedLocation(lat: Double, lon: Double, address: String?) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_PARKED_LAT] = lat
            preferences[PreferencesKeys.LAST_PARKED_LON] = lon
            if (address != null) {
                preferences[PreferencesKeys.LAST_PARKED_ADDRESS] = address
            } else {
                preferences.remove(PreferencesKeys.LAST_PARKED_ADDRESS)
            }
            preferences[PreferencesKeys.LAST_PARKED_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    override suspend fun updateDriverFatigueAlert(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DRIVER_FATIGUE_ALERT] = enabled
        }
    }

    override suspend fun updatePreferredMusicApp(app: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PREFERRED_MUSIC_APP] = app
        }
    }

    override suspend fun updateVoiceAssistantEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VOICE_ASSISTANT_ENABLED] = enabled
        }
    }

    override suspend fun updateVoiceAssistantLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VOICE_ASSISTANT_LANGUAGE] = language.trim().lowercase()
        }
    }

    override suspend fun updateHeyDriveMateEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HEY_DRIVEMATE_ENABLED] = enabled
        }
    }

    override suspend fun updateWakeWordSensitivity(sensitivity: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WAKE_WORD_SENSITIVITY] = sensitivity.coerceIn(0.1f, 1.0f)
        }
    }

    override suspend fun syncActiveProfile(
        driverName: String,
        vehicleBrand: String,
        vehicleModel: String,
        vehicleVariant: String,
        registrationNumber: String,
        odometerKm: Double?,
        photoUri: String?
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DRIVER_NAME] = driverName
            preferences[PreferencesKeys.VEHICLE_BRAND] = vehicleBrand
            preferences[PreferencesKeys.VEHICLE_MODEL] = vehicleModel
            preferences[PreferencesKeys.VEHICLE_VARIANT] = vehicleVariant
            preferences[PreferencesKeys.VEHICLE_REGISTRATION] = registrationNumber
            odometerKm?.let { preferences[PreferencesKeys.ODOMETER_DOUBLE] = it }
            if (photoUri != null) {
                preferences[PreferencesKeys.VEHICLE_PHOTO_URI] = photoUri
            } else {
                preferences.remove(PreferencesKeys.VEHICLE_PHOTO_URI)
            }
        }
    }

    override suspend fun clearActiveUserSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.DRIVER_NAME)
            preferences.remove(PreferencesKeys.VEHICLE_BRAND)
            preferences.remove(PreferencesKeys.VEHICLE_MODEL)
            preferences.remove(PreferencesKeys.VEHICLE_VARIANT)
            preferences.remove(PreferencesKeys.VEHICLE_REGISTRATION)
            preferences.remove(PreferencesKeys.VEHICLE_PHOTO_URI)
            preferences.remove(PreferencesKeys.ODOMETER_DOUBLE)
            preferences.remove(PreferencesKeys.ODOMETER_KM)
            preferences.remove(PreferencesKeys.LAST_PARKED_LAT)
            preferences.remove(PreferencesKeys.LAST_PARKED_LON)
            preferences.remove(PreferencesKeys.LAST_PARKED_ADDRESS)
        }
    }

    override suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
