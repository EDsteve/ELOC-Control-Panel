package de.eloc.eloc_control_panel.driver

class BtConfig {
    companion object {
        const val ENABLE_DURING_RECORD = KEY_BT_ENABLE_DURING_RECORD
        const val ENABLE_AT_START = KEY_BT_ENABLE_AT_START
        const val ENABLE_ON_TAPPING = KEY_BT_ENABLE_ON_TAPPING
        const val OFF_TIME_OUT_SECONDS = KEY_BT_OFF_TIMEOUT_SECONDS
    }

    var enableAtStart = false
        internal set

    var enableOnTapping = false
        internal set

    var enableDuringRecord = false
        internal set

    var offTimeoutSeconds = 0
        internal set
}