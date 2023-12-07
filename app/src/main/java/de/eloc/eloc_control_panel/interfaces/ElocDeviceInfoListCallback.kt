package de.eloc.eloc_control_panel.interfaces

import de.eloc.eloc_control_panel.data.ElocDeviceInfo

fun interface ElocDeviceInfoListCallback {
    fun handler(list: ArrayList<ElocDeviceInfo>?)
}
