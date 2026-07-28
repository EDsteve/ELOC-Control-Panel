package de.eloc.eloc_control_panel.activities.themable.editors.eloc_settings

import android.content.Context
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.prettifyTime
import de.eloc.eloc_control_panel.driver.DeviceDriver
import de.eloc.eloc_control_panel.driver.DutyCycle
import de.eloc.eloc_control_panel.driver.Intruder
import de.eloc.eloc_control_panel.driver.LoraWan

/**
 * One definition per setting of how it is edited.
 *
 * The LoRa, Scheduler and Intruder settings are reachable from two screens: the cards on the
 * Status page (`DeviceActivity`) and the matching sections of Device Settings
 * (`DeviceSettingsActivity`). Each screen used to build its own editor intent, so the two could
 * silently drift apart — the LoRa region picker was added to Device Settings while the Status
 * card still opened a free-text editor for the same value. Both screens call these functions
 * instead, so each setting has exactly one editor with one set of options and limits, and a
 * change here reaches every entry point.
 *
 * The caller still owns *whether* the row is editable (recording state, section switch); this
 * only owns what opens once it is.
 */
object SettingEditors {

    fun openLoraRegion(context: Context) {
        // Known frequency plans as a picker, but still free-text editable so a region the
        // firmware supports and this app build does not can be entered by hand.
        OptionEditorActivity.open(
            context,
            LoraWan.REGION,
            context.getString(R.string.region),
            DeviceDriver.lorawan.region,
            LoraWan.REGIONS.map { "$it|$it" },
            allowCustom = true,
        )
    }

    fun openLoraUplinkInterval(context: Context) = openSecondsRange(
        context,
        LoraWan.UPLINK_INTERVAL,
        context.getString(R.string.uplink_interval),
        DeviceDriver.lorawan.uplinkIntervalSeconds,
        LoraWan.MIN_INTERVAL_SECS,
        LoraWan.MAX_INTERVAL_SECS,
    )

    fun openDutyCycleAwakeDuration(context: Context) = openSecondsRange(
        context,
        DutyCycle.AWAKE_DURATION_S,
        context.getString(R.string.duty_cycle_awake_duration),
        DeviceDriver.dutyCycle.awakeDurationS,
        DutyCycle.MIN_AWAKE_DURATION_S,
        DutyCycle.MAX_AWAKE_DURATION_S,
    )

    fun openDutyCycleSleepDuration(context: Context) = openSecondsRange(
        context,
        DutyCycle.SLEEP_DURATION_S,
        context.getString(R.string.duty_cycle_sleep_duration),
        DeviceDriver.dutyCycle.sleepDurationS,
        DutyCycle.MIN_SLEEP_DURATION_S,
        DutyCycle.MAX_SLEEP_DURATION_S,
    )

    fun openIntruderThreshold(context: Context) = TextEditorActivity.open(
        context,
        Intruder.THRESHOLD,
        context.getString(R.string.intruder_threshold),
        DeviceDriver.intruder.threshold.toString(),
        isNumeric = true,
    )

    fun openIntruderWindowsMs(context: Context) = TextEditorActivity.open(
        context,
        Intruder.WINDOWS_MS,
        context.getString(R.string.intruder_windows_ms),
        DeviceDriver.intruder.windowsMs.toString(),
        isNumeric = true,
    )

    // Durations are shown to the slider as raw seconds plus a human-readable form, e.g.
    // "21600 (06h 00m 00s)".
    private fun openSecondsRange(
        context: Context,
        property: String,
        settingName: String,
        currentSeconds: Int,
        minimumSeconds: Int,
        maximumSeconds: Int,
    ) = RangeEditorActivity.openRangeEditor(
        context,
        property,
        settingName,
        "$currentSeconds (${prettifyTime(currentSeconds)})",
        currentSeconds.toFloat(),
        minimumSeconds.toFloat(),
        maximumSeconds.toFloat(),
    )
}
