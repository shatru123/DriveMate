package com.shatrughna.drivemate.core.telemetry

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface TpmsProvider {
    val tpmsState: StateFlow<TpmsState>
    val isSupported: Boolean
}

class AndroidAutoTpmsProvider : TpmsProvider {
    private val _tpmsState = MutableStateFlow(
        TpmsState(
            frontLeft = TpmsWheelPressure(pressurePsi = null, status = TpmsStatus.UNAVAILABLE),
            frontRight = TpmsWheelPressure(pressurePsi = null, status = TpmsStatus.UNAVAILABLE),
            rearLeft = TpmsWheelPressure(pressurePsi = null, status = TpmsStatus.UNAVAILABLE),
            rearRight = TpmsWheelPressure(pressurePsi = null, status = TpmsStatus.UNAVAILABLE),
            availability = TelemetryAvailability.NOT_SUPPORTED,
            notice = "Direct wheel pressures are not exposed through Android Auto. Check the vehicle instrument cluster."
        )
    )

    override val tpmsState: StateFlow<TpmsState> = _tpmsState.asStateFlow()
    override val isSupported: Boolean = false
}
