package de.eloc.eloc_control_panel.ng3.interfaces

import de.eloc.eloc_control_panel.ng3.data.UserProfile

fun interface ProfileCallback {
    fun handler(profile: UserProfile)
}
