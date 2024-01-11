package de.eloc.eloc_control_panel.interfaces

import de.eloc.eloc_control_panel.data.CommandType

fun interface GetCommandCompletedCallback {
    fun handler(type: CommandType)
}