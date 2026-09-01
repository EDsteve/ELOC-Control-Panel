package de.eloc.eloc_control_panel.data.util

import de.eloc.eloc_control_panel.App
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.data.FirmwareRelease
import de.eloc.eloc_control_panel.data.GpsData
import de.eloc.eloc_control_panel.data.MainMenuPosition
import de.eloc.eloc_control_panel.data.PreferredFontSize
import de.eloc.eloc_control_panel.data.RssiLabel
import de.eloc.eloc_control_panel.data.StatusUploadInterval
import de.eloc.eloc_control_panel.data.helpers.LocationHelper
import de.eloc.eloc_control_panel.data.helpers.firebase.AuthHelper
import de.eloc.eloc_control_panel.driver.FirmwareVersion
import org.json.JSONObject

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
    private const val PREF_LAST_KNOWN_LOCATION = "last_known_location"

    private const val PREF_ACCOUNT_RANGER_NAME = "account_ranger_name"
    private const val PREF_ACCOUNT_PROFILE_PIC_URL = "account_pfp_url"
    private const val PREF_RSS_LABEL_TYPE = "rssi_label_type"

    // Firmware releases (Phase 3): channel opt-in, the one cached release, when
    // GitHub was last polled, and the per-device "don't offer this again" list.
    private const val PREF_FIRMWARE_BETA_CHANNEL = "firmware_beta_channel"
    private const val PREF_FIRMWARE_RELEASE = "firmware_cached_release"
    private const val PREF_FIRMWARE_LAST_CHECK = "firmware_last_check"
    private const val PREF_FIRMWARE_SKIPPED = "firmware_skipped_versions"
    const val CHANNEL_STABLE = "stable"
    const val CHANNEL_BETA = "beta"
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

    var betaFirmwareChannel: Boolean
        get() = preferences.getBoolean(PREF_FIRMWARE_BETA_CHANNEL, false)
        set(value) = preferences.edit().putBoolean(PREF_FIRMWARE_BETA_CHANNEL, value).apply()

    /** Which GitHub release the prefetch reads: the latest full release, or the newest prerelease. */
    val firmwareChannel: String
        get() = if (betaFirmwareChannel) CHANNEL_BETA else CHANNEL_STABLE

    /**
     * The single release the app carries. Downloaded and verified in town; the
     * card and the release-mode update screen read it offline in the field.
     */
    var cachedFirmwareRelease: FirmwareRelease?
        get() = FirmwareRelease.deserialize(
            preferences.getString(PREF_FIRMWARE_RELEASE, "") ?: ""
        )
        set(value) = preferences.edit()
            .putString(PREF_FIRMWARE_RELEASE, value?.serialize() ?: "")
            .apply()

    var lastFirmwareCheckMillis: Long
        get() = preferences.getLong(PREF_FIRMWARE_LAST_CHECK, 0L)
        set(value) = preferences.edit().putLong(PREF_FIRMWARE_LAST_CHECK, value).apply()

    /**
     * Version the tech dismissed for this device, keyed on **mac_address** (not
     * device_name, which is not unique). Without this, deliberately reverting a
     * device would make the card reappear on every connect telling them to undo it.
     */
    fun skippedFirmwareVersion(macAddress: String): String {
        val key = macAddress.trim()
        if (key.isEmpty()) {
            return ""
        }
        return readFirmwareSkips().optString(key, "")
    }

    fun skipFirmwareVersion(macAddress: String, version: String) {
        val key = macAddress.trim()
        if (key.isEmpty() || version.isBlank()) {
            return
        }
        writeFirmwareSkips(readFirmwareSkips().put(key, version))
    }

    /**
     * Drop the dismissals a newer release supersedes: once something newer than
     * what a tech skipped is published, that device is offered again.
     */
    fun pruneFirmwareSkips(newVersion: String) {
        val skips = readFirmwareSkips()
        val stale = skips.keys().asSequence().filter {
            FirmwareVersion.isNewer(newVersion, skips.optString(it, ""))
        }.toList()
        if (stale.isNotEmpty()) {
            stale.forEach { skips.remove(it) }
            writeFirmwareSkips(skips)
        }
    }

    private fun readFirmwareSkips(): JSONObject {
        return try {
            JSONObject(preferences.getString(PREF_FIRMWARE_SKIPPED, "{}") ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
    }

    private fun writeFirmwareSkips(skips: JSONObject) =
        preferences.edit().putString(PREF_FIRMWARE_SKIPPED, skips.toString()).apply()

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
