package de.eloc.eloc_control_panel.driver

/**
 * LoRa coverage survey ("signal strength mapper") settings — firmware >= 1.72, `surveyCfg`.
 *
 * The survey walks or drives a route and maps where the gateway can hear the device. Sampling is
 * triggered by DISTANCE with a time floor rather than by a fixed interval, because a fixed interval
 * gives wildly different spacing on foot (~1.2 m/s) and from a vehicle (~5.6 m/s) — and because a
 * device standing still would otherwise keep transmitting the same coordinate until the day's
 * airtime allowance was gone.
 *
 * The limits below mirror the firmware's own clamps in ElocConfig.cpp. They are deliberately wider
 * than the sensible working range: the real rate limit is the per-spreading-factor duty-cycle floor
 * the firmware applies at runtime, which silently raises the interval when the SF goes up.
 */
class Survey {
    companion object {
        internal const val ENABLE = KEY_SURVEY_ENABLE
        internal const val MIN_INTERVAL_S = KEY_SURVEY_MIN_INTERVAL_S
        internal const val MIN_DISTANCE_M = KEY_SURVEY_MIN_DISTANCE_M
        internal const val START_SF = KEY_SURVEY_START_SF

        // 5 s is below every legal duty-cycle floor, but sendReceive() blocks ~6-7 s through the
        // RX windows anyway, so nothing faster could be honoured even if it were allowed.
        internal const val MIN_INTERVAL_SECS = 5
        internal const val MAX_INTERVAL_SECS = 3600

        // 5 m is inside GPS noise; 5 km is a coarse drive-along survey.
        internal const val MIN_DISTANCE_METRES = 5
        internal const val MAX_DISTANCE_METRES = 5000

        internal const val MIN_SF = 7
        internal const val MAX_SF = 12

        /**
         * Presets for the two ways a survey actually happens. Both land at roughly three hours of
         * surveying inside TTN's daily uplink allowance, at a spacing that suits how fast you move.
         */
        internal const val PRESET_WALK_DISTANCE_M = 25
        internal const val PRESET_DRIVE_DISTANCE_M = 100

        /**
         * Spreading factors offered by the picker. SF7 is the default: it has the tightest
         * coverage, so mapping it maps the worst case, and its margin converts to any higher SF by
         * a fixed offset (+5 dB at SF9, +12.5 dB at SF12).
         */
        internal val SPREADING_FACTORS = listOf(7, 9, 10, 12)
    }

    var enabled = false
        internal set

    var minIntervalS: Int = 10
        internal set(value) {
            field = value.coerceIn(MIN_INTERVAL_SECS, MAX_INTERVAL_SECS)
        }

    var minDistanceM: Int = 25
        internal set(value) {
            field = value.coerceIn(MIN_DISTANCE_METRES, MAX_DISTANCE_METRES)
        }

    var startSF: Int = 7
        internal set(value) {
            field = value.coerceIn(MIN_SF, MAX_SF)
        }

    /**
     * Firmware older than 1.72 has no `surveyCfg` block at all, so every field parses as 0. Set
     * explicitly on every getConfig rather than inferred from a field value, which would go stale
     * when the app connects to an older device after a newer one. Used to grey the section out
     * rather than offering settings that would be silently ignored.
     */
    var isSupported = false
        internal set
}
