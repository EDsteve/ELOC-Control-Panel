package de.eloc.eloc_control_panel.ng3.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.LayoutListItemBinding

class ListItem : ConstraintLayout {
    private lateinit var binding: LayoutListItemBinding

    private var itemLabel = ""
        set(value) {
            field = value
            binding.labelTextView.text = field
        }

    private var showDivider = false
        set(value) {
            field = value
            binding.divider.visibility = if (field) View.VISIBLE else View.GONE
        }

    var itemValue = ""
        set(value) {
            field = value
            binding.valueTextView.text = field
        }

    constructor(context: Context) : super(context) {
        initialize(null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initialize(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defaultStyleAttr: Int) : super(
        context,
        attrs,
        defaultStyleAttr
    ) {
        initialize(attrs)
    }

    private fun initialize(attrs: AttributeSet?) {
        val view = inflate(context, R.layout.layout_list_item, this)
        binding = LayoutListItemBinding.bind(view)
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ListItem)
            itemLabel = typedArray.getString(R.styleable.ListItem_itemLabel) ?: "-"
            itemValue = typedArray.getString(R.styleable.ListItem_itemValue) ?: "-"
            showDivider = typedArray.getBoolean(R.styleable.ListItem_showDivider, false)
            typedArray.recycle()
        }
    }
}