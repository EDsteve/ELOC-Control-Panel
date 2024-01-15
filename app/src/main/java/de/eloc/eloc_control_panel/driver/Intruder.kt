package de.eloc.eloc_control_panel.driver

class Intruder {
    companion object {
        const val ENABLED = KEY_INTRUDER_ENABLED
        const val THRESHOLD = KEY_INTRUDER_THRESHOLD
        const val WINDOWS_MS = KEY_INTRUDER_WINDOWS_MS
    }

    var enabled = false
        internal set

    var threshold = 0
        internal set

    var windowsMs = 0
        internal set
}