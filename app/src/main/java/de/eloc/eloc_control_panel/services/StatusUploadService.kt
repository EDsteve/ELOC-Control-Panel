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
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.themable.HomeActivity
import de.eloc.eloc_control_panel.activities.themable.UpdateAppActivity
import de.eloc.eloc_control_panel.data.UploadResult
import de.eloc.eloc_control_panel.data.helpers.HttpHelper
import de.eloc.eloc_control_panel.data.helpers.firebase.FirestoreHelper
import java.util.concurrent.Executors

private const val EXTRA_STOP = "stop_service"
private const val TAG = "StatusUploadService"

/**
 * The interactive half of the status upload: one attempt, with a notification so
 * the person standing at the device sees what happened.
 *
 * It deliberately does NOT retry in a loop any more. A service only lives as long
 * as its process, so a loop here could never survive a kill — and while it ran it
 * burned the Android 15+ six-hour daily budget for dataSync services. Anything
 * that does not go out on this attempt is handed to [StatusUploadWorker], which
 * WorkManager persists across process death and reboot.
 */
class StatusUploadService : Service() {

    companion object {
        private var stopRequested = false

        private var foregroundNotificationId: Int = -1
        var isRunningTask = false
            private set

        private var lastUsedNotificationId = 0

        fun start(context: Context) {
            // Queue the durable attempt first: if the service cannot start — or
            // starts and is killed mid-flight — the upload still happens later.
            StatusUploadWorker.enqueue(context)
            try {
                val serviceIntent = Intent(context, StatusUploadService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (e: Exception) {
                // e.g. ForegroundServiceStartNotAllowedException when the caller is
                // in the background. The worker above still covers us.
                Log.w(TAG, "start: foreground service refused; worker will handle it", e)
            }
        }

        private fun createNewNotificationId(): Int {
            lastUsedNotificationId++
            return lastUsedNotificationId
        }
    }

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
        try {
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
                return@Runnable
            }

            when (doUpload()) {
                UploadResult.Uploaded -> {
                    val (id, notification) = createNotification(
                        getString(R.string.status_upload),
                        getString(R.string.eloc_info_uploaded)
                    )
                    showNotification(id, notification)
                }

                UploadResult.Failed -> {
                    // Hand the retry to WorkManager — it outlives this process.
                    StatusUploadWorker.enqueue(applicationContext)
                    val (id, notification) = createNotification(
                        getString(R.string.status_upload),
                        getString(R.string.upload_will_retry)
                    )
                    showNotification(id, notification)
                }

                UploadResult.NoData -> {
                    Log.d(TAG, "uploadTask: nothing pending")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "uploadTask: attempt failed; leaving it to the worker", e)
            StatusUploadWorker.enqueue(applicationContext)
        } finally {
            // Always, so the service can never be left busy forever. A stuck
            // isRunningTask is what used to make every later attempt answer
            // "An upload is already in progress" and then do nothing at all.
            abortService()
        }
    }

    private fun abortService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
        stopRequested = false
        isRunningTask = false
        stopSelf()
    }

    private fun doUpload(): UploadResult {
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
        var result: UploadResult
        try {
            if (serviceNotificationBuilder != null) {
                val message = getString(R.string.uploading_status)
                serviceNotificationBuilder!!.setContentText(message)
                showNotification(foregroundNotificationId, serviceNotificationBuilder!!.build())
            }
            result = FirestoreHelper.instance.uploadDataFiles()
        } catch (e: Exception) {
            Log.w(TAG, "doUpload: upload failed", e)
            result = UploadResult.Failed
        }
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

        try {
            startForeground(foregroundNotificationId, notification)
        } catch (e: Exception) {
            // Android refuses a dataSync foreground service started from the
            // background, and from BOOT_COMPLETED outright. Do not take the app
            // down over it — the worker already has the job.
            Log.w(TAG, "onStartCommand: could not go foreground", e)
            StatusUploadWorker.enqueue(applicationContext)
            stopSelf()
            return START_NOT_STICKY
        }

        if (stopRequested) {
            StatusUploadWorker.cancel(applicationContext)
            abortService()
            return START_NOT_STICKY
        }

        if (!isRunningTask) {
            Executors.newSingleThreadExecutor().execute(uploadTask)
        }
        // One attempt only; the worker owns anything that needs retrying, so there
        // is nothing worth redelivering if this process dies.
        return START_NOT_STICKY
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
