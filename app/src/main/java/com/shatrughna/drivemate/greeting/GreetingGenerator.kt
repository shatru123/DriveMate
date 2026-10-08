package com.shatrughna.drivemate.greeting

import com.shatrughna.drivemate.data.model.GreetingStyle
import com.shatrughna.drivemate.data.model.WeatherInfo
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Categorization of the day for tailored automotive greetings.
 */
enum class TimePeriod(val tokenValue: String, val salutation: String) {
    MORNING("morning", "Good morning"),
    AFTERNOON("afternoon", "Good afternoon"),
    EVENING("evening", "Good evening"),
    NIGHT("night", "Good evening") // Per Nexon specification, uses "Good evening" salutation at night
}

/**
 * Result of custom template validation.
 */
sealed class TemplateValidationResult {
    data object Valid : TemplateValidationResult()
    data class Invalid(val reason: String, val unknownTokens: List<String>) : TemplateValidationResult()
}

/**
 * Contract for generating personalized vehicle greetings.
 * Kept completely decoupled from Android UI and TTS engines.
 */
interface GreetingGenerator {

    fun generateGreeting(
        driverName: String,
        vehicleBrand: String,
        vehicleModel: String,
        vehicleVariant: String,
        style: GreetingStyle,
        customTemplate: String? = null,
        weatherInfo: WeatherInfo? = null,
        careReminder: String? = null,
        timestampEpochMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String

    fun getTimePeriod(
        timestampEpochMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): TimePeriod

    fun validateTemplate(template: String): TemplateValidationResult
}

class GreetingGeneratorImpl : GreetingGenerator {

    companion object {
        val SUPPORTED_PLACEHOLDERS = setOf(
            "{name}",
            "{brand}",
            "{model}",
            "{variant}",
            "{timeOfDay}",
            // V2 Placeholders
            "{weather}",
            "{temperature}",
            "{condition}",
            "{city}",
            "{careReminder}"
        )
    }

    override fun getTimePeriod(timestampEpochMillis: Long, zoneId: ZoneId): TimePeriod {
        val zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestampEpochMillis), zoneId)
        val hour = zonedDateTime.hour
        return when (hour) {
            in 5..11 -> TimePeriod.MORNING
            in 12..16 -> TimePeriod.AFTERNOON
            in 17..21 -> TimePeriod.EVENING
            else -> TimePeriod.NIGHT
        }
    }

    override fun validateTemplate(template: String): TemplateValidationResult {
        if (template.isBlank()) {
            return TemplateValidationResult.Invalid("Template cannot be empty.", emptyList())
        }

        val tokenRegex = Regex("\\{([^}]+)\\}")
        val matches = tokenRegex.findAll(template).map { it.value }.toList()
        val unknownTokens = matches.filter { it !in SUPPORTED_PLACEHOLDERS }

        return if (unknownTokens.isNotEmpty()) {
            TemplateValidationResult.Invalid(
                reason = "Unsupported placeholders detected: ${unknownTokens.joinToString(", ")}. Allowed: ${SUPPORTED_PLACEHOLDERS.joinToString(", ")}",
                unknownTokens = unknownTokens
            )
        } else {
            TemplateValidationResult.Valid
        }
    }

    override fun generateGreeting(
        driverName: String,
        vehicleBrand: String,
        vehicleModel: String,
        vehicleVariant: String,
        style: GreetingStyle,
        customTemplate: String?,
        weatherInfo: WeatherInfo?,
        careReminder: String?,
        timestampEpochMillis: Long,
        zoneId: ZoneId
    ): String {
        val name = driverName.ifBlank { "Driver" }
        val brand = vehicleBrand.trim()
        val model = vehicleModel.trim()
        val variant = vehicleVariant.trim()
        val vehicle = listOf(brand, model).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "vehicle" }
        val timePeriod = getTimePeriod(timestampEpochMillis, zoneId)

        return when (style) {
            GreetingStyle.SHORT -> {
                "Hey $name, welcome to your $vehicle."
            }

            GreetingStyle.NORMAL -> {
                val hasWeather = weatherInfo != null && weatherInfo.isAvailable
                val weatherPhrase = if (hasWeather) " It's ${weatherInfo.speechFormattedDescription}." else ""
                when (timePeriod) {
                    TimePeriod.MORNING -> {
                        "Good morning, $name.$weatherPhrase Welcome to your $vehicle. Have a safe drive."
                    }
                    TimePeriod.AFTERNOON -> {
                        val afternoonWeather = if (hasWeather) " It's ${weatherInfo.displayTemperature} outside." else ""
                        "Good afternoon, $name.$afternoonWeather Welcome back to your $vehicle. Have a pleasant journey."
                    }
                    TimePeriod.EVENING -> {
                        "Good evening, $name. Welcome back to your $vehicle. Drive safely."
                    }
                    TimePeriod.NIGHT -> {
                        "Good evening, $name. Welcome to your $vehicle. Please drive safely."
                    }
                }
            }

            GreetingStyle.DETAILED -> {
                val hasWeather = weatherInfo != null && weatherInfo.isAvailable
                val salutation = "${timePeriod.salutation}, $name."
                val weatherPhrase = if (hasWeather) " It is currently ${weatherInfo.speechFormattedDescription}." else ""
                val reminderPhrase = if (!careReminder.isNullOrBlank()) " $careReminder" else ""
                "$salutation$weatherPhrase Welcome back to your $vehicle${if (variant.isNotBlank()) " $variant" else ""}. Your journey is ready.$reminderPhrase Have a safe and pleasant drive."
            }

            GreetingStyle.CUSTOM -> {
                val template = customTemplate?.trim()
                if (template.isNullOrBlank() || validateTemplate(template) is TemplateValidationResult.Invalid) {
                    // Fall back to clean normal greeting
                    generateGreeting(name, brand, model, variant, GreetingStyle.NORMAL, null, weatherInfo, careReminder, timestampEpochMillis, zoneId)
                } else {
                    val weatherDesc = weatherInfo?.speechFormattedDescription ?: "pleasant"
                    val temp = weatherInfo?.displayTemperature ?: ""
                    val cond = weatherInfo?.conditionText?.lowercase() ?: "clear"
                    val city = weatherInfo?.cityName ?: ""
                    val reminder = careReminder ?: ""

                    template
                        .replace("{name}", name)
                        .replace("{brand}", brand)
                        .replace("{model}", model)
                        .replace("{variant}", variant)
                        .replace("{timeOfDay}", timePeriod.tokenValue)
                        .replace("{weather}", weatherDesc)
                        .replace("{temperature}", temp)
                        .replace("{condition}", cond)
                        .replace("{city}", city)
                        .replace("{careReminder}", reminder)
                }
            }
        }
    }
}
