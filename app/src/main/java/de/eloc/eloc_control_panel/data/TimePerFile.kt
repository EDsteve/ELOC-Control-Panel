package de.eloc.eloc_control_panel.data

import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.R

enum class TimePerFile(val seconds: Int) {
    Time10s(10),
    Time1m(60),
    Time1h(3600),
    Time4h(3600 * 4),
    Time12h(3600 * 12),
    Unknown(-1);

    override fun toString(): String {
        val resId = when (this) {
            Time10s -> R.string._10s
            Time1m -> R.string._1m
            Time1h -> R.string._1h
            Time4h -> R.string._4h
            Time12h -> R.string._12h
            else -> R.string.unknown
        }
        return App.instance.getString(resId)
    }

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