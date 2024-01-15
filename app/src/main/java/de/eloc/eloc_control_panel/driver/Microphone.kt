package de.eloc.eloc_control_panel.driver

import de.eloc.eloc_control_panel.data.Channel
import de.eloc.eloc_control_panel.data.GainType
import de.eloc.eloc_control_panel.data.SampleRate

class Microphone {
    companion object {
        const val TYPE = KEY_MICROPHONE_TYPE
        const val GAIN = KEY_MICROPHONE_GAIN
        const val CHANNEL = KEY_MICROPHONE_CHANNEL
        const val SAMPLE_RATE = KEY_MICROPHONE_SAMPLE_RATE
        const val USE_APLL = KEY_MICROPHONE_APPLL
        const val USE_TIMING_FIX = KEY_MICROPHONE_TIMING_FIX
    }

    var sampleRate = SampleRate.Unknown
        internal set

    var type = ""
        internal set

    var gain = GainType.Low
        internal set

    var channel = Channel.Unknown
        internal set

    var useAPLL = false
        internal set

    var useTimingFix = false
        internal set
}