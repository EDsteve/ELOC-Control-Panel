package de.eloc.eloc_control_panel.driver

import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.activities.DAY_SECONDS

/**
 * Represents LoRa signal strength description based on RSSI values.
 * LoRa RSSI typically ranges from -40 dBm (very strong) to -150 dBm (very weak).
 */
enum class LoraSignalStrength(val minRssi: Double, val maxRssi: Double) {
    Excellent(-90.0, 0.0),      // > -90 dBm
    Good(-110.0, -90.0),        // -90 to -110 dBm
    Fair(-120.0, -110.0),       // -110 to -120 dBm
    Poor(-130.0, -120.0),       // -120 to -130 dBm
    VeryPoor(-200.0, -130.0);   // < -130 dBm

    companion object {
        fun fromRssi(rssi: Double): LoraSignalStrength {
            return when {
                rssi > -90 -> Excellent
                rssi > -110 -> Good
                rssi > -120 -> Fair
                rssi > -130 -> Poor
                else -> VeryPoor
            }
        }
    }

    val iconResource: Int
        get() = when (this) {
            Excellent -> R.drawable.rssi_5
            Good -> R.drawable.rssi_4
            Fair -> R.drawable.rssi_3
            Poor -> R.drawable.rssi_2
            VeryPoor -> R.drawable.rssi_1
        }
}

class LoraWan {
    companion object {

        internal const val ENABLED = KEY_LORAWAN_ENABLE
        internal const val UPLINK_INTERVAL = KEY_LORAWAN_UPLINK_INTERVAL
        internal const val REGION = KEY_LORAWAN_REGION
        internal const val MIN_INTERVAL_SECS = 60 // 1 minute
        internal const val MAX_INTERVAL_SECS = DAY_SECONDS * 3 // 3 days
        internal const val REGION_MAX_LEN = 50
    }

    // Config properties (from getConfig)
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

    // Status properties (from getStatus)
    var joined = false
    var hasSignalInfo = false
    var rssi: Double = 0.0
    var snr: Double = 0.0

    val signalStrength: LoraSignalStrength
        get() = LoraSignalStrength.fromRssi(rssi)

    /**
     * Returns true if LoRa is enabled, joined to network, and has valid signal info
     */
    val hasValidSignal: Boolean
        get() = enabled && joined && hasSignalInfo
}
