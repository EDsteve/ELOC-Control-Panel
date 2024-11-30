package de.eloc.eloc_control_panel.activities.themable

import android.os.Bundle
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.data.AppState
import de.eloc.eloc_control_panel.data.helpers.FileSystemHelper
import de.eloc.eloc_control_panel.data.helpers.firebase.AuthHelper
import de.eloc.eloc_control_panel.data.helpers.firebase.FirestoreHelper
import de.eloc.eloc_control_panel.activities.open
import de.eloc.eloc_control_panel.activities.showModalAlert

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
                AppState.loadFrom(file)
            }
            checkProfile()
        } else {
            FirestoreHelper.instance.getProfile(authHelper.userId, ::checkProfile)
        }
    }

    private fun checkProfile() {
        if (AppState.hasValidProfile) {
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