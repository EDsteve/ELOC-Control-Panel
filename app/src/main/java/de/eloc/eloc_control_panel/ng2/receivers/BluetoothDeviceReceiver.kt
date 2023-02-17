package de.eloc.eloc_control_panel.ng2.receivers

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import de.eloc.eloc_control_panel.ng2.interfaces.ElocCallback
import de.eloc.eloc_control_panel.ng2.models.ElocInfo

class BluetoothDeviceReceiver(handler: ElocCallback?) : BroadcastReceiver() {
    private val callback: ElocCallback? = handler

    override fun onReceive(context: Context?, intent: Intent?) {
        if (BluetoothDevice.ACTION_FOUND == intent?.action) {
            @Suppress("DEPRECATION") val device =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
                    intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE,
                        BluetoothDevice::class.java
                    ) else
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            if (device != null) {
                addDevice(ElocInfo(device))
                Log.d("TAG---------------------------", "onReceive: " + device.name)
            }
        }
    }

    fun addDevice(info: ElocInfo) {
        callback?.handler(info)
    }
}