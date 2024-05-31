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
import de.eloc.eloc_control_panel.activities.HomeActivity
import de.eloc.eloc_control_panel.activities.UpdateAppActivity
import de.eloc.eloc_control_panel.data.UploadResult
import de.eloc.eloc_control_panel.data.helpers.HttpHelper
import de.eloc.eloc_control_panel.data.helpers.PreferencesHelper
import de.eloc.eloc_control_panel.data.helpers.TimeHelper
import de.eloc.eloc_control_panel.data.helpers.firebase.FirestoreHelper
import java.util.concurrent.Executors

private const val EXTRA_STOP = "stop_service"

class StatusUploadService : Service() {

    companion object {
        private var abort = false

        private var foregroundNotificationId: Int = -1
        var isRunning = false
            private set

        private var skipWait = false

        var uploadResult = UploadResult.Failed
        private var lastUsedNotificationId = 0
        private var statusUpdateIntervalMillis =
            PreferencesHelper.instance.getStatusUploadInterval().millis

        fun updateStatusUpdateInterval() {
            statusUpdateIntervalMillis = PreferencesHelper.instance.getStatusUploadInterval().millis
        }

        fun start(context: Context, skipWait: Boolean = false) {
            StatusUploadService.skipWait = skipWait
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

    private val task = Runnable {
        isRunning = true
        elapsedMillis = 0
        uploadResult = UploadResult.Failed
        abort = false
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
            stopSelf()
            isRunning = false
            return@Runnable
        }

        val uploadCompleted = fun() =
            ((uploadResult == UploadResult.NoData) || (uploadResult == UploadResult.Uploaded))

        val sleepInterval = 1000L
        while (!uploadCompleted()) {
            if (abort) {
                break
            }

            try {
                Thread.sleep(sleepInterval)
            } catch (_: Exception) {
            }

            if (abort) {
                break
            }

            // Try an upload is interval has elapsed
            updateStatusUpdateInterval()
            elapsedMillis += sleepInterval
            val updateIntervalMillis = statusUpdateIntervalMillis
            if (elapsedMillis >= updateIntervalMillis) {
                doUpload()
            }

            if (uploadCompleted()) {
                break
            } else {
                if (serviceNotificationBuilder != null) {
                    val remainingMillis = updateIntervalMillis - elapsedMillis
                    val remaining =
                        TimeHelper.formatMillis(applicationContext, remainingMillis, true)
                    val message = getString(R.string.retrying_in_time, remaining)
                    serviceNotificationBuilder!!.setContentText(message)
                    showNotification(foregroundNotificationId, serviceNotificationBuilder!!.build())
                }
            }
        }
        if (uploadResult == UploadResult.Uploaded) {
            val (id, notification) = createNotification(
                getString(R.string.status_upload),
                getString(R.string.eloc_status_uploaded)
            )
            showNotification(id, notification)
        }
        stopService()
    }

    private fun stopService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        elapsedMillis = 0
        uploadResult = UploadResult.Failed
        abort = false
        isRunning = false
        stopSelf()
    }

    private fun doUpload() {
        try {
            if (serviceNotificationBuilder != null) {
                val message = getString(R.string.uploading_status)
                serviceNotificationBuilder!!.setContentText(message)
                showNotification(foregroundNotificationId, serviceNotificationBuilder!!.build())
            }
            FirestoreHelper.instance.uploadDataFiles()
        } catch (_: Exception) {
        }
        elapsedMillis = 0L
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val extras = intent?.extras
            abort = extras?.getBoolean(EXTRA_STOP, false) ?: false
            val title = getString(R.string.status_upload)
            val messageResId = if (abort)
                R.string.stopping_upload_service
            else
                R.string.checking_eloc_status_to_upload
            val message = getString(messageResId)
            val (id, notification) = createNotification(title, message, true)
            if (foregroundNotificationId <= 0) {
                foregroundNotificationId = id
            }
            // todo: there seems to be a condition that causes the code to
            // fail calling startForeground() causing the app to crash
            // when attempting to upload files. Remove the try-catch when root cause of
            // issue has been isolated
            startForeground(foregroundNotificationId, notification)
            val doTask = skipWait || (!isRunning)
            if (doTask) {
                Executors.newSingleThreadExecutor().execute(task)
            }
        } catch (_: Exception) {
            // todo: See notes inside try block.
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