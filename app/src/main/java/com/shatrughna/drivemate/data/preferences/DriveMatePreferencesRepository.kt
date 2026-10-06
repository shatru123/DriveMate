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
    suspend fun updateVehicleCare(odometerKm: Int, nextServiceKm: Int, fuelReminder: Boolean)
    suspend fun updateFavoriteAddresses(home: String, office: String)
    suspend fun recordCompletedTrip(distanceKm: Float, durationMinutes: Long)
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

        // V2 Keys
        val INCLUDE_WEATHER = booleanPreferencesKey("include_weather_in_greeting")
        val WEATHER_CITY = stringPreferencesKey("weather_city_name")
        val WEATHER_LAT = doublePreferencesKey("weather_latitude")
        val WEATHER_LON = doublePreferencesKey("weather_longitude")
        val ODOMETER_KM = intPreferencesKey("odometer_km")
        val NEXT_SERVICE_KM = intPreferencesKey("next_service_km")
        val FUEL_REMINDER = booleanPreferencesKey("fuel_reminder_enabled")
        val HOME_ADDRESS = stringPreferencesKey("home_address")
        val OFFICE_ADDRESS = stringPreferencesKey("office_address")

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
                targetBluetoothName = preferences[PreferencesKeys.TARGET_BT_NAME] ?: "Tata Nexon",
                includeWeatherInGreeting = preferences[PreferencesKeys.INCLUDE_WEATHER] ?: true,
                weatherCityName = preferences[PreferencesKeys.WEATHER_CITY] ?: "Pune",
                weatherLatitude = preferences[PreferencesKeys.WEATHER_LAT] ?: 18.5204,
                weatherLongitude = preferences[PreferencesKeys.WEATHER_LON] ?: 73.8567,
                odometerKm = preferences[PreferencesKeys.ODOMETER_KM] ?: 12500,
                nextServiceKm = preferences[PreferencesKeys.NEXT_SERVICE_KM] ?: 15000,
                fuelReminderEnabled = preferences[PreferencesKeys.FUEL_REMINDER] ?: true,
                homeAddress = preferences[PreferencesKeys.HOME_ADDRESS] ?: "Home",
                officeAddress = preferences[PreferencesKeys.OFFICE_ADDRESS] ?: "Office"
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

    override suspend fun updateVehicleCare(odometerKm: Int, nextServiceKm: Int, fuelReminder: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ODOMETER_KM] = odometerKm
            preferences[PreferencesKeys.NEXT_SERVICE_KM] = nextServiceKm
            preferences[PreferencesKeys.FUEL_REMINDER] = fuelReminder
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

            // Update odometer automatically with the driven distance
            val currentOdometer = preferences[PreferencesKeys.ODOMETER_KM] ?: 12500
            preferences[PreferencesKeys.ODOMETER_KM] = currentOdometer + distanceKm.toInt()
        }
    }

    override suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
