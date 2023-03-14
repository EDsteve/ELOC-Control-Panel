package de.eloc.eloc_control_panel.ng2.models

import android.graphics.Color
import androidx.core.content.ContextCompat
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.ng2.App

enum class LabelColor(val color: Int) {
    on(ContextCompat.getColor(App.instance, R.color.on_color)),
    off(ContextCompat.getColor(App.instance, R.color.off_color)),
    middle(ContextCompat.getColor(App.instance, R.color.middle_color)),
    white(Color.WHITE)
}