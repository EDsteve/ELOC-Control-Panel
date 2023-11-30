package de.eloc.eloc_control_panel.ng3.data.helpers

import de.eloc.eloc_control_panel.ng3.App
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.ng3.data.PreferredFontSize
import de.eloc.eloc_control_panel.ng3.data.StatusUploadInterval

// todo: remove dead code

class PreferencesHelper {
    private val preferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(App.instance)

    fun setShowAllBluetoothDevices(show: Boolean) =
        preferences.edit().putBoolean(PREF_SHOW_ALL_BT_DEVICES, show).apply()

    fun showingAllBluetoothDevices(): Boolean =
        preferences.getBoolean(PREF_SHOW_ALL_BT_DEVICES, false)

    fun isLocationPromptDisabled(): Boolean =
        preferences.getBoolean(PREF_DISABLE_LOCATION_PROMPT, false)

    fun getLastGoogleTimestamp(): Long =
        preferences.getLong(PREF_LAST_GOOGLE_TIMESTAMP, 0)

    fun getCurrentElapsedTime(): Long =
        preferences.getLong(PREF_ELAPSED_TIME_AT_GOOGLE_TIMESTAMP, 0)

    fun getPromptedBluetooth(): Boolean =
        preferences.getBoolean(PREF_PROMPTED_BLUETOOTH, false)

    fun getMicData(): String = preferences.getString(PREF_MICROPHONE_SETTINGS, "") ?: ""

    fun getPromptedLocation(): Boolean =
        preferences.getBoolean(PREF_PROMPTED_LOCATION, false)

    fun getBluetoothRequested(): Boolean = preferences.getBoolean(PREF_BLUETOOTH_REQUESTED, false)

    fun getNotificationsRequested(): Boolean = preferences.getBoolean(PREF_NOTIFICATIONS_REQUESTED, false)

    fun getLocationRequested(): Boolean = preferences.getBoolean(PREF_LOCATION_REQUESTED, false)

    fun getCameraRequested(): Boolean = preferences.getBoolean(PREF_CAMERA_REQUESTED, false)

    fun getDeviceSettings(): String = preferences.getString(PREF_DEVICE_SETTINGS, "") ?: ""

    // Assume default state to be ON (i.e., return true), because that is default behavior when the preference
    // has not yet been set
    fun getBluetoothRecordingState(): Boolean = preferences.getBoolean(
        PREF_BLUETOOTH_RECORDING_STATE, true
    )

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

    fun setDeviceSettings(data: String) =
        preferences.edit().putString(PREF_DEVICE_SETTINGS, data).apply()

    private fun setCurrentElapsedTime(elapsed: Long) =
        preferences.edit().putLong(PREF_ELAPSED_TIME_AT_GOOGLE_TIMESTAMP, elapsed).apply()

    private fun setLastGoogleTimestamp(ts: Long) =
        preferences.edit().putLong(PREF_LAST_GOOGLE_TIMESTAMP, ts).apply()

    fun setLocationPromptDisabled() =
        preferences.edit().putBoolean(PREF_DISABLE_LOCATION_PROMPT, true).apply()

    fun setPromptedLocation() =
        preferences.edit().putBoolean(PREF_PROMPTED_LOCATION, true).apply()

    fun setPromptedBluetooth() =
        preferences.edit().putBoolean(PREF_PROMPTED_BLUETOOTH, true).apply()

    fun setMicrophoneSettings(data: String) =
        preferences.edit().putString(PREF_MICROPHONE_SETTINGS, data).apply()

    fun setBluetoothRecordingState(btOn: Boolean) = preferences.edit().putBoolean(
        PREF_BLUETOOTH_RECORDING_STATE, btOn
    ).apply()

    fun saveTimestamps(currentElapsedTimeMS: Long, lastGoogleSyncTimestampMS: Long) {
        setCurrentElapsedTime(currentElapsedTimeMS)
        setLastGoogleTimestamp(lastGoogleSyncTimestampMS)
    }

    fun getPreferredFontSize(): PreferredFontSize {
        val code = preferences.getInt(PREF_USER_FONT_SIZE, -1)
        return PreferredFontSize.parse(code)
    }

    fun setPreferredFontSize(fontSize: PreferredFontSize) =
        preferences.edit().putInt(PREF_USER_FONT_SIZE, fontSize.code).apply()


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
        private const val PREF_PROMPTED_BLUETOOTH = "has_prompted_bluetooth"
        private const val PREF_PROMPTED_LOCATION = "has_prompted_location"
        private const val PREF_MICROPHONE_SETTINGS = "microphone_settings"
        private const val PREF_DEVICE_SETTINGS = "device_settings"
        private const val PREF_ELAPSED_TIME_AT_GOOGLE_TIMESTAMP = "elapsedTimeAtGoogleTimestamp"
        private const val PREF_LAST_GOOGLE_TIMESTAMP = "lastGoogleTimestamp"
        private const val PREF_DISABLE_LOCATION_PROMPT = "disableLocationPrompt"
        private const val PREF_BLUETOOTH_RECORDING_STATE = "bluetooth_recording_state"
        private const val PREF_USER_FONT_SIZE = "user_font_size"

        val instance get() = PreferencesHelper()
    }
}
