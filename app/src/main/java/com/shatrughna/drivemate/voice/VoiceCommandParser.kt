package com.shatrughna.drivemate.voice

import java.util.Locale

/**
 * Fast, offline natural language rule parser that maps driver speech into VoiceCommand intents.
 * Supports arbitrary dynamic destination navigation, music, weather, vehicle care, and parking.
 */
class VoiceCommandParser {

    fun parse(rawInput: String): VoiceCommand {
        val input = rawInput.trim().lowercase(Locale.getDefault())
        if (input.isBlank()) {
            return VoiceCommand.Unknown(rawInput)
        }

        // 1. Parking / Find Car (Priority check before generic "find" searches)
        val parkingKeywords = listOf("where did i park", "where is my car", "find my car", "where is my nexon", "parking location", "parked location")
        if (parkingKeywords.any { input.contains(it) }) {
            return VoiceCommand.FindCar
        }

        // 2. YouTube Video Requests
        val isVideoRequest = input.startsWith("watch ") ||
                (input.contains(" on youtube") && !input.contains(" on youtube music")) ||
                input.startsWith("open youtube") ||
                input.startsWith("play video")
        if (isVideoRequest) {
            val query = input
                .removePrefix("watch ")
                .removePrefix("play video ")
                .removePrefix("open youtube ")
                .replace("on youtube", "")
                .trim()
            return VoiceCommand.WatchVideo(query.ifBlank { "YouTube" })
        }

        // 3. Music Streaming
        val isMusicRequest = input.startsWith("play ") || input.startsWith("listen to ") || input.contains(" on spotify") || input.contains(" on youtube music")
        if (isMusicRequest) {
            var targetApp: String? = null
            var cleanQuery = input

            if (cleanQuery.contains(" on spotify")) {
                targetApp = "Spotify"
                cleanQuery = cleanQuery.replace(" on spotify", "")
            } else if (cleanQuery.contains(" on youtube music")) {
                targetApp = "YouTube Music"
                cleanQuery = cleanQuery.replace(" on youtube music", "")
            }

            cleanQuery = cleanQuery
                .removePrefix("play song ")
                .removePrefix("play track ")
                .removePrefix("play music ")
                .removePrefix("play ")
                .removePrefix("listen to song ")
                .removePrefix("listen to ")
                .trim()

            if (cleanQuery.isNotBlank()) {
                return VoiceCommand.PlayMusic(query = cleanQuery, appName = targetApp)
            }
        }

        // 4. Navigation & Arbitrary Map Search
        val navPrefixes = listOf(
            "navigate to ",
            "directions to ",
            "direction to ",
            "take me to ",
            "route to ",
            "drive to ",
            "go to ",
            "search for ",
            "find nearest ",
            "locate nearest ",
            "find the nearest ",
            "find a ",
            "find the ",
            "find "
        )
        for (prefix in navPrefixes) {
            if (input.startsWith(prefix)) {
                val destination = input.removePrefix(prefix).trim()
                if (destination.isNotBlank()) {
                    return VoiceCommand.Navigate(destination)
                }
            }
        }
        if (input.contains("nearest petrol pump") || input.contains("nearest gas station") ||
            input.contains("nearest ev charger") || input.contains("nearest cafe") ||
            input.contains("nearest hospital") || input.contains("nearest tata service center")
        ) {
            return VoiceCommand.Navigate(input)
        }

        // 5. Weather Queries
        val weatherKeywords = listOf("weather", "temperature", "forecast", "will it rain", "is it raining", "climate")
        if (weatherKeywords.any { input.contains(it) }) {
            return VoiceCommand.CheckWeather
        }

        // 6. Trip Stats
        val tripKeywords = listOf(
            "trip status",
            "trip info",
            "how long have i been driving",
            "how long have we been driving",
            "how far have i driven",
            "trip distance",
            "trip time"
        )
        if (tripKeywords.any { input.contains(it) }) {
            return VoiceCommand.CheckTripStats
        }

        // 7. Vehicle Care & Service
        val serviceKeywords = listOf("service status", "next service", "service reminder", "car maintenance", "when is service", "odometer")
        if (serviceKeywords.any { input.contains(it) }) {
            return VoiceCommand.CheckVehicleCare
        }

        return VoiceCommand.Unknown(rawInput)
    }
}
