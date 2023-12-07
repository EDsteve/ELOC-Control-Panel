package de.eloc.eloc_control_panel.data

enum class DeviceState(val code: Int) {
    Recording(2),
    Detecting(-1), // todo: update when firmware implements detecting
    Ready(0);

    companion object {
        fun fromCode(code: Number): DeviceState =
            when (code.toInt()) {
                Recording.code -> Recording
                Detecting.code -> Detecting
                Ready.code -> Ready
                else -> Ready
            }
    }
}