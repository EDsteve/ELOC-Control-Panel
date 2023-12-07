package de.eloc.eloc_control_panel.receivers

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.eloc.eloc_control_panel.interfaces.VoidCallback

class BluetoothWatcher() : BroadcastReceiver() {
    private var callback: VoidCallback? = null

    constructor(callback: VoidCallback?) : this() {
        this.callback = callback
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if (BluetoothAdapter.ACTION_STATE_CHANGED == action) {
            callback?.handler()
        }
    }
}



