package de.eloc.eloc_control_panel.data.viewholders

import android.view.View

import androidx.recyclerview.widget.RecyclerView
import de.eloc.eloc_control_panel.data.BtDevice

import de.eloc.eloc_control_panel.databinding.LayoutElocInfoBinding
import de.eloc.eloc_control_panel.interfaces.StringCallback

class BtDeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val binding: LayoutElocInfoBinding = LayoutElocInfoBinding.bind(itemView)

    fun setInfo(device: BtDevice, checkable: Boolean, callback: StringCallback?) {
        binding.nameTextView.text = device.name
        binding.addressTextView.text = device.address
        binding.checkbox.visibility = if (checkable) View.VISIBLE else View.GONE
        binding.indent.visibility = if (checkable) View.GONE else View.VISIBLE
        binding.root.setOnClickListener { callback?.handler(device.address) }
        binding.checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
            device.selected = isChecked
        }

        if (checkable) {
            binding.nameTextView.isClickable = true
            binding.nameTextView.isFocusable = true
            binding.addressTextView.isFocusable = true
            binding.addressTextView.isClickable = true
            binding.nameTextView.setOnClickListener { binding.checkbox.performClick() }
            binding.addressTextView.setOnClickListener { binding.checkbox.performClick() }
        }
    }
}
