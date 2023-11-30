package de.eloc.eloc_control_panel.ng3.interfaces

import de.eloc.eloc_control_panel.ng3.data.ElocDeviceInfo

fun interface ElocDeviceInfoListCallback {
    fun handler(list: ArrayList<ElocDeviceInfo>?)
}
