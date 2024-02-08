package de.eloc.eloc_control_panel.driver

import de.eloc.eloc_control_panel.data.TimePerFile

class General {
    companion object {
        const val NODE_NAME = KEY_GENERAL_NODE_NAME
        const val FILE_HEADER = KEY_GENERAL_FILE_HEADER
        const val SECONDS_PER_FILE = KEY_GENERAL_SECONDS_PER_FILE
    }

    var nodeName = ""
        internal set

    var fileHeader = ""
        internal set

    var timePerFile = TimePerFile.Unknown
        internal set

    var recHoursSinceBoot = 0.0
        internal set

    var lastLocation = ""
        internal set

    var version = ""
        internal set

    var uptimeHours = 0.0
        internal set
}