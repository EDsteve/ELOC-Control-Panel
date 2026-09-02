package de.eloc.eloc_control_panel.activities.themable.editors.eloc_settings

import android.content.Context
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.prettifyTime
import de.eloc.eloc_control_panel.driver.DeviceDriver
import de.eloc.eloc_control_panel.driver.DutyCycle
import de.eloc.eloc_control_panel.driver.Intruder
import de.eloc.eloc_control_panel.driver.LoraWan
import de.eloc.eloc_control_panel.driver.Survey

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

    fun openIntruderAlarmInterval(context: Context) = openSecondsRange(
        context,
        Intruder.ALARM_INTERVAL_S,
        context.getString(R.string.intruder_alarm_interval),
        DeviceDriver.intruder.alarmIntervalS,
        Intruder.MIN_ALARM_INTERVAL_S,
        Intruder.MAX_ALARM_INTERVAL_S,
    )

    fun openIntruderIdleInterval(context: Context) = openSecondsRange(
        context,
        Intruder.IDLE_INTERVAL_S,
        context.getString(R.string.intruder_idle_interval),
        DeviceDriver.intruder.editableIdleIntervalS,
        Intruder.MIN_ALARM_INTERVAL_S,
        Intruder.MAX_IDLE_INTERVAL_S,
    )

    fun openIntruderConfirmWindow(context: Context) = openSecondsRange(
        context,
        Intruder.CONFIRM_WINDOW_S,
        context.getString(R.string.intruder_confirm_window),
        DeviceDriver.intruder.confirmWindowS,
        Intruder.MIN_CONFIRM_WINDOW_S,
        Intruder.MAX_CONFIRM_WINDOW_S,
    )

    fun openIntruderQuiet(context: Context) = openSecondsRange(
        context,
        Intruder.QUIET_S,
        context.getString(R.string.intruder_quiet),
        DeviceDriver.intruder.quietS,
        Intruder.MIN_QUIET_S,
        Intruder.MAX_QUIET_S,
    )

    fun openIntruderAlarmTimeout(context: Context) = RangeEditorActivity.openRangeEditor(
        context,
        Intruder.ALARM_TIMEOUT_H,
        context.getString(R.string.intruder_alarm_timeout),
        // Hours, not seconds - and 0 has its own meaning, so it is spelled out rather than shown
        // as "0 h", which would read like a mistake.
        if (DeviceDriver.intruder.alarmTimeoutH == 0) {
            context.getString(R.string.intruder_alarm_timeout_never)
        } else {
            context.getString(R.string.intruder_alarm_timeout_value, DeviceDriver.intruder.alarmTimeoutH)
        },
        DeviceDriver.intruder.alarmTimeoutH.toFloat(),
        Intruder.MIN_ALARM_TIMEOUT_H.toFloat(),
        Intruder.MAX_ALARM_TIMEOUT_H.toFloat(),
    )

    fun openSurveyStartSf(context: Context) {
        // A picker, not free text: SF is a radio parameter with four sensible values, and a typo
        // would silently change how much airtime every sample costs (SF12 is 26x SF7).
        OptionEditorActivity.open(
            context,
            Survey.START_SF,
            context.getString(R.string.survey_start_sf),
            DeviceDriver.survey.startSF.toString(),
            Survey.SPREADING_FACTORS.map { "$it|SF$it" },
            allowCustom = false,
        )
    }

    fun openSurveyMinDistance(context: Context) = RangeEditorActivity.openRangeEditor(
        context,
        Survey.MIN_DISTANCE_M,
        context.getString(R.string.survey_min_distance),
        // Name the two presets in the value line: 25 m suits walking, 100 m a drive-along survey,
        // and both land near three hours of surveying inside the daily uplink allowance.
        context.getString(
            R.string.survey_min_distance_value,
            DeviceDriver.survey.minDistanceM,
            distanceHint(context, DeviceDriver.survey.minDistanceM),
        ),
        DeviceDriver.survey.minDistanceM.toFloat(),
        Survey.MIN_DISTANCE_METRES.toFloat(),
        Survey.MAX_DISTANCE_METRES.toFloat(),
    )

    fun openSurveyMinInterval(context: Context) = openSecondsRange(
        context,
        Survey.MIN_INTERVAL_S,
        context.getString(R.string.survey_min_interval),
        DeviceDriver.survey.minIntervalS,
        Survey.MIN_INTERVAL_SECS,
        Survey.MAX_INTERVAL_SECS,
    )

    private fun distanceHint(context: Context, metres: Int): String = when {
        metres <= Survey.PRESET_WALK_DISTANCE_M -> context.getString(R.string.survey_preset_walk)
        metres >= Survey.PRESET_DRIVE_DISTANCE_M -> context.getString(R.string.survey_preset_drive)
        else -> context.getString(R.string.survey_preset_mixed)
    }

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
