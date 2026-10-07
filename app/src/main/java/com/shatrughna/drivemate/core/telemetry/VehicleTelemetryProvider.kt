package com.shatrughna.drivemate.core.telemetry

import kotlinx.coroutines.flow.StateFlow

interface VehicleTelemetryProvider {
    val name: String
    val source: TelemetrySource
    val isAvailable: Boolean
    val telemetry: StateFlow<VehicleTelemetry>

    fun start()
    fun stop()
}
