package de.eloc.eloc_control_panel.data

import java.io.File
import java.util.Base64

class UserProfile(
    val userId: String,
    var profilePictureUrl: String = "",
    var emailAddress: String = ""
) {
    companion object {
        fun from(f: File): UserProfile? {
            if (f.isFile) {
                val bytes = f.readBytes()
                val content = String(Base64.getDecoder().decode(bytes))
                val lines = content.split("\n")
                if (lines.size >= 3) {
                    return UserProfile(lines[0], lines[1], lines[2])
                }
            }
            return null
        }
    }
}
