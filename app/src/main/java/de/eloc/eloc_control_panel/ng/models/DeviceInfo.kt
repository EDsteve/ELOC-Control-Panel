package de.eloc.eloc_control_panel.ng.models

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import de.eloc.eloc_control_panel.ng2.models.BluetoothHelper

class DeviceInfo(val name: String, val address: String) {

    companion object {
        const val DEFAULT_NAME = "<unknown device>"
        private const val DEFAULT_ADDRESS = "00:00:00:00:00:00"

        val default: DeviceInfo = DeviceInfo(DEFAULT_NAME, DEFAULT_ADDRESS)

        @SuppressLint("MissingPermission")
        fun fromDevice(device: BluetoothDevice): DeviceInfo {
            if (!BluetoothHelper.instance.hasConnectPermission()) {
                return default
            }

            val name = device.name ?: DEFAULT_NAME
            val address = device.address ?: DEFAULT_ADDRESS
            return DeviceInfo(name, address)
        }
    }
}
