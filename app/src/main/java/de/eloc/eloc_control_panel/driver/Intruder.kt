package de.eloc.eloc_control_panel.driver

class Intruder {
    companion object {
        const val ENABLED = KEY_INTRUDER_ENABLED
        const val THRESHOLD = KEY_INTRUDER_THRESHOLD
        const val WINDOWS_MS = KEY_INTRUDER_WINDOWS_MS
        const val ALARM_INTERVAL_S = KEY_INTRUDER_ALARM_INTERVAL_S
        const val IDLE_INTERVAL_S = KEY_INTRUDER_IDLE_INTERVAL_S

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

    // Seconds between alarm uplinks once the device has stopped moving, when the firmware also
    // powers the GPS down and repeats the last known fix. 0 means the firmware does not report it
    // (older than 1.70) - which is also how the Alarm row knows whether to trust `moving`. A value
    // at or below alarmIntervalS disables the backoff.
    var idleIntervalS = 0
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

    // --- Alarm status (getStatus -> "intruder", firmware >= 1.69) ---------------------------

    // Effective arming. Knock detection is a 24/7-only feature, so this is false in duty-cycle
    // mode even when `enabled` is set.
    var armed = false
        internal set

    // True while the knock alarm is firing. The alarm latches: the device keeps sending LoRa
    // alarm uplinks with its GPS position until detection is switched off or it reboots.
    var alarmActive = false
        internal set

    // The siren stops 30 s after the trigger while the alarm itself carries on.
    var sirenActive = false
        internal set

    // Seconds since the alarm triggered (0 when no alarm is active).
    var alarmAgeS = 0
        internal set
}