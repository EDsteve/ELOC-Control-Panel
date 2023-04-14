package de.eloc.eloc_control_panel.ng2.models

import de.eloc.eloc_control_panel.ng2.App
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class PreferencesHelper {
    private val preferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(App.instance)

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

    fun getLocationRequested(): Boolean = preferences.getBoolean(PREF_LOCATION_REQUESTED, false)

    fun getRangerName(): String = preferences.getString(PREF_RANGER_NAME, DEFAULT_RANGER_NAME)
        ?: DEFAULT_RANGER_NAME

    fun getDeviceSettings(): String = preferences.getString(PREF_DEVICE_SETTINGS, "") ?: ""

    // Assume default state to be ON (i.e., return true), because that is default behavior when the preference
    // has not yet been set
    fun getBluetoothRecordingState(): Boolean = preferences.getBoolean(
        PREF_BLUETOOTH_RECORDING_STATE, true
    )

    fun setBluetoothRequested() =
        preferences.edit().putBoolean(PREF_BLUETOOTH_REQUESTED, true).apply()

    fun setLocationRequested() =
        preferences.edit().putBoolean(PREF_LOCATION_REQUESTED, true).apply()

    fun setRangerName(name: String) =
        preferences.edit().putString(PREF_RANGER_NAME, name.trim()).apply()

    fun setDeviceSettings(data: String) =
        preferences.edit().putString(PREF_DEVICE_SETTINGS, data).apply()

    fun setCurrentElapsedTime(elapsed: Long) =
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

    fun getPreferredFontSize(): Int =
        preferences.getInt(PREF_USER_FONT_SIZE, -1)

    fun setPreferredFontSize(size: Int) {
        /*
        Notes:
        - negative value = small
        - zero = medium
        - positive value = large
         */
        preferences.edit().putInt(PREF_USER_FONT_SIZE, size).apply()
    }

    companion object {
        private var cInstance: PreferencesHelper? = null
        private const val PREF_LOCATION_REQUESTED = "location_requested"
        private const val PREF_BLUETOOTH_REQUESTED = "bluetooth_requested"
        private const val PREF_RANGER_NAME = "rangerName"
        private const val PREF_PROMPTED_BLUETOOTH = "has_prompted_bluetooth"
        private const val PREF_PROMPTED_LOCATION = "has_prompted_location"
        private const val PREF_MICROPHONE_SETTINGS = "microphone_settings"
        private const val PREF_DEVICE_SETTINGS = "device_settings"
        private const val PREF_ELAPSED_TIME_AT_GOOGLE_TIMESTAMP = "elapsedTimeAtGoogleTimestamp"
        private const val PREF_LAST_GOOGLE_TIMESTAMP = "lastGoogleTimestamp"
        private const val PREF_DISABLE_LOCATION_PROMPT = "disableLocationPrompt"
        private const val PREF_BLUETOOTH_RECORDING_STATE = "bluetooth_recording_state"
        private const val PREF_USER_FONT_SIZE = "user_font_size";
        const val DEFAULT_RANGER_NAME = "notSet"

        val instance: PreferencesHelper
            get() {
                if (cInstance == null) {
                    cInstance = PreferencesHelper()
                }
                return cInstance!!
            }
    }
}
