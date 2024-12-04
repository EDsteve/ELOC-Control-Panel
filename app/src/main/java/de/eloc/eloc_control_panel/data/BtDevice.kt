package de.eloc.eloc_control_panel.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import de.eloc.eloc_control_panel.data.helpers.BluetoothHelper
import de.eloc.eloc_control_panel.data.util.Preferences

class BtDevice(private val bluetoothDevice: BluetoothDevice, val associationId: Int? = null) {
    companion object {
        const val DEFAULT_NAME = "<unknown device>"
    }

    var selected = false
    var rssi: Rssi? = null

    val name: String
        @SuppressLint("MissingPermission")
        get() {
            if (!BluetoothHelper.hasConnectPermission) {
                return DEFAULT_NAME
            }
            return bluetoothDevice.name ?: DEFAULT_NAME
        }

    val address: String = bluetoothDevice.address

    fun isEloc(): Boolean {
        return if (Preferences.showAllBluetoothDevices) {
            true
        } else {
            @SuppressLint("MissingPermission")
            val deviceName = if (BluetoothHelper.hasConnectPermission)
                bluetoothDevice.name ?: DEFAULT_NAME
            else
                ""
            deviceName.trim().uppercase().contains("ELOC")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is BtDevice) {
            return other.bluetoothDevice.address == bluetoothDevice.address
        }
        return false
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }
}
