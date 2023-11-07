package de.eloc.eloc_control_panel.ng3.data

import android.graphics.Color
import androidx.core.content.ContextCompat
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.ng3.App

enum class LabelColor(val color: Int) {
    On(ContextCompat.getColor(App.instance, R.color.on_color)),
    Off(ContextCompat.getColor(App.instance, R.color.off_color)),
    Middle(ContextCompat.getColor(App.instance, R.color.middle_color)),
    White(Color.WHITE)
}