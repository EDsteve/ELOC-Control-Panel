package de.eloc.eloc_control_panel.data

import kotlin.math.abs

class MicrophoneVolumePower {
    companion object {
        const val MAXIMUM = 0f
        const val MINIMUM = -8f

        fun parse(input: String?): MicrophoneVolumePower? {
            val inputValue = input?.toFloatOrNull()
            if (inputValue != null) {
                if ((inputValue <= MAXIMUM) && (inputValue >= MINIMUM)) {
                    return MicrophoneVolumePower().apply { rawValue = inputValue }
                }
            }
            return null
        }
    }

    constructor(initialValue: Float) {
        rawValue = initialValue
    }

    constructor() : this(MINIMUM)

    var rawValue: Float = MINIMUM
        set(value) {
            if ((value <= MAXIMUM) && (value >= MINIMUM)) {
                field = value
            }
        }


    val percentage: String
        get() {
            val range = abs(MAXIMUM - MINIMUM)
            val power = abs(MINIMUM - rawValue)
            val floatPerc = 100.0f * power / range
            return "%.1f %%".format(floatPerc)
        }
}
