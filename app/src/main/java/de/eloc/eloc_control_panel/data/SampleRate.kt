package de.eloc.eloc_control_panel.data

enum class SampleRate(val code: Int) {
    Rate8k(8000),
    Rate16k(16000),
    Rate22k(22050),
    Rate32k(32000),
    Rate44k(44100),
    Unknown(-1);

    companion object {
        fun parse(code: Int): SampleRate =
            when (code) {
                Rate8k.code -> Rate8k
                Rate16k.code -> Rate16k
                Rate22k.code -> Rate22k
                Rate32k.code -> Rate32k
                Rate44k.code -> Rate44k
                else -> Unknown
            }

    }
}