package de.eloc.eloc_control_panel.data

import de.eloc.eloc_control_panel.App
import de.eloc.eloc_control_panel.R

enum class SampleRate(val code: Int) {
    Rate8k(8000),
    Rate16k(16000),
    Rate22k(22050),
    Rate32k(32000),
    Rate44k(44100),
    Unknown(-1);

    override fun toString(): String {
        val resId = when (this) {
            Rate8k -> R.string._8k
            Rate16k -> R.string._16k
            Rate22k -> R.string._22k
            Rate32k -> R.string._32k
            Rate44k -> R.string._44k
            else -> R.string.invalid
        }
        return App.instance.getString(resId)
    }

    companion object {
        fun parse(code: Double): SampleRate =
            when (code.toInt()) {
                Rate8k.code -> Rate8k
                Rate16k.code -> Rate16k
                Rate22k.code -> Rate22k
                Rate32k.code -> Rate32k
                Rate44k.code -> Rate44k
                else -> Unknown
            }

    }
}