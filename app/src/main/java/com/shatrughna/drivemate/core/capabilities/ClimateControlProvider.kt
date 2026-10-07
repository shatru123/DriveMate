package com.shatrughna.drivemate.core.capabilities

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Climate control state representation.
 */
data class ClimateState(
    val targetTemperatureCelsius: Float = 22.0f,
    val fanSpeed: Int = 3,
    val isAcOn: Boolean = true,
    val isAutoMode: Boolean = true,
    val isDefrostActive: Boolean = false,
    val isRecirculationActive: Boolean = true,
    val isDualZone: Boolean = false,
    val passengerTemperatureCelsius: Float = 22.0f
)

/**
 * Climate control abstraction.
 */
interface ClimateControlProvider {
    val climateState: StateFlow<ClimateState>

    fun setTemperature(tempCelsius: Float): CapabilityActionResult

    fun setFanSpeed(speed: Int): CapabilityActionResult

    fun setAcEnabled(enabled: Boolean): CapabilityActionResult

    fun setAutoMode(enabled: Boolean): CapabilityActionResult

    fun setDefrostActive(active: Boolean): CapabilityActionResult

    fun setRecirculationActive(active: Boolean): CapabilityActionResult
}

class VehicleClimateControlProviderImpl(
    private val capabilityManager: VehicleCapabilityManager
) : ClimateControlProvider {

    private val _climateState = MutableStateFlow(ClimateState())
    override val climateState: StateFlow<ClimateState> = _climateState.asStateFlow()

    override fun setTemperature(tempCelsius: Float): CapabilityActionResult {
        val clamped = tempCelsius.coerceIn(16.0f, 30.0f)
        _climateState.value = _climateState.value.copy(targetTemperatureCelsius = clamped)
        val result = capabilityManager.evaluateAction("climate_control", "climate temperature adjustment")
        return if (!result.success) {
            CapabilityActionResult(
                success = false,
                status = result.status,
                userMessage = "Target temperature set to ${clamped.toInt()}°C in companion mode. Note: Your vehicle doesn't currently provide AC control access to DriveMate."
            )
        } else result
    }

    override fun setFanSpeed(speed: Int): CapabilityActionResult {
        val clamped = speed.coerceIn(1, 7)
        _climateState.value = _climateState.value.copy(fanSpeed = clamped)
        val result = capabilityManager.evaluateAction("climate_control", "fan speed change")
        return if (!result.success) {
            CapabilityActionResult(
                success = false,
                status = result.status,
                userMessage = "Fan speed set to $clamped in companion mode. Note: Direct vehicle HVAC actuation is not available."
            )
        } else result
    }

    override fun setAcEnabled(enabled: Boolean): CapabilityActionResult {
        _climateState.value = _climateState.value.copy(isAcOn = enabled)
        val result = capabilityManager.evaluateAction("climate_control", if (enabled) "AC turn on" else "AC turn off")
        return if (!result.success) {
            CapabilityActionResult(
                success = false,
                status = result.status,
                userMessage = "AC ${if (enabled) "turned on" else "turned off"} in companion mode. Note: Your vehicle doesn't currently provide AC control access to DriveMate."
            )
        } else result
    }

    override fun setAutoMode(enabled: Boolean): CapabilityActionResult {
        _climateState.value = _climateState.value.copy(isAutoMode = enabled)
        return capabilityManager.evaluateAction("climate_control", "Auto climate mode")
    }

    override fun setDefrostActive(active: Boolean): CapabilityActionResult {
        _climateState.value = _climateState.value.copy(isDefrostActive = active)
        return capabilityManager.evaluateAction("climate_control", "windshield defrost")
    }

    override fun setRecirculationActive(active: Boolean): CapabilityActionResult {
        _climateState.value = _climateState.value.copy(isRecirculationActive = active)
        return capabilityManager.evaluateAction("climate_control", "air recirculation")
    }
}
