package de.eloc.eloc_control_panel.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.LayoutProgressIndicatorBinding

class ProgressIndicator : LinearLayout {
    private lateinit var binding: LayoutProgressIndicatorBinding

    var text = ""
        set(value) {
            field = value
            binding.textView.text = field
        }

    var infoMode = false
        set(value) {
            field = value
            binding.linearProgressIndicator.visibility = if (field) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }

    private var compact = false
        set(value) {
            field = value
            if (field) {
                binding.space.visibility = View.GONE
                binding.textView.visibility = View.GONE
            } else {
                binding.space.visibility = View.VISIBLE
                binding.textView.visibility = View.VISIBLE
            }
        }

    constructor(context: Context) : super(context) {
        initialize(context)
    }

    constructor(
        context: Context,
        attrs: AttributeSet
    ) : super(context, attrs) {
        initialize(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        initialize(context, attrs)
    }

    private fun initialize(context: Context, attrs: AttributeSet? = null) {
        val view = inflate(context, R.layout.layout_progress_indicator, this)
        binding = LayoutProgressIndicatorBinding.bind(view)
        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.ProgressIndicator)
            text = ta.getString(R.styleable.ProgressIndicator_text) ?: ""
            compact = ta.getBoolean(R.styleable.ProgressIndicator_compact, false)
            ta.recycle()
        }
    }
}