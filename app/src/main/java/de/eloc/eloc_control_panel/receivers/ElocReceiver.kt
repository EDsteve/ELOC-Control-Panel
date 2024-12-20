package de.eloc.eloc_control_panel.receivers

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import de.eloc.eloc_control_panel.data.BtDevice
import de.eloc.eloc_control_panel.data.Rssi

class ElocReceiver() : BroadcastReceiver() {

    private var stateChangedCallback: (() -> Unit)? = null
    private var deviceFoundCallback: ((BtDevice) -> Unit)? = null

    constructor(
        stateChanged: (() -> Unit)? = null,
        deviceFound: ((BtDevice) -> Unit)? = null
    ) : this() {
        stateChangedCallback = stateChanged
        deviceFoundCallback = deviceFound
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
            stateChangedCallback?.invoke()
        } else if (action == BluetoothDevice.ACTION_FOUND) {
            @Suppress("DEPRECATION") val device =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    intent.getParcelableExtra(
                        BluetoothDevice.EXTRA_DEVICE,
                        BluetoothDevice::class.java
                    ) else
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

            val dBm: Int = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()

            if (device != null) {
                val eloc = BtDevice(device)
                eloc.rssi = Rssi(dBm)
                deviceFoundCallback?.invoke(eloc)
            }
        }
    }

    fun register(context: Context) {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        context.registerReceiver(this, filter)
    }

    fun unregister(context: Context) {
        context.unregisterReceiver(this)
    }
}



