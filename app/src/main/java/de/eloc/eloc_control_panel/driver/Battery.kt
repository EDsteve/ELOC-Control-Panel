package de.eloc.eloc_control_panel.driver

class Battery {

    companion object {
        const val UPDATE_INTERVAL = KEY_BATTERY_UPDATE_INTERVAL_MS
        const val AVERAGE_SAMPLES = KEY_BATTERY_AVG_SAMPLES
        const val AVERAGE_INTERVAL = KEY_BATTERY_AVG_INTERVAL_MS
        const val NO_BATTERY_MODE = KEY_BATTERY_NO_BAT_MODE
    }

    var level = 0.0
        internal set

    var voltage = 0.0
        internal set

    var type = ""
        internal set

    var updateIntervalSecs = 0.0
        internal set

    var avgSamples = 0
        internal set

    var avgIntervalSecs = 0.0
        internal set

    var noBatteryMode = false
        internal set

}