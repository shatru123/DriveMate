package com.shatrughna.drivemate.car

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shatrughna.drivemate.DriveMateApplication
import com.shatrughna.drivemate.util.AppLogger

/**
 * BroadcastReceiver monitoring Bluetooth device connection events.
 */
class BluetoothConnectionReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action ?: return
        val device: BluetoothDevice? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

        val deviceName = try {
            device?.name ?: "Bluetooth Vehicle"
        } catch (e: SecurityException) {
            "Bluetooth Audio Device"
        }

        val app = context.applicationContext as? DriveMateApplication
        val carConnectionManager = app?.carConnectionManager ?: return

        when (action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                AppLogger.i(AppLogger.Tag.CAR_CONNECTION, "Bluetooth ACL connected: $deviceName")
                carConnectionManager.onBluetoothConnected(deviceName)
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                AppLogger.i(AppLogger.Tag.CAR_CONNECTION, "Bluetooth ACL disconnected: $deviceName")
                carConnectionManager.onBluetoothDisconnected(deviceName)
            }
        }
    }
}
