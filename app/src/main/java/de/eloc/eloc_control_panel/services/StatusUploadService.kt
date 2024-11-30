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
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.themable.HomeActivity
import de.eloc.eloc_control_panel.activities.themable.UpdateAppActivity
import de.eloc.eloc_control_panel.data.UploadResult
import de.eloc.eloc_control_panel.data.helpers.HttpHelper
import de.eloc.eloc_control_panel.data.util.Preferences
import de.eloc.eloc_control_panel.data.helpers.TimeHelper
import de.eloc.eloc_control_panel.data.helpers.firebase.FirestoreHelper
import java.util.concurrent.Executors

private const val EXTRA_STOP = "stop_service"

class StatusUploadService : Service() {

    companion object {
        private var stopRequested = false

        private var foregroundNotificationId: Int = -1
        var isRunningTask = false
            private set

        private var lastUsedNotificationId = 0
        private var statusUpdateIntervalMillis = Preferences.statusUploadInterval.millis

        fun start(context: Context) {
            val serviceIntent = Intent(context, StatusUploadService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }

        private fun createNewNotificationId(): Int {
            lastUsedNotificationId++
            return lastUsedNotificationId
        }
    }

    private var elapsedMillis = 0L
    private val notificationManager = NotificationManagerCompat.from(App.instance)
    private var serviceNotificationBuilder: NotificationCompat.Builder? = null

    private val uploadServiceNotificationChannel = NotificationChannelCompat.Builder(
        "Service",
        NotificationManagerCompat.IMPORTANCE_LOW
    ).setDescription("Uploading service notifications")
        .setName("Upload Notifications")
        .setShowBadge(false)
        .build()

    private val uploadTask = Runnable {
        isRunningTask = true
        elapsedMillis = 0
        var taskResult = UploadResult.Failed
        val appProtocolVersion = HttpHelper.instance.getAppProtocolVersion()
        if (App.APP_PROTOCOL_VERSION < appProtocolVersion) {

            // If different, notify user and download new app version
            val (id, notification) = createNotification(
                getString(R.string.app_name),
                getString(R.string.update_app),
            )
            val intent = Intent(this, UpdateAppActivity::class.java)
            notification.contentIntent =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            showNotification(id, notification)
            notificationManager.cancel(foregroundNotificationId)
            abortService()
            return@Runnable
        }

        val sleepInterval = 1000L
        var firstIteration = true
        while (true) {
            if (stopRequested || (taskResult == UploadResult.NoData) || (taskResult == UploadResult.Uploaded)) {
                break
            }

            // Interval can be changed at any time in app settings,
            // so get the latest value for every loop iteration
            statusUpdateIntervalMillis = Preferences.statusUploadInterval.millis

            // Try an upload if interval has elapsed
            elapsedMillis += sleepInterval
            if (firstIteration || (elapsedMillis >= statusUpdateIntervalMillis)) {
                firstIteration = false
                taskResult = doUpload()
            }

            if (serviceNotificationBuilder != null) {
                val remainingMillis = statusUpdateIntervalMillis - elapsedMillis
                val remaining =
                    TimeHelper.formatMillis(applicationContext, remainingMillis, true)
                val message = getString(R.string.retrying_in_time, remaining)
                serviceNotificationBuilder!!.setContentText(message)
                showNotification(foregroundNotificationId, serviceNotificationBuilder!!.build())
            }

            try {
                Thread.sleep(sleepInterval)
            } catch (_: Exception) {
            }
        }

        if (taskResult == UploadResult.Uploaded) {
            val (id, notification) = createNotification(
                getString(R.string.status_upload),
                getString(R.string.eloc_info_uploaded)
            )
            showNotification(id, notification)
        }
        abortService()
    }

    private fun abortService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        elapsedMillis = 0
        stopRequested = false
        isRunningTask = false
        stopSelf()
    }

    private fun doUpload(): UploadResult {
        var result = UploadResult.NoData
        try {
            if (serviceNotificationBuilder != null) {
                val message = getString(R.string.uploading_status)
                serviceNotificationBuilder!!.setContentText(message)
                showNotification(foregroundNotificationId, serviceNotificationBuilder!!.build())
            }
            result = FirestoreHelper.instance.uploadDataFiles()
        } catch (_: Exception) {
        }
        elapsedMillis = 0L
        return result
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val extras = intent?.extras
        stopRequested = extras?.getBoolean(EXTRA_STOP, false) ?: false
        val title = getString(R.string.status_upload)
        val messageResId = if (stopRequested)
            R.string.stopping_upload_service
        else
            R.string.checking_eloc_status_to_upload
        val message = getString(messageResId)
        val (id, notification) = createNotification(title, message, true)
        if (foregroundNotificationId <= 0) {
            foregroundNotificationId = id
        }

        startForeground(foregroundNotificationId, notification)
        if (!isRunningTask) {
            Executors.newSingleThreadExecutor().execute(uploadTask)
        }
        return START_REDELIVER_INTENT
    }

    private fun createNotification(
        title: String,
        message: String,
        isForService: Boolean = false

    ): Pair<Int, Notification> {
        notificationManager.createNotificationChannel(uploadServiceNotificationChannel)
        val target = Intent(applicationContext, HomeActivity::class.java)
        val pi = PendingIntent.getActivity(
            applicationContext,
            0,
            target,
            PendingIntent.FLAG_IMMUTABLE
        )
        val builder =
            NotificationCompat.Builder(applicationContext, uploadServiceNotificationChannel.id)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setShowWhen(true)
                .setOnlyAlertOnce(isForService)
                .setOngoing(isForService)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.notification_icon)
        val notification = builder.build()
        if (isForService) {
            val cancel = getString(R.string.cancel)
            val cancelTarget = Intent(applicationContext, StatusUploadService::class.java)
            cancelTarget.putExtra(EXTRA_STOP, true)
            val cancelPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(
                    applicationContext,
                    0,
                    cancelTarget,
                    PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getService(
                    applicationContext,
                    0,
                    cancelTarget,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                cancel,
                cancelPendingIntent
            )
            serviceNotificationBuilder = builder
        }
        notification.color = ContextCompat.getColor(applicationContext, R.color.colorPrimary)
        val id = createNewNotificationId()
        return id to notification
    }

    private fun showNotification(id: Int, notification: Notification) {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status =
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            (status == PackageManager.PERMISSION_GRANTED)
        } else {
            true
        }
        if (hasPermission) {
            notificationManager.notify(id, notification)
        }
    }
}