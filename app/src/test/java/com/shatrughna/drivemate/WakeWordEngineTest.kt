package com.shatrughna.drivemate

import com.shatrughna.drivemate.voice.wakeword.SpeechRecognizerWakeWordEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeWordEngineTest {

    @Test
    fun testParseWakePhrase_standaloneHeyDriveMate() {
        val (isWake, command) = SpeechRecognizerWakeWordEngine.parseWakePhrase("Hey DriveMate")
        assertTrue(isWake)
        assertNull(command)
    }

    @Test
    fun testParseWakePhrase_standaloneHeyDriveMate_withPunctuation() {
        val (isWake, command) = SpeechRecognizerWakeWordEngine.parseWakePhrase("Hey DriveMate!")
        assertTrue(isWake)
        assertNull(command)
    }

    @Test
    fun testParseWakePhrase_standaloneOkDriveMate() {
        val (isWake, command) = SpeechRecognizerWakeWordEngine.parseWakePhrase("OK DriveMate")
        assertTrue(isWake)
        assertNull(command)
    }

    @Test
    fun testParseWakePhrase_compoundTrailingCommand() {
        val (isWake, command) = SpeechRecognizerWakeWordEngine.parseWakePhrase("Hey DriveMate, navigate to office")
        assertTrue(isWake)
        assertEquals("navigate to office", command)
    }

    @Test
    fun testParseWakePhrase_compoundMusicCommand() {
        val (isWake, command) = SpeechRecognizerWakeWordEngine.parseWakePhrase("ok drivemate play believer on spotify")
        assertTrue(isWake)
        assertEquals("play believer on spotify", command)
    }

    @Test
    fun testParseWakePhrase_rejectsFalsePositives() {
        // Individual isolated words must not trigger
        assertFalse(SpeechRecognizerWakeWordEngine.parseWakePhrase("hey").first)
        assertFalse(SpeechRecognizerWakeWordEngine.parseWakePhrase("drive").first)
        assertFalse(SpeechRecognizerWakeWordEngine.parseWakePhrase("mate").first)
        assertFalse(SpeechRecognizerWakeWordEngine.parseWakePhrase("hey drive").first)
        assertFalse(SpeechRecognizerWakeWordEngine.parseWakePhrase("hey driver").first)
        assertFalse(SpeechRecognizerWakeWordEngine.parseWakePhrase("hey driver how are you").first)
        assertFalse(SpeechRecognizerWakeWordEngine.parseWakePhrase("hello google").first)
        assertFalse(SpeechRecognizerWakeWordEngine.parseWakePhrase("navigate to home").first)
    }

    @Test
    fun testParseWakePhrase_standaloneDriveMateOnlyWhenAllowed() {
        // By default, standalone "DriveMate" without "Hey" / "OK" is ignored to prevent false fires
        val defaultResult = SpeechRecognizerWakeWordEngine.parseWakePhrase("DriveMate", allowStandalone = false)
        assertFalse(defaultResult.first)

        // When sensitivity is high and allowStandalone is true:
        val allowedResult = SpeechRecognizerWakeWordEngine.parseWakePhrase("DriveMate", allowStandalone = true)
        assertTrue(allowedResult.first)
        assertNull(allowedResult.second)
    }
}
