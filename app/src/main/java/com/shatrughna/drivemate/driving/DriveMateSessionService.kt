package com.shatrughna.drivemate.driving

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
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

        @Volatile
        var isServiceRunning = false
            private set

        fun startService(context: Context) {
            val intent = Intent(context, DriveMateSessionService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                AppLogger.w(AppLogger.Tag.SESSION, "Unable to start DriveMateSessionService (background restriction): ${e.message}")
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, DriveMateSessionService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                AppLogger.w(AppLogger.Tag.SESSION, "Unable to stop DriveMateSessionService: ${e.message}")
            }
        }
    }

    private var serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isCollectingSession = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        AppLogger.i(AppLogger.Tag.SESSION, "DriveMateSessionService created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            AppLogger.i(AppLogger.Tag.SESSION, "DriveMateSessionService received STOP action.")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val notification = buildNotification("Monitoring vehicle connection...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val fgsType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                startForeground(NOTIFICATION_ID, notification, fgsType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isServiceRunning = true
        } catch (e: Exception) {
            AppLogger.e(AppLogger.Tag.SESSION, "Failed to call startForeground: ${e.message}", e)
            stopSelf()
            return START_NOT_STICKY
        }

        val app = application as? DriveMateApplication
        if (app != null && !isCollectingSession) {
            isCollectingSession = true
            serviceScope.launch {
                app.sessionManager.isSessionActive.collectLatest { isActive ->
                    if (!isActive) {
                        AppLogger.i(AppLogger.Tag.SESSION, "DriveMateSessionService: Active driving session ended, stopping foreground service.")
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }

            serviceScope.launch {
                app.carConnectionManager.connectionState.collectLatest { state ->
                    val notificationText = when (state) {
                        is CarConnectionState.Connected -> "Connected to ${state.deviceOrVehicleName}"
                        is CarConnectionState.Disconnected -> "Driving session completed"
                        CarConnectionState.Unknown -> "DriveMate Active Companion"
                    }
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    notificationManager?.notify(NOTIFICATION_ID, buildNotification(notificationText))
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        isCollectingSession = false
        serviceScope.cancel()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(NOTIFICATION_ID)
        AppLogger.i(AppLogger.Tag.SESSION, "DriveMateSessionService destroyed.")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "DriveMate Active Driving Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live DriveMate driving-session status"
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
            .setContentTitle("DriveMate • Driving session")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
