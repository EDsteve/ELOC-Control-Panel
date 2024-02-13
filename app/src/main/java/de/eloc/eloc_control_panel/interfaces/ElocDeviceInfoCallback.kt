package de.eloc.eloc_control_panel.interfaces

import de.eloc.eloc_control_panel.data.ElocDeviceInfo

fun interface ElocDeviceInfoCallback {
    fun handler(info: ElocDeviceInfo)
}
