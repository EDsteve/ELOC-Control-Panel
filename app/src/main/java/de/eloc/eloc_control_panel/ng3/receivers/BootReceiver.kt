package de.eloc.eloc_control_panel.ng3.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.eloc.eloc_control_panel.ng3.services.StatusUploadService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if ((context != null) &&
            (intent?.action == Intent.ACTION_BOOT_COMPLETED)
        ) {
            StatusUploadService.start(context)
        }
    }
}