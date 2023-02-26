package de.eloc.eloc_control_panel.ng2.interfaces

import de.eloc.eloc_control_panel.ng2.models.ElocInfo

fun interface ElocCallback {
    fun handler(info: ElocInfo)
}