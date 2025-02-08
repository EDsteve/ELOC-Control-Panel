package de.eloc.eloc_control_panel.driver

import de.eloc.eloc_control_panel.data.RecordState

class Session {
    var id = ""
    var recordingDurationSeconds = 0.0
    var detecting = false
    var detectingDurationSeconds = 0.0
    var eventsDetected = 0
    var aiModel = ""
    var recordingState = RecordState.Invalid
}