package de.eloc.eloc_control_panel.data.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.data.AssociatedDeviceInfo
import de.eloc.eloc_control_panel.data.BtDevice
import de.eloc.eloc_control_panel.data.RssiDescription
import de.eloc.eloc_control_panel.data.RssiLabel
import de.eloc.eloc_control_panel.data.helpers.DataManager
import de.eloc.eloc_control_panel.data.util.Preferences
import de.eloc.eloc_control_panel.databinding.LayoutElocInfoBinding

class BtDeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val binding: LayoutElocInfoBinding = LayoutElocInfoBinding.bind(itemView)

    private fun setName(info: AssociatedDeviceInfo?) {
        if (info != null) {
            itemView.post {
                if (info.name.isNotEmpty() && (info.name != BtDevice.DEFAULT_NAME)) {
                    binding.nameTextView.text = info.name
                }
            }
        }
    }

    fun setInfo(device: BtDevice, checkable: Boolean, callback: ((BtDevice) -> Unit)?) {
        val deviceName = device.name
        binding.nameTextView.text = deviceName
        if (deviceName == BtDevice.DEFAULT_NAME) {
            DataManager.findAssociation(device.address, ::setName)
        }

        binding.addressTextView.text = device.address
        binding.checkbox.visibility = if (checkable) View.VISIBLE else View.GONE
        binding.indent.visibility = if (checkable) View.GONE else View.VISIBLE
        binding.root.setOnClickListener {
            if (callback != null) {
                callback(device)
            }
        }

        val icon = when (device.rssi?.description) {
            RssiDescription.Strong -> R.drawable.rssi_5
            RssiDescription.Good -> R.drawable.rssi_4
            RssiDescription.Fair -> R.drawable.rssi_3
            RssiDescription.Weak -> R.drawable.rssi_2
            RssiDescription.Poor -> R.drawable.rssi_1
            else -> R.drawable.rssi_0
        }
        binding.rssiIcon.setImageResource(icon)
        val description = device.rssi?.description?.text ?: ""
        val power = device.rssi?.dBm?.toString() ?: ""
        val text = when (Preferences.rssiLabel) {
            RssiLabel.None -> ""
            RssiLabel.DescriptionOnly -> description
            RssiLabel.PowerOnly -> "$power dBm"
            RssiLabel.DescriptionAndPower -> {
                var str = ""
                if (description.trim().isNotEmpty()) {
                    str = description
                }
                if (power.trim().isNotEmpty()) {
                    str = "$str ($power dBm)"
                }
                str
            }
        }
        binding.rssiLabel.text = text

        binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
            device.selected = isChecked
        }

        if (checkable) {
            binding.rssiIcon.visibility = View.GONE
            binding.rssiLabel.visibility = View.GONE
            binding.nameTextView.isClickable = true
            binding.nameTextView.isFocusable = true
            binding.addressTextView.isFocusable = true
            binding.addressTextView.isClickable = true
            binding.nameTextView.setOnClickListener { binding.checkbox.performClick() }
            binding.addressTextView.setOnClickListener { binding.checkbox.performClick() }
        }
    }
}
