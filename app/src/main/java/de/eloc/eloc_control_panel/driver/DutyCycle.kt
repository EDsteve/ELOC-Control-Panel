package de.eloc.eloc_control_panel.driver

class DutyCycle {
    companion object {
        internal const val ENABLE = KEY_DUTY_CYCLE_ENABLE
        internal const val SLEEP_DURATION_S = KEY_DUTY_CYCLE_SLEEP_DURATION_S
        internal const val AWAKE_DURATION_S = KEY_DUTY_CYCLE_AWAKE_DURATION_S
        internal const val MIN_SLEEP_DURATION_S = 60
        internal const val MAX_SLEEP_DURATION_S = 900
        internal const val MIN_AWAKE_DURATION_S = 20
        internal const val MAX_AWAKE_DURATION_S = 120
    }

    var enabled = false
        internal set

    var sleepDurationS: Int = 300
        internal set(value) {
            field = if (value < MIN_SLEEP_DURATION_S) {
                MIN_SLEEP_DURATION_S
            } else if (value > MAX_SLEEP_DURATION_S) {
                MAX_SLEEP_DURATION_S
            } else {
                value
            }
        }

    var awakeDurationS: Int = 30
        internal set(value) {
            field = if (value < MIN_AWAKE_DURATION_S) {
                MIN_AWAKE_DURATION_S
            } else if (value > MAX_AWAKE_DURATION_S) {
                MAX_AWAKE_DURATION_S
            } else {
                value
            }
        }
}
