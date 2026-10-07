package com.shatrughna.drivemate

import com.shatrughna.drivemate.voice.AudioInputCoordinatorImpl
import com.shatrughna.drivemate.voice.AudioOwnerState
import com.shatrughna.drivemate.voice.AudioResourceOwner
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AudioInputCoordinatorTest {

    private lateinit var coordinator: AudioInputCoordinatorImpl

    @Before
    fun setUp() {
        coordinator = AudioInputCoordinatorImpl()
    }

    @Test
    fun testInitialStateIsIdle() {
        assertEquals(AudioOwnerState.IDLE, coordinator.state.value)
        assertEquals(AudioResourceOwner.NONE, coordinator.activeOwner.value)
    }

    @Test
    fun testWakeWordEngineMicrophoneGrant() = runTest {
        val granted = coordinator.requestWakeWordListening()
        assertTrue(granted)
        assertEquals(AudioOwnerState.WAKE_WORD_LISTENING, coordinator.state.value)
        assertEquals(AudioResourceOwner.WAKE_WORD_ENGINE, coordinator.activeOwner.value)
    }

    @Test
    fun testWakeWordDetectedReleasesMicrophone() = runTest {
        coordinator.requestWakeWordListening()
        coordinator.onWakeWordDetected()

        assertEquals(AudioOwnerState.WAKE_WORD_DETECTED, coordinator.state.value)
        assertEquals(AudioResourceOwner.NONE, coordinator.activeOwner.value)
    }

    @Test
    fun testTtsSpeechPlaybackLocksMicrophone() = runTest {
        coordinator.requestWakeWordListening()
        coordinator.onWakeWordDetected()

        // TTS starts speaking ("Yes?" or greeting)
        coordinator.onTtsStarted()
        assertEquals(AudioOwnerState.TTS_RESPONSE, coordinator.state.value)
        assertEquals(AudioResourceOwner.TTS_PLAYBACK, coordinator.activeOwner.value)

        // Wake word request must be DENIED during TTS playback to avoid audio feedback
        val deniedWakeWord = coordinator.requestWakeWordListening()
        assertFalse(deniedWakeWord)
        assertEquals(AudioResourceOwner.TTS_PLAYBACK, coordinator.activeOwner.value)

        // Command listening request must ALSO be DENIED during TTS playback
        val deniedCommandDuringTts = coordinator.requestCommandListening()
        assertFalse(deniedCommandDuringTts)
        assertEquals(AudioResourceOwner.TTS_PLAYBACK, coordinator.activeOwner.value)

        // Command request is permitted only after TTS finishes
        coordinator.onTtsCompleted()
        assertEquals(AudioResourceOwner.NONE, coordinator.activeOwner.value)

        // Now command listening can safely claim the microphone
        val grantedCommand = coordinator.requestCommandListening()
        assertTrue(grantedCommand)
        assertEquals(AudioOwnerState.COMMAND_LISTENING, coordinator.state.value)
        assertEquals(AudioResourceOwner.VOICE_ASSISTANT_COMMAND, coordinator.activeOwner.value)
    }

    @Test
    fun testMutualExclusionBetweenWakeWordAndAssistant() = runTest {
        coordinator.requestCommandListening()
        assertEquals(AudioResourceOwner.VOICE_ASSISTANT_COMMAND, coordinator.activeOwner.value)

        // Wake word cannot interrupt active command listening
        val wakeWordGranted = coordinator.requestWakeWordListening()
        assertFalse(wakeWordGranted)
        assertEquals(AudioResourceOwner.VOICE_ASSISTANT_COMMAND, coordinator.activeOwner.value)
    }

    @Test
    fun testReleaseAllResetsState() = runTest {
        coordinator.requestCommandListening()
        coordinator.releaseAll()

        assertEquals(AudioOwnerState.IDLE, coordinator.state.value)
        assertEquals(AudioResourceOwner.NONE, coordinator.activeOwner.value)
    }
}
