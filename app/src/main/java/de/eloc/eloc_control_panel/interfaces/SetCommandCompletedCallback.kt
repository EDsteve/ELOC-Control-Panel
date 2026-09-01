package de.eloc.eloc_control_panel.interfaces

import de.eloc.eloc_control_panel.data.CommandType

fun interface SetCommandCompletedCallback {
    /**
     * @param success true when the device reported ecode 0
     * @param type which set command completed
     * @param errorMessage the device's rejection reason (the response's "error" field), empty when
     *                     the command succeeded or the device gave no explanation. Shown to the
     *                     user, e.g. when recording is refused because no SD card is inserted.
     */
    fun handler(success: Boolean, type: CommandType, errorMessage: String)
}
