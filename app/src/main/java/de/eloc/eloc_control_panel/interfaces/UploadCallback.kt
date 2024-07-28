package de.eloc.eloc_control_panel.interfaces

import de.eloc.eloc_control_panel.data.UploadResult

fun interface UploadCallback {
    fun handler(result: UploadResult)
}