package de.eloc.eloc_control_panel.data.util

import de.eloc.eloc_control_panel.App
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.data.GpsData
import de.eloc.eloc_control_panel.data.MainMenuPosition
import de.eloc.eloc_control_panel.data.PreferredFontSize
import de.eloc.eloc_control_panel.data.RssiLabel
import de.eloc.eloc_control_panel.data.StatusUploadInterval
import de.eloc.eloc_control_panel.data.helpers.LocationHelper
import de.eloc.eloc_control_panel.data.helpers.firebase.AuthHelper

object Preferences {
    private const val PREF_LOG_BT_TRAFFIC = "log_bt_traffic"
    const val PREF_STATUS_UPLOAD_INTERVAL = "status_upload_interval"
    private const val PREF_CAMERA_REQUESTED = "camera_requested"
    private const val PREF_SHOW_ALL_BT_DEVICES = "show_all_bt_devices"
    const val PREF_MAIN_MENU_POSITION = "app_menu_position"
    private const val PREF_LOCATION_REQUESTED = "location_requested"
    private const val PREF_BLUETOOTH_REQUESTED = "bluetooth_requested"
    private const val PREF_NOTIFICATIONS_REQUESTED = "notifications_requested"
    const val PREF_USER_FONT_SIZE = "user_font_size"
    const val PREF_GPS_LOCATION_TIMEOUT = "gps_location_timeout"
    private const val PREF_AUTO_GOOGLE_SIGN_IN = "auto_google_sign_in"
    private const val PREF_LAST_KNOWN_LOCATION = "last_known_location"

    private const val PREF_ACCOUNT_RANGER_NAME = "account_ranger_name"
    private const val PREF_ACCOUNT_PROFILE_PIC_URL = "account_pfp_url"
    private const val PREF_RSS_LABEL_TYPE = "rssi_label_type"
    private const val RANGER_NOT_SET = "<ranger not set>"
    const val MIN_GPS_TIMEOUT_SECONDS = 15
    const val MAX_GPS_TIMEOUT_SECONDS = 120

    private val preferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(App.instance)

    var logBtTraffic: Boolean
        get() = preferences.getBoolean(PREF_LOG_BT_TRAFFIC, false)
        set(value) = preferences.edit().putBoolean(PREF_LOG_BT_TRAFFIC, value).apply()

    var gpsLocationTimeoutSeconds: Int
        get() = preferences.getInt(PREF_GPS_LOCATION_TIMEOUT, MIN_GPS_TIMEOUT_SECONDS)
        set(value) {
            val sanitizedValue = if (value < MIN_GPS_TIMEOUT_SECONDS) {
                MIN_GPS_TIMEOUT_SECONDS
            } else if (value > MAX_GPS_TIMEOUT_SECONDS) {
                MAX_GPS_TIMEOUT_SECONDS
            } else {
                value
            }
            preferences.edit().putInt(PREF_GPS_LOCATION_TIMEOUT, sanitizedValue).apply()
        }

    var lastKnownGpsLocation: GpsData?
        get() {
            val data =
                preferences.getString(PREF_LAST_KNOWN_LOCATION, LocationHelper.unknownLocation)
                    ?: LocationHelper.unknownLocation
            return GpsData.deserialize(data)
        }
        set(value) = preferences.edit()
            .putString(PREF_LAST_KNOWN_LOCATION, value?.serialize() ?: "")
            .apply()

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

    var mainMenuPosition: MainMenuPosition
        get() {
            val code = preferences.getInt(PREF_MAIN_MENU_POSITION, -1)
            return MainMenuPosition.parse(code)
        }
        set(value) = preferences.edit().putInt(PREF_MAIN_MENU_POSITION, value.code).apply()

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
