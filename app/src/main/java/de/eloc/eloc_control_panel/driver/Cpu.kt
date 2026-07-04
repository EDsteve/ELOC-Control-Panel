package de.eloc.eloc_control_panel.driver

class Cpu {
    companion object {
        const val MAX_FREQUENCY = KEY_CPU_MAX_FREQUENCY_MHZ
        const val MIN_FREQUENCY = KEY_CPU_MIN_FREQUENCY_MHZ
        const val ENABLE_LIGHT_SLEEP = KEY_CPU_ENABLE_LIGHT_SLEEP

        // Selectable ESP32 CPU clocks (MHz). Max is PLL-derived (80/160/240); min may also use the
        // crystal-derived low frequencies for low-power idle. Must match the firmware's
        // isValidCpuMaxFrequency() / isValidCpuMinFrequency() in ElocConfig.cpp.
        val VALID_MAX_FREQUENCIES = listOf(240, 160, 80)
        val VALID_MIN_FREQUENCIES = listOf(240, 160, 80, 40, 20, 10)
    }

    var maxFrequencyMHz = 0
        internal set

    var minFrequencyMHz = 0
        internal set

    var enableLightSleep = false
        internal set
}