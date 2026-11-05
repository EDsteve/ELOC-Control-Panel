package de.eloc.eloc_control_panel.services

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.FirebaseApp
import de.eloc.eloc_control_panel.data.UploadResult
import de.eloc.eloc_control_panel.data.helpers.firebase.FirestoreHelper
import de.eloc.eloc_control_panel.data.util.Preferences
import java.util.concurrent.TimeUnit

/**
 * The durable half of the status upload.
 *
 * [StatusUploadService] gives the person standing at the device immediate feedback,
 * but it only lives as long as its process: once Android stops it — a low-memory
 * kill, a Play services update, the Android 15+ six-hour cap on `dataSync`
 * services — anything still queued used to wait for the next app launch. That is
 * how a status captured in the evening could surface the next morning.
 *
 * WorkManager keeps its queue on disk, so a pending upload survives process death
 * and reboot, and runs as soon as the phone has a network again.
 */
class StatusUploadWorker(context: Context, params: WorkerParameters) :
    Worker(context, params) {

    companion object {
        private const val TAG = "StatusUploadWorker"
        private const val WORK_NAME = "eloc_status_upload"

        /**
         * Queue a drain of the pending-upload folder. Safe to call as often as you
         * like: [ExistingWorkPolicy.KEEP] lets an already-scheduled attempt stand.
         */
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<StatusUploadWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    Preferences.statusUploadInterval.millis,
                    TimeUnit.MILLISECONDS
                )
                .build()
            try {
                WorkManager.getInstance(context.applicationContext)
                    .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
                Log.i(TAG, "enqueue: status upload queued")
            } catch (e: Exception) {
                Log.w(TAG, "enqueue: could not queue status upload", e)
            }
        }

        /** Drop a scheduled attempt — the Cancel action on the service notification. */
        fun cancel(context: Context) {
            try {
                WorkManager.getInstance(context.applicationContext)
                    .cancelUniqueWork(WORK_NAME)
            } catch (e: Exception) {
                Log.w(TAG, "cancel: could not cancel status upload", e)
            }
        }
    }

    override fun doWork(): Result {
        if (FirebaseApp.getApps(applicationContext).isEmpty()) {
            FirebaseApp.initializeApp(applicationContext)
        }
        return try {
            when (FirestoreHelper.instance.uploadDataFiles()) {
                UploadResult.Uploaded, UploadResult.NoData -> {
                    Log.i(TAG, "doWork: pending uploads drained")
                    Result.success()
                }

                UploadResult.Failed -> {
                    Log.w(TAG, "doWork: files still pending — will retry")
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "doWork: upload attempt failed", e)
            Result.retry()
        }
    }
}
