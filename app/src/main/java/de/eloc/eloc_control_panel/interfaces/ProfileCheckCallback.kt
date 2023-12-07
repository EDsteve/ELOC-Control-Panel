package de.eloc.eloc_control_panel.interfaces

fun interface ProfileCheckCallback {
    fun handler(hasProfile: Boolean, unavailable: Boolean)
}