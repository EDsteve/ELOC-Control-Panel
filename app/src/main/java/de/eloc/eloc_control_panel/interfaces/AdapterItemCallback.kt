package de.eloc.eloc_control_panel.interfaces

fun interface AdapterItemCallback {
    fun handler(name: String, address: String)
}