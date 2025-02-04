package de.eloc.eloc_control_panel.data.util

import de.eloc.eloc_control_panel.App
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.data.PreferredFontSize
import de.eloc.eloc_control_panel.data.RssiLabel
import de.eloc.eloc_control_panel.data.StatusUploadInterval
import de.eloc.eloc_control_panel.data.helpers.firebase.AuthHelper

object Preferences {
    private const val PREF_STATUS_UPLOAD_INTERVAL = "status_upload_interval"
    private const val PREF_CAMERA_REQUESTED = "camera_requested"
    private const val PREF_SHOW_ALL_BT_DEVICES = "show_all_bt_devices"
    private const val PREF_MAIN_MENU_POSITION = "main_menu_position"
    private const val PREF_LOCATION_REQUESTED = "location_requested"
    private const val PREF_BLUETOOTH_REQUESTED = "bluetooth_requested"
    private const val PREF_NOTIFICATIONS_REQUESTED = "notifications_requested"
    private const val PREF_USER_FONT_SIZE = "user_font_size"
    private const val PREF_AUTO_GOOGLE_SIGN_IN = "auto_google_sign_in"

    private const val PREF_ACCOUNT_RANGER_NAME = "account_ranger_name"
    private const val PREF_ACCOUNT_PROFILE_PIC_URL = "account_pfp_url"
    private const val PREF_RSS_LABEL_TYPE = "rssi_label_type"
    private const val RANGER_NOT_SET = "<ranger not set>"

    private val preferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(App.instance)

    var rssiLabel: RssiLabel
        get() {
            val code = preferences.getInt(PREF_RSS_LABEL_TYPE, RssiLabel.PowerOnly.type)
            return RssiLabel.valueOf(code)
        }
        set(value) = preferences.edit().putInt(PREF_RSS_LABEL_TYPE, value.type).apply()


    var rangerName: String
        get() {
            var name = RANGER_NOT_SET
            try {
                name = preferences.getString(PREF_ACCOUNT_RANGER_NAME, RANGER_NOT_SET)
                    ?: RANGER_NOT_SET
            } catch (_: Exception) {
            }
            return name
        }
        set(value) = preferences.edit().putString(PREF_ACCOUNT_RANGER_NAME, value).apply()

    var profilePictureUrl: String
        get() {
            var url = ""
            try {
                url = preferences.getString(PREF_ACCOUNT_PROFILE_PIC_URL, "") ?: ""
            } catch (_: Exception) {
            }
            return url
        }
        set(value) = preferences.edit().putString(PREF_ACCOUNT_PROFILE_PIC_URL, value).apply()

    var isMainMenuOnLeft: Boolean
        get() = preferences.getBoolean(PREF_MAIN_MENU_POSITION, false)
        set(value) = preferences.edit().putBoolean(PREF_MAIN_MENU_POSITION, value).apply()

    var autoGoogleSignIn: Boolean
        get() = preferences.getBoolean(PREF_AUTO_GOOGLE_SIGN_IN, false)
        set(value) = preferences.edit().putBoolean(PREF_AUTO_GOOGLE_SIGN_IN, value).apply()

    var preferredFontSize: PreferredFontSize
        get() {
            val code = preferences.getInt(PREF_USER_FONT_SIZE, -1)
            return PreferredFontSize.parse(code)
        }
        set(value) = preferences.edit().putInt(PREF_USER_FONT_SIZE, value.code).apply()

    var showAllBluetoothDevices: Boolean
        get() = preferences.getBoolean(PREF_SHOW_ALL_BT_DEVICES, false)
        set(value) =
            preferences.edit().putBoolean(PREF_SHOW_ALL_BT_DEVICES, value).apply()

    var statusUploadInterval: StatusUploadInterval
        get() {
            val code = preferences.getInt(PREF_STATUS_UPLOAD_INTERVAL, -1)
            return StatusUploadInterval.parse(code)
        }
        set(value) = preferences.edit().putInt(PREF_STATUS_UPLOAD_INTERVAL, value.seconds).apply()

    val cameraRequested: Boolean get() = preferences.getBoolean(PREF_CAMERA_REQUESTED, false)
    val locationRequested: Boolean get() = preferences.getBoolean(PREF_LOCATION_REQUESTED, false)
    val bluetoothRequested: Boolean get() = preferences.getBoolean(PREF_BLUETOOTH_REQUESTED, false)
    val notificationsRequested: Boolean
        get() = preferences.getBoolean(
            PREF_NOTIFICATIONS_REQUESTED,
            false
        )
    val preferredFontThemeID: Int
        get() = when (preferredFontSize) {
            PreferredFontSize.Small -> R.style.AppTheme
            PreferredFontSize.Large -> R.style.AppThemeLarge
            else -> R.style.AppThemeMedium
        }
    val hasValidProfile: Boolean
        get() {
            val hasName = rangerName.trim().isNotEmpty()
            val hasEmailAddress = AuthHelper.instance.emailAddress.trim().isNotEmpty()
            return hasName && hasEmailAddress
        }

    fun setLocationRequested() =
        preferences.edit().putBoolean(PREF_LOCATION_REQUESTED, true).apply()

    fun setBluetoothRequested() =
        preferences.edit().putBoolean(PREF_BLUETOOTH_REQUESTED, true).apply()

    fun setNotificationsRequested() =
        preferences.edit().putBoolean(PREF_NOTIFICATIONS_REQUESTED, true).apply()

    fun setCameraRequested() =
        preferences.edit().putBoolean(PREF_CAMERA_REQUESTED, true).apply()

    fun clearProfileData() {
        rangerName = ""
        profilePictureUrl = ""
    }

}
