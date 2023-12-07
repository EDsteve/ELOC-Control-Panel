package de.eloc.eloc_control_panel.interfaces

import de.eloc.eloc_control_panel.data.ConnectionStatus

fun interface ConnectionStatusListener {
    fun onStatusChanged(status: ConnectionStatus)
}