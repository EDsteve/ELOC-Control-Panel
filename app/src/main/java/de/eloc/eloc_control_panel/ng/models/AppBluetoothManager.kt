package de.eloc.eloc_control_panel.ng.models

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import de.eloc.eloc_control_panel.ng.App

object AppBluetoothManager {
    private var adapter: BluetoothAdapter? = null

    init {
        adapter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            val bluetoothManager =
                App.getInstance().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
            bluetoothManager?.adapter
        } else {
            BluetoothAdapter.getDefaultAdapter()
        }
    }

    fun hasInitializedAdapter(): Boolean {
        return adapter != null
    }

    fun isAdapterOn(): Boolean {
        return adapter?.state == BluetoothAdapter.STATE_ON
    }
}