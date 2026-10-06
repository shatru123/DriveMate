package com.shatrughna.drivemate.data.model

/**
 * Represents a saved favorite or suggested driving destination.
 */
data class Destination(
    val id: String,
    val title: String,
    val subtitle: String,
    val searchQueryOrAddress: String,
    val iconType: DestinationType = DestinationType.CUSTOM
)

enum class DestinationType {
    HOME,
    OFFICE,
    GYM,
    CUSTOM
}
