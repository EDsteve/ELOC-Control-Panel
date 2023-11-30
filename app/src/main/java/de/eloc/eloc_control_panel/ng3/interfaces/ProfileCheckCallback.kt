package de.eloc.eloc_control_panel.ng3.interfaces

fun interface ProfileCheckCallback {
    fun handler(hasProfile: Boolean, unavailable: Boolean)
}