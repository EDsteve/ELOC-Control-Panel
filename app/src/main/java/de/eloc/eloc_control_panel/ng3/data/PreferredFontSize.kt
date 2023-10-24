package de.eloc.eloc_control_panel.ng3.data

enum class PreferredFontSize(val code: Int) {
    Small(-1),
    Medium(0),
    Large(1);

    companion object {
        fun parse(code: Int) = if (code < 0) {
            Small
        } else if (code > 0) {
            Large
        } else {
            Medium
        }
    }
}