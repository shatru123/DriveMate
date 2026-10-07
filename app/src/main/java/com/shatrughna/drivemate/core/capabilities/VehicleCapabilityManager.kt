package com.shatrughna.drivemate.core.capabilities

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for querying whether vehicle actions are possible.
 * Delivers transparent, honest feedback to driver voice commands and UI.
 */
interface VehicleCapabilityManager {
    val capabilities: StateFlow<VehicleCapabilitiesState>

    fun getCapability(featureId: String): FeatureCapability?

    fun evaluateAction(featureId: String, actionName: String): CapabilityActionResult
}

class VehicleCapabilityManagerImpl : VehicleCapabilityManager {

    private val _capabilities = MutableStateFlow(VehicleCapabilitiesState())
    override val capabilities: StateFlow<VehicleCapabilitiesState> = _capabilities.asStateFlow()

    override fun getCapability(featureId: String): FeatureCapability? {
        return _capabilities.value.allCapabilities.find { it.featureId == featureId }
    }

    override fun evaluateAction(featureId: String, actionName: String): CapabilityActionResult {
        val capability = getCapability(featureId)
            ?: return CapabilityActionResult(
                success = false,
                status = CapabilityStatus.NOT_SUPPORTED_BY_VEHICLE,
                userMessage = "This vehicle feature is not recognized by DriveMate."
            )

        return when (capability.status) {
            CapabilityStatus.SUPPORTED -> {
                CapabilityActionResult(
                    success = true,
                    status = CapabilityStatus.SUPPORTED,
                    userMessage = "$actionName is supported and executed."
                )
            }
            CapabilityStatus.NOT_SUPPORTED_BY_VEHICLE -> {
                CapabilityActionResult(
                    success = false,
                    status = CapabilityStatus.NOT_SUPPORTED_BY_VEHICLE,
                    userMessage = "Your vehicle doesn't currently provide direct $actionName access to DriveMate."
                )
            }
            CapabilityStatus.UNAVAILABLE -> {
                CapabilityActionResult(
                    success = false,
                    status = CapabilityStatus.UNAVAILABLE,
                    userMessage = "$actionName is temporarily unavailable. Check vehicle ignition."
                )
            }
            CapabilityStatus.NOT_CONNECTED -> {
                CapabilityActionResult(
                    success = false,
                    status = CapabilityStatus.NOT_CONNECTED,
                    userMessage = "Phone is not connected to vehicle bus. Cannot perform $actionName."
                )
            }
            CapabilityStatus.NOT_AUTHORIZED -> {
                CapabilityActionResult(
                    success = false,
                    status = CapabilityStatus.NOT_AUTHORIZED,
                    userMessage = "DriveMate is not authorized to perform $actionName."
                )
            }
            CapabilityStatus.COMING_SOON -> {
                CapabilityActionResult(
                    success = false,
                    status = CapabilityStatus.COMING_SOON,
                    userMessage = "$actionName integration is coming soon in a future update."
                )
            }
        }
    }
}
