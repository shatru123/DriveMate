package com.shatrughna.drivemate

import com.shatrughna.drivemate.data.model.GreetingStyle
import com.shatrughna.drivemate.greeting.GreetingGeneratorImpl
import com.shatrughna.drivemate.greeting.TemplateValidationResult
import com.shatrughna.drivemate.greeting.TimePeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class GreetingGeneratorTest {

    private lateinit var generator: GreetingGeneratorImpl
    private val zoneId = ZoneId.of("Asia/Kolkata")

    @Before
    fun setUp() {
        generator = GreetingGeneratorImpl()
    }

    private fun epochMillisForHour(hour: Int): Long {
        return ZonedDateTime.of(2026, 10, 6, hour, 30, 0, 0, zoneId)
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun testTimePeriodDetection() {
        // Morning: 05:00 - 11:59
        assertEquals(TimePeriod.MORNING, generator.getTimePeriod(epochMillisForHour(5), zoneId))
        assertEquals(TimePeriod.MORNING, generator.getTimePeriod(epochMillisForHour(8), zoneId))
        assertEquals(TimePeriod.MORNING, generator.getTimePeriod(epochMillisForHour(11), zoneId))

        // Afternoon: 12:00 - 16:59
        assertEquals(TimePeriod.AFTERNOON, generator.getTimePeriod(epochMillisForHour(12), zoneId))
        assertEquals(TimePeriod.AFTERNOON, generator.getTimePeriod(epochMillisForHour(14), zoneId))
        assertEquals(TimePeriod.AFTERNOON, generator.getTimePeriod(epochMillisForHour(16), zoneId))

        // Evening: 17:00 - 21:59
        assertEquals(TimePeriod.EVENING, generator.getTimePeriod(epochMillisForHour(17), zoneId))
        assertEquals(TimePeriod.EVENING, generator.getTimePeriod(epochMillisForHour(19), zoneId))
        assertEquals(TimePeriod.EVENING, generator.getTimePeriod(epochMillisForHour(21), zoneId))

        // Night: 22:00 - 04:59
        assertEquals(TimePeriod.NIGHT, generator.getTimePeriod(epochMillisForHour(22), zoneId))
        assertEquals(TimePeriod.NIGHT, generator.getTimePeriod(epochMillisForHour(23), zoneId))
        assertEquals(TimePeriod.NIGHT, generator.getTimePeriod(epochMillisForHour(1), zoneId))
        assertEquals(TimePeriod.NIGHT, generator.getTimePeriod(epochMillisForHour(4), zoneId))
    }

    @Test
    fun testMorningGreeting() {
        val greeting = generator.generateGreeting(
            driverName = "Shatrughna",
            vehicleBrand = "Tata",
            vehicleModel = "Nexon",
            vehicleVariant = "Creative+ S",
            style = GreetingStyle.NORMAL,
            timestampEpochMillis = epochMillisForHour(8),
            zoneId = zoneId
        )
        assertEquals("Good morning, Shatrughna. Welcome to your Tata Nexon. Have a safe drive.", greeting)
    }

    @Test
    fun testAfternoonGreeting() {
        val greeting = generator.generateGreeting(
            driverName = "Shatrughna",
            vehicleBrand = "Tata",
            vehicleModel = "Nexon",
            vehicleVariant = "Creative+ S",
            style = GreetingStyle.NORMAL,
            timestampEpochMillis = epochMillisForHour(14),
            zoneId = zoneId
        )
        assertEquals("Good afternoon, Shatrughna. Welcome back to your Tata Nexon. Have a pleasant journey.", greeting)
    }

    @Test
    fun testEveningGreeting() {
        val greeting = generator.generateGreeting(
            driverName = "Shatrughna",
            vehicleBrand = "Tata",
            vehicleModel = "Nexon",
            vehicleVariant = "Creative+ S",
            style = GreetingStyle.NORMAL,
            timestampEpochMillis = epochMillisForHour(19),
            zoneId = zoneId
        )
        assertEquals("Good evening, Shatrughna. Welcome back to your Nexon. Drive safely.", greeting)
    }

    @Test
    fun testNightGreeting() {
        val greeting = generator.generateGreeting(
            driverName = "Shatrughna",
            vehicleBrand = "Tata",
            vehicleModel = "Nexon",
            vehicleVariant = "Creative+ S",
            style = GreetingStyle.NORMAL,
            timestampEpochMillis = epochMillisForHour(23),
            zoneId = zoneId
        )
        assertEquals("Good evening, Shatrughna. Welcome to your Tata Nexon. Please drive safely.", greeting)
    }

    @Test
    fun testShortGreeting() {
        val greeting = generator.generateGreeting(
            driverName = "Shatrughna",
            vehicleBrand = "Tata",
            vehicleModel = "Nexon",
            vehicleVariant = "Creative+ S",
            style = GreetingStyle.SHORT,
            timestampEpochMillis = epochMillisForHour(10),
            zoneId = zoneId
        )
        assertEquals("Hey Shatrughna, welcome to your Nexon.", greeting)
    }

    @Test
    fun testDetailedGreeting() {
        val greeting = generator.generateGreeting(
            driverName = "Shatrughna",
            vehicleBrand = "Tata",
            vehicleModel = "Nexon",
            vehicleVariant = "Creative+ S",
            style = GreetingStyle.DETAILED,
            timestampEpochMillis = epochMillisForHour(18),
            zoneId = zoneId
        )
        assertEquals(
            "Good evening, Shatrughna. Welcome back to your Tata Nexon Creative+ S. Your journey is ready. Have a safe and pleasant drive.",
            greeting
        )
    }

    @Test
    fun testCustomGreetingPlaceholderReplacement() {
        val template = "Good {timeOfDay}, {name}. Welcome to your {brand} {model} {variant}."
        val greeting = generator.generateGreeting(
            driverName = "Shatrughna",
            vehicleBrand = "Tata",
            vehicleModel = "Nexon",
            vehicleVariant = "Creative+ S",
            style = GreetingStyle.CUSTOM,
            customTemplate = template,
            timestampEpochMillis = epochMillisForHour(9),
            zoneId = zoneId
        )
        assertEquals("Good morning, Shatrughna. Welcome to your Tata Nexon Creative+ S.", greeting)
    }

    @Test
    fun testTemplateValidation() {
        val validTemplate = "Hello {name}, driving your {brand} {model}!"
        assertTrue(generator.validateTemplate(validTemplate) is TemplateValidationResult.Valid)

        val invalidTemplate = "Hello {driver}, enjoy your {car}!"
        val result = generator.validateTemplate(invalidTemplate)
        assertTrue(result is TemplateValidationResult.Invalid)
        val invalidResult = result as TemplateValidationResult.Invalid
        assertTrue(invalidResult.unknownTokens.contains("{driver}"))
        assertTrue(invalidResult.unknownTokens.contains("{car}"))
    }
}
