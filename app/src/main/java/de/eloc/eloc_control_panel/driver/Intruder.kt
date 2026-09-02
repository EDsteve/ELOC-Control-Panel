package de.eloc.eloc_control_panel.driver

class Intruder {
    companion object {
        const val ENABLED = KEY_INTRUDER_ENABLED
        const val THRESHOLD = KEY_INTRUDER_THRESHOLD
        const val WINDOWS_MS = KEY_INTRUDER_WINDOWS_MS
        const val ALARM_INTERVAL_S = KEY_INTRUDER_ALARM_INTERVAL_S
        const val IDLE_INTERVAL_S = KEY_INTRUDER_IDLE_INTERVAL_S
        const val CONFIRM_WINDOW_S = KEY_INTRUDER_CONFIRM_WINDOW_S
        const val QUIET_S = KEY_INTRUDER_QUIET_S
        const val ALARM_TIMEOUT_H = KEY_INTRUDER_ALARM_TIMEOUT_H
        const val ARM_DELAY_S = KEY_INTRUDER_ARM_DELAY_S

        // Setup grace after recording starts. 0 arms immediately; the upper bound is a practical
        // one - beyond a day it stops being a setup grace and becomes a way to disable detection.
        internal const val MIN_ARM_DELAY_S = 0
        internal const val MAX_ARM_DELAY_S = 86400

        // Movement-confirmation window. Long enough to cover someone knocking then unclipping a
        // strap; short enough that a candidate does not sit open after a branch has rattled the
        // device. The firmware clamps to settleMs + 2 s at the bottom.
        internal const val MIN_CONFIRM_WINDOW_S = 5
        internal const val MAX_CONFIRM_WINDOW_S = 600

        // Stillness before a moving device counts as stopped, which is when the firmware powers
        // the GPS down and stops the tracking uplinks.
        internal const val MIN_QUIET_S = 10
        internal const val MAX_QUIET_S = 3600

        // Auto-clear for a latched alarm. 0 means never.
        internal const val MIN_ALARM_TIMEOUT_H = 0
        internal const val MAX_ALARM_TIMEOUT_H = 168   // a week

        // The firmware clamps anything below 60 s (C_MIN_INTRUDER_INTERVAL_S) to protect the LoRa
        // duty cycle, so the editor does not offer a value it would silently override. The upper
        // bound is a practical one: an alarm reporting less than hourly is no use for tracking.
        internal const val MIN_ALARM_INTERVAL_S = 60
        internal const val MAX_ALARM_INTERVAL_S = 3600

        // A parked device may report as rarely as once a day; it is a liveness signal at that
        // point, not a track.
        internal const val MAX_IDLE_INTERVAL_S = 86400
    }

    var enabled = false
        internal set

    var threshold = 0
        internal set

    var windowsMs = 0
        internal set

    // Seconds between the LoRa alarm uplinks the device sends while an alarm is active, each
    // carrying its current GPS position. Firmware default 600; values below MIN_ALARM_INTERVAL_S
    // are clamped by the device.
    var alarmIntervalS = 600
        internal set

    // DEPRECATED in firmware 1.73: a stopped device now transmits nothing on the alarm path at
    // all, so there is no idle cadence left to configure. Still parsed, because it is also how
    // reportsMotion tells firmware >= 1.70 from older builds.
    var idleIntervalS = 0
        internal set

    // How long a knock burst waits for real movement before it is written off. Firmware >= 1.73;
    // 0 on older builds, which is what reportsCandidateState keys on.
    var confirmWindowS = 0
        internal set

    // Stillness before a moving device counts as stopped.
    var quietS = 0
        internal set

    // Hours of stillness after which a latched alarm clears itself. 0 = never.
    var alarmTimeoutH = 0
        internal set

    // Grace period after a recording mode is started (and after boot) during which knocks are
    // ignored, so a ranger mounting the device cannot set it off in their hands. 0 = arm at once.
    var armDelayS = 0
        internal set

    // Seconds still to run on that grace period; 0 once armed. From getStatus, so it counts down.
    var armsInS = 0
        internal set

    // Whether the device was moving at the last status read. Only meaningful while alarmActive is
    // true and the firmware reports idleIntervalS.
    var moving = false
        internal set

    // Editing starts from a sane value even on firmware that reports nothing.
    val editableIdleIntervalS: Int
        get() = if (idleIntervalS >= MIN_ALARM_INTERVAL_S) idleIntervalS else 3600

    // True when the firmware is new enough to report the moving/parked state.
    val reportsMotion: Boolean get() = idleIntervalS > 0

    // True on firmware >= 1.73, which separates a knock candidate from a confirmed alarm. Used to
    // hide the new settings and the candidate row on older devices rather than showing values the
    // firmware would ignore.
    val reportsCandidateState: Boolean get() = confirmWindowS > 0

    // --- Alarm status (getStatus -> "intruder", firmware >= 1.69) ---------------------------

    // Effective arming. Knock detection is a 24/7-only feature, so this is false in duty-cycle
    // mode even when `enabled` is set.
    var armed = false
        internal set

    // True while a CONFIRMED alarm is up - knocks followed by real movement. It latches: a device
    // carried off and put down stays alarmed, so picking it up again resumes tracking at once.
    // Since 1.73 the uplinks only go out while it is actually moving.
    var alarmActive = false
        internal set

    // A knock burst waiting to see whether the device actually moves. Nothing transmits and the
    // GPS stays off in this state - it is surfaced only so a field tech can tell "it saw my
    // knocks" from "it ignored them", which knocking alone no longer reveals.
    var candidate = false
        internal set

    // The siren stops 30 s after the trigger while the alarm itself carries on.
    var sirenActive = false
        internal set

    // Seconds since the alarm triggered (0 when no alarm is active).
    var alarmAgeS = 0
        internal set
}