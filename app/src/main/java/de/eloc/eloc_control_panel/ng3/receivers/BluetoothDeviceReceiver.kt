package de.eloc.eloc_control_panel.ng3.receivers

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import de.eloc.eloc_control_panel.ng3.data.BtDevice
import de.eloc.eloc_control_panel.ng3.interfaces.BtDeviceCallback

class BluetoothDeviceReceiver(handler: BtDeviceCallback?) : BroadcastReceiver() {
    private val callback: BtDeviceCallback? = handler

    override fun onReceive(context: Context?, intent: Intent?) {
        if (BluetoothDevice.ACTION_FOUND == intent?.action) {
            @Suppress("DEPRECATION") val device =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE,
                        BluetoothDevice::class.java
                    ) else
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            if (device != null) {
                callback?.handler(BtDevice(device))
            }
        }
    }
}