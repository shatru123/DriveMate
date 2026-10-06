package com.shatrughna.drivemate.data.model

/**
 * Supported greeting verbosity and styling options.
 */
enum class GreetingStyle(val displayName: String, val description: String) {
    SHORT(
        displayName = "Short",
        description = "Quick and concise greeting upon connection"
    ),
    NORMAL(
        displayName = "Normal",
        description = "Balanced, time-aware greeting wishing safe travels"
    ),
    DETAILED(
        displayName = "Detailed",
        description = "Full vehicle and journey greeting with variant details"
    ),
    CUSTOM(
        displayName = "Custom",
        description = "User-defined greeting template with placeholder tokens"
    );

    companion object {
        fun fromName(name: String?, default: GreetingStyle = NORMAL): GreetingStyle {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: default
        }
    }
}
