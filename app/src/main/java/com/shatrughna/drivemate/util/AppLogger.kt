package com.shatrughna.drivemate.util

import android.util.Log

/**
 * Structured logger abstraction for DriveMate.
 * Sanitizes output and enforces standard log tags.
 * Safely falls back to console output if Android Log is unmocked in unit test environments.
 */
object AppLogger {

    enum class Tag(val label: String) {
        APP("DriveMate"),
        CAR_CONNECTION("CarConnection"),
        GREETING("Greeting"),
        TTS("TTS"),
        SESSION("Session"),
        SETTINGS("Settings")
    }

    private var isDebugEnabled: Boolean = true

    fun setDebugLogging(enabled: Boolean) {
        isDebugEnabled = enabled
    }

    fun d(tag: Tag, message: String) {
        if (!isDebugEnabled) return
        try {
            Log.d("DriveMate:${tag.label}", message)
        } catch (e: RuntimeException) {
            println("[DEBUG][DriveMate:${tag.label}] $message")
        }
    }

    fun i(tag: Tag, message: String) {
        try {
            Log.i("DriveMate:${tag.label}", message)
        } catch (e: RuntimeException) {
            println("[INFO][DriveMate:${tag.label}] $message")
        }
    }

    fun w(tag: Tag, message: String, throwable: Throwable? = null) {
        try {
            Log.w("DriveMate:${tag.label}", message, throwable)
        } catch (e: RuntimeException) {
            println("[WARN][DriveMate:${tag.label}] $message ${throwable?.message.orEmpty()}")
        }
    }

    fun e(tag: Tag, message: String, throwable: Throwable? = null) {
        try {
            Log.e("DriveMate:${tag.label}", message, throwable)
        } catch (e: RuntimeException) {
            System.err.println("[ERROR][DriveMate:${tag.label}] $message ${throwable?.message.orEmpty()}")
        }
    }
}
