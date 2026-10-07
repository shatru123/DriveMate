package com.shatrughna.drivemate.data.model

import java.util.UUID

/**
 * Origin of how the destination was created or resolved.
 */
enum class DestinationSource {
    SAVED,
    MAP_SEARCH,
    VOICE_SEARCH,
    RECENT,
    SUGGESTED
}

/**
 * High-level category for driver-safe icons and categorization.
 */
enum class DestinationCategory {
    HOME,
    OFFICE,
    FUEL,
    SERVICE_CENTER,
    AIRPORT,
    STATION,
    FOOD,
    SHOPPING,
    HOSPITAL,
    PARKING,
    CUSTOM
}

/**
 * Core destination model for arbitrary search, recent history, voice targets, and contextual suggestions.
 */
data class Destination(
    val id: String? = UUID.randomUUID().toString(),
    val name: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val source: DestinationSource = DestinationSource.SUGGESTED,
    val query: String? = null,
    val category: DestinationCategory = DestinationCategory.CUSTOM,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    val title: String get() = name
    val subtitle: String get() = address ?: query ?: ""
    val searchQueryOrAddress: String get() = query ?: address ?: name
}
