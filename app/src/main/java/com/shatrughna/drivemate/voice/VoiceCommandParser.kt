package com.shatrughna.drivemate.voice

import java.util.Locale

/**
 * Fast, offline natural language rule parser that maps driver speech into VoiceCommand intents.
 */
class VoiceCommandParser {

    fun parse(rawInput: String): VoiceCommand {
        val input = rawInput.trim().lowercase(Locale.getDefault())
        if (input.isBlank()) {
            return VoiceCommand.Unknown(rawInput)
        }

        // 1. YouTube Video Requests
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

        // 2. Music Streaming
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

        // 3. Navigation
        val navPrefixes = listOf(
            "navigate to ",
            "directions to ",
            "take me to ",
            "route to ",
            "drive to ",
            "go to ",
            "direction to ",
            "find nearest ",
            "locate nearest "
        )
        for (prefix in navPrefixes) {
            if (input.startsWith(prefix)) {
                val destination = input.removePrefix(prefix).trim()
                if (destination.isNotBlank()) {
                    return VoiceCommand.Navigate(destination)
                }
            }
        }
        if (input.contains("nearest petrol pump") || input.contains("nearest gas station") || input.contains("nearest ev charger") || input.contains("nearest cafe")) {
            return VoiceCommand.Navigate(input)
        }

        // 4. Weather Queries
        val weatherKeywords = listOf("weather", "temperature", "forecast", "will it rain", "is it raining", "climate")
        if (weatherKeywords.any { input.contains(it) }) {
            return VoiceCommand.CheckWeather
        }

        // 5. Trip Stats
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

        // 6. Vehicle Care & Service
        val serviceKeywords = listOf("service status", "next service", "service reminder", "car maintenance", "when is service", "odometer")
        if (serviceKeywords.any { input.contains(it) }) {
            return VoiceCommand.CheckVehicleCare
        }

        // 7. Parking / Find Car
        val parkingKeywords = listOf("where did i park", "where is my car", "find my car", "where is my nexon", "parking location", "parked location")
        if (parkingKeywords.any { input.contains(it) }) {
            return VoiceCommand.FindCar
        }

        return VoiceCommand.Unknown(rawInput)
    }
}
