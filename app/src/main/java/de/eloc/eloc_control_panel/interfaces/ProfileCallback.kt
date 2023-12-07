package de.eloc.eloc_control_panel.interfaces

import de.eloc.eloc_control_panel.data.UserProfile

fun interface ProfileCallback {
    fun handler(profile: UserProfile?)
}
