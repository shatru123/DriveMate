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
        val result = capabilityManager.evaluateAction("climate_control", "climate temperature adjustment")
        if (!result.success) {
            return CapabilityActionResult(
                success = false,
                status = result.status,
                userMessage = "Climate controls are read-only. Your Tata Nexon does not expose direct HVAC actuation to DriveMate."
            )
        }
        val clamped = tempCelsius.coerceIn(16.0f, 30.0f)
        _climateState.value = _climateState.value.copy(targetTemperatureCelsius = clamped)
        return result
    }

    override fun setFanSpeed(speed: Int): CapabilityActionResult {
        val result = capabilityManager.evaluateAction("climate_control", "fan speed change")
        if (!result.success) {
            return CapabilityActionResult(
                success = false,
                status = result.status,
                userMessage = "Direct vehicle HVAC fan speed actuation is not available."
            )
        }
        val clamped = speed.coerceIn(1, 7)
        _climateState.value = _climateState.value.copy(fanSpeed = clamped)
        return result
    }

    override fun setAcEnabled(enabled: Boolean): CapabilityActionResult {
        val result = capabilityManager.evaluateAction("climate_control", if (enabled) "AC turn on" else "AC turn off")
        if (!result.success) {
            return CapabilityActionResult(
                success = false,
                status = result.status,
                userMessage = "Direct vehicle AC control is not available."
            )
        }
        _climateState.value = _climateState.value.copy(isAcOn = enabled)
        return result
    }

    override fun setAutoMode(enabled: Boolean): CapabilityActionResult {
        val result = capabilityManager.evaluateAction("climate_control", "Auto climate mode")
        if (!result.success) return result
        _climateState.value = _climateState.value.copy(isAutoMode = enabled)
        return result
    }

    override fun setDefrostActive(active: Boolean): CapabilityActionResult {
        val result = capabilityManager.evaluateAction("climate_control", "windshield defrost")
        if (!result.success) return result
        _climateState.value = _climateState.value.copy(isDefrostActive = active)
        return result
    }

    override fun setRecirculationActive(active: Boolean): CapabilityActionResult {
        val result = capabilityManager.evaluateAction("climate_control", "air recirculation")
        if (!result.success) return result
        _climateState.value = _climateState.value.copy(isRecirculationActive = active)
        return result
    }
}
