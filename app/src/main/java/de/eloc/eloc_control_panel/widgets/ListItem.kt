package de.eloc.eloc_control_panel.widgets

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.LayoutListItemBinding

private const val TYPE_NORMAL = 0
private const val TYPE_BUTTON = 1
private const val TYPE_TOGGLE = 2

class ListItem : LinearLayout {
    private lateinit var binding: LayoutListItemBinding

    private var keyText = ""
        set(value) {
            field = value
            binding.labelTextView.text = field
        }

    var valueText = ""
        set(value) {
            field = value
            binding.valueTextView.text = field
        }

    val isChecked get() = binding.toggle.isChecked

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
            keyText = typedArray.getString(R.styleable.ListItem_keyText) ?: "-"
            valueText = typedArray.getString(R.styleable.ListItem_valueText) ?: "-"

            var indentDp = typedArray.getDimension(R.styleable.ListItem_indent, 0f)
            if (indentDp <= 0) {
                indentDp = 0f
            }
            val params = binding.indent.layoutParams
            params.width = indentDp.toInt()
            binding.indent.layoutParams = params

            when (typedArray.getInteger(R.styleable.ListItem_itemType, TYPE_NORMAL)) {
                TYPE_BUTTON -> {
                    binding.valueTextView.visibility = View.VISIBLE
                    binding.toggle.visibility = View.GONE
                    binding.icon.visibility = View.VISIBLE
                    val typedValue = TypedValue()
                    context.theme.resolveAttribute(
                        android.R.attr.selectableItemBackground,
                        typedValue,
                        true
                    )
                    binding.root.isFocusable = true
                    binding.root.isClickable = true
                    binding.root.setBackgroundResource(typedValue.resourceId)
                }

                TYPE_TOGGLE -> {
                    binding.icon.visibility = View.GONE
                    binding.toggle.visibility = View.VISIBLE
                    binding.valueTextView.visibility = View.GONE
                    binding.root.isFocusable = false
                    binding.root.isClickable = false
                    binding.root.background = null
                }

                else -> {
                    binding.valueTextView.visibility = View.VISIBLE
                    binding.icon.visibility = View.GONE
                    binding.toggle.visibility = View.GONE
                    binding.root.isFocusable = false
                    binding.root.isClickable = false
                    binding.root.background = null
                }
            }
            typedArray.recycle()
        }
    }

    fun setSwitch(on: Boolean) {
        binding.toggle.isChecked = on
    }

    fun setSwitchClickedListener(listener: OnClickListener) {
        binding.toggle.setOnClickListener(listener)
    }
}