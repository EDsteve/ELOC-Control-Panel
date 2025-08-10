package de.eloc.eloc_control_panel.driver

import de.eloc.eloc_control_panel.activities.DAY_SECONDS

class LoraWan {
    companion object {

        internal const val ENABLED = KEY_LORAWAN_ENABLE
        internal const val UPLINK_INTERVAL = KEY_LORAWAN_UPLINK_INTERVAL
        internal const val REGION = KEY_LORAWAN_REGION
        internal const val MIN_INTERVAL_SECS = 60 // 1 minute
        internal const val MAX_INTERVAL_SECS = DAY_SECONDS * 3 // 3 days
        internal const val REGION_MAX_LEN = 50
    }

    var region: String = ""
        set(value) {
            var sanitizedName = value.trim()
            if (sanitizedName.length > REGION_MAX_LEN) {
                sanitizedName = sanitizedName.substring(0, REGION_MAX_LEN - 1)
            }
            field = sanitizedName
        }

    var uplinkIntervalSeconds: Int = 60
        set(value) {
            val sanitizedValue = if (value < MIN_INTERVAL_SECS) {
                MIN_INTERVAL_SECS
            } else if (value > MAX_INTERVAL_SECS) {
                MAX_INTERVAL_SECS
            } else {
                value
            }
            field = sanitizedValue
        }

    var enabled = false
}