package de.eloc.eloc_control_panel.driver

import de.eloc.eloc_control_panel.data.Channel
import de.eloc.eloc_control_panel.data.MicrophoneVolumePower
import de.eloc.eloc_control_panel.data.SampleRate

class Microphone {
    companion object {
        const val TYPE = KEY_MICROPHONE_TYPE
        const val VOLUME_POWER = KEY_MICROPHONE_VOLUME_POWER
        const val CHANNEL = KEY_MICROPHONE_CHANNEL
        const val SAMPLE_RATE = KEY_MICROPHONE_SAMPLE_RATE
        const val USE_APLL = KEY_MICROPHONE_APPLL
    }

    var sampleRate = SampleRate.Unknown
        internal set

    var type = ""
        internal set

    var volumePower = MicrophoneVolumePower()
        internal set

    var channel = Channel.Unknown
        internal set

    var useAPLL = false
        internal set
}