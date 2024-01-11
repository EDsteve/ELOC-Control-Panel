package de.eloc.eloc_control_panel.data.helpers

import de.eloc.eloc_control_panel.App
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.data.PreferredFontSize
import de.eloc.eloc_control_panel.data.StatusUploadInterval

// todo: remove dead code

class PreferencesHelper {
    private val preferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(App.instance)

    fun setShowAllBluetoothDevices(show: Boolean) =
        preferences.edit().putBoolean(PREF_SHOW_ALL_BT_DEVICES, show).apply()

    fun showingAllBluetoothDevices(): Boolean =
        preferences.getBoolean(PREF_SHOW_ALL_BT_DEVICES, false)

    fun getBluetoothRequested(): Boolean = preferences.getBoolean(PREF_BLUETOOTH_REQUESTED, false)

    fun getNotificationsRequested(): Boolean =
        preferences.getBoolean(PREF_NOTIFICATIONS_REQUESTED, false)

    fun getLocationRequested(): Boolean = preferences.getBoolean(PREF_LOCATION_REQUESTED, false)

    fun getCameraRequested(): Boolean = preferences.getBoolean(PREF_CAMERA_REQUESTED, false)

    fun getStatusUploadInterval(): StatusUploadInterval {
        val code = preferences.getInt(PREF_STATUS_UPLOAD_INTERVAL, -1)
        return StatusUploadInterval.parse(code)
    }

    fun setBluetoothRequested() =
        preferences.edit().putBoolean(PREF_BLUETOOTH_REQUESTED, true).apply()

    fun setNotificationsRequested() =
        preferences.edit().putBoolean(PREF_NOTIFICATIONS_REQUESTED, true).apply()

    fun setStatusUploadInterval(interval: StatusUploadInterval) =
        preferences.edit().putInt(PREF_STATUS_UPLOAD_INTERVAL, interval.seconds).apply()

    fun setLocationRequested() =
        preferences.edit().putBoolean(PREF_LOCATION_REQUESTED, true).apply()

    fun setCameraRequested() =
        preferences.edit().putBoolean(PREF_CAMERA_REQUESTED, true).apply()

    private fun setCurrentElapsedTime(elapsed: Long) =
        preferences.edit().putLong(PREF_ELAPSED_TIME_AT_GOOGLE_TIMESTAMP, elapsed).apply()

    private fun setLastGoogleTimestamp(ts: Long) =
        preferences.edit().putLong(PREF_LAST_GOOGLE_TIMESTAMP, ts).apply()

    fun saveTimestamps(currentElapsedTimeMS: Long, lastGoogleSyncTimestampMS: Long) {
        setCurrentElapsedTime(currentElapsedTimeMS)
        setLastGoogleTimestamp(lastGoogleSyncTimestampMS)
    }

    fun getPreferredFontSize(): PreferredFontSize {
        val code = preferences.getInt(PREF_USER_FONT_SIZE, -1)
        return PreferredFontSize.parse(code)
    }

    fun getAutoGoogleSignIn(): Boolean = preferences.getBoolean(PREF_AUTO_GOOGLE_SIGN_IN, false)

    fun setPreferredFontSize(fontSize: PreferredFontSize) =
        preferences.edit().putInt(PREF_USER_FONT_SIZE, fontSize.code).apply()

    fun setAutoGoogleSignIn(autoSignIn: Boolean) = preferences.edit().putBoolean(
        PREF_AUTO_GOOGLE_SIGN_IN, autoSignIn
    ).apply()


    fun setMainMenuPosition(left: Boolean) {
        preferences.edit().putBoolean(PREF_MAIN_MENU_POSITION, left).apply()
    }

    fun isMainMenuOnLeft(): Boolean {
        return preferences.getBoolean(PREF_MAIN_MENU_POSITION, false)
    }

    fun getPreferredFontThemeID(): Int = when (getPreferredFontSize()) {
        PreferredFontSize.Small -> R.style.AppTheme
        PreferredFontSize.Large -> R.style.AppThemeLarge
        else -> R.style.AppThemeMedium
    }

    companion object {
        private const val PREF_STATUS_UPLOAD_INTERVAL = "status_upload_interval"
        private const val PREF_CAMERA_REQUESTED = "camera_requested"
        private const val PREF_SHOW_ALL_BT_DEVICES = "show_all_bt_devices"
        private const val PREF_MAIN_MENU_POSITION = "main_menu_position"
        private const val PREF_LOCATION_REQUESTED = "location_requested"
        private const val PREF_BLUETOOTH_REQUESTED = "bluetooth_requested"
        private const val PREF_NOTIFICATIONS_REQUESTED = "notifications_requested"
        private const val PREF_ELAPSED_TIME_AT_GOOGLE_TIMESTAMP = "elapsedTimeAtGoogleTimestamp"
        private const val PREF_LAST_GOOGLE_TIMESTAMP = "lastGoogleTimestamp"
        private const val PREF_USER_FONT_SIZE = "user_font_size"
        private const val PREF_AUTO_GOOGLE_SIGN_IN = "auto_google_sign_in"

        val instance get() = PreferencesHelper()
    }
}
