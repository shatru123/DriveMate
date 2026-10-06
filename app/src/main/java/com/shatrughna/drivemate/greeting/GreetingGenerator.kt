package com.shatrughna.drivemate.greeting

import com.shatrughna.drivemate.data.model.GreetingStyle
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
            "{timeOfDay}"
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
        timestampEpochMillis: Long,
        zoneId: ZoneId
    ): String {
        val name = driverName.ifBlank { "Driver" }
        val brand = vehicleBrand.ifBlank { "TATA" }
        val model = vehicleModel.ifBlank { "Nexon" }
        val variant = vehicleVariant.ifBlank { "Creative+ S" }
        val timePeriod = getTimePeriod(timestampEpochMillis, zoneId)

        return when (style) {
            GreetingStyle.SHORT -> {
                "Hey $name, welcome to your $model."
            }
            GreetingStyle.NORMAL -> {
                when (timePeriod) {
                    TimePeriod.MORNING -> {
                        "Good morning, $name. Welcome to your $brand $model. Have a safe drive."
                    }
                    TimePeriod.AFTERNOON -> {
                        "Good afternoon, $name. Welcome back to your $brand $model. Have a pleasant journey."
                    }
                    TimePeriod.EVENING -> {
                        "Good evening, $name. Welcome back to your $model. Drive safely."
                    }
                    TimePeriod.NIGHT -> {
                        "Good evening, $name. Welcome to your $brand $model. Please drive safely."
                    }
                }
            }
            GreetingStyle.DETAILED -> {
                val salutation = "${timePeriod.salutation}, $name."
                "$salutation Welcome back to your $brand $model $variant. Your journey is ready. Have a safe and pleasant drive."
            }
            GreetingStyle.CUSTOM -> {
                val template = customTemplate?.trim()
                if (template.isNullOrBlank() || validateTemplate(template) is TemplateValidationResult.Invalid) {
                    // Fall back to clean normal greeting if custom template is empty/invalid
                    generateGreeting(name, brand, model, variant, GreetingStyle.NORMAL, null, timestampEpochMillis, zoneId)
                } else {
                    template
                        .replace("{name}", name)
                        .replace("{brand}", brand)
                        .replace("{model}", model)
                        .replace("{variant}", variant)
                        .replace("{timeOfDay}", timePeriod.tokenValue)
                }
            }
        }
    }
}
