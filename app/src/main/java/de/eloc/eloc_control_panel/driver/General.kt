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

    var locationAccuracy = 0
        internal set

    var version = ""
        internal set

    var uptimeHours = 0.0
        internal set

    // Device wall-clock time as a preformatted local-time string reported by the firmware (getStatus).
    var deviceTime = ""
        internal set

    // Firmware-update capability advertisement (getStatus "device" section).
    // fwUpdateProto == 0 means the connected firmware has no BT update support;
    // the update UI is hidden in that case.
    var fwUpdateProto = 0
        internal set

    // "ei" (Edge Impulse) or "no-ai" — used to pick/refuse the matching release binary.
    var buildVariant = ""
        internal set

    /**
     * Reset all cached config/status fields to defaults.
     *
     * Called on disconnect so that none of these values survive into the next
     * connection. `nodeName` in particular feeds [DeviceDriver.name] (the
     * device_name written to Firestore status uploads); if it lingered, a status
     * read from the next device could be saved with the PREVIOUS device's name
     * even though the MAC (taken from the live socket) is already correct —
     * producing the device_name/MAC desync seen in the status_uploads collection.
     */
    fun reset() {
        nodeName = ""
        fileHeader = ""
        timePerFile = TimePerFile.Unknown
        recHoursSinceBoot = 0.0
        lastLocation = ""
        locationAccuracy = 0
        version = ""
        uptimeHours = 0.0
        deviceTime = ""
        fwUpdateProto = 0
        buildVariant = ""
    }
}