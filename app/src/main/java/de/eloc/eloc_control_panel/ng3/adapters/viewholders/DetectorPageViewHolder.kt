package de.eloc.eloc_control_panel.ng3.adapters.viewholders

import de.eloc.eloc_control_panel.databinding.LayoutDetectorBinding

class DetectorPageViewHolder(binding: LayoutDetectorBinding): PageViewHolder(binding.root) {
    private val pageBinding: LayoutDetectorBinding

    init {
        pageBinding = binding
    }

    fun setText(text: String) {
        pageBinding.textView.text = text
    }
}