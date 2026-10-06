package com.shatrughna.drivemate.car

import android.content.Context
import androidx.car.app.connection.CarConnection
import androidx.lifecycle.Observer
import com.shatrughna.drivemate.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Interface contract for observing car connection events.
 */
interface CarConnectionManager {
    val connectionState: StateFlow<CarConnectionState>
    fun startMonitoring()
    fun stopMonitoring()
    fun onBluetoothConnected(deviceName: String)
    fun onBluetoothDisconnected(deviceName: String)
    fun setSimulatedConnection(connected: Boolean)
}

/**
 * Production implementation using official Android for Cars CarConnection API
 * combined with optional Bluetooth ACL fallback.
 */
class AndroidAutoCarConnectionManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) : CarConnectionManager {

    private val _connectionState = MutableStateFlow<CarConnectionState>(CarConnectionState.Unknown)
    override val connectionState: StateFlow<CarConnectionState> = _connectionState.asStateFlow()

    private var carConnection: CarConnection? = null
    private var isMonitoring = false
    private var isSimulating = false

    private var lastAutoType: Int = CarConnection.CONNECTION_TYPE_NOT_CONNECTED
    private var activeBluetoothDeviceName: String? = null

    private val carConnectionObserver = Observer<Int> { connectionType ->
        AppLogger.i(AppLogger.Tag.CAR_CONNECTION, "Android Auto CarConnection type changed: $connectionType")
        lastAutoType = connectionType
        recalculateState()
    }

    override fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        AppLogger.i(AppLogger.Tag.CAR_CONNECTION, "Starting CarConnection monitoring...")

        try {
            val cc = CarConnection(context.applicationContext)
            carConnection = cc
            // CarConnection.type is a LiveData<Int>
            cc.type.observeForever(carConnectionObserver)
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.CAR_CONNECTION, "Failed to initialize official CarConnection API", e)
            _connectionState.value = CarConnectionState.Disconnected()
        }
    }

    override fun stopMonitoring() {
        if (!isMonitoring) return
        isMonitoring = false
        AppLogger.i(AppLogger.Tag.CAR_CONNECTION, "Stopping CarConnection monitoring...")

        try {
            carConnection?.type?.removeObserver(carConnectionObserver)
            carConnection = null
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tag.CAR_CONNECTION, "Error removing CarConnection observer", e)
        }
    }

    override fun onBluetoothConnected(deviceName: String) {
        AppLogger.i(AppLogger.Tag.CAR_CONNECTION, "Bluetooth device connected: $deviceName")
        activeBluetoothDeviceName = deviceName
        recalculateState()
    }

    override fun onBluetoothDisconnected(deviceName: String) {
        AppLogger.i(AppLogger.Tag.CAR_CONNECTION, "Bluetooth device disconnected: $deviceName")
        if (activeBluetoothDeviceName == deviceName) {
            activeBluetoothDeviceName = null
        }
        recalculateState()
    }

    override fun setSimulatedConnection(connected: Boolean) {
        isSimulating = connected
        AppLogger.i(AppLogger.Tag.CAR_CONNECTION, "Simulated connection set to: $connected")
        recalculateState()
    }

    private fun recalculateState() {
        if (isSimulating) {
            _connectionState.value = CarConnectionState.Connected(
                connectionType = CarConnectionType.SIMULATED,
                deviceOrVehicleName = "Tata Nexon (Simulated)"
            )
            return
        }

        when (lastAutoType) {
            CarConnection.CONNECTION_TYPE_PROJECTION -> {
                _connectionState.value = CarConnectionState.Connected(
                    connectionType = CarConnectionType.ANDROID_AUTO_PROJECTION,
                    deviceOrVehicleName = "Tata Nexon Infotainment (Android Auto)"
                )
            }
            CarConnection.CONNECTION_TYPE_NATIVE -> {
                _connectionState.value = CarConnectionState.Connected(
                    connectionType = CarConnectionType.ANDROID_AUTOMOTIVE_NATIVE,
                    deviceOrVehicleName = "Tata Nexon Automotive OS"
                )
            }
            else -> {
                val btDevice = activeBluetoothDeviceName
                if (btDevice != null) {
                    _connectionState.value = CarConnectionState.Connected(
                        connectionType = CarConnectionType.BLUETOOTH_CAR_UNIT,
                        deviceOrVehicleName = btDevice
                    )
                } else {
                    _connectionState.value = CarConnectionState.Disconnected()
                }
            }
        }
    }
}
