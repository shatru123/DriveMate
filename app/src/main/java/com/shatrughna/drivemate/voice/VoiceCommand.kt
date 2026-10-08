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
     * Inquire about current vehicle driving speed.
     * e.g., "What is my speed?", "मेरी स्पीड कितनी है?", "माझा स्पीड किती आहे?"
     */
    data object CheckSpeed : VoiceCommand()

    /**
     * Inquire about vehicle odometer distance.
     * e.g., "What is my odometer?", "मेरी गाड़ी कितने किलोमीटर चली है?", "माझ्या गाडीचे किती किलोमीटर झाले?"
     */
    data object CheckOdometer : VoiceCommand()

    /**
     * Inquire about remaining fuel or battery percentage.
     * e.g., "How much fuel is left?", "गाड़ी में कितना पेट्रोल है?", "गाडीमध्ये किती पेट्रोल आहे?"
     */
    data object CheckFuel : VoiceCommand()

    /**
     * Inquire about estimated remaining driving range.
     * e.g., "What's my range?", "कितनी range बची है?", "किती range बाकी आहे?"
     */
    data object CheckRange : VoiceCommand()

    /**
     * Inquire about overall vehicle connection and hardware diagnostic status.
     * e.g., "Is my car okay?", "गाड़ी की स्थिति क्या है?", "गाडीची स्थिती काय आहे?"
     */
    data object CheckVehicleStatus : VoiceCommand()

    /**
     * Inquire about average driving speed for active trip.
     * e.g., "What is my average speed?", "एवरेज स्पीड कितनी है?", "सरासरी वेग किती आहे?"
     */
    data object CheckAverageSpeed : VoiceCommand()

    /**
     * Inquire about today's cumulative driving distance and trips.
     * e.g., "How many km did I drive today?", "आज कितने किलोमीटर गाड़ी चलाई?", "आज किती किलोमीटर गाडी चालवली?"
     */
    data object CheckTodayDriving : VoiceCommand()

    /**
     * Inquire about assistant capabilities and help commands.
     * e.g., "Help", "What can you do?", "तुम क्या कर सकते हो?", "तू काय करू शकतोस?"
     */
    data object Help : VoiceCommand()

    /**
     * Ask about assistant identity and info.
     * e.g., "Who are you?", "तुम कौन हो?", "तू कोण आहेस?"
     */
    data object AboutAssistant : VoiceCommand()

    /**
     * Ask about assistant operational readiness and listening status.
     * e.g., "Assistant status", "Are you listening?"
     */
    data object AssistantStatus : VoiceCommand()

    /**
     * Request the assistant to stop listening or cancel active speech.
     * e.g., "Stop", "Cancel", "Never mind", "रुको", "रहने दो", "थांब", "राहू दे"
     */
    data object StopAssistant : VoiceCommand()

    /**
     * Driver conversational greetings.
     * e.g., "Hello", "Good morning", "नमस्ते", "नमस्कार"
     */
    data object Greeting : VoiceCommand()

    /**
     * Driver appreciation / closing remarks.
     * e.g., "Thank you", "Thanks", "धन्यवाद", "आभार"
     */
    data object ThankYou : VoiceCommand()

    /**
     * Unrecognized query.
     */
    data class Unknown(val rawQuery: String) : VoiceCommand()
}
