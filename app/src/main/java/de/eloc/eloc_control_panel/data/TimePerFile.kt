package de.eloc.eloc_control_panel.data

enum class TimePerFile(val seconds: Int) {
    Time10s(10),
    Time1m(60),
    Time1h(3600),
    Time4h(3600 * 4),
    Time12h(3600 * 12),
    Unknown(-1);

    companion object {
        fun parse(seconds: Int): TimePerFile =
            when (seconds) {
                Time10s.seconds -> Time10s
                Time1m.seconds -> Time1m
                Time1h.seconds -> Time1h
                Time4h.seconds -> Time4h
                Time12h.seconds -> Time12h
                else -> Unknown
            }
    }
}