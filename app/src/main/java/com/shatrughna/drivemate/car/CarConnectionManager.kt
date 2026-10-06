package com.shatrughna.drivemate.car

import android.content.Context
import androidx.car.app.connection.CarConnection
import androidx.lifecycle.Observer
import com.shatrughna.drivemate.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
 * combined with secondary Bluetooth ACL metadata.
 * Never treats Bluetooth alone as proof of an Android Auto projection session.
 */
class AndroidAutoCarConnectionManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) : CarConnectionManager {

    private val stateMutex = Mutex()
    private val _connectionState = MutableStateFlow<CarConnectionState>(CarConnectionState.Unknown)
    override val connectionState: StateFlow<CarConnectionState> = _connectionState.asStateFlow()

    private var carConnection: CarConnection? = null
    private var isMonitoring = false
    private var isSimulating = false

    private var lastAutoType: Int = CarConnection.CONNECTION_TYPE_NOT_CONNECTED
    private var activeBluetoothDeviceName: String? = null
    private var unknownStateTimeoutJob: Job? = null

    private val carConnectionObserver = Observer<Int> { connectionType ->
        AppLogger.i(AppLogger.Tag.CAR_CONNECTION, "Android Auto CarConnection type changed: $connectionType")
        unknownStateTimeoutJob?.cancel()
        lastAutoType = connectionType
        scope.launch {
            recalculateState()
        }
    }

    override fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        AppLogger.i(AppLogger.Tag.CAR_CONNECTION, "Starting CarConnection monitoring...")

        // Timeout guard: If no CarConnection callback fires within 2.5s, resolve Unknown to Disconnected
        unknownStateTimeoutJob?.cancel()
        unknownStateTimeoutJob = scope.launch {
            delay(2500L)
            if (_connectionState.value is CarConnectionState.Unknown) {
                AppLogger.d(AppLogger.Tag.CAR_CONNECTION, "Resolving initial Unknown connection state to Disconnected")
                recalculateState()
            }
        }

        try {
            val cc = CarConnection(context.applicationContext)
            carConnection = cc
            cc.type.observeForever(carConnectionObserver)
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.CAR_CONNECTION, "Failed to initialize official CarConnection API", e)
            scope.launch {
                recalculateState()
            }
        }
    }

    override fun stopMonitoring() {
        if (!isMonitoring) return
        isMonitoring = false
        AppLogger.i(AppLogger.Tag.CAR_CONNECTION, "Stopping CarConnection monitoring...")

        unknownStateTimeoutJob?.cancel()
        unknownStateTimeoutJob = null

        try {
            carConnection?.type?.removeObserver(carConnectionObserver)
            carConnection = null
        } catch (e: Exception) {
            AppLogger.w(AppLogger.Tag.CAR_CONNECTION, "Error removing CarConnection observer", e)
        }
    }

    override fun onBluetoothConnected(deviceName: String) {
        AppLogger.i(AppLogger.Tag.CAR_CONNECTION, "Bluetooth device connected: $deviceName (Supplementary Info)")
        activeBluetoothDeviceName = deviceName
        scope.launch {
            recalculateState()
        }
    }

    override fun onBluetoothDisconnected(deviceName: String) {
        AppLogger.i(AppLogger.Tag.CAR_CONNECTION, "Bluetooth device disconnected: $deviceName")
        if (activeBluetoothDeviceName == deviceName) {
            activeBluetoothDeviceName = null
        }
        scope.launch {
            recalculateState()
        }
    }

    override fun setSimulatedConnection(connected: Boolean) {
        isSimulating = connected
        AppLogger.i(AppLogger.Tag.CAR_CONNECTION, "Simulated connection set to: $connected")
        scope.launch {
            recalculateState()
        }
    }

    private suspend fun recalculateState() = stateMutex.withLock {
        val btConnected = activeBluetoothDeviceName != null
        val btName = activeBluetoothDeviceName

        if (isSimulating) {
            _connectionState.value = CarConnectionState.Connected(
                connectionType = CarConnectionType.SIMULATED,
                deviceOrVehicleName = "Tata Nexon (Simulated)",
                isBluetoothConnected = btConnected,
                activeBluetoothDeviceName = btName
            )
            return@withLock
        }

        when (lastAutoType) {
            CarConnection.CONNECTION_TYPE_PROJECTION -> {
                _connectionState.value = CarConnectionState.Connected(
                    connectionType = CarConnectionType.ANDROID_AUTO_PROJECTION,
                    deviceOrVehicleName = "Tata Nexon (Android Auto)",
                    isBluetoothConnected = btConnected,
                    activeBluetoothDeviceName = btName
                )
            }
            CarConnection.CONNECTION_TYPE_NATIVE -> {
                _connectionState.value = CarConnectionState.Connected(
                    connectionType = CarConnectionType.ANDROID_AUTOMOTIVE_NATIVE,
                    deviceOrVehicleName = "Tata Nexon Automotive OS",
                    isBluetoothConnected = btConnected,
                    activeBluetoothDeviceName = btName
                )
            }
            else -> {
                // Not connected to Android Auto
                if (btConnected && btName != null) {
                    _connectionState.value = CarConnectionState.Connected(
                        connectionType = CarConnectionType.BLUETOOTH_ONLY,
                        deviceOrVehicleName = btName,
                        isBluetoothConnected = true,
                        activeBluetoothDeviceName = btName
                    )
                } else {
                    _connectionState.value = CarConnectionState.Disconnected(
                        isBluetoothConnected = false,
                        activeBluetoothDeviceName = null
                    )
                }
            }
        }
    }
}
