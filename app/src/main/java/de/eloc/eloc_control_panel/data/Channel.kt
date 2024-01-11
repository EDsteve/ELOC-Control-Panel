package de.eloc.eloc_control_panel.data

enum class Channel(val value: String) {

    Left("Left"),
    Right("Right"),
    Stereo("Stereo"),
    Unknown("Unknown");

    companion object {
        fun parse(s: String?): Channel =
            when (s) {
                Left.value -> Left
                Right.value -> Right
                Stereo.value -> Stereo
                else -> Unknown
            }
    }
}
