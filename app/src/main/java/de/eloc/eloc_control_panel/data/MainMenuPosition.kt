package de.eloc.eloc_control_panel.data

enum class MainMenuPosition(val code: Int) {
    TopLeft(0), TopRight(1);

    override fun toString(): String = when (this) {
        TopLeft -> "Top left"
        TopRight -> "Top right"
    }

    companion object {
        fun parse(code: Int): MainMenuPosition =
            if (code == TopLeft.code) {
                TopLeft
            } else {
                TopRight
            }
    }
}