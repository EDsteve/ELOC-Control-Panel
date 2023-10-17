package de.eloc.eloc_control_panel.ng3.adapters.viewholders

import de.eloc.eloc_control_panel.databinding.LayoutRecorderBinding

class RecorderPageViewHolder(binding: LayoutRecorderBinding) : PageViewHolder(binding.root) {
    private val pageBinding: LayoutRecorderBinding

    init {
        pageBinding = binding
    }

    fun setText(text: String) {
        pageBinding.textView.text = text
    }
}