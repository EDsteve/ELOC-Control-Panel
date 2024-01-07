package de.eloc.eloc_control_panel.data

import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.R

enum class GainType(val value: Int) {
    High(11), // Old forest (HIGH)
    Low(14), // Old mahout (LOW)
    Unknown(-1);

    override fun toString(): String = when (this) {
        High -> App.instance.getString(R.string.high)
        Low -> App.instance.getString(R.string.low)
        else -> App.instance.getString(R.string.unknown)
    }

    companion object {
        fun fromValue(s: String?): GainType =
            if (s != null) {
                fromValue(s.toInt())
            } else {
                Unknown
            }

        fun fromValue(i: Int): GainType =
            when (i) {
                High.value -> High
                Low.value -> Low
                else -> Unknown
            }
    }
}
