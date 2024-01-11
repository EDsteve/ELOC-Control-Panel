package de.eloc.eloc_control_panel.data


import android.view.View

import androidx.recyclerview.widget.RecyclerView

import de.eloc.eloc_control_panel.databinding.LayoutElocInfoBinding
import de.eloc.eloc_control_panel.interfaces.StringCallback

class BtDeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val binding: LayoutElocInfoBinding = LayoutElocInfoBinding.bind(itemView)

    fun setInfo(device: BtDevice, callback: StringCallback) {
        binding.nameTextView.text = device.name
        binding.addressTextView.text = device.address
        binding.root.setOnClickListener { callback.handler(device.address) }
    }
}
