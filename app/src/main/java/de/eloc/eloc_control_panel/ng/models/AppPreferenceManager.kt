package de.eloc.eloc_control_panel.ng.models

import androidx.preference.PreferenceManager
import de.eloc.eloc_control_panel.ng2.App

object AppPreferenceManager {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(App.instance)

    private const val PREF_PROMPTED_BLUETOOTH = "has_prompted_bluetooth"
    private const val PREF_PROMPTED_LOCATION = "has_prompted_location"
    private const val PREF_MICROPHONE_SETTINGS = "microphone_settings"
    private const val PREF_DEVICE_SETTINGS = "device_settings"

    private const val PREF_ELAPSED_TIME_AT_GOOGLE_TIMESTAMP = "elapsedTimeAtGoogleTimestamp"
    private const val PREF_LAST_GOOGLE_TIMESTAMP = "lastGoogleTimestamp"

    fun setDeviceSettings(data: String) {
        preferences.edit()
            .putString(PREF_DEVICE_SETTINGS, data)
            .apply()
    }

    fun getDeviceSettings(): String = preferences.getString(PREF_DEVICE_SETTINGS, "") ?: ""

    fun setMicrophoneSettings(data: String) {
        preferences.edit()
            .putString(PREF_MICROPHONE_SETTINGS, data)
            .apply()
    }

    fun getMicData(): String = preferences.getString(PREF_MICROPHONE_SETTINGS, "") ?: ""

    fun setPromptedBluetooth() {
        preferences.edit()
            .putBoolean(PREF_PROMPTED_BLUETOOTH, true)
            .apply()
    }

    fun getPromptedLocation(): Boolean =
        preferences.getBoolean(PREF_PROMPTED_LOCATION, false)

    fun setPromptedLocation() {
        preferences.edit()
            .putBoolean(PREF_PROMPTED_LOCATION, true)
            .apply()
    }

    fun getPromptedBluetooth(): Boolean =
        preferences.getBoolean(PREF_PROMPTED_BLUETOOTH, false)

    fun saveTimestamps(currentElapsedTimeMS: Long, lastGoogleSyncTimestampMS: Long) {
        setCurrentElapsedTime(currentElapsedTimeMS)
        setLastGoogleTimestamp(lastGoogleSyncTimestampMS)
    }

    private fun setCurrentElapsedTime(elapsed: Long) {
        preferences.edit()
            .putLong(PREF_ELAPSED_TIME_AT_GOOGLE_TIMESTAMP, elapsed)
            .apply()
    }

    fun getCurrentElapsedTime(): Long =
        preferences.getLong(PREF_ELAPSED_TIME_AT_GOOGLE_TIMESTAMP, 0)

    private fun setLastGoogleTimestamp(ts: Long) {
        preferences.edit()
            .putLong(PREF_LAST_GOOGLE_TIMESTAMP, ts)
            .apply()
    }

    fun getLastGoogleTimestamp(): Long =
        preferences.getLong(PREF_LAST_GOOGLE_TIMESTAMP, 0)

}