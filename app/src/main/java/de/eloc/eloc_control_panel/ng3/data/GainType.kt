package de.eloc.eloc_control_panel.ng3.data

enum class GainType(val value: Int) {
    High(11), // Old forest (HIGH)
    Low(14); // Old mahout (LOW)

    companion object {
        fun fromValue(s: String?) = fromValue(s?.toInt())

        fun fromValue(i: Int?): GainType {
            return if (i == 14) {
                Low
            } else {
                High
            }
        }
    }
}