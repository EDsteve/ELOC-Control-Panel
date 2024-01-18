package de.eloc.eloc_control_panel.interfaces

fun interface GoogleSignInCallback {
    fun handler(signedIn: Boolean, filtered: Boolean, error: String)
}