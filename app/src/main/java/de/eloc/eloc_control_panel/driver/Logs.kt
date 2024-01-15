package de.eloc.eloc_control_panel.driver

class Logs {
    companion object {
        const val LOG_TO_SD_CARD = KEY_LOGS_LOG_TO_SD_CARD
        const val FILENAME = KEY_LOGS_FILENAME
        const val MAX_FILES = KEY_LOGS_MAX_FILES
        const val MAX_FILE_SIZE = KEY_LOGS_MAX_FILE_SIZE
    }

    var logToSdCard = true
        internal set

    var filename = ""
        internal set

    var maxFiles = 0
        internal set

    var maxFileSize = 0
        internal set
}