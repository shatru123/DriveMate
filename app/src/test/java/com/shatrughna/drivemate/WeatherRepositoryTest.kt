package com.shatrughna.drivemate

import com.shatrughna.drivemate.data.model.WeatherInfo
import com.shatrughna.drivemate.weather.OpenMeteoWeatherRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WeatherRepositoryTest {

    private lateinit var weatherRepository: OpenMeteoWeatherRepository

    @Before
    fun setUp() {
        weatherRepository = OpenMeteoWeatherRepository()
    }

    @Test
    fun testWmoWeatherCodeMapping() {
        assertEquals("Clear", weatherRepository.mapWmoCodeToCondition(0))
        assertEquals("Mainly clear", weatherRepository.mapWmoCodeToCondition(1))
        assertEquals("Partly cloudy", weatherRepository.mapWmoCodeToCondition(2))
        assertEquals("Overcast", weatherRepository.mapWmoCodeToCondition(3))
        assertEquals("Foggy", weatherRepository.mapWmoCodeToCondition(45))
        assertEquals("Drizzle", weatherRepository.mapWmoCodeToCondition(51))
        assertEquals("Rainy", weatherRepository.mapWmoCodeToCondition(61))
        assertEquals("Heavy rain", weatherRepository.mapWmoCodeToCondition(65))
        assertEquals("Snowy", weatherRepository.mapWmoCodeToCondition(71))
        assertEquals("Thunderstorm", weatherRepository.mapWmoCodeToCondition(95))
        assertEquals("Pleasant", weatherRepository.mapWmoCodeToCondition(999))
    }

    @Test
    fun testWeatherInfoSpeechFormatting() {
        val weatherInfo = WeatherInfo(
            temperatureCelsius = 25.4f,
            weatherCode = 0,
            conditionText = "Clear",
            cityName = "Pune"
        )
        assertEquals("25°C and clear in Pune", weatherInfo.speechFormattedDescription)
        assertEquals("25°C", weatherInfo.displayTemperature)
    }
}
