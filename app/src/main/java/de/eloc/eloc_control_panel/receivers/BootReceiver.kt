package de.eloc.eloc_control_panel.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.eloc.eloc_control_panel.services.StatusUploadWorker

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            if (context != null) {
                // Never start the foreground service from here: Android refuses a
                // dataSync service launched from BOOT_COMPLETED and throws
                // ForegroundServiceStartNotAllowedException, which took the whole
                // app down on every reboot. WorkManager is the supported route,
                // and it reschedules its own queue after a reboot anyway.
                StatusUploadWorker.enqueue(context)
            }
        }
    }
}
