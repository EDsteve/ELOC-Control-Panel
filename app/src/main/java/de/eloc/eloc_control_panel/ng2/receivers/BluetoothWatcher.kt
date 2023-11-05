package de.eloc.eloc_control_panel.ng2.receivers

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.eloc.eloc_control_panel.ng3.interfaces.VoidCallback

class BluetoothWatcher(private val callback: VoidCallback?) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
            callback?.handler()
        }
    }
}



