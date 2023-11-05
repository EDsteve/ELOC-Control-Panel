package de.eloc.eloc_control_panel.ng3.interfaces

import de.eloc.eloc_control_panel.ng3.data.ConnectionStatus

fun interface ConnectionStatusListener {
    fun onStatusChanged(status: ConnectionStatus)
}