package com.shatrughna.drivemate

import com.shatrughna.drivemate.voice.AudioInputCoordinatorImpl
import com.shatrughna.drivemate.voice.AudioOwnerState
import com.shatrughna.drivemate.voice.AudioResourceOwner
import com.shatrughna.drivemate.voice.wakeword.DisabledWakeWordEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AudioFocusLifecycleTest {

    private lateinit var coordinator: AudioInputCoordinatorImpl

    @Before
    fun setUp() {
        coordinator = AudioInputCoordinatorImpl()
    }

    @Test
    fun testCompleteVoiceLifecycleTransitions() = runTest {
        // 1. Initial State: IDLE
        assertEquals(AudioOwnerState.IDLE, coordinator.state.value)
        assertEquals(AudioResourceOwner.NONE, coordinator.activeOwner.value)

        // 2. Push-to-Talk triggered: USER_REQUESTED_VOICE -> REQUESTING_AUDIO_FOCUS
        val recordingGranted = coordinator.requestVoiceInput()
        assertTrue(recordingGranted)
        assertEquals(AudioOwnerState.REQUESTING_AUDIO_FOCUS, coordinator.state.value)

        // 3. Audio focus acquired and mic opened: RECORDING
        coordinator.onRecordingStarted()
        assertEquals(AudioOwnerState.RECORDING, coordinator.state.value)
        assertEquals(AudioResourceOwner.MICROPHONE_RECORDING, coordinator.activeOwner.value)

        // 4. User finishes speaking: RECORDING -> PROCESSING
        coordinator.onRecordingFinished()
        assertEquals(AudioOwnerState.PROCESSING, coordinator.state.value)
        assertEquals(AudioResourceOwner.NONE, coordinator.activeOwner.value)

        // 5. TTS Response ready: PROCESSING -> TTS
        coordinator.onTtsStarted()
        assertEquals(AudioOwnerState.TTS, coordinator.state.value)
        assertEquals(AudioResourceOwner.TTS_PLAYBACK, coordinator.activeOwner.value)

        // 6. Utterance finishes: TTS -> RELEASE_AUDIO_FOCUS -> IDLE
        coordinator.onTtsCompleted()
        assertEquals(AudioOwnerState.IDLE, coordinator.state.value)
        assertEquals(AudioResourceOwner.NONE, coordinator.activeOwner.value)
    }

    @Test
    fun testMutualExclusionDeniesSimultaneousMicAndTts() = runTest {
        // When in TTS mode, microphone acquisition MUST be denied to protect audio focus
        coordinator.onTtsStarted()
        assertEquals(AudioOwnerState.TTS, coordinator.state.value)

        val micGrantedDuringTts = coordinator.requestVoiceInput()
        assertFalse(micGrantedDuringTts)
        assertEquals(AudioOwnerState.TTS, coordinator.state.value)

        // When in Recording mode, TTS cannot preempt until recording is done
        coordinator.onTtsCompleted()
        coordinator.requestVoiceInput()
        coordinator.onRecordingStarted()
        assertEquals(AudioOwnerState.RECORDING, coordinator.state.value)

        // Cannot start wake word while recording
        val wakeWordDenied = coordinator.requestWakeWordListening()
        assertFalse(wakeWordDenied)
    }

    @Test
    fun testAbandonAudioFocusOnCancelOrError() = runTest {
        coordinator.requestVoiceInput()
        coordinator.onRecordingStarted()
        assertEquals(AudioOwnerState.RECORDING, coordinator.state.value)

        // User cancels / disconnects / error occurs
        coordinator.abandonAudioFocus()
        assertEquals(AudioOwnerState.IDLE, coordinator.state.value)
        assertEquals(AudioResourceOwner.NONE, coordinator.activeOwner.value)
    }

    @Test
    fun testDisabledWakeWordEngineOnAndroidAuto() = runTest {
        val disabledEngine = DisabledWakeWordEngine()
        assertFalse(disabledEngine.isRunning)

        disabledEngine.start()
        // Must remain non-listening to avoid freezing Spotify
        assertFalse(disabledEngine.isRunning)

        disabledEngine.stop()
        assertFalse(disabledEngine.isRunning)
    }
}
