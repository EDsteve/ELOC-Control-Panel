package de.eloc.eloc_control_panel.ng.models

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import de.eloc.eloc_control_panel.ng2.App

class DeviceInfo(val name: String, val address: String) {

    companion object {
        private const val DEFAULT_NAME = "<unknown device>"
        private const val DEFAULT_ADDRESS = "00:00:00:00:00:00"

        private fun getDefault(): DeviceInfo = DeviceInfo(DEFAULT_NAME, DEFAULT_ADDRESS)

        fun fromDevice(device: BluetoothDevice): DeviceInfo {
            if (ActivityCompat.checkSelfPermission(
                    App.instance,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return getDefault()
            }

            val name = device.name ?: DEFAULT_NAME
            val address = device.address ?: DEFAULT_ADDRESS
            return DeviceInfo(name, address)
        }
    }
}
