package de.eloc.eloc_control_panel.driver

class LoraWan {
    companion object {
        private const val DAY_SECONDS = 86400
        private const val HOUR_SECONDS = 3600
        internal const val ENABLED = KEY_LORAWAN_ENABLE
        internal const val UPLINK_INTERVAL = KEY_LORAWAN_UPLINK_INTERVAL
        internal const val REGION = KEY_LORAWAN_REGION
        internal const val MIN_INTERVAL_SECS = 60 // 1 minute
        internal const val MAX_INTERVAL_SECS = DAY_SECONDS * 3 // 3 days
        internal const val REGION_MAX_LEN = 50

        fun prettifyTime(seconds: Int): String {
            val s = if (seconds < MIN_INTERVAL_SECS) {
                MIN_INTERVAL_SECS
            } else if (seconds > MAX_INTERVAL_SECS) {
                MAX_INTERVAL_SECS
            } else {
                seconds
            }
            val doubleSeconds = s.toDouble()
            val days = (doubleSeconds / DAY_SECONDS).toInt()
            val hours = ((doubleSeconds % DAY_SECONDS) / HOUR_SECONDS).toInt()
            val mins = ((doubleSeconds % HOUR_SECONDS) / 60.0).toInt()
            val secs = (doubleSeconds % 60.0).toInt()
            val prettyDays = if (days > 0) "${days}d" else ""
            val prettyHours = if (prettyDays.isEmpty() && (hours <= 0)) {
                ""
            } else if (hours <= 9) {
                "0${hours}h"
            } else {
                "${hours}h"
            }
            val prettyMins = if (prettyHours.isEmpty() && (mins <= 0)) {
                ""
            } else if (mins <= 9) {
                "0${mins}m"
            } else {
                "${mins}m"
            }
            val prettySecs = if (secs <= 0) {
                "0${secs}s"
            } else {
                "${secs}s"
            }
            return "$prettyDays $prettyHours $prettyMins $prettySecs".trim()
        }
    }

    var region = ""
        private set

    var uplinkIntervalSeconds = 60
        private set

    var enabled = false
        internal set

    fun setRegion(name: String) {
        var sanitizedName = name.trim()
        if (sanitizedName.length > REGION_MAX_LEN) {
            sanitizedName = name.substring(0, REGION_MAX_LEN - 1)
        }
        region = sanitizedName
    }

    fun setUplinkInterval(seconds: Int) {
        val sanitizedValue = if (seconds < MIN_INTERVAL_SECS) {
            MIN_INTERVAL_SECS
        } else if (seconds > MAX_INTERVAL_SECS) {
            MAX_INTERVAL_SECS
        } else {
            seconds
        }
        uplinkIntervalSeconds = sanitizedValue
    }
}