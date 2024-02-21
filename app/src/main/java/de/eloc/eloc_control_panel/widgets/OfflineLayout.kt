package de.eloc.eloc_control_panel.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.LayoutOfflineBinding

class OfflineLayout : LinearLayout {
    private lateinit var binding: LayoutOfflineBinding

    constructor(context: Context) : super(context) {
        initialize(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initialize(context)
    }

    private fun initialize(context: Context) {
        val view = inflate(context, R.layout.layout_offline, this)
        binding = LayoutOfflineBinding.bind(view)
    }
}