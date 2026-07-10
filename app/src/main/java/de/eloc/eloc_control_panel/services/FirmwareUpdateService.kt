package de.eloc.eloc_control_panel.services

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.themable.FirmwareUpdateActivity
import de.eloc.eloc_control_panel.driver.FirmwareUpdater

/**
 * Foreground companion of [FirmwareUpdater] (modeled on [StatusUploadService]):
 * keeps the process alive with a progress notification and a partial wake lock
 * while a firmware transfer runs, so screen-off or backgrounding cannot kill
 * the update mid-flight. Started by [FirmwareUpdateActivity] when a transfer
 * begins; stops itself on any terminal updater state.
 */
class FirmwareUpdateService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 7001
        private const val LISTENER_ID = "firmwareUpdateService"
        private const val WAKE_LOCK_TAG = "eloc:FirmwareUpdate"
        // The whole update (transfer + flash + reboots) takes a few minutes;
        // cap the wake lock well above that so it can never leak indefinitely.
        private const val WAKE_LOCK_TIMEOUT_MS = 30L * 60 * 1000

        fun start(context: Context) {
            val serviceIntent = Intent(context, FirmwareUpdateService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }

    private val notificationManager = NotificationManagerCompat.from(App.instance)
    private var wakeLock: PowerManager.WakeLock? = null

    private val notificationChannel = NotificationChannelCompat.Builder(
        "FirmwareUpdate",
        NotificationManagerCompat.IMPORTANCE_LOW
    ).setDescription("Firmware update progress")
        .setName("Firmware Updates")
        .setShowBadge(false)
        .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationManager.createNotificationChannel(notificationChannel)
        startForeground(NOTIFICATION_ID, buildNotification(FirmwareUpdater.lastMessage, true))
        acquireWakeLock()

        FirmwareUpdater.addStateListener(LISTENER_ID) { state, message ->
            when (state) {
                FirmwareUpdater.State.Success,
                FirmwareUpdater.State.RolledBack,
                FirmwareUpdater.State.Aborted,
                FirmwareUpdater.State.Failed -> finish(state, message)

                else -> updateNotification(buildNotification(message, true))
            }
        }
        FirmwareUpdater.addProgressListener(LISTENER_ID) { sent, total, rate ->
            updateNotification(buildProgressNotification(sent, total, rate))
        }

        // The transfer may already have finished before the listeners attached
        if (!FirmwareUpdater.isBusy && FirmwareUpdater.state != FirmwareUpdater.State.Idle) {
            finish(FirmwareUpdater.state, FirmwareUpdater.lastMessage)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        FirmwareUpdater.removeStateListener(LISTENER_ID)
        FirmwareUpdater.removeProgressListener(LISTENER_ID)
        releaseWakeLock()
        super.onDestroy()
    }

    private fun finish(state: FirmwareUpdater.State, message: String) {
        FirmwareUpdater.removeStateListener(LISTENER_ID)
        FirmwareUpdater.removeProgressListener(LISTENER_ID)
        val title = when (state) {
            FirmwareUpdater.State.Success -> getString(R.string.firmware_update_succeeded)
            FirmwareUpdater.State.RolledBack -> getString(R.string.firmware_update_rolled_back)
            FirmwareUpdater.State.Aborted -> getString(R.string.firmware_update_cancelled)
            else -> getString(R.string.firmware_update_failed)
        }
        showFinalNotification(title, message)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        releaseWakeLock()
        stopSelf()
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            try {
                val powerManager = getSystemService(POWER_SERVICE) as PowerManager
                wakeLock =
                    powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
                        acquire(WAKE_LOCK_TIMEOUT_MS)
                    }
            } catch (_: Exception) {
            }
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }
        wakeLock = null
    }

    private fun contentIntent(): PendingIntent {
        val target = Intent(applicationContext, FirmwareUpdateActivity::class.java)
        return PendingIntent.getActivity(
            applicationContext,
            0,
            target,
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun baseBuilder(): NotificationCompat.Builder =
        NotificationCompat.Builder(applicationContext, notificationChannel.id)
            .setContentTitle(getString(R.string.firmware_update))
            .setContentIntent(contentIntent())
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setSmallIcon(R.drawable.notification_icon)

    private fun buildNotification(message: String, ongoing: Boolean): Notification =
        baseBuilder()
            .setContentText(message)
            .setOngoing(ongoing)
            .build()

    private fun buildProgressNotification(sent: Long, total: Long, rate: Double): Notification {
        val percent = if (total > 0) ((sent * 100) / total).toInt() else 0
        val kbPerSec = (rate / 1024).toInt()
        return baseBuilder()
            .setContentText(
                getString(
                    R.string.firmware_transfer_progress,
                    percent,
                    kbPerSec
                )
            )
            .setProgress(100, percent, false)
            .setOngoing(true)
            .build()
    }

    private fun showFinalNotification(title: String, message: String) {
        val notification = baseBuilder()
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setOngoing(false)
            .setAutoCancel(true)
            .build()
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (hasPermission) {
            notificationManager.notify(NOTIFICATION_ID + 1, notification)
        }
    }

    private fun updateNotification(notification: Notification) {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (hasPermission) {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }
}
