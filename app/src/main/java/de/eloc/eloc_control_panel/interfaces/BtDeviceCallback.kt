package de.eloc.eloc_control_panel.interfaces

import de.eloc.eloc_control_panel.data.BtDevice

fun interface BtDeviceCallback {
    fun handler(device: BtDevice)
}