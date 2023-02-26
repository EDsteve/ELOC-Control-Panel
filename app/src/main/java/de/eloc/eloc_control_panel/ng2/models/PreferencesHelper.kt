package de.eloc.eloc_control_panel.ng2.models

import de.eloc.eloc_control_panel.ng2.App
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class PreferencesHelper {
    private val preferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(App.instance)

    fun getBluetoothRequested(): Boolean = preferences.getBoolean(PREF_BLUETOOTH_REQUESTED, false)

    fun getLocationRequested(): Boolean = preferences.getBoolean(PREF_LOCATION_REQUESTED, false)

    fun getRangerName(): String = preferences.getString(PREF_RANGER_NAME, DEFAULT_RANGER_NAME)
        ?: DEFAULT_RANGER_NAME

    fun setBluetoothRequested() =
        preferences.edit().putBoolean(PREF_BLUETOOTH_REQUESTED, true).apply()

    fun setLocationRequested() =
        preferences.edit().putBoolean(PREF_LOCATION_REQUESTED, true).apply()

    fun setRangerName(name: String) =
        preferences.edit().putString(PREF_RANGER_NAME, name.trim()).apply()

    companion object {
        private var cInstance: PreferencesHelper? = null
        private const val PREF_LOCATION_REQUESTED = "location_requested"
        private const val PREF_BLUETOOTH_REQUESTED = "bluetooth_requested"
        private const val PREF_RANGER_NAME = "rangerName"
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
