package de.eloc.eloc_control_panel.data

import android.util.Base64
import de.eloc.eloc_control_panel.data.helpers.firebase.AuthHelper
import java.io.File

object AppState {
    var rangerName: String = ""
        set(value) {
            field = value.trim()
        }

    var profilePictureUrl: String = ""
        set(value) {
            field = value.trim()
        }

    val emailAddress: String
        get() = AuthHelper.instance.emailAddress

    val hasValidProfile: Boolean
        get() {
            val hasName = rangerName.trim().isNotEmpty()
            val hasEmailAddress = emailAddress.trim().isNotEmpty()
            return hasName && hasEmailAddress
        }

    val isSignedIn = AuthHelper.instance.isSignedIn

    val isEmailAddressVerified = AuthHelper.instance.isEmailAddressVerified

    fun clear() {
        rangerName = ""
        profilePictureUrl = ""
    }

    fun loadFrom(f: File): Boolean {
        if (f.isFile) {
            val bytes = f.readBytes()
            val content = String(Base64.decode(bytes, Base64.DEFAULT))
            val lines = content.split("\n")
            if (lines.size >= 3) {
                rangerName = lines[0]
                profilePictureUrl = lines[2]
            }
        }
        return hasValidProfile
    }
}