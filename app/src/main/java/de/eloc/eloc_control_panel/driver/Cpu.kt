package de.eloc.eloc_control_panel.driver

class Cpu {
    companion object {
        const val MAX_FREQUENCY = KEY_CPU_MAX_FREQUENCY_MHZ
        const val MIN_FREQUENCY = KEY_CPU_MIN_FREQUENCY_MHZ
        const val ENABLE_LIGHT_SLEEP = KEY_CPU_ENABLE_LIGHT_SLEEP
    }

    var maxFrequencyMHz = 0
        internal set

    var minFrequencyMHz = 0
        internal set

    var enableLightSleep = false
        internal set
}