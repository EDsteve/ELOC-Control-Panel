package de.eloc.eloc_control_panel.ng2.models

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import de.eloc.eloc_control_panel.ng2.App

class ElocInfo(private val device: BluetoothDevice) {
    val name: String
        get() {
            if (ActivityCompat.checkSelfPermission(
                    App.instance,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    return DEFAULT_NAME
                }
            }
            return device.name ?: DEFAULT_NAME
        }

    val address: String = device.address

    val isValidDevice: Boolean
        get() {
            return name.lowercase().contains("eloc")
        }

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