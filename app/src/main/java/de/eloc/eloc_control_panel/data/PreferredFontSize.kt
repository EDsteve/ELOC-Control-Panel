package de.eloc.eloc_control_panel.data

enum class PreferredFontSize(val code: Int) {
    // Negative value --> small
    Small(-1),

    // Zero --> medium
    Medium(0),

    // Positive value --> large
    Large(1);

    companion object {
        fun parse(code: Int) = when (code) {
            Small.code -> Small
            Large.code -> Large
            else -> Medium
        }
    }
}