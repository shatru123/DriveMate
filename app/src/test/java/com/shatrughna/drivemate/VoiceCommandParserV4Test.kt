package com.shatrughna.drivemate

import com.shatrughna.drivemate.voice.VoiceCommand
import com.shatrughna.drivemate.voice.VoiceCommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VoiceCommandParserV4Test {

    private lateinit var parser: VoiceCommandParser

    @Before
    fun setUp() {
        parser = VoiceCommandParser()
    }

    @Test
    fun testParseClimateControlTurnOnAc() {
        val result = parser.parse("turn on ac")
        assertTrue(result is VoiceCommand.ControlClimate)
        val cmd = result as VoiceCommand.ControlClimate
        assertTrue(cmd.action.contains("ac"))
    }

    @Test
    fun testParseClimateControlSetTemperature() {
        val result = parser.parse("set temperature to 22")
        assertTrue(result is VoiceCommand.ControlClimate)
        val cmd = result as VoiceCommand.ControlClimate
        assertEquals(22.0f, cmd.temperature)
    }

    @Test
    fun testParseCameraRequests() {
        val res360 = parser.parse("open 360 camera")
        assertTrue(res360 is VoiceCommand.ViewCamera)
        assertEquals("360", (res360 as VoiceCommand.ViewCamera).cameraType)

        val resReverse = parser.parse("show reverse camera")
        assertTrue(resReverse is VoiceCommand.ViewCamera)
        assertEquals("reverse", (resReverse as VoiceCommand.ViewCamera).cameraType)
    }

    @Test
    fun testParseDocumentInquiries() {
        val resInsurance = parser.parse("is my insurance valid")
        assertTrue(resInsurance is VoiceCommand.CheckDocument)

        val resPuc = parser.parse("when does puc expire")
        assertTrue(resPuc is VoiceCommand.CheckDocument)
    }

    @Test
    fun testParseSaveParking() {
        val resSave = parser.parse("save parking spot")
        assertTrue(resSave is VoiceCommand.SaveParking)

        val resSave2 = parser.parse("remember where i parked")
        assertTrue(resSave2 is VoiceCommand.SaveParking)
    }

    @Test
    fun testParseExpenses() {
        val resFuel = parser.parse("how much did i spend on fuel")
        assertTrue(resFuel is VoiceCommand.CheckExpenses)

        val resTotal = parser.parse("show total expenses")
        assertTrue(resTotal is VoiceCommand.CheckExpenses)
    }
}
