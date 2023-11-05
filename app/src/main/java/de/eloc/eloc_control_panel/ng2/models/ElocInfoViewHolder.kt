package de.eloc.eloc_control_panel.ng2.models


import android.view.View

import androidx.recyclerview.widget.RecyclerView

import de.eloc.eloc_control_panel.databinding.LayoutElocInfoBinding
import de.eloc.eloc_control_panel.ng3.interfaces.AdapterItemCallback

class ElocInfoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val binding: LayoutElocInfoBinding = LayoutElocInfoBinding.bind(itemView)

    fun setInfo(info: ElocInfo, callback: AdapterItemCallback) {
        binding.nameTextView.text = info.name
        binding.addressTextView.text = info.address
        binding.root.setOnClickListener { callback.handler(info.name, info.address) }
    }
}
