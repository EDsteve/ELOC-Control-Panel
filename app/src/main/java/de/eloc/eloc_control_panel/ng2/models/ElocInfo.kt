package de.eloc.eloc_control_panel.ng2.models

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice

class ElocInfo(val device: BluetoothDevice) {
    val name: String
        @SuppressLint("MissingPermission")
        get() {
            if (!BluetoothHelper.instance.hasConnectPermission()) {
                return DEFAULT_NAME
            }
            return device.name ?: DEFAULT_NAME
        }

    val address: String = device.address

    override fun equals(other: Any?): Boolean {
        if (other is ElocInfo) {
            return other.device.address == device.address
        }
        return false
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }

    companion object {
        private const val DEFAULT_NAME = "<unknown device>"
    }
}
