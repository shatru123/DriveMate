package com.shatrughna.drivemate.voice

/**
 * Driver intents recognized by DriveMate Voice Assistant.
 */
sealed class VoiceCommand {
    /**
     * Search and stream music / podcasts on Spotify, YouTube Music, or default media player.
     * e.g., "Play Believer on Spotify", "Play retro Hindi songs"
     */
    data class PlayMusic(val query: String, val appName: String? = null) : VoiceCommand()

    /**
     * Start turn-by-turn navigation via Google Maps or default GPS navigator.
     * e.g., "Navigate to Phoenix Mall", "Directions to Home", "Find nearest petrol pump"
     */
    data class Navigate(val destination: String) : VoiceCommand()

    /**
     * Ask for live weather report for current location.
     * e.g., "What is the weather?", "Will it rain today?"
     */
    data object CheckWeather : VoiceCommand()

    /**
     * Inquire about active trip stats.
     * e.g., "Trip status", "How long have I been driving?"
     */
    data object CheckTripStats : VoiceCommand()

    /**
     * Inquire about vehicle maintenance or service countdown.
     * e.g., "When is my next service?", "Check odometer"
     */
    data object CheckVehicleCare : VoiceCommand()

    /**
     * Inquire where the car is parked.
     * e.g., "Where did I park?", "Find my car"
     */
    data object FindCar : VoiceCommand()

    /**
     * Save the vehicle's current parked location.
     * e.g., "Save parking spot", "I parked here"
     */
    data object SaveParking : VoiceCommand()

    /**
     * Climate control actuation inquiry or request.
     * e.g., "Turn on AC", "Set temperature to 22", "Turn off AC"
     */
    data class ControlClimate(val action: String, val temperature: Float? = null) : VoiceCommand()

    /**
     * Check status or expiry of driver/vehicle documents.
     * e.g., "Is my insurance valid?", "When does PUC expire?", "Show my RC"
     */
    data class CheckDocument(val documentQuery: String) : VoiceCommand()

    /**
     * Check vehicle maintenance schedule or next service countdown.
     * e.g., "When is next service due?", "Service status", "Maintenance check"
     */
    data object CheckMaintenance : VoiceCommand()

    /**
     * Query vehicle running costs and expenses.
     * e.g., "How much did I spend on fuel?", "Total expenses this month"
     */
    data class CheckExpenses(val categoryQuery: String? = null) : VoiceCommand()

    /**
     * Request 360 or reverse camera view.
     * e.g., "Open 360 camera", "Show reverse camera"
     */
    data class ViewCamera(val cameraType: String) : VoiceCommand()

    /**
     * User requested video content (e.g. YouTube video).
     * Handled according to car safety regulations (audio when moving, phone video when parked).
     */
    data class WatchVideo(val query: String) : VoiceCommand()

    /**
     * Unrecognized query.
     */
    data class Unknown(val rawQuery: String) : VoiceCommand()
}
