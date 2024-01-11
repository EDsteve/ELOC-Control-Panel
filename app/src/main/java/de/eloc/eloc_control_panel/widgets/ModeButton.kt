package de.eloc.eloc_control_panel.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.LayoutModeButtonBinding

class ModeButton : FrameLayout {
    private lateinit var binding: LayoutModeButtonBinding
    private var listener: OnClickListener? = null

    var isBusy = true
        set(value) {
            field = value
            if (field) {
                binding.progressLayout.visibility = View.VISIBLE
                binding.contentLayout.visibility = View.GONE
            } else {
                binding.progressLayout.visibility = View.GONE
                binding.contentLayout.visibility = View.VISIBLE
            }
        }

    var text = ""
        set(value) {
            field = value
            binding.textView.text = field
        }

    @StringRes
    var busyText = R.string.getting_mode
        set(value) {
            field = value
            binding.busyTextView.setText(field)
        }

    constructor(context: Context) : super(context) {
        initialize()
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

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            listener?.onClick(this)
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun initialize(attrs: AttributeSet? = null) {
        val view = inflate(context, R.layout.layout_mode_button, this)
        binding = LayoutModeButtonBinding.bind(view)
        if (attrs != null) {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ModeButton)
            isBusy = typedArray.getBoolean(R.styleable.ModeButton_isBusy, true)
            typedArray.recycle()
        }
    }

    fun addClickListener(onClickListener: OnClickListener) {
        listener = onClickListener
    }

    fun setButtonColor(@ColorRes colorResource: Int) {
        binding.root.setBackgroundResource(colorResource)
    }
}