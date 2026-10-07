package com.shatrughna.drivemate.voice.wakeword

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Phone-based wake word engine running on handheld device when explicitly enabled.
 */
class PhoneWakeWordEngine(
    context: Context,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) : WakeWordEngine {

    private val delegate = SpeechRecognizerWakeWordEngine(context, scope)

    override val isRunning: Boolean
        get() = delegate.isRunning

    override fun start() = delegate.start()

    override fun stop() = delegate.stop()

    override fun setListener(listener: WakeWordListener?) = delegate.setListener(listener)
}
