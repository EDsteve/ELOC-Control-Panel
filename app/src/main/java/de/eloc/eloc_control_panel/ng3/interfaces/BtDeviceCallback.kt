package de.eloc.eloc_control_panel.ng3.interfaces

import de.eloc.eloc_control_panel.ng3.data.BtDevice

fun interface BtDeviceCallback {
    fun handler(device: BtDevice)
}