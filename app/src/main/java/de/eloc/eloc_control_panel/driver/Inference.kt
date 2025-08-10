package de.eloc.eloc_control_panel.driver

class Inference {
    companion object {
        internal const val THRESHOLD = KEY_INFERENCE_THRESHOLD
        internal const val OBS_WINDOW_SECS = KEY_INFERENCE_OBS_WINDOW_SECS
        internal const val REQ_DETECTIONS = KEY_INFERENCE_REQ_DETECTIONS
        internal const val MIN_THRESHOLD = 45
        internal const val MAX_THRESHOLD = 100
        internal const val MIN_OBS_WINDOW_SECS = 1
        internal const val MAX_OBS_WINDOW_SECS = 600
        internal const val MIN_DETECTIONS = 1
        internal const val MAX_DETECTIONS = 100
    }

    var threshold: Int = 85
        set(value) {
            field = if (value < MIN_THRESHOLD) {
                MIN_THRESHOLD
            } else if (value > MAX_THRESHOLD) {
                MAX_THRESHOLD
            } else {
                value
            }
        }

    var observationWindow: Int = 10
        set(value) {
            field = if (value < MIN_OBS_WINDOW_SECS) {
                MIN_OBS_WINDOW_SECS
            } else if (value > MAX_OBS_WINDOW_SECS) {
                MAX_OBS_WINDOW_SECS
            } else {
                value
            }
        }

    var requiredDetections = 5
        set(value) {
            field = if (value < MIN_DETECTIONS) {
                MIN_DETECTIONS
            } else if (value > MAX_DETECTIONS) {
                MAX_DETECTIONS
            } else {
                value
            }
        }
}