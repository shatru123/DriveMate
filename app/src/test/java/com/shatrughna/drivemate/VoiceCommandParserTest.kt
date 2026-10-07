package com.shatrughna.drivemate

import com.shatrughna.drivemate.voice.VoiceCommand
import com.shatrughna.drivemate.voice.VoiceCommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VoiceCommandParserTest {

    private lateinit var parser: VoiceCommandParser

    @Before
    fun setUp() {
        parser = VoiceCommandParser()
    }

    @Test
    fun testParsePlayMusicSpotify() {
        val result = parser.parse("Play Believer on Spotify")
        assertTrue(result is VoiceCommand.PlayMusic)
        val cmd = result as VoiceCommand.PlayMusic
        assertEquals("believer", cmd.query)
        assertEquals("Spotify", cmd.appName)
    }

    @Test
    fun testParsePlayMusicYouTubeMusic() {
        val result = parser.parse("play classical playlist on YouTube Music")
        assertTrue(result is VoiceCommand.PlayMusic)
        val cmd = result as VoiceCommand.PlayMusic
        assertEquals("classical playlist", cmd.query)
        assertEquals("YouTube Music", cmd.appName)
    }

    @Test
    fun testParsePlayMusicDefault() {
        val result = parser.parse("listen to Arijit Singh")
        assertTrue(result is VoiceCommand.PlayMusic)
        val cmd = result as VoiceCommand.PlayMusic
        assertEquals("arijit singh", cmd.query)
    }

    @Test
    fun testParseNavigationDestination() {
        val result = parser.parse("navigate to Phoenix Mall")
        assertTrue(result is VoiceCommand.Navigate)
        val cmd = result as VoiceCommand.Navigate
        assertEquals("phoenix mall", cmd.destination)
    }

    @Test
    fun testParseDirectionsHome() {
        val result = parser.parse("directions to Home")
        assertTrue(result is VoiceCommand.Navigate)
        val cmd = result as VoiceCommand.Navigate
        assertEquals("home", cmd.destination)
    }

    @Test
    fun testParseNearestPetrolPump() {
        val result = parser.parse("find nearest petrol pump")
        assertTrue(result is VoiceCommand.Navigate)
        val cmd = result as VoiceCommand.Navigate
        assertTrue(cmd.destination.contains("petrol pump"))
    }

    @Test
    fun testParseWeatherQueries() {
        assertTrue(parser.parse("what is the weather today?") is VoiceCommand.CheckWeather)
        assertTrue(parser.parse("how is the temperature") is VoiceCommand.CheckWeather)
        assertTrue(parser.parse("will it rain") is VoiceCommand.CheckWeather)
    }

    @Test
    fun testParseTripStats() {
        assertTrue(parser.parse("trip status") is VoiceCommand.CheckTripStats)
        assertTrue(parser.parse("how long have I been driving") is VoiceCommand.CheckTripStats)
        assertTrue(parser.parse("how far have I driven") is VoiceCommand.CheckTripStats)
    }

    @Test
    fun testParseVehicleCare() {
        assertTrue(parser.parse("when is my next service") is VoiceCommand.CheckVehicleCare)
        assertTrue(parser.parse("service status") is VoiceCommand.CheckVehicleCare)
        assertTrue(parser.parse("check odometer") is VoiceCommand.CheckVehicleCare)
    }

    @Test
    fun testParseFindCar() {
        assertTrue(parser.parse("where did I park") is VoiceCommand.FindCar)
        assertTrue(parser.parse("where is my car") is VoiceCommand.FindCar)
        assertTrue(parser.parse("find my car") is VoiceCommand.FindCar)
        assertTrue(parser.parse("where is my nexon") is VoiceCommand.FindCar)
    }

    @Test
    fun testParseWatchVideoYouTube() {
        val result = parser.parse("watch car review on YouTube")
        assertTrue(result is VoiceCommand.WatchVideo)
        val cmd = result as VoiceCommand.WatchVideo
        assertEquals("car review", cmd.query)
    }

    @Test
    fun testParseUnknownCommand() {
        val result = parser.parse("xyz random gibberish 123")
        assertTrue(result is VoiceCommand.Unknown)
    }
}
