package de.eloc.eloc_control_panel.activities.themable

import android.os.Bundle
import android.util.Base64
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.open
import de.eloc.eloc_control_panel.activities.showModalAlert
import de.eloc.eloc_control_panel.data.helpers.FileSystemHelper
import de.eloc.eloc_control_panel.data.helpers.firebase.AuthHelper
import de.eloc.eloc_control_panel.data.helpers.firebase.FirestoreHelper
import de.eloc.eloc_control_panel.data.util.Preferences

class LoadProfileActivity : ThemableActivity() {
    companion object {
        const val EXTRA_IS_OFFLINE_MODE = "is_offline_mode"
    }

    private var isOfflineMode = true
    private val authHelper = AuthHelper.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_load_profile)
        isOfflineMode = intent.extras?.getBoolean(EXTRA_IS_OFFLINE_MODE, false) ?: false
    }

    override fun onResume() {
        super.onResume()
        if (isOfflineMode) {
            val file = FileSystemHelper.getSavedProfileFile()
            if (file != null) {
                if (file.isFile) {
                    val bytes = file.readBytes()
                    val content = String(Base64.decode(bytes, Base64.DEFAULT))
                    val lines = content.split("\n")
                    if (lines.size >= 3) {
                        Preferences.rangerName = lines[0]
                        Preferences.profilePictureUrl = lines[2]
                    }
                }
            }
            checkProfile()
        } else {
            FirestoreHelper.instance.getProfile(authHelper.userId, ::checkProfile)
        }
    }

    private fun checkProfile() {
        if (Preferences.hasValidProfile) {
            open(HomeActivity::class.java, true)
        } else {
            showModalAlert(
                getString(R.string.ranger_profile_missing),
                getString(R.string.ranger_profile_missing_details)
            ) {
                authHelper.signOut(this@LoadProfileActivity)
            }
        }
    }
}