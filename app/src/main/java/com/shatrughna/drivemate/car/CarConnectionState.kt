package com.shatrughna.drivemate.car

/**
 * High-level connection mediums detected by DriveMate.
 */
enum class CarConnectionType(val displayName: String) {
    NONE("None"),
    ANDROID_AUTO_PROJECTION("Android Auto"),
    ANDROID_AUTOMOTIVE_NATIVE("Android Automotive OS"),
    BLUETOOTH_CAR_UNIT("Car Bluetooth"),
    SIMULATED("Simulated Test Session")
}

/**
 * Represents the official connection status of the phone to the vehicle.
 */
sealed class CarConnectionState {

    abstract val isConnected: Boolean
    abstract val statusDescription: String
    abstract val connectionType: CarConnectionType

    data object Unknown : CarConnectionState() {
        override val isConnected: Boolean = false
        override val statusDescription: String = "Detecting connection..."
        override val connectionType: CarConnectionType = CarConnectionType.NONE
    }

    data class Disconnected(
        val timestampMillis: Long = System.currentTimeMillis()
    ) : CarConnectionState() {
        override val isConnected: Boolean = false
        override val statusDescription: String = "Not connected"
        override val connectionType: CarConnectionType = CarConnectionType.NONE
    }

    data class Connected(
        override val connectionType: CarConnectionType,
        val deviceOrVehicleName: String,
        val timestampMillis: Long = System.currentTimeMillis()
    ) : CarConnectionState() {
        override val isConnected: Boolean = true
        override val statusDescription: String = "Connected via ${connectionType.displayName}"
    }
}
