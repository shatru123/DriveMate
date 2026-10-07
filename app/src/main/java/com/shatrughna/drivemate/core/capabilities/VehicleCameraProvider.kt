package com.shatrughna.drivemate.core.capabilities

/**
 * Abstraction for vehicle cameras and 360 surround view feeds.
 * Reports OEM camera availability transparently without mocking fake video streams.
 */
interface VehicleCameraProvider {
    fun request360SurroundView(): CapabilityActionResult
    fun requestReverseCamera(): CapabilityActionResult
    fun requestFrontCamera(): CapabilityActionResult
}

class VehicleCameraProviderImpl(
    private val capabilityManager: VehicleCapabilityManager
) : VehicleCameraProvider {

    override fun request360SurroundView(): CapabilityActionResult {
        val result = capabilityManager.evaluateAction("camera_360", "360° surround view camera")
        return if (!result.success) {
            CapabilityActionResult(
                success = false,
                status = result.status,
                userMessage = "OEM 360° camera feed is displayed on your Nexon infotainment display. Phone companion access is not available on this vehicle."
            )
        } else result
    }

    override fun requestReverseCamera(): CapabilityActionResult {
        val result = capabilityManager.evaluateAction("camera_360", "reverse parking camera")
        return if (!result.success) {
            CapabilityActionResult(
                success = false,
                status = result.status,
                userMessage = "Reverse parking camera is displayed on your vehicle screen when reverse gear (R) is engaged."
            )
        } else result
    }

    override fun requestFrontCamera(): CapabilityActionResult {
        val result = capabilityManager.evaluateAction("camera_360", "front camera")
        return if (!result.success) {
            CapabilityActionResult(
                success = false,
                status = result.status,
                userMessage = "Front camera view is only accessible via your Nexon infotainment touchscreen."
            )
        } else result
    }
}
