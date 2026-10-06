package com.shatrughna.drivemate.car

/**
 * High-level connection mediums detected by DriveMate.
 */
enum class CarConnectionType(val displayName: String) {
    NONE("None"),
    ANDROID_AUTO_PROJECTION("Android Auto Projection"),
    ANDROID_AUTOMOTIVE_NATIVE("Android Automotive OS"),
    BLUETOOTH_ONLY("Car Bluetooth (Audio Only)"),
    SIMULATED("Simulated Test Session")
}

/**
 * Represents the official connection status of the phone to the vehicle.
 * Explicitly separates verified Android Auto projection sessions from Bluetooth-only pairings.
 */
sealed class CarConnectionState {

    abstract val isConnected: Boolean
    abstract val isAndroidAutoConnected: Boolean
    abstract val isBluetoothConnected: Boolean
    abstract val activeBluetoothDeviceName: String?
    abstract val statusDescription: String
    abstract val connectionType: CarConnectionType

    /**
     * Indicates whether this state represents a verified, supported car session
     * (Android Auto projection, Native Automotive OS, or user simulation).
     * Bluetooth audio alone is NOT considered a verified car driving session.
     */
    val isVerifiedCarSession: Boolean
        get() = connectionType == CarConnectionType.ANDROID_AUTO_PROJECTION ||
                connectionType == CarConnectionType.ANDROID_AUTOMOTIVE_NATIVE ||
                connectionType == CarConnectionType.SIMULATED

    data object Unknown : CarConnectionState() {
        override val isConnected: Boolean = false
        override val isAndroidAutoConnected: Boolean = false
        override val isBluetoothConnected: Boolean = false
        override val activeBluetoothDeviceName: String? = null
        override val statusDescription: String = "Detecting connection..."
        override val connectionType: CarConnectionType = CarConnectionType.NONE
    }

    data class Disconnected(
        override val isBluetoothConnected: Boolean = false,
        override val activeBluetoothDeviceName: String? = null,
        val timestampMillis: Long = System.currentTimeMillis()
    ) : CarConnectionState() {
        override val isConnected: Boolean = isBluetoothConnected
        override val isAndroidAutoConnected: Boolean = false
        override val connectionType: CarConnectionType =
            if (isBluetoothConnected) CarConnectionType.BLUETOOTH_ONLY else CarConnectionType.NONE
        override val statusDescription: String =
            if (isBluetoothConnected) "Connected to Bluetooth ($activeBluetoothDeviceName)" else "Not connected"
    }

    data class Connected(
        override val connectionType: CarConnectionType,
        val deviceOrVehicleName: String,
        override val isBluetoothConnected: Boolean = false,
        override val activeBluetoothDeviceName: String? = null,
        val timestampMillis: Long = System.currentTimeMillis()
    ) : CarConnectionState() {
        override val isConnected: Boolean = true
        override val isAndroidAutoConnected: Boolean =
            connectionType == CarConnectionType.ANDROID_AUTO_PROJECTION ||
                    connectionType == CarConnectionType.ANDROID_AUTOMOTIVE_NATIVE ||
                    connectionType == CarConnectionType.SIMULATED
        override val statusDescription: String =
            when (connectionType) {
                CarConnectionType.ANDROID_AUTO_PROJECTION -> "Connected via Android Auto Projection"
                CarConnectionType.ANDROID_AUTOMOTIVE_NATIVE -> "Connected via Android Automotive OS"
                CarConnectionType.SIMULATED -> "Connected via Simulated Session"
                CarConnectionType.BLUETOOTH_ONLY -> "Connected via Bluetooth ($deviceOrVehicleName)"
                CarConnectionType.NONE -> "Not connected"
            }
    }
}
