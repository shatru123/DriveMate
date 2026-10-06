package com.shatrughna.drivemate.driving

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.shatrughna.drivemate.DriveMateApplication
import com.shatrughna.drivemate.MainActivity
import com.shatrughna.drivemate.car.CarConnectionState
import com.shatrughna.drivemate.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground Service that maintains connection awareness and ensures
 * legitimate audio focus permissions while driving.
 */
class DriveMateSessionService : Service() {

    companion object {
        const val CHANNEL_ID = "drivemate_session_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.shatrughna.drivemate.action.START_SERVICE"
        const val ACTION_STOP = "com.shatrughna.drivemate.action.STOP_SERVICE"

        fun startService(context: Context) {
            val intent = Intent(context, DriveMateSessionService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, DriveMateSessionService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        AppLogger.i(AppLogger.Tag.SESSION, "DriveMateSessionService created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification("Monitoring vehicle connection..."))

        val app = application as? DriveMateApplication
        if (app != null) {
            serviceScope.launch {
                app.carConnectionManager.connectionState.collectLatest { state ->
                    val notificationText = when (state) {
                        is CarConnectionState.Connected -> "Connected to ${state.deviceOrVehicleName}"
                        is CarConnectionState.Disconnected -> "Searching for Tata Nexon connection..."
                        CarConnectionState.Unknown -> "Initializing companion..."
                    }
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(NOTIFICATION_ID, buildNotification(notificationText))
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        AppLogger.i(AppLogger.Tag.SESSION, "DriveMateSessionService destroyed.")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "DriveMate Active Driving Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live connection status to your Tata Nexon"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("DriveMate • Tata Nexon")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
